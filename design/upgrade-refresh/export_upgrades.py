"""Writes the remote upgrade visual line (docs/design/UPGRADE_VISUALS.md) into the NeoForge resources."""
import json
import shutil

from PIL import Image

from gen_feeding import ROOT
from upgrade_visuals import UPGRADES, Upgrade

ASSETS = ROOT / "neoforge/src/main/resources/assets/sophisticated_backpacks_digital_storages_expansion"
MOD_ID = "sophisticated_backpacks_digital_storages_expansion"
# Design key -> backend id stored in the link, in the order of the item's "network" property.
BACKENDS = {"ae2": "ae2", "rs": "refinedstorage", "tss": "toms_storage", "scc": "scc"}


def model(texture):
    return {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD_ID}:item/{texture}"}}


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def export(upgrade):
    item = upgrade.item
    upgrade.unlinked().save(ASSETS / f"textures/item/{item}.png")

    # Last matching override wins: linked sprites by "network", then the animation canvas
    # whenever "link_frame" is set. LinkedUpgradeAppearance writes the current frame into it.
    overrides = []
    for index, (key, backend) in enumerate(BACKENDS.items(), start=1):
        name = f"{item}_linked_{backend}"
        upgrade.linked(key).save(ASSETS / f"textures/item/{name}.png")
        write_json(ASSETS / f"models/item/{name}.json", model(name))
        overrides.append({"predicate": {f"{MOD_ID}:network": index}, "model": f"{MOD_ID}:item/{name}"})
    canvas = f"{item}_link_canvas"
    upgrade.unlinked().save(ASSETS / f"textures/item/{canvas}.png")
    write_json(ASSETS / f"models/item/{canvas}.json", model(canvas))
    overrides.append({"predicate": {f"{MOD_ID}:link_frame": 1}, "model": f"{MOD_ID}:item/{canvas}"})
    write_json(ASSETS / f"models/item/{item}.json", {**model(item), "overrides": overrides})

    # One vertical strip per (previous state, network), outside the item atlas.
    strips = ASSETS / f"textures/link_animation/{item}"
    strips.mkdir(parents=True, exist_ok=True)
    for key, backend in BACKENDS.items():
        for previous, previous_name in [(None, "unlinked")] + list(BACKENDS.items()):
            seq = upgrade.frames(key, previous)
            strip = Image.new("RGBA", (16, 16 * len(seq)))
            for k, im in enumerate(seq):
                strip.paste(im, (0, 16 * k))
            strip.save(strips / f"{previous_name}_to_{backend}.png")
    print(item, len(seq), "frames per strip")


def main():
    # Per-frame models from the first Feeding pilot.
    for stale in ("textures/gui/link_animation", "textures/item/link_animation", "models/item/link_animation",
                  "textures/link_animation"):
        shutil.rmtree(ASSETS / stale, ignore_errors=True)
    for name in UPGRADES:
        export(Upgrade(name))


if __name__ == "__main__":
    main()
