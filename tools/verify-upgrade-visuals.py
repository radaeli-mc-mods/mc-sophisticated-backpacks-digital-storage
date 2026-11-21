"""Verify the pilot's model/strip contracts and optionally render a review sheet.

Usage: python tools/verify-upgrade-visuals.py [--preview build/integrated-audit/phase2-visuals.png]
Requires Pillow, as does the existing asset generator. Production-JAR checks also run in Gradle.
"""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
MOD = "sophisticated_backpacks_digital_storages_expansion"
PILOT = ROOT / f"targets/1.21.1/neoforge/src/main/resources/assets/{MOD}"
SHARED = ROOT / f"shared/src/main/resources/assets/{MOD}"
UPGRADES = ["deposit", "restock", "pickup", "magnet", "refill", "feeding", "alchemy", "pump", "crafting"]
BACKENDS = ["ae2", "refinedstorage", "toms_storage", "scc", "integrateddynamics"]


def raster(relative):
    path = PILOT / relative
    if not path.exists():
        path = SHARED / relative
    with Image.open(path) as image:
        return image.convert("RGBA")


def verify(preview=None, target="1.21.1"):
    global PILOT
    PILOT = ROOT / f"targets/{target}/neoforge/src/main/resources/assets/{MOD}"
    sheet = Image.new("RGB", (560, 48 + len(UPGRADES) * 82), "#1b1d22")
    draw = ImageDraw.Draw(sheet)
    for column, name in enumerate(["unlinked", "AE2", "RS", "Tom's", "SCC", "Integrated"]):
        draw.text((90 + column * 76, 12), name, fill="white")
    for row, upgrade in enumerate(UPGRADES):
        item = f"remote_{upgrade}_upgrade"
        original = raster(f"textures/item/{item}.png")
        states = {"unlinked": original}
        model = json.loads((PILOT / f"models/item/{item}.json").read_text())
        if target != "26.1.2":
            assert len(model["overrides"]) == 6, item
        for index, backend in enumerate(BACKENDS, 1):
            if target == "26.1.2":
                definition = json.loads((PILOT / f"items/{item}.json").read_text())["model"]
                assert len(definition["entries"]) == 6, item
                entry = definition["entries"][index - 1]
                assert entry["threshold"] == index, item
                assert entry["model"]["model"].endswith(f"{item}_linked_{backend}"), item
                assert definition["entries"][-1]["threshold"] == 6, item
            else:
                override = model["overrides"][index - 1]
                assert override["predicate"] == {f"{MOD}:network": index}, item
                assert override["model"].endswith(f"{item}_linked_{backend}"), item
            image = raster(f"textures/item/{item}_linked_{backend}.png")
            assert image.size == (16, 16), item
            # All four corner glyphs remain byte-identical; the new accent occupies the top center.
            for x, y, dx, dy in [(1, 1, 1, 1), (14, 1, -1, 1), (1, 14, 1, -1), (14, 14, -1, -1)]:
                for point in [(x, y), (x + dx, y), (x, y + dy), (x + 2 * dx, y), (x, y + 2 * dy), (x + dx, y + dy)]:
                    assert image.getpixel(point) == original.getpixel(point), (item, backend, point)
            if backend == "integrateddynamics":
                assert image.getpixel((7, 1))[:3] == (176, 242, 231), item
                assert image.getpixel((8, 1))[:3] == (54, 185, 178), item
            states[backend] = image
        if target != "26.1.2":
            assert model["overrides"][-1]["predicate"] == {f"{MOD}:link_frame": 1}, item
        for destination in BACKENDS:
            for source, image in states.items():
                strip = raster(f"textures/link_animation/{item}/{source}_to_{destination}.png")
                assert strip.size == (16, 400), (item, source, destination)
                assert strip.crop((0, 0, 16, 16)).tobytes() == image.tobytes(), (item, source, destination, "first frame")
                assert strip.crop((0, 384, 16, 400)).tobytes() == states[destination].tobytes(), (item, source, destination, "last frame")
        draw.text((4, 54 + row * 82), upgrade, fill="white")
        for column, image in enumerate(states.values()):
            sheet.paste(image.resize((64, 64), Image.Resampling.NEAREST), (90 + column * 76, 42 + row * 82), image.resize((64, 64), Image.Resampling.NEAREST))
    if preview:
        preview.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(preview)
    print("Verified nine upgrades: five linked states, unchanged corners, stable indices and all 270 transition strips (25 frames each)")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--preview", type=Path)
    parser.add_argument("--target", choices=["1.21.1", "26.1.2"], default="1.21.1")
    args = parser.parse_args()
    verify(args.preview, args.target)
