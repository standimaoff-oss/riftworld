/* Riftworld — редактор брони: рисуем иконки 16×16 И ванильные карты брони 64×32,
   экспортируем JSON. Данные (ванильные иконки, карты брони и 3D-модели) лежат рядом в data/.

   v0.52.0: добавлен режим «слои брони» — это те самые карты 64×32
   (layer_1 = шлем/нагрудник/ботинки, layer_2 = поножи), из которых моду
   собирает настоящую надетую броню с ванильной геометрией. */
'use strict';

const SETS = [
  { id: 'leather',    name: 'Кожаная',   ref: 'leather' },
  { id: 'netherite',  name: 'Незерит',   ref: 'netherite' },
  { id: 'diamond',    name: 'Алмазная',  ref: 'diamond' },
];
const PARTS = [
  { id: 'helmet',    name: 'Шлем',      file: 'helmet' },
  { id: 'chestplate',name: 'Нагрудник', file: 'chestplate' },
  { id: 'leggings',  name: 'Поножи',    file: 'leggings' },
  { id: 'boots',     name: 'Ботинки',   file: 'boots' },
];
/* Режимы работы. 'icon' — иконка предмета 16×16; 'layer' — карта брони 64×32. */
const MODES = [
  { id: 'icon',  name: 'Иконки',   w: 16, h: 16 },
  { id: 'layer', name: 'Слои',     w: 64, h: 32 },
];
/* Слои брони: layer_1 — шлем/нагрудник/ботинки, layer_2 — поножи. */
const LAYERS = [
  { id: 'layer_1', name: 'Слой 1 (шлем/нагрудник/ботинки)' },
  { id: 'layer_2', name: 'Слой 2 (поножи)' },
];

var state = {
  set: 'leather',
  part: 'helmet',
  mode: 'icon',        // 'icon' | 'layer'
  layer: 'layer_1',    // текущая карта брони (в режиме 'layer')
  // pixels[set][part] = Uint8ClampedArray(16*16*4)   — иконки
  pixels: {},
  // layers[set][layerId] = Uint8ClampedArray(64*32*4) — карты брони
  layers: {},
  // размеры текущего холста
  size: [16, 16],
  width: 16,
  tool: 'brush',
  color: '#b8b8b8',
  alpha: 255,
  brush: 1,
  zoom: 26,
  mirror: false,
  spin: false,
  spinAngle: 0.6,
  drag: null,
  dirty: false,
};

// ---------------------------------------------------------------- инициализация
var imgs = {};        // референсные картинки (ванильные иконки)
var models3d = {};    // 3D-модели брони

function px(img, x, y) {
  const c = document.createElement('canvas');
  c.width = img.width; c.height = img.height;
  const cx = c.getContext('2d');
  cx.drawImage(img, 0, 0);
  return cx.getImageData(0, 0, img.width, img.height);
}

function loadImage(src) {
  return new Promise((res, rej) => {
    const i = new Image();
    i.onload = () => res(i);
    i.onerror = rej;
    i.src = src;
  });
}

/* Одностраничная сборка встраивает данные как ARMOR_DATA (base64);
   при разработке файлы лежат рядом в data/ и грузятся обычным путём. */
var EMBEDDED = (typeof ARMOR_DATA !== 'undefined' && ARMOR_DATA) ? ARMOR_DATA : null;

async function loadPng(key) {
  if (EMBEDDED && EMBEDDED.icons && EMBEDDED.icons[key]) {
    return await loadImage('data:image/png;base64,' + EMBEDDED.icons[key]);
  }
  return await loadImage(`data/${key}.png`);
}

async function init() {
  for (const s of SETS) {
    state.pixels[s.id] = {};
    state.layers[s.id] = {};
    for (const p of PARTS) {
      // ванильная иконка как стартовая основа
      const img = await loadPng(`${s.ref}_${p.file}`);
      imgs[`${s.id}_${p.id}`] = img;
      const d = px(img, 0, 0);
      state.pixels[s.id][p.id] = d.data;
    }
    // ванильные карты брони 64×32 — стартовая основа для «слоёв»
    for (const L of LAYERS) {
      const li = await loadPng(`${s.ref}_${L.id}`);
      imgs[`${s.id}_${L.id}`] = li;
      state.layers[s.id][L.id] = px(li, 0, 0).data;
    }
  }
  for (const p of PARTS) {
    if (EMBEDDED && EMBEDDED.models && EMBEDDED.models[p.id]) {
      models3d[p.id] = EMBEDDED.models[p.id];
    } else {
      const r = await fetch(`data/model3d_${p.file}.json`);
      models3d[p.id] = await r.json();
    }
  }
  buildSets();
  buildSlots();
  buildSwatches();
  buildModes();
  buildLayers();
  wireCanvas();
  wireTools();
  update();
  setStatus('готов — рисуй');
}

