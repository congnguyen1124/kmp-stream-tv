#!/usr/bin/env python3
"""Capture StreamTV KMP screenshots and interaction GIFs from a phone.

`README.md` pairs every screen with two columns, Android and iOS, because the whole point of this
project is that one shared `StateFlow` drives two native user interfaces. A README that showed only
one of them would be arguing the opposite.

Android is fully automated here. iOS is not, and cannot be: `xcrun simctl` can record a simulator
but cannot touch it, so the iOS column is recorded while a human performs the gestures. This tool
still owns the recording half of that — `ios <name>` starts the recording, prints the gesture
script, and converts the result — so both columns land in the same format, at the same size, under
the same names.

Stills versus GIFs: a still is enough when the point is layout, hierarchy or colour. A GIF is the
only honest way to show a mini player being dragged out of a detail screen, because the thing being
demonstrated is the *change* between frames.

Targets are resolved through `uiautomator`, not through pixel coordinates. A hard-coded tap point
survives exactly until the first spacing change and then silently captures the wrong card; a
content description is the same string the accessibility layer already guarantees.

Usage
-----
    python3 tools/capture_media.py list
    python3 tools/capture_media.py shot player-detail
    python3 tools/capture_media.py gif player-mini
    python3 tools/capture_media.py all
    python3 tools/capture_media.py ios player-mini      # record the iOS half by hand

Requirements: `adb` on PATH with exactly one device, `ffmpeg` for WebP/GIF conversion, and for the
iOS half, `xcrun simctl` with exactly one booted simulator running StreamTV.
"""

from __future__ import annotations

import argparse
import re
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass, field
from pathlib import Path

PACKAGE = "com.congnguyencn.kmpstreamtv"
ACTIVITY = f"{PACKAGE}/.MainActivity"
IOS_BUNDLE = "com.congnguyencn.kmpstreamtv.StreamTV"
OUTPUT_DIR = Path(__file__).resolve().parent.parent / "docs" / "images"

# Every file carries a platform suffix so the two columns of a README table can be written once and
# filled in independently. Renaming one column's file would silently break the other's link.
ANDROID_SUFFIX = "android"
IOS_SUFFIX = "ios"

# A phone screenshot is portrait and tall. These widths keep a two-column table readable on GitHub
# without either column dominating a clone.
GIF_WIDTH = 270
GIF_FPS = 8
GIF_COLORS = 80
STILL_WIDTH = 420
STILL_QUALITY = 82

# The app is a debug build: the first frames after a cold start are spent verifying classes, and a
# gesture sent into that window is swallowed. Everything waits on a real UI node instead of a clock.
LAUNCH_TIMEOUT = 150.0
WAIT_POLL = 1.0


@dataclass(frozen=True)
class Capture:
    """One reproducible capture.

    `setup` runs before recording starts and is never shown; `steps` are the interaction itself.
    Splitting them keeps a GIF about the behaviour rather than about the navigation needed to reach
    it, which is what makes a six-second budget enough.

    `ios_steps` is prose, not a program. It is what the person driving the simulator performs while
    `ios <name>` records, and it has to describe the same demonstration the Android column shows.
    """

    name: str
    description: str
    setup: list[str] = field(default_factory=list)
    steps: list[str] = field(default_factory=list)
    ios_steps: str = ""
    network: str = "full"
    settle: float = 2.0
    duration: int = 6
    gif_fps: int = GIF_FPS
    gif_colors: int = GIF_COLORS
    gif_width: int = GIF_WIDTH


# --------------------------------------------------------------------------------------------
# adb plumbing
# --------------------------------------------------------------------------------------------


def run(args: list[str], **kwargs) -> subprocess.CompletedProcess:
    return subprocess.run(args, check=True, capture_output=True, **kwargs)


def adb(*args: str) -> subprocess.CompletedProcess:
    return run(["adb", *args])


def screen_size() -> tuple[int, int]:
    out = adb("shell", "wm", "size").stdout.decode()
    width, height = re.search(r"(\d+)x(\d+)", out).groups()
    return int(width), int(height)


def ui_tree() -> str:
    """The current accessibility hierarchy as XML.

    `uiautomator dump` occasionally reports the window as busy while an animation is running, which
    is a transient state and not an error; the caller polls, so returning empty is the right answer.
    """
    try:
        return adb("exec-out", "uiautomator", "dump", "/dev/tty").stdout.decode("utf-8", "replace")
    except subprocess.CalledProcessError:
        return ""


