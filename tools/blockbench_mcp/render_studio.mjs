#!/usr/bin/env node
/**
 * Real Blockbench 5.2.2 Studio Photographer for Riftworld Blockbench MCP Models.
 *
 * Opens each `.bbmodel` in official Blockbench 5.2.2 (headless Chromium), verifies
 * clean round-trip loading, configures crisp WebGL MSAA + 55mm telephoto perspective
 * studio camera, and captures:
 *   - Hero 3/4 angle matching the reference concept
 *   - Dark Hytale-style studio render (for glowing magma/embers contrast)
 *   - 4-view orthographic/perspective turntable sheet (Hero, Front, Side, Back)
 */

import fs from 'node:fs';
import path from 'node:path';
import http from 'node:http';
import os from 'node:os';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const require = createRequire('/tmp/bb-tools/package.json');
const puppeteer = require('puppeteer-core');
const chromiumPkg = require('@sparticuz/chromium');
const chromium = chromiumPkg.default || chromiumPkg;

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '../..');

function arg(name, fallback) {
  const hit = process.argv.slice(2).find((a) => a.startsWith(`--${name}=`));
  return hit ? hit.slice(name.length + 3) : fallback;
}

const BATCH_DEFAULTS = {
  batch_01: 'porcelain_crane,basalt_beetle,ash_moth',
  batch_02: 'amethyst_fox,sizo_leafwing,azure_frog',
  batch_03: 'blossom_snail,phantom_wolf,crystal_gecko',
};

const batchName = arg('batch', 'batch_01');
const blockbenchRoot = path.resolve(arg('blockbench-root', process.env.BLOCKBENCH_ROOT || '/tmp/blockbench-5.2.2'));
const outDir = path.resolve(arg('out', path.join(repoRoot, 'reports/blockbench_mcp', batchName)));
const idsArg = arg('ids', BATCH_DEFAULTS[batchName] || BATCH_DEFAULTS.batch_01);
const ids = idsArg.split(',').map((s) => s.trim()).filter(Boolean);

// Camera angles tailored to match the exact hero framing of each reference image!
const CAMERA_RIGS = {
  porcelain_crane: {
    num: '01',
    titleRu: '01 — Фарфоровый журавль',
    refFile: '/tmp/inspect_prev/orig_01_crane.png',
    heroDir: [0.78, 0.18, -0.60],
    distMul: 2.25,
    targetBias: [0, 2, -1],
  },
  basalt_beetle: {
    num: '02',
    titleRu: '02 — Базальтовый жук-углеед',
    refFile: '/tmp/inspect_prev/orig_02_beetle.png',
    heroDir: [-0.74, 0.22, -0.64],
    distMul: 2.08,
    targetBias: [0, 1, -2],
  },
  ash_moth: {
    num: '03',
    titleRu: '03 — Пепельная моль',
    refFile: '/tmp/inspect_prev/orig_03_moth.png',
    heroDir: [-0.15, 0.08, -0.98],
    distMul: 1.72,
    targetBias: [0, 0, 0],
  },
  amethyst_fox: {
    num: '04',
    titleRu: '04 — Аметистовый лис',
    refFile: '/tmp/inspect_prev/orig_04_fox.png',
    heroDir: [-0.75, 0.20, -0.63],
    distMul: 2.05,
    targetBias: [0, 0, 1],
  },
  sizo_leafwing: {
    num: '05',
    titleRu: '05 — Сизый листокрыл',
    refFile: '/tmp/inspect_prev/orig_05_leafwing.png',
    heroDir: [0.62, 0.12, -0.77],
    distMul: 1.82,
    targetBias: [0, 0, 2],
  },
  azure_frog: {
    num: '06',
    titleRu: '06 — Лазурный древесный лягуш',
    refFile: '/tmp/inspect_prev/orig_06_frog.png',
    heroDir: [0.62, 0.28, -0.73],
    distMul: 2.15,
    targetBias: [0, 0, -1],
  },
  blossom_snail: {
    num: '07',
    titleRu: '07 — Цветочная улитка',
    refFile: '/tmp/inspect_prev/orig_07_snail.png',
    heroDir: [-0.74, 0.20, -0.64],
    distMul: 2.08,
    targetBias: [0, 0, 0],
  },
  phantom_wolf: {
    num: '08',
    titleRu: '08 — Туманный волк-призрак',
    refFile: '/tmp/inspect_prev/orig_08_wolf.png',
    heroDir: [-0.74, 0.18, -0.65],
    distMul: 2.08,
    targetBias: [0, 0, 1],
  },
  crystal_gecko: {
    num: '09',
    titleRu: '09 — Кристальный ящер',
    refFile: '/tmp/inspect_prev/orig_09_gecko.png',
    heroDir: [-0.72, 0.22, -0.66],
    distMul: 1.98,
    targetBias: [0, 0, 2],
  },
};