function setStatus(t, cls) {
  const el = document.getElementById('status');
  el.textContent = t;
  el.className = 'pill' + (cls ? ' ' + cls : '');
}

// ---------------------------------------------------------------- интерфейс
function buildSets() {
  const box = document.getElementById('sets');
  box.innerHTML = '';
  for (const s of SETS) {
    const b = document.createElement('button');
    b.className = 'setbtn' + (s.id === state.set ? ' active' : '');
    b.textContent = s.name;
    b.onclick = () => { state.set = s.id; buildSets(); buildSlots(); update(); };
    box.appendChild(b);
  }
}

function buildSlots() {
  const box = document.getElementById('slots');
  box.innerHTML = '';
  for (const p of PARTS) {
    const d = document.createElement('div');
    d.className = 'slot' + (p.id === state.part ? ' active' : '');
    const c = document.createElement('canvas');
    c.width = 16; c.height = 16;
    const cx = c.getContext('2d');
    const im = cx.createImageData(16, 16);
    im.data.set(state.pixels[state.set][p.id]);
    cx.putImageData(im, 0, 0);
    const sp = document.createElement('span');
    sp.textContent = p.name;
    d.appendChild(c); d.appendChild(sp);
    d.onclick = () => {
      state.part = p.id;
      if (state.mode !== 'icon') setMode('icon');
      else { buildSlots(); update(); }
    };
    box.appendChild(d);
  }
}

function buildSwatches() {
  const box = document.getElementById('swatches');
  box.innerHTML = '';
  const pal = [
    '#00000000','#3f3f3f','#5d5d5d','#7a7a7a','#9e9e9e','#c4c4c4','#e8e8e8','#ffffff',
    '#6b3a1f','#8a5a2b','#a9743d','#c9a15a','#e0c07a','#f0dca8','#c9c9c9','#8f6b4a',
    '#1f1f1f','#2d3b2d','#3f5a3f','#5c7a4a','#7fa05a','#a8c46a','#2a3a4a','#3a5a7a',
    '#5a7a9a','#7a9aba','#3a2a4a','#6a3a7a','#9a4a8a','#c46a7a','#d4a03a','#4a2d1f',
  ];
  for (const c of pal) {
    const d = document.createElement('div');
    d.className = 'sw';
    d.style.background = c;
    d.onclick = () => {
      if (c.length === 9) {
        state.color = '#' + c.slice(1, 7);
        state.alpha = parseInt(c.slice(7, 9), 16);
        document.getElementById('color').value = state.color;
        document.getElementById('alpha').value = state.alpha;
        document.getElementById('alphaVal').textContent = Math.round(state.alpha / 255 * 100) + '%';
      } else {
        state.color = c;
        document.getElementById('color').value = c;
      }
      update();
    };
    box.appendChild(d);
  }
}

// ---------------------------------------------------------------- режимы
/* curPixels из pixels.js работает по state.pixels[set][part].
   В режиме «слои» мы рисуем state.layers[set][layer].
   ВАЖНО: нельзя переопределять саму function curPixels — function-декларации
   поднимаются (hoisting), и получится бесконечная рекурсия. Поэтому
   pixels.js экспортирует curPixels как curPixelsBase, а здесь — обёртка. */
function curPixelsBase(st) { return st.pixels[st.set][st.part]; }
function curPixels(st) {
  if (st.mode === 'layer') return st.layers[st.set][st.layer];
  return curPixelsBase(st);
}

/** Переключает режим «иконки» / «слои брони». */
function setMode(mode) {
  state.mode = mode;
  const m = MODES.find(x => x.id === mode);
  state.size = [m.w, m.h];
  state.width = m.w;
  state.height = m.h;
  canvas.width = m.w;
  canvas.height = m.h;
  overlay.width = m.w;
  overlay.height = m.h;
  resizeCanvas();
  buildModes();
  buildLayers();
  const title = document.getElementById('canvasTitle');
  if (title) title.textContent = 'Холст ' + m.w + '×' + m.h;
  update();
}

