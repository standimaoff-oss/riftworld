/* Прогон страницы редактора в node с минимальной DOM-заглушкой.
   Проверяем, что скрипт не падает на этапе init() и что после инициализации
   есть пиксели, модели брони и рабочий JSON-экспорт.

   Запуск: node tools/armor_editor/test_page.js */
'use strict';
const fs = require('fs');
const path = require('path');

const HTML = path.join(__dirname, 'riftworld-armor-editor.html');
const html = fs.readFileSync(HTML, 'utf8');

let failed = 0;
function check(name, cond, extra) {
  if (cond) console.log('  ok  ' + name);
  else { console.log('  FAIL ' + name + (extra ? ' — ' + extra : '')); failed++; }
}

// --- минимальная DOM-заглушка -----------------------------------------------
function makeCtx() {
  return {
    createImageData: (w, h) => ({ width: w, height: h, data: new Uint8ClampedArray(w * h * 4) }),
    putImageData: () => {},
    getImageData: (x, y, w, h) => ({ width: w, height: h, data: new Uint8ClampedArray(w * h * 4) }),
    drawImage: () => {},
    clearRect: () => {},
    fillRect: () => {},
    beginPath: () => {}, closePath: () => {}, moveTo: () => {}, lineTo: () => {},
    fill: () => {}, stroke: () => {},
    fillStyle: '', strokeStyle: '', lineWidth: 1,
    canvas: { width: 16, height: 16 },
  };
}
function makeEl(tag) {
  const el = {
    tagName: (tag || 'div').toUpperCase(), width: 16, height: 16,
    style: {}, className: '', textContent: '', value: '0',
    children: [], classList: { add() {}, remove() {}, toggle() {} },
    getContext: () => makeCtx(),
    appendChild(c) { this.children.push(c); return c; },
    set onclick(v) { this._onclick = v; },
    get onclick() { return this._onclick; },
    addEventListener() {}, removeEventListener() {},
    getBoundingClientRect: () => ({ left: 0, top: 0, width: 416, height: 416 }),
    querySelector: (sel) => {
      if (!el._q) el._q = {};
      if (!el._q[sel]) {
        el._q[sel] = makeEl('canvas');
        // canvas внутри слота — это сам canvas слота
        if (sel === 'canvas') el._q[sel] = el;
      }
      return el._q[sel];
    },
    getContext2d: null,
  };
  return el;
}
const elements = {};
global.document = {
  getElementById(id) {
    if (!elements[id]) elements[id] = makeEl('div');
    return elements[id];
  },
  createElement: (t) => makeEl(t),
  querySelectorAll: () => [],
  addEventListener() {},
};
global.window = { addEventListener() {}, devicePixelRatio: 1 };
try {
  Object.defineProperty(global, 'navigator', {
    value: { clipboard: { writeText: () => Promise.resolve() } },
    configurable: true, writable: true,
  });
} catch (e) { /* node может не дать переопределить — не критично */ }
/* Заглушка Image: вытаскивает реальный размер PNG из заголовка файла,
   иначе карты брони 64×32 «схлопнутся» в 16×16 и тест будет врать. */
function pngSize(b64) {
  try {
    const buf = Buffer.from(String(b64).split(',').pop(), 'base64');
    // PNG: 8 байт сигнатура, затем IHDR: длина(4) тип(4) width(4) height(4)
    if (buf.length < 24 || buf.readUInt32BE(12) !== 0x49484452) return [16, 16];
    return [buf.readUInt32BE(16), buf.readUInt32BE(20)];
  } catch (e) { return [16, 16]; }
}
global.Image = class {
  constructor() { this.width = 16; this.height = 16; }
  // onload вызывается синхронно — base64-данные уже внутри страницы
  set src(v) {
    this._src = v;
    if (String(v).startsWith('data:image/png;base64,')) {
      const [w, h] = pngSize(v);
      this.width = w; this.height = h;
    }
    if (this.onload) this.onload();
  }
  get src() { return this._src; }
};
global.Blob = class { constructor() {} };
global.URL = { createObjectURL: () => 'blob:x' };
global.requestAnimationFrame = () => 0;

