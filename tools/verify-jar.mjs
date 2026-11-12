import fs from 'node:fs';
import zlib from 'node:zlib';
import assert from 'node:assert/strict';

const [jarPath, minecraft, loader, java, modId, modVersion] = process.argv.slice(2);
assert(jarPath && modVersion, 'Usage: node verify-jar.mjs jar minecraft loader java modId modVersion');
const zip = fs.readFileSync(jarPath);
let end = zip.length - 22;
while (end >= Math.max(0, zip.length - 65557) && zip.readUInt32LE(end) !== 0x06054b50) end--;
assert(end >= 0, 'Missing ZIP central directory');
const entries = new Map();
let offset = zip.readUInt32LE(end + 16);
for (let index = 0; index < zip.readUInt16LE(end + 10); index++) {
  assert.equal(zip.readUInt32LE(offset), 0x02014b50, 'Invalid ZIP directory entry');
  const method = zip.readUInt16LE(offset + 10), size = zip.readUInt32LE(offset + 20);
  const nameLength = zip.readUInt16LE(offset + 28), extraLength = zip.readUInt16LE(offset + 30), commentLength = zip.readUInt16LE(offset + 32);
  const name = zip.subarray(offset + 46, offset + 46 + nameLength).toString('utf8');
  const local = zip.readUInt32LE(offset + 42);
  assert.equal(zip.readUInt32LE(local), 0x04034b50, 'Invalid local ZIP entry');
  const start = local + 30 + zip.readUInt16LE(local + 26) + zip.readUInt16LE(local + 28);
  const compressed = zip.subarray(start, start + size);
  assert(!entries.has(name), `Duplicate ZIP path: ${name}`);
  assert(method === 0 || method === 8, `Unsupported ZIP compression: ${method}`);
  entries.set(name, method === 8 ? zlib.inflateRawSync(compressed) : compressed);
  offset += 46 + nameLength + extraLength + commentLength;
}
const read = name => { assert(entries.has(name), `Missing ${name}`); return entries.get(name).toString('utf8'); };
const packagePath = 'com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/';
assert(entries.has(packagePath + 'SophisticatedBackpacksDigitalStoragesExpansion.class'), 'Missing entry point');
for (const [name, contents] of entries) {
  if (name.endsWith('.jar')) assert(loader === 'forge' && name.toLowerCase().includes('mixinextras'), `Bundled JAR dependency: ${name}`);
  assert(!['de/scc/', 'appeng/', 'com/refinedmods/', 'com/tom/', 'net/p3pp3rf1y/', 'guideme/'].some(prefix => name.startsWith(prefix)), `Bundled dependency: ${name}`);
  assert(!name.includes('digitalstorage_tests') && !name.includes('UpgradeNetworkGameTests') && !name.includes('RealNetworks.class'), `Bundled integration fixture: ${name}`);
  assert(!name.startsWith(packagePath + 'gametest/'), `Bundled GameTest package: ${name}`);
  if (name.startsWith(packagePath) && name.endsWith('.class')) {
    assert.equal(contents.readUInt32BE(0), 0xcafebabe, `Invalid class: ${name}`);
    assert(contents.readUInt16BE(6) <= Number(java) + 44, `Wrong bytecode target: ${name}`);
  }
}
const metadata = read(loader === 'forge' ? 'META-INF/mods.toml' : 'META-INF/neoforge.mods.toml');
assert(metadata.includes(`modId="${modId}"`), 'Wrong mod ID');
assert(metadata.includes(`version="${modVersion}+${minecraft}"`), 'Wrong mod version');
assert(metadata.includes(minecraft), 'Missing Minecraft version');
assert(!metadata.includes('${'), 'Unexpanded metadata');
JSON.parse(read('pack.mcmeta'));
assert(metadata.includes(`modId="${loader}"`), 'Missing loader dependency');
JSON.parse(read(`data/${modId}/${loader === 'forge' ? 'recipes' : 'recipe'}/unlink_upgrade.json`));
// Check every local model and texture reference, including linked states and animation canvases.
const requireReference = (id, directory, suffix) => {
  if (typeof id === 'string' && id.startsWith(modId + ':')) {
    assert(entries.has(`assets/${modId}/${directory}/${id.slice(modId.length + 1)}${suffix}`), `Missing asset reference: ${id}`);
  }
};
const checkDefinition = model => {
  if (typeof model?.model === 'string') requireReference(model.model, 'models', '.json');
  if (model?.fallback) checkDefinition(model.fallback);
  for (const entry of model?.entries ?? []) checkDefinition(entry.model);
};
for (const [name] of entries) {
  if (name.startsWith(`assets/${modId}/models/`) && name.endsWith('.json')) {
    const model = JSON.parse(read(name));
    requireReference(model.parent, 'models', '.json');
    for (const texture of Object.values(model.textures ?? {})) requireReference(texture, 'textures', '.png');
    for (const override of model.overrides ?? []) requireReference(override.model, 'models', '.json');
  }
  if (name.startsWith(`assets/${modId}/items/`) && name.endsWith('.json')) checkDefinition(JSON.parse(read(name)).model);
}
const recipes = loader === 'forge' ? 'recipes' : 'recipe';
const tag = JSON.parse(read(`data/sophisticatedbackpacks/tags/${loader === 'forge' ? 'items' : 'item'}/upgrade.json`));
const en = JSON.parse(read(`assets/${modId}/lang/en_us.json`)), pt = JSON.parse(read(`assets/${modId}/lang/pt_br.json`));
for (const upgrade of ['deposit','restock','pickup','magnet','refill','feeding','alchemy','pump','crafting']) {
  const item = `remote_${upgrade}_upgrade`;
  assert(tag.values.includes(`${modId}:${item}`), `Missing upgrade tag: ${item}`);
  JSON.parse(read(`data/${modId}/${recipes}/${item}.json`));
  JSON.parse(read(`assets/${modId}/models/item/${item}.json`));
  assert(en[`item.${modId}.${item}`] && pt[`item.${modId}.${item}`], `Missing translation: ${item}`);
  if (minecraft === '26.1.2') {
    const definition = JSON.parse(read(`assets/${modId}/items/${item}.json`));
    assert.equal(definition.model.property, `${modId}:linked_state`);
    assert.equal(definition.model.entries.length, 5, `Missing linked or animation models: ${item}`);
  }
}
const mixins = JSON.parse(read(`${modId}.mixins.json`));
for (const name of ['PickupContextMixin','MagnetUpgradeWrapperAccessor','ItemEntityPickupDelayAccessor','BackpackFluidCapabilityMixin','AlchemyDefinitionsAccessor']) {
  assert(mixins.mixins.includes(name), `Missing Mixin: ${name}`);
  assert(entries.has(packagePath + 'mixin/' + name + '.class'), `Missing Mixin class: ${name}`);
}
console.log(`Verified ${loader} ${minecraft}: nine upgrades, resources, metadata, bytecode and packaging`);