function buildModes() {
  const box = document.getElementById('modes');
  if (!box) return;
  box.innerHTML = '';
  for (const m of MODES) {
    const b = document.createElement('button');
    b.className = 'modebtn' + (m.id === state.mode ? ' active' : '');
    b.textContent = m.name;
    b.onclick = () => setMode(m.id);
    box.appendChild(b);
  }
}

/** Переключает слой брони (layer_1 / layer_2) — только в режиме «слои». */
function buildLayers() {
  const box = document.getElementById('layerpick');
  if (!box) return;
  box.innerHTML = '';
  if (state.mode !== 'layer') { box.style.display = 'none'; return; }
  box.style.display = '';
  for (const L of LAYERS) {
    const b = document.createElement('button');
    b.className = 'modebtn' + (L.id === state.layer ? ' active' : '');
    b.textContent = L.id === 'layer_1' ? 'слой 1' : 'слой 2';
    b.onclick = () => { state.layer = L.id; buildLayers(); update(); };
    box.appendChild(b);
  }
}

// ---------------------------------------------------------------- холст
// ---------------------------------------------------------------- холст
const canvas = document.getElementById('canvas');
const ctx = canvas.getContext('2d', { willReadFrequently: true });
const overlay = document.getElementById('overlay');
const octx = overlay.getContext('2d');







function canvasPos(ev) {
  const r = canvas.getBoundingClientRect();
  const x = Math.floor((ev.clientX - r.left) / (r.width / state.width));
  const y = Math.floor((ev.clientY - r.top) / (r.height / state.height));
  return [x, y];
}

function wireCanvas() {
  const onDown = (ev) => {
    ev.preventDefault();
    const [x, y] = canvasPos(ev);
    const data = curPixels(state);
    const rgba = hexToRGBA(state.color, state.alpha);
    state.drag = { x, y, x0: x, y0: y };
    if (state.tool === 'brush' || state.tool === 'eraser') {
      const c = state.tool === 'eraser' ? [0, 0, 0, 0] : rgba;
      stamp(state, data, x, y, c);
    } else if (state.tool === 'bucket') {
      bucketFill(state, data, x, y, rgba);
    } else if (state.tool === 'pick') {
      const c = getPixel(data, x, y);
      if (c[3] > 0) {
        state.color = '#' + c.slice(0, 3).map(v => v.toString(16).padStart(2, '0')).join('');
        state.alpha = c[3];
        document.getElementById('color').value = state.color;
        document.getElementById('alpha').value = state.alpha;
        document.getElementById('alphaVal').textContent = Math.round(c[3] / 255 * 100) + '%';
      }
      state.tool = 'brush';
      syncTools();
    }
    update();
  };
  const onMove = (ev) => {
    if (!state.drag) return;
    const [x, y] = canvasPos(ev);
    const data = curPixels(state);
    const rgba = hexToRGBA(state.color, state.alpha);
    const d = state.drag;
    if (state.tool === 'brush' || state.tool === 'eraser') {
      const c = state.tool === 'eraser' ? [0, 0, 0, 0] : rgba;
      drawLine(state, data, d.x, d.y, x, y, c);
      d.x = x; d.y = y;
    } else if (state.tool === 'line') {
      // предпросход: рисуем на копии
      previewDrag(data, x, y);
    }
    update();
  };
  const onUp = (ev) => {
    if (!state.drag) return;
    const [x, y] = canvasPos(ev);
    const data = curPixels(state);
    const rgba = hexToRGBA(state.color, state.alpha);
    if (state.tool === 'line') {
      drawLine(state, data, state.drag.x0, state.drag.y0, x, y, rgba);
    } else if (state.tool === 'rect') {
      rect(state, data, state.drag.x0, state.drag.y0, x, y, rgba);
    }
    state.drag = null;
    update();
  };
  canvas.addEventListener('pointerdown', onDown);
  canvas.addEventListener('pointermove', onMove);
  window.addEventListener('pointerup', onUp);
}

let dragBackup = null;
function previewDrag(data, x, y) {
  if (!dragBackup) dragBackup = data.slice();
  data.set(dragBackup);
  drawLine(state, data, state.drag.x0, state.drag.y0, x, y, hexToRGBA(state.color, state.alpha));
}

