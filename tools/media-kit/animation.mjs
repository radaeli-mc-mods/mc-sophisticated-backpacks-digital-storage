import fs from "node:fs/promises";
import path from "node:path";
import sharp from "sharp";

// Compose decoded frames against one browser-rendered background. No wall-clock
// capture: the source frame delays and loop count remain exact.
export async function exportAnimation(page, section, base, out, id, scale) {
  if (section.layout !== "hero")
    throw Error("Animated export requires a hero layout");
  const source = await fs.readFile(path.resolve(base, section.image));
  const meta = await sharp(source, { animated: true }).metadata();
  if (!(meta.pages > 1)) throw Error(`${id}: animated WebP input required`);
  const image = page.locator("section > img");
  const box = await image.boundingBox();
  const card = await page.locator("section").boundingBox();
  await image.evaluate((el) => (el.style.visibility = "hidden"));
  try {
    const background = await page
      .locator("section")
      .screenshot({ omitBackground: true });
    const frames = [];
    for (let frame = 0; frame < meta.pages; frame++) {
      const overlay = await sharp(source, { page: frame, pages: 1 })
        .resize(Math.round(box.width * scale), Math.round(box.height * scale), {
          fit: "contain",
          background: "#00000000",
        })
        .png()
        .toBuffer();
      const composed = await sharp(background)
        .composite([
          {
            input: overlay,
            left: Math.round((box.x - card.x) * scale),
            top: Math.round((box.y - card.y) * scale),
          },
        ])
        .png()
        .toBuffer();
      if (frame === 0)
        await fs.writeFile(path.join(out, `${id}.png`), composed);
      frames.push(composed);
    }
    await sharp(frames, { join: { animated: true } })
      .webp({
        lossless: true,
        effort: 2,
        loop: meta.loop ?? 0,
        delay: meta.delay,
      })
      .toFile(path.join(out, `${id}.webp`));
    console.log(`Animated ${id}: ${meta.pages} frames`);
  } finally {
    await image.evaluate((el) => (el.style.visibility = ""));
  }
}