// --- вытаскиваем и выполняем скрипты страницы -----------------------------
const scripts = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].map(m => m[1]);
check('в странице три скрипта', scripts.length === 3, 'найдено ' + scripts.length);

// Скрипты выполняются в ОДНОМ общем контексте, как в браузере: верхнеуровневый
// var в <script> — это глобальная переменная, и следующий <script> её видит.
// Модульный eval() такую видимость не даёт, поэтому используем vm.
const vm = require('vm');
const sandbox = {
  document: global.document, window: global.window, navigator: global.navigator,
  Image: global.Image, Blob: global.Blob, URL: global.URL,
  requestAnimationFrame: global.requestAnimationFrame, console,
  setTimeout, Math, JSON, Uint8ClampedArray, Promise, parseInt, parseFloat, isNaN,
};
sandbox.globalThis = sandbox;
const ctx = vm.createContext(sandbox);
const errors = [];
for (const s of scripts) {
  try {
    vm.runInContext(s, ctx, { filename: 'page-script.js' });
  } catch (e) {
    errors.push(e.message);
  }
}
check('все скрипты выполняются без исключений', errors.length === 0, errors.join(' | '));

// init() — асинхронный; дожидаемся его через цикл микрозадач.
setTimeout(() => {
  console.log('  (после init)');
  check('статус страницы — не ошибка',
    elements.status && !/ошибка/i.test(elements.status.textContent),
    elements.status ? elements.status.textContent : 'нет статуса');
  check('слоты собраны (4 части)', elements.slots && elements.slots.children.length === 4,
    elements.slots ? String(elements.slots.children.length) : 'нет');
  check('кнопки комплектов собраны (3)', elements.sets && elements.sets.children.length === 3,
    elements.sets ? String(elements.sets.children.length) : 'нет');
  check('палитра собрана', elements.swatches && elements.swatches.children.length > 20,
    elements.swatches ? String(elements.swatches.children.length) : 'нет');
  check('JSON-экспорт показан', elements.jsonOut && elements.jsonOut.textContent.includes('"pixels"'),
    elements.jsonOut ? elements.jsonOut.textContent.slice(0, 80) : 'нет');
  check('превью нарисовано', elements.preview && elements.preview.getContext);
  // v0.52.0: в JSON должны быть и иконки (parts), и ванильные карты брони (layers)
  // Полный JSON (предпросмотр в странице обрезан по длине) — получаем из
  // контекста страницы, а не из node-области: скрипты живут в vm-контексте.
  const full = vm.runInContext('JSON.stringify(buildJson())', ctx);
  const fj = JSON.parse(full);
  check('в JSON есть карты брони 64×32',
    !!fj.sets && !!fj.sets.leather && !!fj.sets.leather.layers
      && !!fj.sets.leather.layers.layer_1 && !!fj.sets.leather.layers.layer_2
      && fj.sets.leather.layers.layer_1.size.join(',') === '64,32'
      && fj.sets.leather.layers.layer_1.pixels.length === 32
      && fj.sets.leather.layers.layer_1.pixels[0].length === 64,
    'layers нет');
  check('иконки остались 16×16',
    fj.sets.leather.parts.helmet.size.join(',') === '16,16'
      && fj.sets.leather.parts.helmet.pixels.length === 16);
  check('кнопки режимов собраны (2)', elements.modes && elements.modes.children.length === 2,
    elements.modes ? String(elements.modes.children.length) : 'нет');

  console.log(failed ? `\nПРОВАЛЕНО: ${failed}` : '\nСТРАНИЦА ПОДНИМАЕТСЯ');
  process.exit(failed ? 1 : 0);
}, 50);