function wireTools() {
  document.querySelectorAll('.tool[data-tool]').forEach(b => {
    b.onclick = () => { state.tool = b.dataset.tool; syncTools(); };
  });
  document.getElementById('btnMirror').onclick = () => {
    state.mirror = !state.mirror;
    document.getElementById('btnMirror').classList.toggle('active', state.mirror);
    update();
  };
  document.getElementById('btnClear').onclick = () => {
    const d = curPixels(state);
    d.fill(0);
    update();
  };
  document.getElementById('btnPrevSpin').onclick = () => {
    state.spin = !state.spin;
    document.getElementById('btnPrevSpin').classList.toggle('active', state.spin);
    drawPreview();
  };
  document.getElementById('color').oninput = e => { state.color = e.target.value; };
  document.getElementById('alpha').oninput = e => {
    state.alpha = +e.target.value;
    document.getElementById('alphaVal').textContent = Math.round(state.alpha / 255 * 100) + '%';
  };
  document.getElementById('brushSize').oninput = e => {
    state.brush = +e.target.value;
    document.getElementById('brushVal').textContent = state.brush + ' px';
  };
  document.getElementById('zoom').oninput = e => {
    state.zoom = +e.target.value;
    document.getElementById('zoomVal').textContent = state.zoom + '×';
    resizeCanvas();
    update();
  };
  document.getElementById('grid').onchange = () => update();
  document.getElementById('btnExport').onclick = doExport;
  document.getElementById('btnCopy').onclick = doCopy;
  document.getElementById('btnReset').onclick = () => {
    if (!confirm('Сбросить весь комплект «' + SETS.find(s => s.id === state.set).name + '» к ванильным иконкам?')) return;
    const s = SETS.find(x => x.id === state.set);
    for (const p of PARTS) {
      const im = imgs[`${s.id}_${p.id}`];
      state.pixels[s.id][p.id] = px(im, 0, 0).data;
    }
    update();
    setStatus('комплект сброшен');
  };
}

function syncTools() {
  document.querySelectorAll('.tool[data-tool]').forEach(b =>
    b.classList.toggle('active', b.dataset.tool === state.tool));
}

function resizeCanvas() {
  const z = state.zoom;
  const w = state.width, h = state.height;
  // в режиме карт 64×32 холст шире — масштабируем по ширине, чтобы влезал
  const zx = (state.mode === 'layer') ? Math.max(4, Math.round(z * 16 / w)) : z;
  canvas.style.width = (w * zx) + 'px';
  canvas.style.height = (h * zx) + 'px';
  overlay.style.width = (w * zx) + 'px';
  overlay.style.height = (h * zx) + 'px';
}

// ---------------------------------------------------------------- отрисовка
function update() {
  const w = state.width, h = state.height;
  const data = curPixels(state);
  const im = ctx.createImageData(w, h);
  im.data.set(data);
  ctx.putImageData(im, 0, 0);

  // сетка
  const g = document.getElementById('grid');
  octx.clearRect(0, 0, w, h);
  if (g.checked) {
    octx.fillStyle = 'rgba(255,255,255,.10)';
    for (let i = 1; i < w; i++) {
      octx.fillRect(i, 0, 1 / w, h);
    }
    for (let j = 1; j < h; j++) {
      octx.fillRect(0, j, w, 1 / h);
    }
  }

  const part = PARTS.find(p => p.id === state.part);
  const set = SETS.find(s => s.id === state.set);
  const mode = MODES.find(m => m.id === state.mode);
  const what = state.mode === 'layer'
    ? `<b>${set.name} — ${LAYERS.find(L => L.id === state.layer).name}</b> — ${w}×${h}.`
    : `<b>${set.name} ${part.name}</b> — ${w}×${h}.`;
  document.getElementById('slotInfo').innerHTML =
    what + ` Инструмент: ${toolName(state.tool)}, кисть ${state.brush} px.` +
    (state.mirror ? ' <span class="ok">зеркало включено</span>' : '');

  drawPreview();
  drawSlotThumbs();
  refreshJson();
  state.dirty = true;
}

function toolName(t) {
  return { brush: 'кисть', eraser: 'ластик', line: 'линия', rect: 'прямоугольник', bucket: 'заливка', pick: 'пипетка' }[t] || t;
}

