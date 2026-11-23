import path from "node:path";
import { fileURLToPath } from "node:url";

// Default: sibling workspace tool. Override when the tool lives elsewhere.
export const kitRoot = process.env.MEDIA_KIT_HOME
  ? path.resolve(process.env.MEDIA_KIT_HOME)
  : fileURLToPath(new URL("../../../media-kit/", import.meta.url));
