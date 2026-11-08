#!/usr/bin/env node
/**
 * Headless Blockbench API runner for Riftworld fauna.
 *
 * Serves a built Blockbench web app locally, loads it in headless Chromium,
 * injects riftworld_fauna_plugin.js, and writes the generated artifacts.
 *
 * Usage:
 *   BLOCKBENCH_ROOT=/path/to/blockbench-5.2.2 node tools/blockbench/run_headless.mjs
 *   node tools/blockbench/run_headless.mjs --ids=porcelain_crane,ash_moth
 */
import fs from 'node:fs';
import path from 'node:path';
import http from 'node:http';
import os from 'node:os';
import { fileURLToPath } from 'node:url';
import puppeteer from 'puppeteer-core';
import chromium, { inflate } from '@sparticuz/chromium';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '../..');

function arg(name, fallback) {
  const hit = process.argv.slice(2).find(a => a.startsWith('--' + name + '='));
  return hit ? hit.slice(name.length + 3) : fallback;
}

const blockbenchRoot = path.resolve(arg('blockbench-root', process.env.BLOCKBENCH_ROOT || path.join(repoRoot, '../.cache/riftworld-blockbench/blockbench-5.2.2')));
const outRoot = path.resolve(arg('out', repoRoot));
const pluginPath = path.resolve(arg('plugin', path.join(__dirname, 'riftworld_fauna_plugin.js')));
const idsArg = arg('ids', '');
const ids = idsArg ? idsArg.split(',').map(s => s.trim()).filter(Boolean) : null;

if (!fs.existsSync(path.join(blockbenchRoot, 'dist/bundle.js'))) {
  console.error('Missing built Blockbench web bundle: ' + path.join(blockbenchRoot, 'dist/bundle.js'));
  console.error('Build it first: npm ci --ignore-scripts && node build.js --target=web');
  process.exit(2);
}

const mime = {
  '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.css': 'text/css',
  '.json': 'application/json', '.png': 'image/png', '.svg': 'image/svg+xml',
  '.webmanifest': 'application/manifest+json', '.woff': 'font/woff', '.woff2': 'font/woff2',
  '.ttf': 'font/ttf', '.bbtheme': 'text/plain', '.bbkeymap': 'application/json', '.wasm': 'application/wasm'
};

const server = http.createServer((req, res) => {
  let p = decodeURIComponent((req.url || '/').split('?')[0]);
  if (p === '/') p = '/index.html';
  if (p === '/service_worker.js') {
    res.writeHead(200, { 'Content-Type': 'text/javascript' });
    res.end('/* service worker disabled for headless generation */');
    return;
  }
  const file = path.join(blockbenchRoot, p);
  if (!file.startsWith(blockbenchRoot) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
    res.writeHead(404); res.end('not found'); return;
  }
  res.writeHead(200, {
    'Content-Type': mime[path.extname(file)] || 'application/octet-stream',
    'Access-Control-Allow-Origin': '*',
    'Cache-Control': 'no-store'
  });
  fs.createReadStream(file).pipe(res);
});

await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
const port = server.address().port;

// @sparticuz/chromium only auto-extracts its AL2023 NSS/NSPR libs on Amazon Linux.
// Extract them explicitly so the same runner works in this Debian-like sandbox.
const chromiumEntry = fileURLToPath(import.meta.resolve('@sparticuz/chromium'));
const binDir = path.resolve(path.dirname(chromiumEntry), '../bin');
const tmp = os.tmpdir();
for (const name of ['al2023', 'fonts', 'swiftshader']) {
  const archive = path.join(binDir, name + '.tar.br');
  const marker = name === 'al2023' ? path.join(tmp, 'al2023/lib/libnss3.so')
    : name === 'fonts' ? path.join(tmp, 'fonts/fonts.conf')
    : path.join(tmp, 'libEGL.so');
  if (fs.existsSync(archive) && !fs.existsSync(marker)) await inflate(archive);
}

const executablePath = await chromium.executablePath();
const env = {
  ...process.env,
  LD_LIBRARY_PATH: [path.join(tmp, 'al2023/lib'), tmp, process.env.LD_LIBRARY_PATH].filter(Boolean).join(':'),
  FONTCONFIG_PATH: path.join(tmp, 'fonts'),
  HOME: process.env.HOME || tmp
};

const browser = await puppeteer.launch({
  executablePath,
  args: [...chromium.args.filter(a => !a.startsWith('--headless')), '--disable-dev-shm-usage'],
  headless: 'shell',
  env,
  protocolTimeout: 240000
});