function drawSlotThumbs() {
  const box = document.getElementById('slots');
  const kids = box.children;
  for (let i = 0; i < kids.length; i++) {
    const c = kids[i].querySelector('canvas');
    const cx = c.getContext('2d');
    const im = cx.createImageData(16, 16);
    im.data.set(state.pixels[state.set][PARTS[i].id]);
    cx.putImageData(im, 0, 0);
  }
}

// ---------------------------------------------------------------- 3D-превью
// Рисуем коробки 3D-модели брони в изометрии. Текстура берётся из
// нарисованной иконки 16×16 (по «палитре» UV выбираем цвет грани).
const pv = document.getElementById('preview');
const pctx = pv.getContext('2d');
let models = null;

function loadModels() {
  if (models) return models;
  models = { helmet: null, chestplate: null, leggings: null, boots: null };
  return models;
}

const FACE_DIR = {
  north: [0, 0, -1], south: [0, 0, 1], east: [1, 0, 0], west: [-1, 0, 0],
  up: [0, 1, 0], down: [0, -1, 0],
};

function drawPreview() {
  const W = pv.width, H = pv.height;
  pctx.clearRect(0, 0, W, H);
  pctx.fillStyle = '#0e1016';
  pctx.fillRect(0, 0, W, H);

  const m = models3d[state.part];
  if (!m) return;

  const tex = buildTextureFromPixels();
  const a = state.spin ? state.spinAngle : 0.6;
  const cos = Math.cos(a), sin = Math.sin(a);
  const S = 26;                       // масштаб
  const ox = W / 2, oy = H * 0.62;

  // список граней
  const faces = [];
  for (const el of m.elements) {
    const [x0, y0, z0] = el.from, [x1, y1, z1] = el.to;
    for (const [name, f] of Object.entries(el.faces)) {
      const uv = f.uv;
      // UV в модели: [u1,v1,u2,v2] в пикселях палитры; берём центральный пиксель
      const c = samplePalette(tex, uv);
      faces.push({ name, uv, color: c, box: [x0, y0, z0, x1, y1, z1] });
    }
  }

  // сортировка по глубине
  const proj = faces.map(f => {
    const [x0, y0, z0, x1, y1, z1] = f.box;
    const d = (f.name === 'north' || f.name === 'south')
      ? (f.name === 'north' ? z0 : z1)
      : (f.name === 'east' ? x1 : f.name === 'west' ? x0 : (f.name === 'up' ? y1 : y0));
    const dd = d * cos + (x0 + x1) / 2 * sin * 0.0 + d * 0;
    return { f, depth: (x0 + x1) / 2 * -sin + d * cos };
  }).sort((p, q) => p.depth - q.depth);

  // рисуем только «внешние» грани (взвешенная видимость по кубу)
  const drawn = new Set();
  for (const { f } of proj) {
    const [x0, y0, z0, x1, y1, z1] = f.box;
    const pts = quadPoints(f.name, x0, y0, z0, x1, y1, z1);
    // заливка
    pctx.beginPath();
    pts.forEach((p, i) => {
      const X = ox + (p[0] * cos - p[2] * sin) * S;
      const Y = oy + (p[1] * 0.82 - (p[0] * sin + p[2] * cos) * 0.5) * S;
      i ? pctx.lineTo(X, Y) : pctx.moveTo(X, Y);
    });
    pctx.closePath();
    const shade = faceShade(f.name);
    const [r, g, b] = f.color;
    pctx.fillStyle = `rgb(${Math.min(255, r * shade) | 0},${Math.min(255, g * shade) | 0},${Math.min(255, b * shade) | 0})`;
    pctx.fill();
    pctx.strokeStyle = 'rgba(0,0,0,.35)';
    pctx.lineWidth = 1;
    pctx.stroke();
  }
  document.getElementById('previewInfo').textContent =
    `${m.elements.length} коробов · ${tex.length ? 'ваша текстура' : '—'}`;
}

function faceShade(name) {
  return { north: 0.86, south: 0.72, east: 0.94, west: 0.62, up: 1.0, down: 0.5 }[name] || 0.8;
}

