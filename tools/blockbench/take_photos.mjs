#!/usr/bin/env node
/**
 * Take Blockbench API screenshots ("photos") of the saved Riftworld fauna .bbmodel files.
 *
 * Usage:
 *   BLOCKBENCH_ROOT=/path/to/blockbench-5.2.2 node tools/blockbench/take_photos.mjs
 */
import fs from 'node:fs';
import path from 'node:path';
import http from 'node:http';
import os from 'node:os';
import { execFileSync } from 'node:child_process';
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
const outDir = path.resolve(arg('out', path.join(repoRoot, 'reports/blockbench_first_batch/photos')));
const idsArg = arg('ids', 'porcelain_crane,basalt_beetle,ash_moth,amethyst_fox,sizo_leafwing,azure_frog');
const ids = idsArg.split(',').map(s => s.trim()).filter(Boolean);
const background = arg('background', '#f2f4f8');
const view = arg('view', 'default');
if (!['default', 'front', 'side'].includes(view)) throw new Error('Unknown view: ' + view);
const resolutionArg = arg('resolution', '1600x1200').split('x').map(Number);
const finalResolution = resolutionArg.length === 2 && resolutionArg.every(v => v > 0) ? resolutionArg : [1600, 1200];

const titles = {
  porcelain_crane: 'Фарфоровый журавль',
  basalt_beetle: 'Базальтовый жук-углеед',
  ash_moth: 'Пепельная моль',
  amethyst_fox: 'Аметистовый лис',
  sizo_leafwing: 'Сизый листокрыл',
  azure_frog: 'Лазурный древесный лягуш'
};

if (!fs.existsSync(path.join(blockbenchRoot, 'dist/bundle.js'))) {
  console.error('Missing built Blockbench web bundle: ' + path.join(blockbenchRoot, 'dist/bundle.js'));
  process.exit(2);
}
fs.mkdirSync(outDir, { recursive: true });

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
    res.end('/* service worker disabled for headless photos */');
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

const browser = await puppeteer.launch({
  executablePath: await chromium.executablePath(),
  args: [...chromium.args.filter(a => !a.startsWith('--headless')), '--disable-dev-shm-usage'],
  headless: 'shell',
  env: {
    ...process.env,
    LD_LIBRARY_PATH: [path.join(tmp, 'al2023/lib'), tmp, process.env.LD_LIBRARY_PATH].filter(Boolean).join(':'),
    FONTCONFIG_PATH: path.join(tmp, 'fonts'),
    HOME: process.env.HOME || tmp
  },
  protocolTimeout: 240000
});

const report = {
  generator: 'Blockbench 5.2.2 Screencam.advancedScreenshot via headless Chromium',
  background,
  photos: []
};