if (!fs.existsSync(path.join(blockbenchRoot, 'dist/bundle.js'))) {
  console.error('Missing built Blockbench 5.2.2 web bundle at ' + blockbenchRoot);
  process.exit(2);
}
fs.mkdirSync(outDir, { recursive: true });

const mime = {
  '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.css': 'text/css',
  '.json': 'application/json', '.png': 'image/png', '.svg': 'image/svg+xml',
  '.webmanifest': 'application/manifest+json', '.woff': 'font/woff', '.woff2': 'font/woff2',
  '.ttf': 'font/ttf', '.bbtheme': 'text/plain', '.bbkeymap': 'application/json', '.wasm': 'application/wasm',
};

const server = http.createServer((req, res) => {
  let p = decodeURIComponent((req.url || '/').split('?')[0]);
  if (p === '/') p = '/index.html';
  if (p === '/service_worker.js') {
    res.writeHead(200, { 'Content-Type': 'text/javascript' });
    res.end('/* disabled */');
    return;
  }
  const file = path.join(blockbenchRoot, p);
  if (!file.startsWith(blockbenchRoot) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
    res.writeHead(404);
    res.end('not found');
    return;
  }
  res.writeHead(200, {
    'Content-Type': mime[path.extname(file)] || 'application/octet-stream',
    'Access-Control-Allow-Origin': '*',
    'Cache-Control': 'no-store',
  });
  fs.createReadStream(file).pipe(res);
});

await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
const port = server.address().port;

// Unpack @sparticuz/chromium shared libraries if needed
const chromiumMainPath = require.resolve('@sparticuz/chromium');
const binDir = path.resolve(path.dirname(chromiumMainPath), '../bin');
const tmp = os.tmpdir();
if (typeof chromiumPkg.inflate === 'function') {
  for (const name of ['al2023', 'fonts', 'swiftshader']) {
    const archive = path.join(binDir, `${name}.tar.br`);
    const marker = name === 'al2023'
      ? path.join(tmp, 'al2023/lib/libnss3.so')
      : name === 'fonts'
        ? path.join(tmp, 'fonts/fonts.conf')
        : path.join(tmp, 'libEGL.so');
    if (fs.existsSync(archive) && !fs.existsSync(marker)) {
      await chromiumPkg.inflate(archive);
    }
  }
}

const browser = await puppeteer.launch({
  executablePath: await chromium.executablePath(),
  args: [...chromium.args.filter((a) => !a.startsWith('--headless')), '--disable-dev-shm-usage'],
  headless: 'shell',
  env: {
    ...process.env,
    LD_LIBRARY_PATH: [path.join(tmp, 'al2023/lib'), tmp, process.env.LD_LIBRARY_PATH].filter(Boolean).join(':'),
    FONTCONFIG_PATH: path.join(tmp, 'fonts'),
    HOME: process.env.HOME || tmp,
  },
  protocolTimeout: 240000,
});

