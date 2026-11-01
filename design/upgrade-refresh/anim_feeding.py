"""Simulates the link animation of direction A: rim fill, diagonal screen wipe, rim retract."""
from PIL import Image

from gen_feeding import (CANDIDATES, CORNERS, NETWORKS, OUT, SCREENS, H, linked_final, slot)

ORIGIN = {"ae2": (1, 1), "scc": (14, 1), "tss": (1, 14), "rs": (14, 14)}
TICK_MS = 50  # one Minecraft tick per frame


def ring():
    """Outer rim pixels in clockwise order, starting at the top-left corner."""
    top = [(x, 1) for x in range(1, 15)]
    right = [(14, y) for y in range(2, 15)]
    bottom = [(x, 14) for x in range(13, 0, -1)]
    left = [(1, y) for y in range(13, 1, -1)]
    return top + right + bottom + left


def ring_distance(origin):
    """Distance of each rim pixel from the origin, walking both ways."""
    path = ring()
    start, n = path.index(origin), len(path)
    return {p: min((i - start) % n, (start - i) % n) for i, p in enumerate(path)}


def frames(network, wipe_repeat=2, previous=None):
    """Link animation into `network`, from the unlinked sprite or from the sprite linked to `previous`."""
    lt, mid, dk = NETWORKS[network]
    unlinked = linked_final(previous) if previous else CANDIDATES["A_corners"]
    final = linked_final(network)
    dist = ring_distance(ORIGIN[network])
    far = max(dist.values())
    rim_color = lambda p: dk if p[0] == 14 or p[1] == 14 else mid
    out = [unlinked] * 12

    # 1. Rim fills from the network's corner in both directions.
    glints = {(cx, cy): (cx + dx, cy + dy) for cx, cy, dx, dy in CORNERS}
    for t in range(far + 1):
        im = unlinked.copy()
        for p, d in dist.items():
            if d <= t:
                im.putpixel(p, rim_color(p))
                if p in glints:
                    im.putpixel(glints[p], final.getpixel(glints[p]))
        out.append(im)
    filled = out[-1]

    # 2. Screen darkens along the diagonal, from top-left and bottom-right to the center;
    #    icon pixels take the network color as the front reaches them.
    reach = lambda x, y: min((x - 3) + (y - 3), (12 - x) + (12 - y))
    for t in range(10):
        im = filled.copy()
        for x in range(3, 13):
            for y in range(3, 13):
                if reach(x, y) <= t:
                    im.putpixel((x, y), final.getpixel((x, y)))
        out += [im] * wipe_repeat
    wiped = out[-1]

    # 3. Rim retracts the way it came, back to the four network corners.
    for t in range(far, -1, -1):
        im = wiped.copy()
        for p, d in dist.items():
            if d >= t:
                im.putpixel(p, final.getpixel(p))
        out.append(im)
    out += [final] * 30
    return out


def render(scale=12):
    nets = list(NETWORKS)
    seqs = {n: frames(n) for n in nets}
    length = max(len(s) for s in seqs.values())
    gap, col = 24, 16 * scale + 24
    sheet = []
    for i in range(length):
        canvas = Image.new("RGBA", (gap + col * len(nets), 16 * scale + 18 * 3 + gap * 3), H("#1b1d22"))
        for j, n in enumerate(nets):
            im = seqs[n][min(i, len(seqs[n]) - 1)]
            x = gap + j * col
            canvas.alpha_composite(im.resize((16 * scale, 16 * scale), Image.NEAREST), (x, gap))
            canvas.alpha_composite(slot(im, 3), (x, gap * 2 + 16 * scale))
            canvas.alpha_composite(slot(im, 1), (x + 18 * 3 + 12, gap * 2 + 16 * scale))
        sheet.append(canvas.convert("RGB"))
    sheet[0].save(OUT / "feeding_link_animation.gif", save_all=True, append_images=sheet[1:],
                  duration=TICK_MS, loop=0)
    # Vertical strip per network, as a Minecraft .mcmeta animation would consume it.
    for n, seq in seqs.items():
        strip = Image.new("RGBA", (16, 16 * len(seq)))
        for k, im in enumerate(seq):
            strip.paste(im, (0, 16 * k))
        strip.save(OUT / f"remote_feeding_upgrade_link_anim_{n}.png")
    return length


if __name__ == "__main__":
    print(render(), "frames")
