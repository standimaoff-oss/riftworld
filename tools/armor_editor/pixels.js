/* Чистые пиксельные операции редактора брони.
   Вынесены отдельно от DOM, чтобы их можно было прогнать тестами в node.

   Размер холста задаётся в state.size (по умолчанию 16×16 — иконки предмета).
   Тот же код обслуживает и ванильные карты брони 64×32 (layer_1 / layer_2). */
'use strict';

// state передаётся снаружи (см. editor.js). Здесь он не объявляется:
// два `const state` в одной странице — это SyntaxError, из-за которого
// падает весь скрипт, а с ним и вся страница.

function stateW(state) { return (state && state.size && state.size[0]) || 16; }
function stateH(state) { return (state && state.size && state.size[1]) || 16; }

/* Базовая выборка буфера. В editor.js она оборачивается функцией curPixels(),
   которая в режиме «слои» отдаёт state.layers — поэтому здесь другое имя,
   иначе function-декларации столкнутся (hoisting) и получится рекурсия. */
function curPixelsBase(state) { return state.pixels[state.set][state.part]; }
function curPixels(state) { return curPixelsBase(state); }

function hexToRGBA(hex, a) {
  const n = parseInt(hex.slice(1), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255, a];
}
function setPixel(state, data, x, y, rgba) {
  const w = stateW(state), h = stateH(state);
  if (x < 0 || y < 0 || x >= w || y >= h) return;
  if (state.mirror) { const mx = w - 1 - x; write(data, mx, y, rgba, w); }
  write(data, x, y, rgba, w);
}
/* write/getPixel принимают ширину буфера явно: глобального state здесь нет
   (иначе конфликт `const` в браузере — страница падает целиком). */
function write(data, x, y, rgba, w) {
  const i = (y * (w || 16) + x) * 4;
  data[i] = rgba[0]; data[i + 1] = rgba[1]; data[i + 2] = rgba[2]; data[i + 3] = rgba[3];
}
function getPixel(data, x, y, w) {
  const i = (y * (w || 16) + x) * 4;
  return [data[i], data[i + 1], data[i + 2], data[i + 3]];
}
function same(a, b) { return a[0] === b[0] && a[1] === b[1] && a[2] === b[2] && a[3] === b[3]; }

function bucketFill(state, data, x, y, rgba) {
  const w = stateW(state), h = stateH(state);
  const target = getPixel(data, x, y, w);
  if (same(target, rgba)) return;
  const stack = [[x, y]];
  const seen = new Set();
  while (stack.length) {
    const [cx, cy] = stack.pop();
    const k = cy * w + cx;
    if (seen.has(k)) continue;
    seen.add(k);
    if (cx < 0 || cy < 0 || cx >= w || cy >= h) continue;
    if (!same(getPixel(data, cx, cy, w), target)) continue;
    write(data, cx, cy, rgba, w);
    stack.push([cx + 1, cy], [cx - 1, cy], [cx, cy + 1], [cx, cy - 1]);
  }
}
function drawLine(state, data, x0, y0, x1, y1, rgba) {
  const dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
  const sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
  let err = dx - dy, x = x0, y = y0;
  for (let n = 0; n < 400; n++) {
    stamp(state, data, x, y, rgba);
    if (x === x1 && y === y1) break;
    const e2 = 2 * err;
    if (e2 > -dy) { err -= dy; x += sx; }
    if (e2 < dx) { err += dx; y += sy; }
  }
}
function stamp(state, data, x, y, rgba) {
  const s = state.brush;
  const w = stateW(state);
  for (let dy = 0; dy < s; dy++) {
    for (let dx = 0; dx < s; dx++) {
      const px_ = x + dx - ((s - 1) >> 1), py_ = y + dy - ((s - 1) >> 1);
      if (state.mirror) write(data, w - 1 - px_, py_, rgba, w);
      write(data, px_, py_, rgba, w);
    }
  }
}
function rect(state, data, x0, y0, x1, y1, rgba) {
  const ax = Math.min(x0, x1), bx = Math.max(x0, x1);
  const ay = Math.min(y0, y1), by = Math.max(y0, y1);
  for (let y = ay; y <= by; y++) for (let x = ax; x <= bx; x++) setPixel(state, data, x, y, rgba);
}
/** Раскладывает буфер в массив строк «#RRGGBBAA» (или «0» для прозрачного). */
function pixelsToRows(data, width, height) {
  const w = width || 16;
  const h = height || 16;
  const rows = [];
  for (let y = 0; y < h; y++) {
    const row = [];
    for (let x = 0; x < w; x++) {
      const i = (y * w + x) * 4;
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

// Экспорт для node-тестов: функции принимают state первым аргументом.
if (typeof module !== 'undefined' && typeof module.exports !== 'undefined') {
  // Тестовый state — в браузере используется общий из editor.js.
  const testState = {
    set: 'leather', part: 'helmet', pixels: {}, tool: 'brush',
    color: '#b8b8b8', alpha: 255, brush: 1, mirror: false, drag: null,
    size: [16, 16], width: 16, height: 16,
  };
  module.exports = { state: testState, curPixels, curPixelsBase, hexToRGBA, setPixel, write, getPixel, same,
    bucketFill, drawLine, stamp, rect, pixelsToRows, stateW, stateH };
}
