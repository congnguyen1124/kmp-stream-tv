#!/usr/bin/env python3
"""Mirror Android StreamTV artwork, fonts, and vector drawables into iOS assets."""

from __future__ import annotations

import json
import shutil
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ANDROID_RES = ROOT / "androidApp/src/main/res"
IOS_ROOT = ROOT / "iosApp/iosApp"
ASSETS = IOS_ROOT / "Assets.xcassets"
FONTS = IOS_ROOT / "Resources/Fonts"
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
AAPT_NS = "{http://schemas.android.com/aapt}"

RASTER_NAMES = (
    "bg_story_layout",
    "bg_topbar_blur",
    "ic_fire",
    "ic_notification_active",
    "ic_profile_placeholder",
    "ic_search_with_shadow",
    "img_logo_app",
    "img_topbar_home",
    *(f"number_{index}" for index in range(1, 11)),
)

FONT_NAMES = (
    "svn_gilroy_regular.otf",
    "svn_gilroy_medium.otf",
    "svn_gilroy_semi_bold.otf",
    "svn_gilroy_bold.otf",
)


def android_attr(element: ET.Element, name: str, default: str | None = None) -> str | None:
    return element.attrib.get(f"{ANDROID_NS}{name}", default)


def svg_color(value: str | None) -> tuple[str, str | None]:
    if not value or value == "#00000000":
        return "none", None
    if value.startswith("#") and len(value) == 9:
        alpha = int(value[1:3], 16) / 255
        return f"#{value[3:]}", f"{alpha:.3f}".rstrip("0").rstrip(".")
    return value, None


def add_paint(attributes: dict[str, str], key: str, value: str | None) -> None:
    color, opacity = svg_color(value)
    attributes[key] = color
    if opacity is not None:
        attributes[f"{key}-opacity"] = opacity


def svg_path(path: ET.Element, gradient_id: str | None = None) -> ET.Element:
    attributes: dict[str, str] = {"d": android_attr(path, "pathData", "") or ""}
    if gradient_id:
        attributes["fill"] = f"url(#{gradient_id})"
    else:
        add_paint(attributes, "fill", android_attr(path, "fillColor", "none"))
    stroke_color = android_attr(path, "strokeColor")
    if stroke_color:
        add_paint(attributes, "stroke", stroke_color)
    mapping = {
        "strokeWidth": "stroke-width",
        "strokeLineCap": "stroke-linecap",
        "strokeLineJoin": "stroke-linejoin",
        "fillType": "fill-rule",
    }
    for android_name, svg_name in mapping.items():
        value = android_attr(path, android_name)
        if value:
            attributes[svg_name] = "evenodd" if value == "evenOdd" else value
    return ET.Element("path", attributes)


def linear_gradient(path: ET.Element, gradient_id: str) -> ET.Element | None:
    attr = path.find(f"{AAPT_NS}attr")
    if attr is None:
        return None
    gradient = next(iter(attr), None)
    if gradient is None or gradient.tag.rsplit("}", 1)[-1] != "gradient":
        return None
    result = ET.Element(
        "linearGradient",
        {
            "id": gradient_id,
            "gradientUnits": "userSpaceOnUse",
            "x1": android_attr(gradient, "startX", "0") or "0",
            "y1": android_attr(gradient, "startY", "0") or "0",
            "x2": android_attr(gradient, "endX", "0") or "0",
            "y2": android_attr(gradient, "endY", "0") or "0",
        },
    )
    for item in gradient:
        color, opacity = svg_color(android_attr(item, "color"))
        attributes = {"offset": android_attr(item, "offset", "0") or "0", "stop-color": color}
        if opacity is not None:
            attributes["stop-opacity"] = opacity
        ET.SubElement(result, "stop", attributes)
    return result


def convert_vector(source: Path, destination: Path) -> None:
    vector = ET.parse(source).getroot()
    viewport_width = android_attr(vector, "viewportWidth", "24") or "24"
    viewport_height = android_attr(vector, "viewportHeight", "24") or "24"
    svg = ET.Element(
        "svg",
        {
            "xmlns": "http://www.w3.org/2000/svg",
            "width": android_attr(vector, "width", f"{viewport_width}dp").replace("dp", ""),
            "height": android_attr(vector, "height", f"{viewport_height}dp").replace("dp", ""),
            "viewBox": f"0 0 {viewport_width} {viewport_height}",
        },
    )
    defs = ET.SubElement(svg, "defs")
    content = ET.SubElement(svg, "g")
    gradient_index = 0
    clip_index = 0

    def append_children(android_parent: ET.Element, svg_parent: ET.Element) -> None:
        nonlocal gradient_index, clip_index
        for child in android_parent:
            tag = child.tag.rsplit("}", 1)[-1]
            if tag == "path":
                gradient_id = f"gradient-{gradient_index}"
                gradient = linear_gradient(child, gradient_id)
                if gradient is not None:
                    defs.append(gradient)
                    gradient_index += 1
                    svg_parent.append(svg_path(child, gradient_id))
                else:
                    svg_parent.append(svg_path(child))
            elif tag == "group":
                group = ET.SubElement(svg_parent, "g")
                append_children(child, group)
            elif tag == "clip-path":
                clip_id = f"clip-{clip_index}"
                clip_index += 1
                clip = ET.SubElement(defs, "clipPath", {"id": clip_id})
                ET.SubElement(clip, "path", {"d": android_attr(child, "pathData", "") or ""})
                svg_parent.set("clip-path", f"url(#{clip_id})")

    append_children(vector, content)
    if len(defs) == 0:
        svg.remove(defs)
    ET.indent(svg, space="  ")
    destination.write_text(ET.tostring(svg, encoding="unicode", xml_declaration=True) + "\n")


def write_imageset(name: str, extension: str, preserves_vector: bool = False) -> Path:
    imageset = ASSETS / f"{name}.imageset"
    imageset.mkdir(parents=True, exist_ok=True)
    payload: dict[str, object] = {
        "images": [
            {"filename": f"{name}.{extension}", "idiom": "universal", "scale": "1x"},
            {"idiom": "universal", "scale": "2x"},
            {"idiom": "universal", "scale": "3x"},
        ],
        "info": {"author": "xcode", "version": 1},
    }
    if preserves_vector:
        payload["properties"] = {"preserves-vector-representation": True}
    (imageset / "Contents.json").write_text(json.dumps(payload, indent=2) + "\n")
    return imageset / f"{name}.{extension}"


def sync_vectors() -> None:
    drawable = ANDROID_RES / "drawable"
    for source in sorted(drawable.glob("*.xml")):
        if ET.parse(source).getroot().tag.rsplit("}", 1)[-1] != "vector":
            continue
        destination = write_imageset(source.stem, "svg", preserves_vector=True)
        convert_vector(source, destination)


def sync_rasters() -> None:
    source_root = ANDROID_RES / "drawable-xxxhdpi"
    for name in RASTER_NAMES:
        source = source_root / f"{name}.webp"
        destination = write_imageset(name, "png")
        subprocess.run(
            ["sips", "-s", "format", "png", str(source), "--out", str(destination)],
            check=True,
            stdout=subprocess.DEVNULL,
        )


def sync_fonts() -> None:
    FONTS.mkdir(parents=True, exist_ok=True)
    for name in FONT_NAMES:
        shutil.copy2(ANDROID_RES / "font" / name, FONTS / name)


if __name__ == "__main__":
    sync_vectors()
    sync_rasters()
    sync_fonts()
