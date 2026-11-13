/* Riftworld — редактор брони: рисуем иконки 16×16, экспортируем JSON.
   Данные (ванильные иконки и 3D-модели) лежат рядом в data/. */
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

const state = {
  set: 'leather',
  part: 'helmet',
  // pixels[set][part] = Uint8ClampedArray(16*16*4)
  pixels: {},
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
const imgs = {};        // референсные картинки (ванильные иконки)
const models3d = {};    // 3D-модели брони

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

async function init() {
  for (const s of SETS) {
    state.pixels[s.id] = {};
    for (const p of PARTS) {
      // ванильная иконка как стартовая основа
      const img = await loadImage(`data/${s.ref}_${p.file}.png`);
      imgs[`${s.id}_${p.id}`] = img;
      const d = px(img, 0, 0);
      state.pixels[s.id][p.id] = d.data;
    }
  }
  for (const p of PARTS) {
    const r = await fetch(`data/model3d_${p.file}.json`);
    models3d[p.id] = await r.json();
  }
  buildSets();
  buildSlots();
  buildSwatches();
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
    d.onclick = () => { state.part = p.id; buildSlots(); update(); };
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

// ---------------------------------------------------------------- холст
const canvas = document.getElementById('canvas');
const ctx = canvas.getContext('2d', { willReadFrequently: true });
const overlay = document.getElementById('overlay');
const octx = overlay.getContext('2d');

function curPixels() { return state.pixels[state.set][state.part]; }

function hexToRGBA(hex, a) {
  const n = parseInt(hex.slice(1), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255, a];
}

function setPixel(data, x, y, rgba) {
  if (x < 0 || y < 0 || x >= 16 || y >= 16) return;
  if (state.mirror) { const mx = 15 - x; write(data, mx, y, rgba); }
  write(data, x, y, rgba);
}
function write(data, x, y, rgba) {
  const i = (y * 16 + x) * 4;
  data[i] = rgba[0]; data[i + 1] = rgba[1]; data[i + 2] = rgba[2]; data[i + 3] = rgba[3];
}
function getPixel(data, x, y) {
  const i = (y * 16 + x) * 4;
  return [data[i], data[i + 1], data[i + 2], data[i + 3]];
}
function same(a, b) { return a[0] === b[0] && a[1] === b[1] && a[2] === b[2] && a[3] === b[3]; }

function bucketFill(data, x, y, rgba) {
  const target = getPixel(data, x, y);
  if (same(target, rgba)) return;
  const stack = [[x, y]];
  const seen = new Set();
  while (stack.length) {
    const [cx, cy] = stack.pop();
    const k = cy * 16 + cx;
    if (seen.has(k)) continue;
    seen.add(k);
    if (cx < 0 || cy < 0 || cx >= 16 || cy >= 16) continue;
    if (!same(getPixel(data, cx, cy), target)) continue;
    write(data, cx, cy, rgba);
    stack.push([cx + 1, cy], [cx - 1, cy], [cx, cy + 1], [cx, cy - 1]);
  }
}

function drawLine(data, x0, y0, x1, y1, rgba) {
  const dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
  const sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
  let err = dx - dy, x = x0, y = y0;
  for (let n = 0; n < 200; n++) {
    stamp(data, x, y, rgba);
    if (x === x1 && y === y1) break;
    const e2 = 2 * err;
    if (e2 > -dy) { err -= dy; x += sx; }
    if (e2 < dx) { err += dx; y += sy; }
  }
}

function stamp(data, x, y, rgba) {
  const s = state.brush;
  for (let dy = 0; dy < s; dy++) {
    for (let dx = 0; dx < s; dx++) {
      const px_ = x + dx - ((s - 1) >> 1), py_ = y + dy - ((s - 1) >> 1);
      if (state.mirror) write(data, 15 - px_, py_, rgba);
      write(data, px_, py_, rgba);
    }
  }
}

function canvasPos(ev) {
  const r = canvas.getBoundingClientRect();
  const x = Math.floor((ev.clientX - r.left) / (r.width / 16));
  const y = Math.floor((ev.clientY - r.top) / (r.height / 16));
  return [x, y];
}

function wireCanvas() {
  const onDown = (ev) => {
    ev.preventDefault();
    const [x, y] = canvasPos(ev);
    const data = curPixels();
    const rgba = hexToRGBA(state.color, state.alpha);
    state.drag = { x, y, x0: x, y0: y };
    if (state.tool === 'brush' || state.tool === 'eraser') {
      const c = state.tool === 'eraser' ? [0, 0, 0, 0] : rgba;
      stamp(data, x, y, c);
    } else if (state.tool === 'bucket') {
      bucketFill(data, x, y, rgba);
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
    const data = curPixels();
    const rgba = hexToRGBA(state.color, state.alpha);
    const d = state.drag;
    if (state.tool === 'brush' || state.tool === 'eraser') {
      const c = state.tool === 'eraser' ? [0, 0, 0, 0] : rgba;
      drawLine(data, d.x, d.y, x, y, c);
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
    const data = curPixels();
    const rgba = hexToRGBA(state.color, state.alpha);
    if (state.tool === 'line') {
      drawLine(data, state.drag.x0, state.drag.y0, x, y, rgba);
    } else if (state.tool === 'rect') {
      rect(data, state.drag.x0, state.drag.y0, x, y, rgba);
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
  drawLine(data, state.drag.x0, state.drag.y0, x, y, hexToRGBA(state.color, state.alpha));
}
function rect(data, x0, y0, x1, y1, rgba) {
  const ax = Math.min(x0, x1), bx = Math.max(x0, x1);
  const ay = Math.min(y0, y1), by = Math.max(y0, y1);
  for (let y = ay; y <= by; y++) for (let x = ax; x <= bx; x++) setPixel(data, x, y, rgba);
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
    const d = curPixels();
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
  canvas.style.width = (16 * z) + 'px';
  canvas.style.height = (16 * z) + 'px';
  overlay.style.width = (16 * z) + 'px';
  overlay.style.height = (16 * z) + 'px';
}

// ---------------------------------------------------------------- отрисовка
function update() {
  const data = curPixels();
  const im = ctx.createImageData(16, 16);
  im.data.set(data);
  ctx.putImageData(im, 0, 0);

  // сетка
  const g = document.getElementById('grid');
  if (g.checked) {
    octx.clearRect(0, 0, 16, 16);
    octx.fillStyle = 'rgba(255,255,255,.10)';
    for (let i = 1; i < 16; i++) {
      octx.fillRect(i, 0, 1 / 16, 16);
      octx.fillRect(0, i, 16, 1 / 16);
    }
  } else {
    octx.clearRect(0, 0, 16, 16);
  }

  const part = PARTS.find(p => p.id === state.part);
  const set = SETS.find(s => s.id === state.set);
  document.getElementById('slotInfo').innerHTML =
    `<b>${set.name} ${part.name}</b> — 16×16. Инструмент: ${toolName(state.tool)}, кисть ${state.brush} px.` +
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
  const data = curPixels();
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
function pixelsToRows(data) {
  const rows = [];
  for (let y = 0; y < 16; y++) {
    const row = [];
    for (let x = 0; x < 16; x++) {
      const i = (y * 16 + x) * 4;
      if (data[i + 3] === 0) { row.push('0'); continue; }
      const hex = '#' + [data[i], data[i + 1], data[i + 2]]
        .map(v => v.toString(16).padStart(2, '0')).join('') +
        data[i + 3].toString(16).padStart(2, '0');
      row.push(hex);
    }
    rows.push(row);
  }
  return rows;
}

function buildJson() {
  const out = {
    mod: 'riftworld',
    version: '0.51.0',
    format: 'riftworld-armor-pixelmap/1',
    note: '16x16 item-иконки брони. 0 = прозрачный пиксель, иначе #RRGGBBAA.',
    sets: {},
  };
  for (const s of SETS) {
    const parts = {};
    for (const p of PARTS) {
      parts[p.id] = {
        name: p.name,
        size: [16, 16],
        pixels: pixelsToRows(state.pixels[s.id][p.id]),
      };
    }
    out.sets[s.id] = { name: s.name, parts };
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

resizeCanvas();
init().then(() => { loop(); }).catch(e => {
  setStatus('ошибка загрузки: ' + e.message, 'bad');
  console.error(e);
});
