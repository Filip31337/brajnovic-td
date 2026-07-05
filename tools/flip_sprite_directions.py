#!/usr/bin/env python3
"""Fill in missing directional animations (W, NW, SW) for a sprites-src sheet
by horizontally flipping their drawn counterparts (E, NE, SE).

Usage:
    python tools/flip_sprite_directions.py <atlas-basename>

Example:
    python tools/flip_sprite_directions.py orc_atlas

Reads assets/sprites-src/<basename>.png + <basename>.json and writes both
back in place with the missing directions appended as new rows. Safe to
re-run: animations that already exist are left untouched.
"""
import json
import sys
from pathlib import Path

from PIL import Image

# Only these directions need a hand-drawn source frame; the rest are mirrors.
MIRROR_SUFFIXES = {
    "E": "W",
    "NE": "NW",
    "SE": "SW",
}


def split_prefix_direction(name: str):
    for direction in sorted(MIRROR_SUFFIXES, key=len, reverse=True):
        suffix = "_" + direction
        if name.endswith(suffix):
            return name[: -len(suffix)], direction
    return None, None


def main():
    if len(sys.argv) != 2:
        print("Usage: flip_sprite_directions.py <atlas-basename>")
        sys.exit(1)

    basename = sys.argv[1]
    sprites_dir = Path(__file__).resolve().parent.parent / "assets" / "sprites-src"
    json_path = sprites_dir / f"{basename}.json"
    png_path = sprites_dir / f"{basename}.png"

    data = json.loads(json_path.read_text())
    animations = data.get("animations")
    if animations is None:
        print(f"{json_path.name} has no 'animations' key, nothing to flip.")
        sys.exit(1)

    frame_w, frame_h = data["frame_size"]
    image = Image.open(png_path).convert("RGBA")

    to_generate = []
    for name, frames in list(animations.items()):
        prefix, direction = split_prefix_direction(name)
        if direction is None:
            continue
        mirrored_name = f"{prefix}_{MIRROR_SUFFIXES[direction]}"
        if mirrored_name in animations:
            continue
        to_generate.append((mirrored_name, frames))

    if not to_generate:
        print("Nothing to generate, all mirrored directions already present.")
        return

    next_row_y = image.height
    new_height = image.height + len(to_generate) * frame_h
    expanded = Image.new("RGBA", (image.width, new_height), (0, 0, 0, 0))
    expanded.paste(image, (0, 0))

    for mirrored_name, source_frames in to_generate:
        new_frames = []
        for col, frame in enumerate(source_frames):
            region = image.crop((frame["x"], frame["y"], frame["x"] + frame["w"], frame["y"] + frame["h"]))
            flipped = region.transpose(Image.FLIP_LEFT_RIGHT)
            dest_x = col * frame_w
            expanded.paste(flipped, (dest_x, next_row_y))
            new_frames.append({"x": dest_x, "y": next_row_y, "w": frame["w"], "h": frame["h"]})
        animations[mirrored_name] = new_frames
        print(f"Generated {mirrored_name} ({len(new_frames)} frames)")
        next_row_y += frame_h

    expanded.save(png_path)
    json_path.write_text(json.dumps(data, indent=2) + "\n")
    print(f"Updated {png_path.name} and {json_path.name}")


if __name__ == "__main__":
    main()
