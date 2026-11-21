"""Writes the remote upgrade visual line (docs/design/UPGRADE_VISUALS.md) into the NeoForge resources."""
import json
import argparse
from pathlib import Path

from PIL import Image

from gen_feeding import ROOT
from upgrade_visuals import UPGRADES, Upgrade

MOD_ID = "sophisticated_backpacks_digital_storages_expansion"
# Design key -> backend id stored in the link, in the order of the item's "network" property.
BACKENDS = {"ae2": "ae2", "rs": "refinedstorage", "tss": "toms_storage", "scc": "scc", "integrateddynamics": "integrateddynamics"}


def model(texture):
    return {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD_ID}:item/{texture}"}}


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists() and json.loads(path.read_text()) == data:
        return
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf8")


def write_image(path, image, fallback=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    existing = path if path.exists() else fallback
    if existing and existing.exists():
        with Image.open(existing) as original:
            if original.size == image.size and original.convert("RGBA").tobytes() == image.tobytes():
                return
    image.save(path)


def export(upgrade, assets, backends, shared=None):
    item = upgrade.item
    def texture(relative, image):
        write_image(assets / relative, image, shared / relative if shared else None)
    texture(f"textures/item/{item}.png", upgrade.unlinked())

    # Last matching override wins: linked sprites by "network", then the animation canvas
    # whenever "link_frame" is set. LinkedUpgradeAppearance writes the current frame into it.
    overrides = []
    for index, (key, backend) in enumerate(backends.items(), start=1):
        name = f"{item}_linked_{backend}"
        texture(f"textures/item/{name}.png", upgrade.linked(key))
        write_json(assets / f"models/item/{name}.json", model(name))
        overrides.append({"predicate": {f"{MOD_ID}:network": index}, "model": f"{MOD_ID}:item/{name}"})
    canvas = f"{item}_link_canvas"
    texture(f"textures/item/{canvas}.png", upgrade.unlinked())
    write_json(assets / f"models/item/{canvas}.json", model(canvas))
    overrides.append({"predicate": {f"{MOD_ID}:link_frame": 1}, "model": f"{MOD_ID}:item/{canvas}"})
    write_json(assets / f"models/item/{item}.json", {**model(item), "overrides": overrides})

    # One vertical strip per (previous state, network), outside the item atlas.
    strips = assets / f"textures/link_animation/{item}"
    strips.mkdir(parents=True, exist_ok=True)
    for key, backend in backends.items():
        for previous, previous_name in [(None, "unlinked")] + list(backends.items()):
            seq = upgrade.frames(key, previous)
            strip = Image.new("RGBA", (16, 16 * len(seq)))
            for k, im in enumerate(seq):
                strip.paste(im, (0, 16 * k))
            texture(f"textures/link_animation/{item}/{previous_name}_to_{backend}.png", strip)
    print(item, len(seq), "frames per strip")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", choices=["1.21.1", "1.20.1"], default="1.21.1")
    parser.add_argument("--assets", type=Path, help="Optional staging/export directory, instead of writing target resources")
    args = parser.parse_args()
    loader = "forge" if args.target == "1.20.1" else "neoforge"
    assets = args.assets or ROOT / f"targets/{args.target}/{loader}/src/main/resources/assets/{MOD_ID}"
    # Only the pilot has the fifth backend. The transactional 26.1.2 model format is ported in phase 10.
    backends = BACKENDS if args.target == "1.21.1" else {k: v for k, v in BACKENDS.items() if k != "integrateddynamics"}
    shared = None if args.assets else ROOT / f"shared/src/main/resources/assets/{MOD_ID}"
    for name in UPGRADES:
        export(Upgrade(name), assets, backends, shared)


if __name__ == "__main__":
    main()
