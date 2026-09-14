#!/usr/bin/env python3
"""Draw the simulator's device around a raw `simctl` capture.

`xcrun simctl io … screenshot` and `… recordVideo` hand back the framebuffer and nothing else: a
bare 1170 × 2532 rectangle with square corners. Next to an Android screenshot that is fine, but it
reads as a picture of a *screen*, not a picture of a *phone*, and the README's iOS column is meant
to show the app running on a device.

The screen silhouette is not guessed here. Every `.simdevicetype` bundle ships a `framebufferMask`
PDF at exactly the framebuffer's pixel size whose opaque area is the visible screen — rounded
corners and, on a notched device, the notch cut out of the top edge. That mask is the authority for
where the glass ends; the body is derived from it by dilation, so the bezel has the same thickness
everywhere including around the corners, whatever corner curve Apple used.

Used by `capture_media.py`; runnable on its own to eyeball a frame:

    python3 tools/device_frame.py "iPhone 16e" /tmp/frame.png
"""

from __future__ import annotations

import math
import plistlib
import subprocess
from dataclasses import dataclass
from pathlib import Path
from tempfile import TemporaryDirectory

from PIL import Image, ImageDraw

DEVICE_TYPES = Path("/Library/Developer/CoreSimulator/Profiles/DeviceTypes")

# Thicknesses in framebuffer pixels. At the iPhone 16e's 460 dpi, 58 px is 3.2 mm of black glass
# border and 12 px is the sliver of the side band that faces the viewer head on — both close to the
# real hardware, which is what keeps the result from looking like a generic phone outline.
BEZEL = 58
BAND = 12

GLASS = (8, 8, 10, 255)
BAND_LIGHT = (168, 168, 174, 255)
BAND_DARK = (86, 86, 92, 255)
BUTTON = (122, 122, 128, 255)


@dataclass(frozen=True)
class DeviceFrame:
    """A transparent PNG of the device with a hole where the screen goes."""

    image: Image.Image
    screen_origin: tuple[int, int]
    screen_size: tuple[int, int]
    mask: Image.Image

    @property
    def size(self) -> tuple[int, int]:
        return self.image.size


def device_resources(device: str) -> Path:
    path = DEVICE_TYPES / f"{device}.simdevicetype" / "Contents" / "Resources"
    if not path.is_dir():
        raise SystemExit(f"no simdevicetype for {device!r} under {DEVICE_TYPES}")
    return path


def screen_mask(device: str) -> Image.Image:
    """The device's framebuffer mask, opaque exactly where the screen is visible."""
    resources = device_resources(device)
    profile = plistlib.loads((resources / "profile.plist").read_bytes())
    width = int(profile["mainScreenWidth"])
    pdf = resources / f"{profile['framebufferMask']}.pdf"
    if not pdf.is_file():
        raise SystemExit(f"{device}: no framebuffer mask at {pdf}")

    with TemporaryDirectory() as tmp:
        png = Path(tmp) / "mask.png"
        # `sips` ships with macOS, so the tool needs nothing beyond Pillow to run.
        subprocess.run(
            ["sips", "-s", "format", "png", "--resampleWidth", str(width), str(pdf), "--out", str(png)],
            check=True, capture_output=True,
        )
        return Image.open(png).convert("RGBA").copy()


def _row_extents(mask: Image.Image) -> list[tuple[int, int] | None]:
    """Leftmost and rightmost opaque pixel per row, with the notch filled in.

    The body sits *behind* the notch, so the shape being dilated has to be the glass outline
    without the bite taken out of it — which for a row-wise convex shape is just the span between
    the two extremes.
    """
    alpha = mask.getchannel("A")
    width, height = mask.size
    extents: list[tuple[int, int] | None] = []
    for y in range(height):
        row = alpha.crop((0, y, width, y + 1)).tobytes()
        left = next((x for x in range(width) if row[x] > 127), None)
        if left is None:
            extents.append(None)
            continue
        right = next(x for x in range(width - 1, -1, -1) if row[x] > 127)
        extents.append((left, right))
    return extents