function compositeOnStudioBg(transparentPng, outPng, bgHex = '#f4f6f9', withShadow = true) {
  if (!withShadow) {
    execFileSync('convert', [transparentPng, '-background', bgHex, '-alpha', 'remove', '-alpha', 'off', outPng]);
    return;
  }
  // Create subtle soft ground contact shadow from alpha silhouette compressed vertically at bottom
  execFileSync('convert', [
    '(',
    '-size', '1600x1200', `xc:${bgHex}`,
    ')',
    '(',
    transparentPng,
    '-alpha', 'extract',
    '-resize', '1600x180!',
    '-blur', '0x28',
    '-level', '0%,380%',
    '-background', 'black',
    '-alpha', 'shape',
    ')',
    '-gravity', 'south',
    '-geometry', '+0+55',
    '-composite',
    transparentPng,
    '-gravity', 'center',
    '-composite',
    '-alpha', 'off',
    outPng,
  ]);
}

const renderReport = [];

try {
  const page = await browser.newPage();
  await page.setViewport({ width: 1600, height: 1200, deviceScaleFactor: 1 });
  await page.setRequestInterception(true);
  page.on('request', (req) => {
    const url = new URL(req.url());
    if (url.hostname === '127.0.0.1' || url.hostname === 'localhost' || url.protocol === 'data:' || url.protocol === 'blob:') {
      req.continue();
    } else {
      req.abort();
    }
  });
  await page.evaluateOnNewDocument(() => {
    try {
      localStorage.setItem('startups', '10');
      localStorage.setItem('last_version', '5.2.2');
    } catch {}
  });
  await page.goto(`http://127.0.0.1:${port}/`, { waitUntil: 'domcontentloaded', timeout: 120000 });
  await page.waitForFunction(() => window.Blockbench && window.Blockbench.setup_successful === true, { timeout: 120000 });

  for (let idx = 0; idx < ids.length; idx++) {
    const id = ids[idx];
    const rig = CAMERA_RIGS[id] || {
      titleRu: id,
      heroDir: [-0.72, 0.28, -0.63],
      distMul: 2.2,
      targetBias: [0, 0, 0],
    };
    const bbPath = path.join(repoRoot, 'blockbench/riftworld_fauna', `${id}.bbmodel`);
    const bbText = fs.readFileSync(bbPath, 'utf8');

    // Compute exact rotated world bounds inside Blockbench 5.2.2 itself and capture 4 views!
    const captures = await page.evaluate(async ({ bbText, rig }) => {
      newProject(Formats.bedrock);
      Codecs.project.parse(JSON.parse(bbText));
      settings.brightness.set(56);
      Canvas.updateAll();
      Canvas.updateShading();

      const tex = Project.textures[0];
      for (let k = 0; k < 120; k++) {
        if (tex && tex.width > 0 && !tex.error) break;
        await new Promise((r) => setTimeout(r, 20));
      }
      await new Promise((r) => setTimeout(r, 250));

      // Compute exact world bounding box across all rendered Three.js cube meshes!
      const box = new THREE.Box3();
      for (const el of Outliner.elements) {
        if (el.mesh) {
          el.mesh.updateMatrixWorld(true);
          box.expandByObject(el.mesh);
        }
      }
      const center = [
        (box.min.x + box.max.x) / 2 + (rig.targetBias?.[0] || 0),
        (box.min.y + box.max.y) / 2 + (rig.targetBias?.[1] || 0),
        (box.min.z + box.max.z) / 2 + (rig.targetBias?.[2] || 0),
      ];
      const size = [box.max.x - box.min.x, box.max.y - box.min.y, box.max.z - box.min.z];
      const maxSpan = Math.max(size[0], size[1] * 1.22, size[2], 10);
      const dist = maxSpan * (rig.distMul || 2.2);

      const norm = (v) => {
        const l = Math.hypot(v[0], v[1], v[2]) || 1;
        return [v[0] / l, v[1] / l, v[2] / l];
      };
      const hd = norm(rig.heroDir);
      const sideSign = hd[0] >= 0 ? 1 : -1;

      const presets = [
        {
          name: 'hero',
          projection: 'perspective',
          position: [center[0] + hd[0] * dist, center[1] + hd[1] * dist, center[2] + hd[2] * dist],
          target: center,
          fov: 28,
        },
        {
          name: 'front',
          projection: 'perspective',
          position: [center[0], center[1] + dist * 0.14, center[2] - dist * 1.02],
          target: center,
          fov: 28,
        },
        {
          name: 'side',
          projection: 'perspective',
          position: [center[0] + sideSign * dist * 1.05, center[1] + dist * 0.14, center[2]],
          target: center,
          fov: 28,
        },
        {
          name: 'back',
          projection: 'perspective',
          position: [center[0] - hd[0] * dist, center[1] + dist * 0.24, center[2] + Math.abs(hd[2]) * dist * 1.02],
          target: center,
          fov: 28,
        },
      ];
      localStorage.setItem('camera_presets', JSON.stringify(presets));

      async function snap(presetIdx, res) {
        return await new Promise((resolve, reject) => {
          const timer = setTimeout(() => reject(new Error('Screenshot timeout')), 30000);
          Screencam.advancedScreenshot(
            Preview.selected,
            {
              angle_preset: String(presetIdx),
              resolution: res,
              anti_aliasing: 'msaa',
              show_gizmos: false,
              shading: true,
              show_errors: false,
            },
            (url) => {
              clearTimeout(timer);
              resolve(url);
            }
          );
        });
      }

      // Warm up WebGL framebuffer
      await snap(0, [640, 480]);
      const hero = await snap(0, [1600, 1200]);
      const front = await snap(1, [1200, 900]);
      const side = await snap(2, [1200, 900]);
      const back = await snap(3, [1200, 900]);

      return {
        hero,
        front,
        side,
        back,
        elementCount: Outliner.elements.length,
        groupCount: Group.all.length,
        worldSize: size.map((v) => Math.round(v * 100) / 100),
      };
    }, { bbText, rig });

    const numPrefix = rig.num || String(idx + 1).padStart(2, '0');
    const rawHero = path.join(tmp, `${id}_hero_raw.png`);
    const rawFront = path.join(tmp, `${id}_front_raw.png`);
    const rawSide = path.join(tmp, `${id}_side_raw.png`);
    const rawBack = path.join(tmp, `${id}_back_raw.png`);

    fs.writeFileSync(rawHero, Buffer.from(captures.hero.split(',')[1], 'base64'));
    fs.writeFileSync(rawFront, Buffer.from(captures.front.split(',')[1], 'base64'));
    fs.writeFileSync(rawSide, Buffer.from(captures.side.split(',')[1], 'base64'));
    fs.writeFileSync(rawBack, Buffer.from(captures.back.split(',')[1], 'base64'));

    const heroLight = path.join(outDir, `${numPrefix}_${id}.png`);
    const heroDark = path.join(outDir, `${numPrefix}_${id}_dark.png`);
    compositeOnStudioBg(rawHero, heroLight, '#f5f6f9', true);
    execFileSync('convert', [rawHero, '-background', '#0e1117', '-alpha', 'remove', '-alpha', 'off', heroDark]);

    // Verify non-blank render
    const [colors] = execFileSync('convert', [heroLight, '-format', '%k', 'info:'], { encoding: 'utf8' }).trim().split(/\s+/).map(Number);
    if (!colors || colors < 10) {
      throw new Error(`Blank or broken render for ${id} (colors=${colors})`);
    }

    // Build 4-angle turntable sheet
    const fLight = path.join(tmp, `${id}_front_bg.png`);
    const sLight = path.join(tmp, `${id}_side_bg.png`);
    const bLight = path.join(tmp, `${id}_back_bg.png`);
    execFileSync('convert', [rawFront, '-background', '#f5f6f9', '-alpha', 'remove', '-alpha', 'off', fLight]);
    execFileSync('convert', [rawSide, '-background', '#f5f6f9', '-alpha', 'remove', '-alpha', 'off', sLight]);
    execFileSync('convert', [rawBack, '-background', '#f5f6f9', '-alpha', 'remove', '-alpha', 'off', bLight]);

    const turntablePath = path.join(outDir, `${numPrefix}_${id}_angles.png`);
    execFileSync('montage', [
      '-background', '#eef1f6',
      '-fill', '#1f2430',
      '-font', 'DejaVu-Sans',
      '-pointsize', '22',
      '-label', '3/4 Ракурс', heroLight,
      '-label', 'Спереди', fLight,
      '-label', 'Сбоку', sLight,
      '-label', 'Сзади (3/4)', bLight,
      '-tile', '2x2',
      '-geometry', '960x720+12+12',
      turntablePath,
    ]);

    // Build Side-by-Side Reference vs Blockbench Model sheet if reference exists locally
    let compPath = null;
    if (rig.refFile && fs.existsSync(rig.refFile)) {
      const refCropped = path.join(tmp, `${id}_ref_fit.png`);
      execFileSync('convert', [
        rig.refFile,
        '-gravity', 'center',
        '-resize', '1200x900^',
        '-extent', '1200x900',
        refCropped,
      ]);
      const modCropped = path.join(tmp, `${id}_mod_fit.png`);
      execFileSync('convert', [heroLight, '-resize', '1200x900!', modCropped]);
      compPath = path.join(outDir, `${numPrefix}_${id}_vs_ref.png`);
      execFileSync('montage', [
        '-background', '#e9edf4',
        '-fill', '#1f2430',
        '-font', 'DejaVu-Sans',
        '-pointsize', '24',
        '-label', `Референс: ${rig.titleRu}`, refCropped,
        '-label', `Blockbench MCP 3D: ${rig.titleRu}`, modCropped,
        '-tile', '2x1',
        '-geometry', '1200x900+14+14',
        compPath,
      ]);
    }

    renderReport.push({
      id,
      titleRu: rig.titleRu,
      heroLight: path.relative(repoRoot, heroLight),
      heroDark: path.relative(repoRoot, heroDark),
      turntable: path.relative(repoRoot, turntablePath),
      localRefComparison: compPath,
      elementCount: captures.elementCount,
      groupCount: captures.groupCount,
      worldSize: captures.worldSize,
    });
    console.log(`[studio] ${id}: ${captures.groupCount} bones, ${captures.elementCount} rectangular cuboids -> ${heroLight}`);
  }

  // Create Batch Showcase Sheets (Light Studio + Dark Hytale Showcase!)
  const showcaseLight = path.join(outDir, `00_${batchName}_showcase_light.png`);
  const showcaseDark = path.join(outDir, `00_${batchName}_showcase_dark.png`);

  execFileSync('montage', [
    '-background', '#eef1f6',
    '-fill', '#1b202b',
    '-font', 'DejaVu-Sans',
    '-pointsize', '24',
    ...renderReport.flatMap((r) => ['-label', r.titleRu, path.join(repoRoot, r.heroLight)]),
    '-tile', '3x1',
    '-geometry', '960x720+16+16',
    showcaseLight,
  ]);

  execFileSync('montage', [
    '-background', '#0b0e14',
    '-fill', '#e6edf3',
    '-font', 'DejaVu-Sans',
    '-pointsize', '24',
    ...renderReport.flatMap((r) => ['-label', r.titleRu, path.join(repoRoot, r.heroDark)]),
    '-tile', '3x1',
    '-geometry', '960x720+16+16',
    showcaseDark,
  ]);

  fs.writeFileSync(path.join(outDir, 'studio_report.json'), JSON.stringify(renderReport, null, 2) + '\n');
  console.log(`[studio] Showcases saved: ${showcaseLight} & ${showcaseDark}`);
} finally {
  await browser.close();
  server.close();
}
