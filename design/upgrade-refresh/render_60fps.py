"""Renders one upgrade's link animation at 60 fps with the in-game eased timeline.

Usage: render_60fps.py <upgrade> <network> [previous network]   e.g. render_60fps.py feeding scc ae2
"""
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image

from gen_feeding import OUT, H, slot
from timeline import DURATION, frame_at
from upgrade_visuals import Upgrade

FPS = 60
LEAD_IN_MS, HOLD_MS = 500, 900


def compose(im, scale=16):
    canvas = Image.new("RGBA", (16 * scale + 18 * 4 + 72, 16 * scale + 48), H("#1b1d22"))
    canvas.alpha_composite(im.resize((16 * scale, 16 * scale), Image.NEAREST), (24, 24))
    canvas.alpha_composite(slot(im, 4), (16 * scale + 48, 24))
    canvas.alpha_composite(slot(im, 2), (16 * scale + 48, 24 + 18 * 4 + 16))
    return canvas.convert("RGB")


def render(upgrade, network, previous=None):
    strip = Upgrade(upgrade).frames(network, previous)
    total = LEAD_IN_MS + DURATION + HOLD_MS
    with tempfile.TemporaryDirectory() as tmp:
        count = total * FPS // 1000
        for n in range(count):
            # Same as the client: nearest frame, no blending.
            compose(strip[round(frame_at(n * 1000 / FPS - LEAD_IN_MS))]).save(Path(tmp) / f"{n:05d}.png")
        out = OUT / f"{upgrade}_link_animation_{previous or 'unlinked'}_to_{network}_60fps.mp4"
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(FPS), "-i", str(Path(tmp) / "%05d.png"),
                        "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "12", str(out)], check=True)
    return out, count


if __name__ == "__main__":
    print(*render(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None))
