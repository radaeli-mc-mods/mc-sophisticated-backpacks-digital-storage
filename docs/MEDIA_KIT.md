# Media kit renderer

The shared Node.js engine in the sibling workspace directory `../media-kit` exports PNG sections and animated WebP banners for project pages. This addon keeps its manifest, image inputs and thin launchers in `tools/media-kit`. It does not run Minecraft or change the mod. Its sprite inputs preserve the existing P07 visual identity.

## Install and render

Use Node.js 22 or newer. From `tools/media-kit`:

```powershell
Push-Location ../../../media-kit
npm ci
npx playwright install chromium
Pop-Location
node cli.mjs render --manifest manifests/addon.json --all
node cli.mjs render --manifest manifests/addon.json --section collection
```

Dependencies and their lockfile belong to the shared engine; the addon launchers need no local npm installation. If the engine is elsewhere, set `$env:MEDIA_KIT_HOME` to its directory. The pinned Playwright package selects the Chromium revision. Fonts come from the pinned local Shantell Sans package. After installation, rendering requires no network access. If installation and execution use different browser cache locations, set `PLAYWRIGHT_BROWSERS_PATH` to the directory used for installation.

The output contains `logo.png`, `collection.png`, `supplies.png`, `utilities.png`, `linking.png`, and a self-contained `preview.html`. The default destination is `docs/gallery/media-kit`. Individual exports update only their PNG; the HTML preview always includes all manifest sections and therefore requires all their images.

```powershell
node cli.mjs render --manifest manifests/addon.json --section logo --width 1200 --scale 2 --output-dir .cache/custom
node cli.mjs render --manifest manifests/addon.json --section logo --set 'sections.logo.title=My title'
node cli.mjs render --manifest manifests/addon.json --all --overrides custom.json
```

`--width` sets layout width (400–4096 CSS pixels); `--scale` sets output pixel density (>0 through 4). Height grows with content. At scale 2, a 1200 CSS pixel layout produces a 2400 pixel PNG. Arbitrary narrow widths may fail overflow checks; adapt the theme for smaller layouts.

## Content manifest

Top-level fields are `theme`, `outputDir`, `sections`, optional `blockAssets`, and `provenanceFile` (required by `prepare-assets`). Image paths, asset JAR paths and the default output directory resolve relative to the manifest; CLI output paths resolve relative to the working directory.

`sections` is an object keyed by stable section IDs. Each section has `layout` (`hero`, `features`, `gallery`, or `alternating`), optional `title`, `eyebrow`, `description` and `note`. Only a hero requires a title. Sections without header text render entries directly. `alternating` uses a vertical list with odd images on the left and even images on the right. Optional `introImage` and `introAlt` insert a full-width image between the header and entries; `outroImage`, `outroAlt` and `outroCaption` add a captioned image after the entries. Linking uses this closing block for the upgrade link states sheet and explains that appearance changes with the linked network. Alternating rows use configurable odd/even backgrounds, padding, gap and inset depth tokens. A hero also has `image` and `alt`. Other layouts have a nonempty `entries` array:

```json
{
  "id": "example",
  "title": "Example upgrade",
  "description": "A short player-facing description.",
  "bullets": ["First benefit", "Second benefit"],
  "image": "../assets/example.png",
  "alt": "Example upgrade icon",
  "pixelated": true,
  "secondaryImage": "../assets/detail.png",
  "secondaryAlt": "Optional detail"
}
```

Omit secondary images when unused. `pixelated` preserves sprite pixels; other images use normal browser scaling. Text accepts `**bold**` and `[accent]colored emphasis[/accent]`. HTML is escaped. Images must be local files.

`--overrides` recursively merges an object into the manifest; arrays replace arrays. Repeated `--set path=value` applies afterward, with dot paths including array indices, such as `sections.collection.entries.0.title`. Values are parsed as JSON when valid and otherwise treated as strings. Prototype-related keys are rejected. Explicit width, scale and theme CLI options override their defaults.

## Themes and reuse

The built-in `digital` theme lives in the shared engine at `../media-kit/themes/digital` (relative to the addon root). Copy it to a custom directory and pass the absolute directory with `--theme`; named themes resolve inside the shared engine. Set the manifest `theme` to an absolute custom directory when needed. `theme.json` defines default width and CSS custom-property tokens; `theme.css` defines the complete visual composition. Colors, typography, image sizes, padding, spacing, alignment, borders, shadows and gallery columns are editable here. The rendering templates contain structure, not addon names.

For another mod, keep a manifest and images in that project and invoke the same shared engine from that project's root:

```powershell
node ../media-kit/cli.mjs render --manifest tools/media-kit/manifests/mod.json --all
```

The shared CLI requires `--manifest` and has no default addon content. The addon launcher retains its own default manifest. A manifest's image, JAR and output paths remain relative to that manifest even when the engine is outside the project. The compositor does not require `blockAssets`; local images are sufficient.

## Preparing endpoint images

```powershell
node cli.mjs prepare-assets --manifest manifests/addon.json
```

This optional step reads the specified local 1.21.1 JARs and generates transparent isometric images with Three.js. Recipes specify `jar`, `model`, optional additional `parts`, `output`, and optional `rotation` (X/Y/Z degrees applied to the assembled model before framing). The shipped AE2 recipe combines the antenna, chassis and status components. Parent models, texture aliases, cuboids, UV rotations and element rotations used by the five recipes are resolved. Animated textures use their first frame. Unsupported custom loaders and missing model or texture resources fail explicitly. This is a focused asset helper, not a complete Minecraft renderer: game lighting, custom rendering code and animation are not reproduced.

Prepared images remain local inputs, so ordinary section exports do not require JARs. The manifest sets `provenanceFile` to `../assets/blocks/provenance.json`; this path resolves relative to the manifest. That file records recipes, source filenames, states and SHA-256 hashes. Dependency artwork belongs to its original authors; the helper does not bundle their JARs. The logo is a copy of the existing transparent project artwork; upgrade icons are copies of the existing gallery sprites.

## Verification

```powershell
node verify.mjs
```

This generates fresh static exports in its cache, checks customization and failure paths, and creates reduced 800 pixel previews under `.cache/verification`. Visually inspect these alongside the original PNGs. Fonts and images finish loading before capture; horizontal overflow is rejected. A successful render does not establish gameplay behavior or publish any project page.

## Animated hero export

Set `format: "webp"` on a hero section whose `image` points to a local animated WebP. The addon uses `docs/gallery/logo_animated.webp` directly. The renderer decodes every source frame, fits it into the theme’s hero image box, and composites it over the browser-rendered text and background. It preserves frame delays, loop count, alpha and export scale, using lossless WebP. The output is `logo.webp`; `logo.png` remains a deterministic first-frame fallback. `preview.html` embeds the animated source within the same banner layout. Other sections default to PNG; `format: "png"` forces a static export. Animation currently applies to the hero’s primary image only.
