import path from "node:path";
import { pathToFileURL, fileURLToPath } from "node:url";
import { kitRoot } from "./shared.mjs";

if (!process.argv.includes("--manifest"))
  process.argv.push(
    "--manifest",
    fileURLToPath(new URL("./manifests/addon.json", import.meta.url)),
  );
try {
  await import(pathToFileURL(path.join(kitRoot, "cli.mjs")).href);
} catch (error) {
  console.error(
    `Media kit: could not load shared tool at ${kitRoot}: ${error.message}`,
  );
  process.exitCode = 1;
}
