import fs from "node:fs/promises";
import path from "node:path";
import { createHash } from "node:crypto";
import { unzipSync, strFromU8 } from "fflate";
import { chromium } from "playwright";
export async function prepare(manifest, base, root) {
  const browser = await chromium.launch();
  const provenance = [];
  try {
    for (const recipe of manifest.blockAssets) {
      const jar = await fs.readFile(path.resolve(base, recipe.jar)),
        zip = unzipSync(jar);
      const ref = (id, type, ext) => {
        const [ns, p] = (id.includes(":") ? id : "minecraft:" + id).split(":");
        return `assets/${ns}/${type}/${p}.${ext}`;
      };
      function model(id, seen = []) {
        if (!id.includes(":")) id = "minecraft:" + id;
        if (id === "minecraft:block/cube")
          return {
            elements: [
              {
                from: [0, 0, 0],
                to: [16, 16, 16],
                faces: Object.fromEntries(
                  ["north", "south", "east", "west", "up", "down"].map((s) => [
                    s,
                    { texture: "#" + s, uv: [0, 0, 16, 16] },
                  ]),
                ),
              },
            ],
          };
        if (seen.includes(id)) throw Error(`Model cycle ${id}`);
        const p = ref(id, "models", "json");
        if (!zip[p]) {
          if (
            id === "minecraft:block/block" ||
            id === "minecraft:block/cube_all"
          )
            return {};
          throw Error(`Missing model ${p}`);
        }
        const own = JSON.parse(strFromU8(zip[p]));
        if (own.loader) throw Error(`Unsupported loader in ${id}`);
        const parent = own.parent ? model(own.parent, [...seen, id]) : {};
        return {
          ...parent,
          ...own,
          textures: { ...parent.textures, ...own.textures },
        };
      }
      const m = model(recipe.model);
      for (const id of recipe.parts ?? []) {
        const part = model(id);
        m.elements.push(...part.elements);
        Object.assign(m.textures, part.textures);
      }
      if (!m.elements?.length) throw Error(`No cuboids in ${recipe.model}`);
      const textures = {};
      for (const [key, val] of Object.entries(m.textures)) {
        let id = val;
        const seen = new Set();
        while (id.startsWith("#")) {
          if (seen.has(id)) throw Error(`Texture cycle ${id}`);
          seen.add(id);
          id = m.textures[id.slice(1)];
          if (!id) throw Error(`Missing texture alias ${val}`);
        }
        const file = ref(id, "textures", "png");
        if (!zip[file]) throw Error(`Missing texture ${file}`);
        textures[key] =
          `data:image/png;base64,${Buffer.from(zip[file]).toString("base64")}`;
      }
      const page = await browser.newPage({
        viewport: { width: 512, height: 512 },
      });
      await page.setContent(
        '<html><body style="margin:0"><canvas></canvas></body></html>',
      );
      // Bundle the two local Three modules into importable data URLs; no server or network.
      const core = Buffer.from(
        await fs.readFile(
          path.join(root, "node_modules/three/build/three.core.js"),
        ),
      ).toString("base64");
      const module = (
        await fs.readFile(
          path.join(root, "node_modules/three/build/three.module.js"),
          "utf8",
        )
      ).replaceAll(
        "'./three.core.js'",
        `'data:text/javascript;base64,${core}'`,
      );
      const url =
        "data:text/javascript;base64," + Buffer.from(module).toString("base64");
      await page.evaluate(
        async ({ url, m, textures, rotation }) => {
          const T = await import(url);
          const scene = new T.Scene(),
            group = new T.Group();
          scene.add(group);
          const loader = new T.TextureLoader(),
            tex = {};
          for (const [k, v] of Object.entries(textures)) {
            const im = await loader.loadAsync(v);
            im.magFilter = T.NearestFilter;
            im.minFilter = T.NearestFilter;
            im.colorSpace = T.SRGBColorSpace;
            tex[k] = im;
          }
          const normals = {
            north: [0, 0, -1],
            south: [0, 0, 1],
            east: [1, 0, 0],
            west: [-1, 0, 0],
            up: [0, 1, 0],
            down: [0, -1, 0],
          };
          for (const e of m.elements) {
            const f = e.from,
              t = e.to;
            const faces = {
              north: [
                [t[0], t[1], f[2]],
                [f[0], t[1], f[2]],
                [f[0], f[1], f[2]],
                [t[0], f[1], f[2]],
              ],
              south: [
                [f[0], t[1], t[2]],
                [t[0], t[1], t[2]],
                [t[0], f[1], t[2]],
                [f[0], f[1], t[2]],
              ],
              east: [
                [t[0], t[1], t[2]],
                [t[0], t[1], f[2]],
                [t[0], f[1], f[2]],
                [t[0], f[1], t[2]],
              ],
              west: [
                [f[0], t[1], f[2]],
                [f[0], t[1], t[2]],
                [f[0], f[1], t[2]],
                [f[0], f[1], f[2]],
              ],
              up: [
                [f[0], t[1], f[2]],
                [t[0], t[1], f[2]],
                [t[0], t[1], t[2]],
                [f[0], t[1], t[2]],
              ],
              down: [
                [f[0], f[1], t[2]],
                [t[0], f[1], t[2]],
                [t[0], f[1], f[2]],
                [f[0], f[1], f[2]],
              ],
            };
            for (const [side, face] of Object.entries(e.faces)) {
              if (!face.texture) continue;
              const texture = tex[face.texture.replace("#", "")];
              if (!texture) throw Error("Unsupported texture " + face.texture);
              const image = texture.image;
              const uv = face.uv ?? [0, 0, 16, 16];
              const frameH = Math.min(image.width, image.height);
              const vv = frameH / image.height;
              let uvs = [
                [uv[0] / 16, 1 - (uv[1] / 16) * vv],
                [uv[2] / 16, 1 - (uv[1] / 16) * vv],
                [uv[2] / 16, 1 - (uv[3] / 16) * vv],
                [uv[0] / 16, 1 - (uv[3] / 16) * vv],
              ];
              for (let i = 0; i < (face.rotation ?? 0) / 90; i++)
                uvs.unshift(uvs.pop());
              const geo = new T.BufferGeometry();
              geo.setAttribute(
                "position",
                new T.Float32BufferAttribute(faces[side].flat(), 3),
              );
              geo.setAttribute(
                "uv",
                new T.Float32BufferAttribute(uvs.flat(), 2),
              );
              geo.setIndex([0, 1, 2, 0, 2, 3]);
              geo.computeVertexNormals();
              const shade = {
                up: 1,
                down: 0.5,
                north: 0.82,
                south: 0.82,
                east: 0.68,
                west: 0.68,
              }[side];
              const mesh = new T.Mesh(
                geo,
                new T.MeshBasicMaterial({
                  map: texture,
                  transparent: true,
                  alphaTest: 0.1,
                  side: T.DoubleSide,
                  color: new T.Color(shade, shade, shade),
                }),
              );
              if (e.rotation) {
                const r = e.rotation,
                  o = new T.Vector3(...r.origin);
                mesh.position.sub(o);
                const pivot = new T.Group();
                pivot.position.copy(o);
                pivot.rotation[r.axis] = (r.angle * Math.PI) / 180;
                pivot.add(mesh);
                group.add(pivot);
              } else group.add(mesh);
            }
          }
          if (rotation)
            group.rotation.set(...rotation.map((deg) => (deg * Math.PI) / 180));
          const box = new T.Box3().setFromObject(group),
            center = box.getCenter(new T.Vector3()),
            size = box.getSize(new T.Vector3()),
            span = Math.max(size.x, size.y, size.z) * 1.8;
          group.position.sub(center);
          const camera = new T.OrthographicCamera(
            -span / 2,
            span / 2,
            span / 2,
            -span / 2,
            0.1,
            1000,
          );
          camera.position.set(40, 30, 40);
          camera.lookAt(0, 0, 0);
          const renderer = new T.WebGLRenderer({
            canvas: document.querySelector("canvas"),
            alpha: true,
            preserveDrawingBuffer: true,
            antialias: true,
          });
          renderer.setSize(512, 512);
          renderer.render(scene, camera);
        },
        { url, m, textures, rotation: recipe.rotation },
      );
      const out = path.resolve(base, recipe.output);
      await fs.mkdir(path.dirname(out), { recursive: true });
      await page
        .locator("canvas")
        .screenshot({ path: out, omitBackground: true });
      await page.close();
      provenance.push({
        ...recipe,
        sha256: createHash("sha256").update(jar).digest("hex"),
      });
      console.log(`Prepared ${recipe.output}`);
    }
    await fs.writeFile(
      path.resolve(base, "../assets/blocks/provenance.json"),
      JSON.stringify(provenance, null, 2) + "\n",
    );
  } finally {
    await browser.close();
  }
}
