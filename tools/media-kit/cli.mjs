import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright";
const root = path.dirname(fileURLToPath(import.meta.url));
const esc = (s) =>
  String(s ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const rich = (s) =>
  esc(s)
    .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
    .replace(/\[accent\](.+?)\[\/accent\]/g, "<em>$1</em>");
const read = async (p) => JSON.parse(await fs.readFile(p, "utf8"));
function merge(a, b) {
  for (const [k, v] of Object.entries(b)) {
    if (["__proto__", "constructor", "prototype"].includes(k))
      throw Error("Unsafe field");
    a[k] =
      v && typeof v === "object" && !Array.isArray(v)
        ? merge(a[k] ?? {}, v)
        : v;
  }
  return a;
}
const args = process.argv.slice(2),
  command = args.shift(),
  opts = {},
  sets = [];
while (args.length) {
  const k = args.shift();
  if (
    ![
      "--all",
      "--manifest",
      "--section",
      "--theme",
      "--width",
      "--scale",
      "--output-dir",
      "--overrides",
      "--set",
    ].includes(k)
  )
    throw Error(`Unknown option ${k}`);
  if (!k.startsWith("--")) throw Error(`Unknown argument ${k}`);
  if (k === "--all") opts.all = true;
  else {
    const v = args.shift();
    if (v === undefined) throw Error(`Missing value ${k}`);
    if (k === "--set") sets.push(v);
    else opts[k.slice(2)] = v;
  }
}
async function main() {
  if (command === "help" || !command) {
    console.log(
      "render --manifest file (--all | --section id) [--theme digital] [--width px] [--scale n] [--output-dir dir] [--overrides file] [--set sections.id.title=value]\nprepare-assets --manifest file",
    );
    return;
  }
  const mp = path.resolve(
      opts.manifest ?? path.join(root, "manifests/addon.json"),
    ),
    base = path.dirname(mp);
  let m = await read(mp);
  if (opts.overrides) m = merge(m, await read(path.resolve(opts.overrides)));
  for (const set of sets) {
    const eq = set.indexOf("=");
    if (eq < 1) throw Error("--set requires path=value");
    const keys = set.slice(0, eq).split(".");
    if (keys.some((k) => ["__proto__", "constructor", "prototype"].includes(k)))
      throw Error("Unsafe field");
    let obj = m;
    for (const k of keys.slice(0, -1)) {
      if (!obj[k] || typeof obj[k] !== "object")
        throw Error(`Unknown field ${k}`);
      obj = obj[k];
    }
    let v = set.slice(eq + 1);
    try {
      v = JSON.parse(v);
    } catch {}
    obj[keys.at(-1)] = v;
  }
  if (command === "prepare-assets") {
    const { prepare } = await import("./prepare-assets.mjs");
    await prepare(m, base, root);
    return;
  }
  if (command !== "render") throw Error(`Unknown command ${command}`);
  const themeDir = path.resolve(
      root,
      "themes",
      opts.theme ?? m.theme ?? "digital",
    ),
    t = await read(path.join(themeDir, "theme.json"));
  const width = Number(opts.width ?? t.width),
    scale = Number(opts.scale ?? 1);
  if (
    !Number.isFinite(width) ||
    width < 400 ||
    width > 4096 ||
    !Number.isFinite(scale) ||
    scale <= 0 ||
    scale > 4
  )
    throw Error("width must be 400–4096; scale must be >0 and <=4");
  const out = path.resolve(
    opts["output-dir"] ?? path.resolve(base, m.outputDir),
  );
  const ids = opts.all ? Object.keys(m.sections) : [opts.section];
  if (!ids[0]) throw Error("Specify --all or --section");
  for (const id of ids)
    if (!m.sections[id]) throw Error(`Unknown section ${id}`);
  async function data(p) {
    const full = path.resolve(base, p);
    try {
      return `data:image/${path.extname(p).slice(1)};base64,${(await fs.readFile(full)).toString("base64")}`;
    } catch {
      throw Error(`Missing image: ${p}`);
    }
  }
  const font = await fs.readFile(
    path.join(
      root,
      "node_modules/@fontsource/shantell-sans/files/shantell-sans-latin-400-normal.woff2",
    ),
  );
  const bold = await fs.readFile(
    path.join(
      root,
      "node_modules/@fontsource/shantell-sans/files/shantell-sans-latin-700-normal.woff2",
    ),
  );
  const css = await fs.readFile(path.join(themeDir, "theme.css"), "utf8");
  const vars = Object.entries(t.tokens)
    .map(([k, v]) => `--${k}:${v}`)
    .join(";");
  const head = `<meta charset="utf-8"><style>@font-face{font-family:Shantell;src:url(data:font/woff2;base64,${font.toString("base64")})} @font-face{font-family:Shantell;font-weight:700;src:url(data:font/woff2;base64,${bold.toString("base64")})}:root{${vars};--width:${width}px}${css}</style>`;
  async function section(s, id) {
    if (!["hero", "features", "gallery", "alternating"].includes(s.layout))
      throw Error(`${id}.layout is invalid`);
    if (
      (s.layout === "hero" || s.title !== undefined) &&
      typeof s.title !== "string"
    )
      throw Error(`${id}.title must be a string`);
    const img = async (e) =>
      `<img class="${e.pixelated ? "pixelated" : ""}" src="${await data(e.image)}" alt="${esc(e.alt ?? e.title)}">`;
    if (s.layout === "hero")
      return `<section class="card hero">${await img(s)}<div>${s.eyebrow ? `<div class="eyebrow">${rich(s.eyebrow)}</div>` : ""}<h1>${rich(s.title)}</h1><p>${rich(s.description)}</p></div></section>`;
    if (!Array.isArray(s.entries) || !s.entries.length)
      throw Error(`${id}.entries must be nonempty`);
    return `<section class="card ${s.layout}">${s.title || s.description || s.eyebrow ? `<header>${s.eyebrow ? `<div class="eyebrow">${rich(s.eyebrow)}</div>` : ""}${s.title ? `<h1>${rich(s.title)}</h1>` : ""}${s.description ? `<p>${rich(s.description)}</p>` : ""}</header>` : ""}${s.introImage ? `<img class="intro-image pixelated" src="${await data(s.introImage)}" alt="${esc(s.introAlt ?? "")}">` : ""}<div class="entries">${(
      await Promise.all(
        s.entries.map(async (e, i) => {
          if (typeof e.title !== "string" || !e.image)
            throw Error(`${id}.entries.${i}: title and image required`);
          if (e.bullets && !Array.isArray(e.bullets))
            throw Error(`${id}.entries.${i}.bullets must be an array`);
          return `<article>${await img(e)}<div><h2>${rich(e.title)}</h2><p>${rich(e.description)}</p>${e.bullets?.length ? `<ul>${e.bullets.map((b) => `<li>${rich(b)}</li>`).join("")}</ul>` : ""}</div>${e.secondaryImage ? `<img class="secondary" src="${await data(e.secondaryImage)}" alt="${esc(e.secondaryAlt ?? "")}">` : ""}</article>`;
        }),
      )
    ).join(
      "",
    )}</div>${s.outroImage ? `<figure class="outro">${s.outroCaption ? `<figcaption>${rich(s.outroCaption)}</figcaption>` : ""}<img class="pixelated" src="${await data(s.outroImage)}" alt="${esc(s.outroAlt ?? "")}"></figure>` : ""}${s.note ? `<footer>${rich(s.note)}</footer>` : ""}</section>`;
  }
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage({
      viewport: { width, height: 1000 },
      deviceScaleFactor: scale,
    });
    const htmls = {};
    await fs.mkdir(out, { recursive: true });
    for (const id of ids) {
      const html = head + (await section(m.sections[id], id));
      await page.setContent(html);
      await page.evaluate(async () => {
        await document.fonts.ready;
        await Promise.all([...document.images].map((i) => i.decode()));
      });
      const bad = await page.evaluate(() =>
        [
          ...document.querySelectorAll("section,article,header,h1,h2,p,li"),
        ].some((e) => e.scrollWidth > e.clientWidth + 1),
      );
      if (bad) throw Error(`${id}: horizontal overflow`);
      const format = m.sections[id].format ?? "png";
      if (!["png", "webp"].includes(format))
        throw Error(`${id}: unsupported format ${format}`);
      if (format === "webp") {
        const { exportAnimation } = await import("./animation.mjs");
        await exportAnimation(page, m.sections[id], base, out, id, scale);
      } else
        await page.locator("section").screenshot({
          path: path.join(out, `${id}.png`),
          omitBackground: true,
          animations: "disabled",
        });
      htmls[id] = html;
      console.log(`Rendered ${id}`);
    }
    // Rebuild the complete preview even when exporting only one section.
    const preview = [];
    for (const [id, s] of Object.entries(m.sections))
      preview.push(await section(s, id));
    await fs.writeFile(
      path.join(out, "preview.html"),
      `<!doctype html>${head}<style>body{display:grid;gap:24px;padding:24px;background:#080c12}.card{max-width:100%}</style>${preview.join("")}`,
    );
  } finally {
    await browser.close();
  }
}
main().catch((e) => {
  console.error(`Media kit: ${e.message}`);
  process.exitCode = 1;
});