const pageErrors = [];
try {
  const page = await browser.newPage();
  await page.setRequestInterception(true);
  page.on('request', req => {
    const url = new URL(req.url());
    if (url.hostname === '127.0.0.1' || url.hostname === 'localhost' || url.protocol === 'data:' || url.protocol === 'blob:') {
      req.continue();
    } else {
      req.abort();
    }
  });
  page.on('pageerror', err => pageErrors.push(String(err)));
  await page.evaluateOnNewDocument(() => {
    try {
      localStorage.setItem('startups', '10');
      localStorage.setItem('last_version', '5.2.2');
    } catch {}
  });
  await page.goto(`http://127.0.0.1:${port}/`, { waitUntil: 'domcontentloaded', timeout: 120000 });
  await page.waitForFunction(() => window.Blockbench && window.Blockbench.setup_successful === true, { timeout: 120000 });
  await page.addScriptTag({ path: pluginPath });
  await page.waitForFunction(() => window.RiftworldFauna && typeof window.RiftworldFauna.generateAll === 'function', { timeout: 30000 });

  const artifacts = await page.evaluate(async selectedIds => {
    return await window.RiftworldFauna.generateAll(selectedIds ? { ids: selectedIds } : {});
  }, ids);

  // Prove the emitted .bbmodel files are editable Blockbench projects: parse each
  // one back through Codecs.project and re-export Bedrock geometry via the API.
  const roundtrips = [];
  for (const art of artifacts) {
    const rt = await page.evaluate(async ({ bbmodel, expected }) => {
      newProject(Formats.bedrock_old);
      Codecs.project.parse(JSON.parse(bbmodel));
      const tex = Project.textures[0];
      for (let i = 0; i < 100; i++) {
        if (tex && tex.width === expected.texture_width && tex.height === expected.texture_height && !tex.error) break;
        await new Promise(r => setTimeout(r, 20));
      }
      const bedrock = Codecs.bedrock_old.compile({ raw: true });
      return {
        bones: bedrock.bones.length,
        cubes: bedrock.bones.reduce((n, b) => n + (b.cubes ? b.cubes.length : 0), 0),
        texture_width: Project.texture_width,
        texture_height: Project.texture_height,
        texture_loaded: !!(tex && tex.width === expected.texture_width && tex.height === expected.texture_height && !tex.error),
        animations: Animator.animations.length
      };
    }, { bbmodel: art.bbmodel, expected: art.geo });
    if (rt.bones !== art.geo.bones.length || rt.cubes !== art.stats.cubes || !rt.texture_loaded || rt.animations < 1) {
      throw new Error('bbmodel round-trip failed for ' + art.id + ': ' + JSON.stringify(rt));
    }
    roundtrips.push({ id: art.id, status: 'ok', ...rt });
  }

  const dirs = [
    path.join(outRoot, 'blockbench/riftworld_fauna'),
    path.join(outRoot, 'mod/src/main/resources/assets/riftworld/geo'),
    path.join(outRoot, 'mod/src/main/resources/assets/riftworld/textures/entity'),
    path.join(outRoot, 'reports/blockbench_first_batch')
  ];
  for (const dir of dirs) fs.mkdirSync(dir, { recursive: true });

  const report = {
    generator: 'Blockbench 5.2.2 web API via headless Chromium',
    blockbench_root: blockbenchRoot,
    plugin: path.relative(repoRoot, pluginPath),
    models: []
  };

  for (const art of artifacts) {
    const bbPath = path.join(outRoot, 'blockbench/riftworld_fauna', art.id + '.bbmodel');
    const geoPath = path.join(outRoot, 'mod/src/main/resources/assets/riftworld/geo', art.id + '.json');
    const texPath = path.join(outRoot, 'mod/src/main/resources/assets/riftworld/textures/entity', art.id + '.png');
    const statsPath = path.join(outRoot, 'reports/blockbench_first_batch', art.id + '_stats.json');
    const bedrockPath = path.join(outRoot, 'reports/blockbench_first_batch', art.id + '.bedrock.geo.json');

    fs.writeFileSync(bbPath, art.bbmodel.endsWith('\n') ? art.bbmodel : art.bbmodel + '\n');
    fs.writeFileSync(geoPath, JSON.stringify(art.geo, null, 2) + '\n');
    fs.writeFileSync(texPath, Buffer.from(art.texture.split(',')[1], 'base64'));
    fs.writeFileSync(statsPath, JSON.stringify(art.stats, null, 2) + '\n');
    fs.writeFileSync(bedrockPath, JSON.stringify(art.bedrock, null, 2) + '\n');

    // Lightweight structural validation against the custom GeoModel loader contract.
    const seen = new Set();
    let cubeCount = 0;
    for (const bone of art.geo.bones) {
      if (seen.has(bone.name)) throw new Error('duplicate runtime bone ' + bone.name);
      if (bone.parent && !seen.has(bone.parent)) throw new Error('parent after child: ' + bone.name + ' -> ' + bone.parent);
      seen.add(bone.name);
      if (!Array.isArray(bone.pivot) || bone.pivot.length !== 3) throw new Error('bad pivot ' + bone.name);
      for (const cube of bone.cubes || []) {
        cubeCount++;
        if (cube.size.some(v => !Number.isInteger(v) || v <= 0)) throw new Error('non-positive/integer cube size in ' + bone.name);
        const [u, v] = cube.uv;
        const [w, h, d] = cube.size;
        if (u < 0 || v < 0 || u + 2 * w + 2 * d > art.geo.texture_width || v + h + d > art.geo.texture_height) {
          throw new Error('UV out of atlas bounds in ' + bone.name + '/' + JSON.stringify(cube));
        }
      }
    }
    if (cubeCount !== art.stats.cubes) throw new Error('cube count mismatch for ' + art.id);

    report.models.push({
      id: art.id,
      title_ru: art.title_ru,
      biome: art.biome,
      bbmodel: path.relative(outRoot, bbPath),
      geo: path.relative(outRoot, geoPath),
      texture: path.relative(outRoot, texPath),
      stats: art.stats,
      structural_validation: 'ok'
    });
    console.log(`[ok] ${art.id}: ${art.stats.bones} bones, ${art.stats.cubes} cubes, atlas ${art.stats.atlas.join('x')}`);
  }

  report.roundtrip_validation = roundtrips;
  // Blockbench's web startup probes the public plugin CDN; the runner blocks all
  // non-local requests, which surfaces as a benign rejected promise in the page.
  report.benign_startup_errors = pageErrors.filter(e => e === 'Object: Object');
  report.page_errors = pageErrors.filter(e => e !== 'Object: Object');
  fs.writeFileSync(path.join(outRoot, 'reports/blockbench_first_batch/generation_report.json'), JSON.stringify(report, null, 2) + '\n');
  console.log('Wrote generation report: reports/blockbench_first_batch/generation_report.json');
} finally {
  await browser.close();
  server.close();
}