try {
  const page = await browser.newPage();
  await page.setViewport({ width: 1600, height: 1200, deviceScaleFactor: 1 });
  await page.setRequestInterception(true);
  page.on('request', req => {
    const url = new URL(req.url());
    if (url.hostname === '127.0.0.1' || url.hostname === 'localhost' || url.protocol === 'data:' || url.protocol === 'blob:') req.continue();
    else req.abort();
  });
  await page.evaluateOnNewDocument(() => {
    try {
      localStorage.setItem('startups', '10');
      localStorage.setItem('last_version', '5.2.2');
    } catch {}
  });
  await page.goto(`http://127.0.0.1:${port}/`, { waitUntil: 'domcontentloaded', timeout: 120000 });
  await page.waitForFunction(() => window.Blockbench && window.Blockbench.setup_successful === true, { timeout: 120000 });

  for (let i = 0; i < ids.length; i++) {
    const id = ids[i];
    const bbPath = path.join(repoRoot, 'blockbench/riftworld_fauna', id + '.bbmodel');
    const bbmodel = fs.readFileSync(bbPath, 'utf8');
    const project = JSON.parse(bbmodel);
    const bounds = { min: [Infinity, Infinity, Infinity], max: [-Infinity, -Infinity, -Infinity] };
    for (const element of project.elements || []) {
      if (!element.from || !element.to) continue;
      for (let axis = 0; axis < 3; axis++) {
        bounds.min[axis] = Math.min(bounds.min[axis], element.from[axis]);
        bounds.max[axis] = Math.max(bounds.max[axis], element.to[axis]);
      }
    }
    const center = bounds.min.map((v, axis) => (v + bounds.max[axis]) / 2);
    const extent = bounds.max.map((v, axis) => v - bounds.min[axis]);
    const maxExtent = Math.max(...extent, 1);
    // Fill the frame instead of keeping the old fixed zoom across 4x-sized projects.
    const zoomPercent = Math.max(4, Math.min(42, 2600 / maxExtent));
    const cameraPositions = {
      porcelain_crane: [512, 512 * 0.55, -320],
      basalt_beetle: [-512, 512 * 0.6, -512],
      ash_moth: [0, 135, -512],
      amethyst_fox: [-512, 220, -280],
      sizo_leafwing: [-512, 250, -512],
      azure_frog: [-512, 270, -512]
    };
    const offset = view === 'front' ? [0, 130, -512]
      : view === 'side' ? [512, 130, 0]
      : (cameraPositions[id] || [-512, 512 * 0.6, -512]);
    const cameraPreset = {
      name: 'Riftworld reference view',
      projection: 'orthographic',
      position: [center[0] + offset[0], center[1] + offset[1], center[2] + offset[2]],
      target: center,
      zoom: 0.5
    };
    const shot = await page.evaluate(async ({ bbmodel, cameraPreset, zoomPercent, finalResolution }) => {
      localStorage.setItem('camera_presets', JSON.stringify([cameraPreset]));
      newProject(Formats.bedrock_old);
      Codecs.project.parse(JSON.parse(bbmodel));
      const tex = Project.textures[0];
      for (let k = 0; k < 100; k++) {
        if (tex && tex.width === Project.texture_width && tex.height === Project.texture_height && !tex.error) break;
        await new Promise(r => setTimeout(r, 20));
      }
      // Let parsed textures/meshes settle, then warm up Blockbench's offscreen
      // screenshot preview at a small resolution before the final large capture.
      await new Promise(r => setTimeout(r, 300));
      async function capture(resolution) {
        const url = await new Promise((resolve, reject) => {
          const timer = setTimeout(() => reject(new Error('Blockbench screenshot timeout')), 30000);
          Screencam.advancedScreenshot(Preview.selected, {
            angle_preset: '0',
            resolution,
            zoom: zoomPercent,
            anti_aliasing: 'off',
            show_gizmos: false,
            shading: true,
            show_errors: false
          }, result => {
            clearTimeout(timer);
            resolve(result);
          });
        });
        if (!url || !url.startsWith('data:image/png;base64,') || url.length < 2000) {
          throw new Error('empty Blockbench screenshot');
        }
        return url;
      }
      await capture([800, 600]);
      const dataUrl = await capture(finalResolution);
      return { dataUrl, resolution: finalResolution, zoomPercent, cameraPreset };
    }, { bbmodel, cameraPreset, zoomPercent, finalResolution });

    const order = Object.keys(titles).indexOf(id);
    if (order < 0) throw new Error('Unknown model: ' + id);
    const filename = String(order + 1).padStart(2, '0') + '_' + id + '.png';
    const file = path.join(outDir, filename);
    fs.writeFileSync(file, Buffer.from(shot.dataUrl.split(',')[1], 'base64'));
    execFileSync('convert', [file, '-background', background, '-alpha', 'remove', '-alpha', 'off', file]);
    // A transparent offscreen frame can still be a valid, large PNG. Check pixels,
    // not base64 length, so a blank capture never reaches the published gallery.
    const [colorCount, deviation] = execFileSync('convert', [file, '-format', '%k %[standard-deviation]', 'info:'], { encoding: 'utf8' }).trim().split(/\s+/).map(Number);
    if (!Number.isFinite(colorCount) || !Number.isFinite(deviation) || colorCount < 2 || deviation <= 0) {
      throw new Error('Blank Blockbench photograph for ' + id);
    }
    report.photos.push({
      id,
      title_ru: titles[id] || id,
      file: path.relative(repoRoot, file),
      source: path.relative(repoRoot, bbPath),
      camera: 'custom_reference_view',
      camera_preset: shot.cameraPreset,
      resolution: shot.resolution,
      zoom_percent: shot.zoomPercent
    });
    console.log(`[photo] ${file}`);
  }
} finally {
  await browser.close();
  server.close();
}

const overview = path.join(outDir, '00_all_models.png');
const montageArgs = ['-background', background, '-fill', '#20242c', '-font', 'DejaVu-Sans', '-pointsize', '24'];
for (const photo of report.photos) {
  montageArgs.push('-label', photo.title_ru, path.join(repoRoot, photo.file));
}
montageArgs.push('-tile', ids.length > 4 ? '3x2' : '2x2', '-geometry', '800x600+14+14', overview);
execFileSync('montage', montageArgs);
for (const extra of fs.readdirSync(outDir)) {
  if (/^00_all_models-\d+\.png$/.test(extra)) fs.rmSync(path.join(outDir, extra));
}
report.overview = path.relative(repoRoot, overview);
fs.writeFileSync(path.join(outDir, 'photos_report.json'), JSON.stringify(report, null, 2) + '\n');
console.log(`[overview] ${overview}`);
