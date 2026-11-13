/* Чистые пиксельные операции редактора брони.
   Вынесены отдельно от DOM, чтобы их можно было прогнать тестами в node. */
'use strict';

const state = {
  set: 'leather', part: 'helmet', pixels: {}, tool: 'brush',
  color: '#b8b8b8', alpha: 255, brush: 1, mirror: false, drag: null,
};

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
function rect(data, x0, y0, x1, y1, rgba) {
  const ax = Math.min(x0, x1), bx = Math.max(x0, x1);
  const ay = Math.min(y0, y1), by = Math.max(y0, y1);
  for (let y = ay; y <= by; y++) for (let x = ax; x <= bx; x++) setPixel(data, x, y, rgba);
}
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

if (typeof module !== 'undefined') {
  module.exports = { state, curPixels, hexToRGBA, setPixel, write, getPixel, same,
    bucketFill, drawLine, stamp, rect, pixelsToRows };
}