def find_node(tree: str, *, desc: str | None = None, text: str | None = None,
              res_id: str | None = None, min_width: int = 0) -> tuple[int, int, int] | None:
    """Centre and width of the widest node matching one attribute.

    Widest, not first, because the same content description appears on a fully laid-out card and on
    the few-pixel sliver of its neighbour peeking in from the edge of a carousel. Tapping the sliver
    lands on the wrong item and still produces a plausible-looking capture.
    """
    best: tuple[int, int, int] | None = None
    for match in re.finditer(r"<node [^>]*>", tree):
        node = match.group(0)
        if desc is not None and f'content-desc="{desc}' not in node:
            continue
        if text is not None and f'text="{text}"' not in node:
            continue
        if res_id is not None and f'resource-id="{PACKAGE}:id/{res_id}"' not in node:
            continue
        bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
        if not bounds:
            continue
        left, top, right, bottom = (int(value) for value in bounds.groups())
        width = right - left
        if width < min_width or bottom - top < 1:
            continue
        if best is None or width > best[2]:
            best = ((left + right) // 2, (top + bottom) // 2, width)
    return best


def wait_for(**kwargs) -> tuple[int, int, int]:
    """Block until a node appears, and return it. Timing out is a hard failure, deliberately.

    A capture that proceeds without its anchor does not fail — it writes a perfectly valid image of
    the wrong screen, which is the one outcome this whole tool exists to prevent.
    """
    timeout = kwargs.pop("timeout", LAUNCH_TIMEOUT)
    deadline = time.time() + timeout
    while time.time() < deadline:
        node = find_node(ui_tree(), **kwargs)
        if node:
            return node
        time.sleep(WAIT_POLL)
    raise RuntimeError(f"timed out waiting for {kwargs}")


def relaunch(attempts: int = 3) -> None:
    """Start every capture from a cold app so none inherits the previous one's scroll or playback.

    Retried, because an emulator that has been up for a while occasionally brings the app back with
    an empty feed — the window is focused and the chrome is drawn, but the section list has no
    children. A second cold start clears it. Without the retry that state produces a screenshot of
    an empty app, which is a valid file and a useless one.
    """
    for attempt in range(attempts):
        adb("shell", "am", "force-stop", PACKAGE)
        time.sleep(2.0)
        adb("shell", "am", "start", "-n", ACTIVITY)
        try:
            wait_for(text="Videos for you", timeout=LAUNCH_TIMEOUT if attempt == 0 else 45.0)
            return
        except RuntimeError:
            if attempt == attempts - 1:
                raise
            print("  feed never arrived; restarting the app")


# --------------------------------------------------------------------------------------------
# the step language
# --------------------------------------------------------------------------------------------


def step(command: str) -> None:
    """Perform one step.

    Fractional coordinates, so the same capture list runs on a phone with different pixel
    dimensions. Everything that can name its target by content description does, and the raw
    coordinate form is reserved for gestures on surfaces that expose no node of their own — the
    video surface, and the empty half of a story.
    """
    verb, _, argument = command.partition(":")
    width, height = screen_size()

    if verb == "sleep":
        time.sleep(float(argument))

    elif verb == "key":
        adb("shell", "input", "keyevent", f"KEYCODE_{argument}")

    elif verb == "tap":
        x, y, _ = resolve_target(argument, width, height)
        adb("shell", "input", "tap", str(x), str(y))

    elif verb == "swipe":
        parts = argument.split(",")
        millis = parts[4] if len(parts) > 4 else "300"
        x1, y1 = int(float(parts[0]) * width), int(float(parts[1]) * height)
        x2, y2 = int(float(parts[2]) * width), int(float(parts[3]) * height)
        adb("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), millis)

    elif verb == "drag":
        # A slow swipe. `MinimizableView` classifies a gesture by how it travels, so the shrink
        # needs a drag the touch handler can follow frame by frame, not a fling.
        parts = argument.split(",")
        millis = parts[4] if len(parts) > 4 else "900"
        x1, y1 = int(float(parts[0]) * width), int(float(parts[1]) * height)
        x2, y2 = int(float(parts[2]) * width), int(float(parts[3]) * height)
        adb("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), millis)

    elif verb == "wait":
        key, _, value = argument.partition("=")
        wait_for(**{{"desc": "desc", "text": "text", "id": "res_id"}[key]: value})

    elif verb == "scrollto":
        key, _, value = argument.partition("=")
        scroll_to(**{{"desc": "desc", "text": "text", "id": "res_id"}[key]: value})

    elif verb == "rotate":
        adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
        adb("shell", "settings", "put", "system", "user_rotation", "1" if argument == "landscape" else "0")
        time.sleep(2.5)

    else:
        raise SystemExit(f"unknown step: {command}")


def resolve_target(argument: str, width: int, height: int) -> tuple[int, int, int]:
    key, sep, value = argument.partition("=")
    if not sep:
        x_fraction, y_fraction = (float(part) for part in argument.split(","))
        return int(x_fraction * width), int(y_fraction * height), 0
    lookup = {"desc": "desc", "text": "text", "id": "res_id"}[key]
    # 200 px filters out carousel slivers without excluding any real card.
    return wait_for(**{lookup: value}, min_width=200)


def scroll_to(**kwargs) -> None:
    """Swipe the feed up until the target is fully laid out, then stop.

    Fully laid out matters: a rail entering from the bottom edge already has a node and already
    reports a centre, but that centre is off screen, and tapping it does nothing at all.
    """
    width, height = screen_size()
    for _ in range(8):
        node = find_node(ui_tree(), **kwargs, min_width=200)
        if node and node[1] < height * 0.80:
            return
        adb("shell", "input", "swipe", str(width // 2), str(int(height * 0.78)),
            str(width // 2), str(int(height * 0.42)), "500")
        time.sleep(1.5)
    raise RuntimeError(f"could not scroll to {kwargs}")


# --------------------------------------------------------------------------------------------
# shared navigation
# --------------------------------------------------------------------------------------------

# Every player demo opens *Pulse of the court*, the first card of the featured carousel, so all of
# them show one video and none of them needs a scroll to reach it.
#
# It is also the only fixture whose stream survives this emulator. The decoder renders H.264
# Baseline correctly and turns Main and High into bands of coloured noise, while position, duration
# and buffering keep reporting healthy values — so a capture of a corrupt picture still succeeds.
# Apple's bipbop ladder is Baseline throughout; Big Buck Bunny and Tears of Steel are not, and
# Sintel's host now answers 403. `updateReadme.md` records how to tell which is which.
OPEN_PLAYER = [
    # The carousel is laid out before its artwork arrives, and a tap that lands while the rail is
    # still settling reaches the wrong card or nothing at all.
    "sleep:3",
    "tap:desc=Pulse of the court",
    "wait:text=Recommended for you",
    "sleep:16",
]

# One tap where play/pause sits, which leaves the controller visible whichever state it was in.
#
# The controller hides itself five seconds after an interaction, but only while `isPlaying` is true,
# and on an emulator that rebuffers this stalls often enough that its visibility cannot be assumed.
# When the controller is hidden the buttons are `isVisible = false` and the tap reaches the mask,
# which shows the controller; when it is showing, the same tap hits play/pause, and pausing cancels
# the auto-hide. Both paths end with the controller up, which is the only property these captures
# need — the play/pause glyph may be either way round.
SHOW_CONTROLLER = ["tap:0.5,0.184", "sleep:1.5"]

# Controller geometry as fractions of the window. The player is a `SurfaceView` and `uiautomator`
# refuses to dump while it renders, so these are the only targets in the whole file that cannot be
# named. Re-measure them from a `player-detail` capture if the controller layout changes.
PLAYER_PIP = "0.747,0.0967"
PLAYER_QUALITY = "0.719,0.242"
# An unoccupied corner of the video box: the shrink drag is rejected if it starts on a control.
PLAYER_EMPTY_SURFACE = "0.2,0.13"

OPEN_STORY = [
    "tap:desc=Before the strike",
    "sleep:10",
]

OPEN_SHORT = [
    "tap:id=shorts",
    "sleep:14",
]


CAPTURES: dict[str, Capture] = {
    "home-overview": Capture(
        name="home-overview",
        description="Story rail over the featured carousel",
        ios_steps="Open the app on Home and let the artwork finish loading. Do not scroll.",
        settle=6.0,
    ),
    "home-feed": Capture(
        name="home-feed",
        description="Scrolling the section list past every view type",
        steps=[
            "swipe:0.5,0.78,0.5,0.34,500", "sleep:1.2",
            "swipe:0.5,0.78,0.5,0.34,500", "sleep:1.2",
            "swipe:0.5,0.78,0.5,0.34,500", "sleep:1.2",
            "swipe:0.5,0.78,0.5,0.34,500", "sleep:1.5",
        ],
        ios_steps="From the top of Home, flick up four times with a beat between each, far enough "
                  "to pass the ranked rail, the series rail and the live channels.",
        settle=6.0,
        duration=9,
    ),
    "home-categories": Capture(
        name="home-categories",
        description="Category bar hiding as the feed scrolls, and returning at the top",
        steps=[
            "swipe:0.5,0.78,0.5,0.30,400", "sleep:1.5",
            "swipe:0.5,0.30,0.5,0.78,400", "sleep:2",
        ],
        ios_steps="Flick the feed up once, wait for the category row to collapse, then flick back "
                  "to the top and wait for it to reappear.",
        settle=6.0,
        duration=7,
    ),
    "story-viewer": Capture(
        name="story-viewer",
        description="Segmented progress, the reaction burst, then tap-right to the next story",
        setup=OPEN_STORY,
        steps=[
            "tap:0.12,0.96", "sleep:0.6",
            "tap:0.30,0.96", "sleep:0.6",
            "tap:0.12,0.96", "sleep:1.5",
            # Short: the next story needs a moment to render, and those frames are black.
            "tap:0.80,0.50", "sleep:1.2",
        ],
        ios_steps="Open the first story by tapping the provider avatar on its card — the rest of "
                  "the card is not tappable on iOS, see `updateReadme.md`. Tap the thumbs-up, the "
                  "heart and the laugh, then tap the right half of the frame to advance.",
        settle=2.0,
        duration=7,
        gif_fps=10,
    ),
    "story-hold": Capture(
        name="story-hold",
        description="Holding the frame pauses playback and the segment stops advancing",
        setup=OPEN_STORY,
        steps=["swipe:0.5,0.5,0.5,0.5,3000", "sleep:2"],
        ios_steps="Open the first story by tapping the provider avatar on its card, then press and "
                  "hold the middle of the frame for about four seconds and release.",
        settle=2.0,
        duration=7,
    ),
    "short-feed": Capture(
        name="short-feed",
        description="Paged vertical feed: swiping hands playback to the next page",
        setup=OPEN_SHORT,
        steps=[
            "swipe:0.5,0.75,0.5,0.25,250", "sleep:3.5",
            "swipe:0.5,0.75,0.5,0.25,250", "sleep:3.5",
        ],
        ios_steps="Open the Short tab, wait for the first video, then swipe up twice with a few "
                  "seconds on each page.",
        settle=2.0,
        duration=10,
    ),
    "short-actions": Capture(
        name="short-actions",
        description="Action rail: follow, like, comment, share and more",
        setup=OPEN_SHORT,
        ios_steps="Open the Short tab and let the first video play until the chrome is steady.",
        settle=4.0,
    ),
    "short-interactions": Capture(
        name="short-interactions",
        description="Optimistic like, follow, and tap-to-pause on the surface",
        setup=OPEN_SHORT,
        steps=[
            "tap:0.90,0.62", "sleep:1.2",
            "tap:0.90,0.47", "sleep:1.5",
            "tap:0.45,0.45", "sleep:1.2",
            "tap:0.45,0.45", "sleep:1.5",
        ],
        ios_steps="On the first short, tap Like, then the follow badge on the avatar, then tap the "
                  "video twice to pause and resume.",
        settle=2.0,
        duration=8,
    ),
    "player-detail": Capture(
        name="player-detail",
        description="Portrait VOD detail: 16:9 player, actions, provider, recommendations",
        setup=OPEN_PLAYER,
        steps=SHOW_CONTROLLER,
        ios_steps="Open *Realm of the Bengal tiger* from Home, wait for playback, then tap the "
                  "video once so the controller is showing.",
        settle=2.0,
    ),
    "player-detail-scroll": Capture(
        name="player-detail-scroll",
        description="The detail list scrolling under a player that stays put",
        setup=OPEN_PLAYER,
        steps=[
            "swipe:0.5,0.85,0.5,0.45,450", "sleep:1.5",
            "swipe:0.5,0.85,0.5,0.45,450", "sleep:2",
        ],
        ios_steps="With the detail screen open, flick the list below the player up twice.",
        settle=2.0,
        duration=7,
    ),
    "player-mini": Capture(
        name="player-mini",
        description="Dragging the player down shrinks it into the floating mini card",
        setup=OPEN_PLAYER,
        steps=[
            f"drag:{PLAYER_EMPTY_SURFACE},0.5,0.72,1100", "sleep:3",
            "tap:0.66,0.78", "sleep:3",
        ],
        ios_steps="From the detail screen, drag the video down to the bottom of the window and "
                  "release, wait for the card to settle above the tab bar, then tap the card to "
                  "restore the detail screen.",
        settle=2.0,
        duration=10,
        gif_fps=10,
    ),
    "player-mini-corner": Capture(
        name="player-mini-corner",
        description="The minimized card parking in whichever corner it is released nearest",
        setup=OPEN_PLAYER + [f"drag:{PLAYER_EMPTY_SURFACE},0.5,0.72,1100", "sleep:3"],
        steps=[
            "drag:0.66,0.78,0.30,0.30,900", "sleep:2.5",
            "drag:0.30,0.30,0.70,0.78,900", "sleep:2.5",
        ],
        ios_steps="With the mini player resting, drag it to the upper left, let it settle, then "
                  "drag it back to the lower right.",
        settle=2.0,
        duration=10,
        gif_fps=10,
    ),
    "player-pip": Capture(
        name="player-pip",
        description="The controller's PiP action handing playback to the system window",
        setup=OPEN_PLAYER + SHOW_CONTROLLER,
        steps=[f"tap:{PLAYER_PIP}", "sleep:5"],
        ios_steps="On a device, not the Simulator: `isPictureInPictureSupported()` is false there "
                  "so the app draws no PiP action. With the controller showing, tap Picture in "
                  "Picture and wait for the system window to settle over the app.",
        settle=1.0,
        duration=8,
    ),
    "player-fullscreen": Capture(
        name="player-fullscreen",
        description="Landscape fullscreen with the full controller",
        setup=OPEN_PLAYER + ["rotate:landscape", "sleep:4"],
        steps=["tap:0.5,0.5", "sleep:1.5"],
        ios_steps="With the detail screen open, rotate to landscape by hand (Device > Rotate — "
                  "nothing in `simctl` rotates a simulator), then tap once so the controller is "
                  "showing.",
        settle=2.0,
    ),
    "player-settings": Capture(
        name="player-settings",
        description="Quality, speed, audio and subtitle columns over the player",
        setup=OPEN_PLAYER + SHOW_CONTROLLER,
        steps=[f"tap:{PLAYER_QUALITY}", "sleep:2"],
        ios_steps="With the controller showing, tap the settings action and wait for the sheet.",
        settle=1.0,
    ),
}

GIF_CAPTURES = {
    "home-feed",
    "home-categories",
    "story-viewer",
    "story-hold",
    "short-feed",
    "short-interactions",
    "player-detail-scroll",
    "player-mini",
    "player-mini-corner",
    "player-pip",
}


# --------------------------------------------------------------------------------------------
# capture
# --------------------------------------------------------------------------------------------


def reset_rotation() -> None:
    adb("shell", "settings", "put", "system", "user_rotation", "0")


def set_network(speed: str) -> None:
    """Cap the emulator's radio. Every capture resets it to `full`; nothing throttles today.

    Kept because it is the escape hatch for the decoder problem described above `OPEN_PLAYER`, and
    because leaving a cap behind is a failure that looks like something else entirely — artwork that
    never loads, a feed that stays empty, a capture that times out waiting for a node. Holding the
    radio under the first Main/High rendition of an ABR ladder does force the player onto a
    decodable one, but it costs more than it sounds like: ExoPlayer picks its first rendition from a
    default one-megabit estimate before it has measured anything, so it still opens on the corrupt
    variant and takes over twenty seconds of slow segments to step down. Choosing a fixture whose
    whole ladder is Baseline is cheaper and does not make every player capture half a minute longer.
    """
    adb("emu", "network", "speed", speed)


def prepare(capture: Capture, attempts: int = 3) -> None:
    """Cold-start the app and walk `setup` until the screen the capture needs is actually up.

    The setup walk is retried as a unit. A tap into a carousel that is still settling scrolls the
    rail instead of opening the item, and the failure surfaces several steps later as a wait that
    never resolves — there is nothing useful to retry at the level of one step.
    """
    for attempt in range(attempts):
        reset_rotation()
        # Launch at full speed so the feed artwork is already cached, then apply any cap.
        set_network("full")
        relaunch()
        set_network(capture.network)
        try:
            for command in capture.setup:
                step(command)
        except RuntimeError as error:
            if attempt == attempts - 1:
                raise
            print(f"  setup did not land ({error}); starting over")
            continue
        time.sleep(capture.settle)
        return


def capture_shot(capture: Capture) -> Path:
    prepare(capture)
    for command in capture.steps:
        step(command)
    time.sleep(1.0)
    raw = OUTPUT_DIR / f"{capture.name}-{ANDROID_SUFFIX}.png"
    raw.write_bytes(adb("exec-out", "screencap", "-p").stdout)
    output = raw.with_suffix(".webp")
    run(["ffmpeg", "-y", "-i", str(raw), "-vf", f"scale={STILL_WIDTH}:-1:flags=lanczos",
         "-quality", str(STILL_QUALITY), str(output)])
    raw.unlink()
    reset_rotation()
    set_network("full")
    return output


def frame_count(path: Path) -> int:
    probe = subprocess.run(
        ["ffprobe", "-v", "error", "-count_frames", "-select_streams", "v:0",
         "-show_entries", "stream=nb_read_frames", "-of", "csv=p=0", str(path)],
        capture_output=True,
    )
    digits = re.sub(r"\D", "", probe.stdout.decode() or "0")
    return int(digits or 0)


def record(capture: Capture) -> Path:
    """Record one clip. `screenrecord` occasionally writes a single-frame file and exits 0."""
    remote = "/sdcard/capture.mp4"
    adb("shell", "rm", "-f", remote)
    recorder = subprocess.Popen(
        ["adb", "shell", "screenrecord", "--bit-rate", "8000000",
         "--time-limit", str(capture.duration), remote],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    # screenrecord needs a moment to open the encoder; a step sent before that is simply not in the
    # file, and the GIF then starts halfway through the gesture it is supposed to show.
    time.sleep(1.5)
    for command in capture.steps:
        step(command)
    recorder.wait(timeout=capture.duration + 45)
    time.sleep(2.0)

    local = OUTPUT_DIR / f"{capture.name}.mp4"
    adb("pull", remote, str(local))
    adb("shell", "rm", "-f", remote)
    return local


def capture_gif(capture: Capture) -> Path:
    for attempt in range(2):
        prepare(capture)
        local = record(capture)
        if frame_count(local) > 4:
            break
        local.unlink()
        print("  recording came back empty; retrying")
    else:
        raise RuntimeError(f"{capture.name}: screenrecord produced no usable frames")

    output = OUTPUT_DIR / f"{capture.name}-{ANDROID_SUFFIX}.gif"
    to_gif(local, output, capture)
    local.unlink()
    reset_rotation()
    set_network("full")
    return output


def to_gif(source: Path, output: Path, capture: Capture) -> None:
    palette = source.with_suffix(".png")
    scale = f"fps={capture.gif_fps},scale={capture.gif_width}:-1:flags=lanczos"
    run(["ffmpeg", "-y", "-i", str(source), "-vf",
         f"{scale},palettegen=max_colors={capture.gif_colors}:stats_mode=diff", str(palette)])
    run(["ffmpeg", "-y", "-i", str(source), "-i", str(palette), "-lavfi",
         f"{scale}[x];[x][1:v]paletteuse=dither=bayer:bayer_scale=3", str(output)])
    palette.unlink()


# --------------------------------------------------------------------------------------------
# the iOS half
# --------------------------------------------------------------------------------------------


def booted_simulator() -> str:
    out = run(["xcrun", "simctl", "list", "devices", "booted"]).stdout.decode()
    ids = re.findall(r"\(([0-9A-Fa-f-]{36})\) \(Booted\)", out)
    if len(ids) != 1:
        raise SystemExit(f"expected exactly one booted simulator, found {len(ids)}")
    return ids[0]


def prompt(message: str) -> None:
    """Wait for the operator, unless nothing is attached to wait for.

    Without a terminal — driven from a script, or from an agent injecting the gestures some other
    way — there is nobody to press Return, and blocking on it would hang the run forever.
    """
    if sys.stdin.isatty():
        input(message)
    else:
        print(f"{message}(no terminal: starting in 3s)", flush=True)
        time.sleep(3.0)


def capture_ios(capture: Capture) -> Path:
    """Record the simulator while a human performs `ios_steps`.

    There is no `simctl` verb that touches the screen, so this half stays manual on purpose rather
    than pretending to be reproducible. What the tool does own is everything after the gesture:
    the same fps, the same width, the same palette and the same file name as the Android column, so
    the two cells of a README row are actually comparable.
    """
    simulator = booted_simulator()
    is_gif = capture.name in GIF_CAPTURES
    print(f"\n  {capture.name} — {capture.description}")
    print(f"  Perform on the simulator: {capture.ios_steps or '(no gesture script recorded)'}")

    if not is_gif:
        prompt("  Set the screen up, then press Return to grab the still. ")
        raw = OUTPUT_DIR / f"{capture.name}-{IOS_SUFFIX}.png"
        run(["xcrun", "simctl", "io", simulator, "screenshot", str(raw)])
        output = raw.with_suffix(".webp")
        run(["ffmpeg", "-y", "-i", str(raw), "-vf", f"scale={STILL_WIDTH}:-1:flags=lanczos",
             "-quality", str(STILL_QUALITY), str(output)])
        raw.unlink()
        return output

    local = OUTPUT_DIR / f"{capture.name}-ios.mov"
    prompt(f"  Press Return to start recording {capture.duration}s, then perform the gesture. ")
    recorder = subprocess.Popen(
        ["xcrun", "simctl", "io", simulator, "recordVideo", "--codec", "h264", "-f", str(local)],
        stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    time.sleep(capture.duration + 1.5)
    recorder.send_signal(2)
    recorder.wait(timeout=30)

    output = OUTPUT_DIR / f"{capture.name}-{IOS_SUFFIX}.gif"
    to_gif(local, output, capture)
    local.unlink()
    return output


# --------------------------------------------------------------------------------------------


def require_tools(need_android: bool) -> None:
    if shutil.which("ffmpeg") is None:
        raise SystemExit("ffmpeg is required")
    if not need_android:
        return
    if shutil.which("adb") is None:
        raise SystemExit("adb is required")
    devices = [line for line in adb("devices").stdout.decode().splitlines()[1:] if "\tdevice" in line]
    if len(devices) != 1:
        raise SystemExit(f"expected exactly one adb device, found {len(devices)}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["list", "shot", "gif", "all", "ios"])
    parser.add_argument("name", nargs="?")
    args = parser.parse_args()

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    if args.command == "list":
        for capture in CAPTURES.values():
            kind = "gif " if capture.name in GIF_CAPTURES else "shot"
            print(f"{kind}  {capture.name:24s} {capture.description}")
        return 0

    require_tools(need_android=args.command != "ios")

    if args.command == "all":
        failed: list[str] = []
        for capture in CAPTURES.values():
            print(f"→ {capture.name}", flush=True)
            try:
                output = capture_gif(capture) if capture.name in GIF_CAPTURES else capture_shot(capture)
                print(f"  {output.relative_to(OUTPUT_DIR.parent.parent)}", flush=True)
            except Exception as error:  # keep going: one flaky screen must not cost the whole run
                failed.append(capture.name)
                print(f"  FAILED: {error}", flush=True)
        if failed:
            print("\nre-run individually: " + ", ".join(failed))
        return 1 if failed else 0

    if args.name not in CAPTURES:
        raise SystemExit(f"unknown capture: {args.name}")
    capture = CAPTURES[args.name]

    if args.command == "ios":
        output = capture_ios(capture)
    elif args.command == "gif":
        output = capture_gif(capture)
    else:
        output = capture_shot(capture)
    print(output.relative_to(OUTPUT_DIR.parent.parent))
    return 0


if __name__ == "__main__":
    sys.exit(main())