def _dilate(extents: list[tuple[int, int] | None], radius: int, pad: int) -> list[tuple[int, int] | None]:
    """Minkowski sum of a row-wise convex shape with a disc of `radius`.

    Growing every row by the same number of pixels would square off the corners; the disc keeps the
    bezel a constant distance from the glass all the way round, which is what the eye checks.
    """
    reach = [int(math.sqrt(max(radius * radius - dy * dy, 0))) for dy in range(-radius, radius + 1)]
    height = len(extents)
    out: list[tuple[int, int] | None] = []
    for y in range(-pad, height + pad):
        left, right = None, None
        for index, dy in enumerate(range(-radius, radius + 1)):
            source = extents[y + dy] if 0 <= y + dy < height else None
            if source is None:
                continue
            grown_left = source[0] - reach[index]
            grown_right = source[1] + reach[index]
            left = grown_left if left is None else min(left, grown_left)
            right = grown_right if right is None else max(right, grown_right)
        out.append(None if left is None else (left, right))
    return out


def _fill(draw: ImageDraw.ImageDraw, extents, offset: tuple[int, int], colour) -> None:
    dx, dy = offset
    for y, extent in enumerate(extents):
        if extent is None:
            continue
        draw.rectangle([extent[0] + dx, y + dy, extent[1] + dx, y + dy], fill=colour)


def build(device: str, bezel: int = BEZEL, band: int = BAND) -> DeviceFrame:
    """Render the device body with a transparent screen cut-out."""
    mask = screen_mask(device)
    screen_w, screen_h = mask.size
    extents = _row_extents(mask)

    pad = bezel + band
    glass = _dilate(extents, bezel, pad)
    body = _dilate(extents, pad, pad)

    size = (screen_w + 2 * pad, screen_h + 2 * pad)
    frame = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(frame)

    # The band is a strip of anodised aluminium seen almost edge on: brightest where it turns away
    # from the viewer on the left, falling off to the right. A flat grey outline reads as a sticker.
    band_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    gradient = Image.new("RGBA", size, (0, 0, 0, 0))
    gradient_draw = ImageDraw.Draw(gradient)
    for x in range(size[0]):
        t = x / max(size[0] - 1, 1)
        shade = tuple(
            round(BAND_LIGHT[i] + (BAND_DARK[i] - BAND_LIGHT[i]) * (0.35 + 0.65 * t)) for i in range(3)
        )
        gradient_draw.rectangle([x, 0, x, size[1]], fill=(*shade, 255))
    _fill(ImageDraw.Draw(band_layer), body, (pad, pad), (255, 255, 255, 255))
    frame.paste(gradient, (0, 0), band_layer)

    _fill(draw, glass, (pad, pad), GLASS)

    _buttons(frame, body, pad, screen_h)

    # Punch the screen out last so nothing drawn above can creep over the glass.
    hole = Image.new("RGBA", size, (0, 0, 0, 0))
    hole.paste(mask, (pad, pad), mask)
    frame.paste((0, 0, 0, 0), (0, 0), hole)

    return DeviceFrame(image=frame, screen_origin=(pad, pad), screen_size=(screen_w, screen_h), mask=mask)


def _buttons(frame: Image.Image, body, pad: int, screen_h: int) -> None:
    """Volume pair and action button on the left, side button on the right.

    Placed as fractions of the screen height so the same numbers work on any phone-shaped device.
    """
    draw = ImageDraw.Draw(frame)
    thickness = max(round(screen_h * 0.0045), 6)

    def edge(fraction: float) -> tuple[int, int]:
        row = body[round(fraction * screen_h) + pad]
        return row if row else (0, frame.size[0] - 1)

    def button(top_fraction: float, bottom_fraction: float, side: str) -> None:
        top = round(top_fraction * screen_h) + pad
        bottom = round(bottom_fraction * screen_h) + pad
        mid = (top_fraction + bottom_fraction) / 2
        left, right = edge(mid)
        if side == "left":
            box = [left - thickness, top, left + thickness, bottom]
        else:
            box = [right - thickness, top, right + thickness, bottom]
        draw.rounded_rectangle(box, radius=thickness, fill=BUTTON)

    button(0.098, 0.132, "left")   # ring / action switch
    button(0.172, 0.238, "left")   # volume up
    button(0.252, 0.318, "left")   # volume down
    button(0.208, 0.318, "right")  # side button


def main() -> int:
    import sys

    device = sys.argv[1] if len(sys.argv) > 1 else "iPhone 16e"
    output = Path(sys.argv[2] if len(sys.argv) > 2 else "frame.png")
    frame = build(device)
    frame.image.save(output)
    print(f"{output}  {frame.size[0]}x{frame.size[1]}  screen at {frame.screen_origin}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