function quadPoints(name, x0, y0, z0, x1, y1, z1) {
  switch (name) {
    case 'north': return [[x0, y0, z0], [x1, y0, z0], [x1, y1, z0], [x0, y1, z0]];
    case 'south': return [[x1, y0, z1], [x0, y0, z1], [x0, y1, z1], [x1, y1, z1]];
    case 'west':  return [[x0, y0, z1], [x0, y0, z0], [x0, y1, z0], [x0, y1, z1]];
    case 'east':  return [[x1, y0, z0], [x1, y0, z1], [x1, y1, z1], [x1, y1, z0]];
    case 'up':    return [[x0, y1, z1], [x1, y1, z1], [x1, y1, z0], [x0, y1, z0]];
    case 'down':  return [[x0, y0, z0], [x1, y0, z0], [x1, y0, z1], [x0, y0, z1]];
  }
  return [];
}

// Собираем «текстуру» из нарисованной иконки: палитра модели задаёт,
// какой цвет грани брать. Разложим нарисованные пиксели по частоте
// (тёмное = тело, светлое = блик) и подставим в грани по UV-индексу.
function buildTextureFromPixels() {
  const data = curPixels(state);
  const pal = new Map(); // цвет -> сколько раз
  for (let i = 0; i < 256; i++) {
    const c = [data[i * 4], data[i * 4 + 1], data[i * 4 + 2], data[i * 4 + 3]];
    if (c[3] < 8) continue;
    const key = c.join(',');
    pal.set(key, (pal.get(key) || 0) + 1);
  }
  const arr = [...pal.entries()].sort((a, b) => b[1] - a[1]).map(e => e[0].split(',').map(Number));
  return arr;
}

// По UV-координатам грани берём цвет из палитры модели.
// Модель брони в проекте ссылается на «pal» (256×256 атлас), но мы рисуем
// иконку 16×16, поэтому сопоставляем через номер UV-региона.
function samplePalette(tex, uv) {
  if (!tex.length) return [140, 140, 150];
  // uv = [u1,v1,u2,v2]; берём «регион» по (u1,v1) и раскладываем по палитре
  const idx = Math.floor(uv[0] / 1) + Math.floor(uv[1] / 1);
  const t = tex[idx % tex.length];
  return t;
}

// ---------------------------------------------------------------- JSON
function buildJson() {
  const out = {
    mod: 'riftworld',
    version: '0.52.0',
    format: 'riftworld-armor-pixelmap/1',
    note: 'Иконки 16x16 (parts) и ванильные карты брони 64x32 (layers). ' +
          '0 = прозрачный пиксель, иначе #RRGGBBAA.',
    sets: {},
  };
  for (const s of SETS) {
    const parts = {};
    for (const p of PARTS) {
      parts[p.id] = {
        name: p.name,
        size: [16, 16],
        pixels: pixelsToRows(state.pixels[s.id][p.id], 16, 16),
      };
    }
    const layers = {};
    for (const L of LAYERS) {
      layers[L.id] = {
        name: L.name,
        size: [64, 32],
        pixels: pixelsToRows(state.layers[s.id][L.id], 64, 32),
      };
    }
    out.sets[s.id] = { name: s.name, parts, layers };
  }
  return out;
}

function refreshJson() {
  const j = buildJson();
  const el = document.getElementById('jsonOut');
  const txt = JSON.stringify(j, null, 1);
  el.textContent = txt.length > 4000 ? txt.slice(0, 4000) + '\n… (в файле полностью)' : txt;
  return j;
}

function doExport() {
  const j = buildJson();
  const blob = new Blob([JSON.stringify(j, null, 2)], { type: 'application/json' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = `riftworld-armor-${state.set}.json`;
  a.click();
  setStatus('файл сохранён', 'ok');
}

function doCopy() {
  const txt = JSON.stringify(buildJson(), null, 2);
  navigator.clipboard.writeText(txt).then(
    () => setStatus('JSON скопирован', 'ok'),
    () => setStatus('не удалось скопировать', 'bad'));
}

// ---------------------------------------------------------------- анимация
function loop() {
  if (state.spin) {
    state.spinAngle += 0.012;
    if (state.spinAngle > Math.PI * 2) state.spinAngle = 0;
    drawPreview();
  }
  requestAnimationFrame(loop);
}

/* Стартовый режим — иконки 16×16; размеры холста должны совпадать
   с размером буфера, иначе putImageData падает. */
state.size = [MODES[0].w, MODES[0].h];
state.width = MODES[0].w;
state.height = MODES[0].h;

resizeCanvas();
init().then(() => { loop(); }).catch(e => {
  setStatus('ошибка загрузки: ' + e.message, 'bad');
  console.error(e);
});
