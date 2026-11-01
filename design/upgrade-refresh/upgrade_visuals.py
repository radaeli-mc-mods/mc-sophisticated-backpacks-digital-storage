"""Remote upgrade visual line (docs/design/UPGRADE_VISUALS.md), generic over the eight upgrades.

Each upgrade keeps its original 16x16 icon (design/upgrade-refresh/originals); everything else
comes from the shared frame, palette and link animation defined here and in gen_feeding.
"""
from pathlib import Path

from PIL import Image

from anim_feeding import ORIGIN, ring_distance
from gen_feeding import CORNERS, NETWORKS, SCREENS, base, paint_corners

ORIGINALS = Path(__file__).resolve().parent / "originals"
UPGRADES = ["deposit", "restock", "pickup", "magnet", "refill", "feeding", "alchemy", "pump"]
# Original remote palette roles inside the icon area.
FIELD = (0x37, 0xAE, 0x6C, 255)
OUTLINE, FILL, DETAIL = (0x22, 0x16, 0x2D, 255), (0x42, 0x2B, 0x52, 255), (0x1F, 0x71, 0x4B, 255)


# Every third step of an animation phase, always ending on its last one; the item model only
# refreshes each 50 ms tick, so finer strips would not be seen.
FRAME_STEP = 3


def steps(first, last):
    values = list(range(first, last + 1, FRAME_STEP))
    if values[-1] != last:
        values.append(last)
    return values


class Upgrade:
    def __init__(self, name):
        self.name = name
        self.item = f"remote_{name}_upgrade"
        src = Image.open(ORIGINALS / f"{self.item}.png").convert("RGBA")
        self.icon = {(x, y): src.getpixel((x, y)) for x in range(3, 13) for y in range(3, 13)
                     if src.getpixel((x, y)) != FIELD}

    def recolor(self, network):
        """Icon tone mapping once linked: outline/fill/detail -> dark/mid/light; one-tone silhouettes -> mid."""
        lt, mid, dk = NETWORKS[network]
        if set(self.icon.values()) == {OUTLINE}:
            return {OUTLINE: mid}
        return {OUTLINE: dk, FILL: mid, DETAIL: lt}

    def unlinked(self):
        im, px = base()
        paint_corners(px)
        for p, c in self.icon.items():
            px[p] = c
        return im

    def linked(self, network):
        im, px = base()
        face, shade = SCREENS["dark"]
        for x in range(3, 13):
            for y in range(3, 13):
                px[x, y] = shade if x == 3 or y == 3 else face
        recolor = self.recolor(network)
        for p, c in self.icon.items():
            px[p] = recolor.get(c, c)
        paint_corners(px)
        return im

    def frames(self, network, previous=None):
        """Animation strip into `network`: first frame is the previous state, last is the linked sprite."""
        lt, mid, dk = NETWORKS[network]
        start = self.linked(previous) if previous else self.unlinked()
        final = self.linked(network)
        dist = ring_distance(ORIGIN[network])
        far = max(dist.values())
        rim_color = lambda p: dk if p[0] == 14 or p[1] == 14 else mid
        out = [start]

        # 1. Rim fills from the network's corner in both directions.
        glints = {(cx, cy): (cx + dx, cy + dy) for cx, cy, dx, dy in CORNERS}
        for t in steps(0, far):
            im = start.copy()
            for p, d in dist.items():
                if d <= t:
                    im.putpixel(p, rim_color(p))
                    if p in glints:
                        im.putpixel(glints[p], final.getpixel(glints[p]))
            out.append(im)

        # 2. Screen darkens along the diagonal toward the center; the icon takes the network color.
        filled = out[-1]
        reach = lambda x, y: min((x - 3) + (y - 3), (12 - x) + (12 - y))
        for t in steps(0, 9):
            im = filled.copy()
            for x in range(3, 13):
                for y in range(3, 13):
                    if reach(x, y) <= t:
                        im.putpixel((x, y), final.getpixel((x, y)))
            out.append(im)

        # 3. Rim retracts the way it came, back to the four network corners.
        wiped = out[-1]
        for t in reversed(steps(0, far)):
            im = wiped.copy()
            for p, d in dist.items():
                if d >= t:
                    im.putpixel(p, final.getpixel(p))
            out.append(im)
        return out
