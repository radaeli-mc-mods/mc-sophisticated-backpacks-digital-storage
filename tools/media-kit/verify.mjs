// Integration smoke checks: real browser exports and explicit failure paths.
import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";
import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { kitRoot } from "./shared.mjs";
const { chromium } = createRequire(path.join(kitRoot, "package.json"))(
  "playwright",
);
const root = path.dirname(fileURLToPath(import.meta.url));
process.chdir(root);
const cache = path.join(root, ".cache/verification");
await fs.mkdir(cache, { recursive: true });
function run(args, expected = 0) {
  const r = spawnSync(
    process.execPath,
    [
      "cli.mjs",
      "render",
      "--manifest",
      "manifests/addon.json",
      "--set",
      "sections.logo.format=png",
      ...args,
    ],
    { encoding: "utf8" },
  );
  assert.equal(r.status, expected, r.stderr);
  return r;
}
const output = ["--output-dir", cache];
run([
  "--section",
  "logo",
  "--width",
  "1200",
  "--scale",
  "2",
  "--set",
  "sections.logo.title=Custom title",
  ...output,
]);
run(["--section", "missing", ...output], 1);
run(
  ["--section", "logo", "--set", "sections.logo.layout=unknown", ...output],
  1,
);
run(
  ["--section", "logo", "--set", "sections.logo.image=missing.png", ...output],
  1,
);
await fs.writeFile(
  path.join(cache, "overrides.json"),
  JSON.stringify({
    sections: {
      logo: {
        title: "Override title",
        description: "Custom description",
        image: "../assets/blocks/rs.png",
      },
    },
  }),
);
run([
  "--section",
  "logo",
  "--overrides",
  path.join(cache, "overrides.json"),
  ...output,
]);
const theme = path.join(cache, "theme");
await fs.mkdir(theme, { recursive: true });
const t = JSON.parse(
  await fs.readFile(path.join(kitRoot, "themes/digital/theme.json")),
);
t.tokens.background = "#302035";
t.tokens.padding = "80px";
await fs.writeFile(path.join(theme, "theme.json"), JSON.stringify(t));
await fs.copyFile(
  path.join(kitRoot, "themes/digital/theme.css"),
  path.join(theme, "theme.css"),
);
run(["--section", "logo", "--theme", theme, ...output]);
const font = path.join(
  kitRoot,
  "node_modules/@fontsource/shantell-sans/files/shantell-sans-latin-400-normal.woff2",
);
await fs.rename(font, font + ".verification");
try {
  run(["--section", "logo", ...output], 1);
} finally {
  await fs.rename(font + ".verification", font);
}
run(["--all", ...output]);
const browser = await chromium.launch();
try {
  const page = await browser.newPage({
    viewport: { width: 800, height: 1000 },
  });
  await page.setContent(
    await fs.readFile(path.join(cache, "preview.html"), "utf8"),
  );
  await page.evaluate(() => document.fonts.ready);
  assert.equal(
    await page.locator(".features header").count(),
    0,
    "Upgrade sections must start with entries",
  );
  assert.equal(
    await page.locator(".alternating .outro img").count(),
    1,
    "Link states image is required",
  );
  assert.deepEqual(
    await page
      .locator(".alternating article")
      .evaluateAll((rows) =>
        rows.map((row) => getComputedStyle(row).flexDirection),
      ),
    ["row", "row-reverse", "row", "row-reverse", "row"],
  );
  assert.ok(
    await page.evaluate(() => document.fonts.check("31px Shantell")),
    "Shantell Sans must load",
  );
  for (const id of ["logo", "collection", "supplies", "utilities", "linking"]) {
    const png = await fs.readFile(path.join(cache, id + ".png"));
    await page.setContent(
      `<style>body{margin:0}img{display:block;width:800px}</style><img src="data:image/png;base64,${png.toString("base64")}">`,
    );
    await page.locator("img").evaluate((i) => i.decode());
    await page.locator("img").screenshot({
      path: path.join(cache, id + "-800.png"),
      omitBackground: true,
    });
  }
} finally {
  await browser.close();
}
console.log(
  "Passed: section export, width/scale, text/image overrides, theme, missing section/image/font and invalid layout. Reduced previews are in .cache/verification.",
);
