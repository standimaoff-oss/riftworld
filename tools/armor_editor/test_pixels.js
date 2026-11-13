/* Тест чистых пиксельных операций редактора брони.
   Запуск: node tools/armor_editor/test_pixels.js */
'use strict';
const P = require('./pixels.js');

let failed = 0;
function check(name, cond, extra) {
  if (cond) { console.log('  ok  ' + name); }
  else { console.log('  FAIL ' + name + (extra ? ' — ' + extra : '')); failed++; }
}

function fresh() {
  const d = new Uint8ClampedArray(16 * 16 * 4);
  P.state.pixels.leather = { helmet: d };
  P.state.set = 'leather';
  P.state.part = 'helmet';
  P.state.mirror = false;
  P.state.brush = 1;
  P.state.size = [16, 16];
  P.state.width = 16;
  P.state.height = 16;
  return d;
}

/** Холст карты брони 64×32. */
function freshLayer() {
  const d = new Uint8ClampedArray(64 * 32 * 4);
  P.state.pixels.leather = { helmet: new Uint8ClampedArray(16 * 16 * 4) };
  P.state.set = 'leather';
  P.state.part = 'helmet';
  P.state.mirror = false;
  P.state.brush = 1;
  P.state.size = [64, 32];
  P.state.width = 64;
  P.state.height = 32;
  return d;
}

console.log('setPixel / getPixel');
{
  const d = fresh();
  P.setPixel(P.state, d, 3, 4, [10, 20, 30, 255]);
  check('пиксель записан', P.getPixel(d, 3, 4).join(',') === '10,20,30,255');
  check('сосед не тронут', P.getPixel(d, 3, 5).join(',') === '0,0,0,0');
  P.setPixel(P.state, d, -1, 0, [1, 1, 1, 255]);
  P.setPixel(P.state, d, 16, 0, [1, 1, 1, 255]);
  check('вне холста не пишется', P.getPixel(d, 0, 0).join(',') === '0,0,0,0');
}

console.log('зеркало по X');
{
  const d = fresh();
  P.state.mirror = true;
  P.setPixel(P.state, d, 2, 5, [9, 8, 7, 255]);
  check('зеркало пишет в 13', P.getPixel(d, 13, 5).join(',') === '9,8,7,255');
  check('оригинал на месте', P.getPixel(d, 2, 5).join(',') === '9,8,7,255');
  P.state.mirror = false;
}

console.log('заливка');
{
  const d = fresh();
  P.bucketFill(P.state, d, 0, 0, [1, 2, 3, 255]);
  check('весь холст залит', P.getPixel(d, 15, 15).join(',') === '1,2,3,255');
  const d2 = fresh();
  P.setPixel(P.state, d2, 5, 5, [50, 50, 50, 255]);
  P.bucketFill(P.state, d2, 0, 0, [1, 2, 3, 255]);
  check('заливка не переливает через препятствие', P.getPixel(d2, 5, 5).join(',') === '50,50,50,255');
  check('остальное залито', P.getPixel(d2, 0, 0).join(',') === '1,2,3,255');
}

console.log('линия');
{
  const d = fresh();
  P.drawLine(P.state, d, 0, 0, 15, 15, [7, 7, 7, 255]);
  check('диагональ проведена', P.getPixel(d, 8, 8).join(',') === '7,7,7,255');
  check('начало линии', P.getPixel(d, 0, 0).join(',') === '7,7,7,255');
  check('конец линии', P.getPixel(d, 15, 15).join(',') === '7,7,7,255');
}

console.log('кисть');
{
  const d = fresh();
  P.state.brush = 3;
  P.stamp(P.state, d, 4, 4, [5, 5, 5, 255]);
  check('центр кисти 3px', P.getPixel(d, 4, 4).join(',') === '5,5,5,255');
  check('край кисти 3px', P.getPixel(d, 5, 5).join(',') === '5,5,5,255');
  P.state.brush = 1;
}

console.log('прямоугольник');
{
  const d = fresh();
  P.rect(P.state, d, 2, 2, 5, 6, [200, 100, 50, 255]);
  check('угол залит', P.getPixel(d, 2, 2).join(',') === '200,100,50,255');
  check('противоположный угол', P.getPixel(d, 5, 6).join(',') === '200,100,50,255');
  check('вне прямоугольника чисто', P.getPixel(d, 6, 7).join(',') === '0,0,0,0');
}

console.log('экспорт в JSON');
{
  const d = fresh();
  P.setPixel(P.state, d, 0, 0, [255, 0, 0, 255]);
  P.setPixel(P.state, d, 1, 0, [0, 255, 0, 128]);
  const rows = P.pixelsToRows(d);
  check('16 строк', rows.length === 16);
  check('16 колонок', rows[0].length === 16);
  check('прозрачный = 0', rows[0][2] === '0');
  check('непрозрачный = #RRGGBBAA', rows[0][0] === '#ff0000ff');
  check('полупрозрачный = #RRGGBBAA', rows[0][1] === '#00ff0080');
}

console.log('карта брони 64×32');
{
  const d = freshLayer();
  P.setPixel(P.state, d, 63, 31, [12, 34, 56, 255]);
  check('пиксель в углу 64×32 записан', P.getPixel(d, 63, 31, 64).join(',') === '12,34,56,255');
  P.setPixel(P.state, d, 64, 0, [1, 1, 1, 255]);
  check('за правой границей не пишется', P.getPixel(d, 63, 0, 64).join(',') === '0,0,0,0');
  P.setPixel(P.state, d, 0, 32, [1, 1, 1, 255]);
  check('за нижней границей не пишется', P.getPixel(d, 0, 31, 64).join(',') === '0,0,0,0');
  P.state.mirror = true;
  P.setPixel(P.state, d, 0, 5, [9, 9, 9, 255]);
  check('зеркало в карте = 63-x', P.getPixel(d, 63, 5, 64).join(',') === '9,9,9,255');
  P.state.mirror = false;
}

console.log('экспорт карты брони в JSON');
{
  const d = freshLayer();
  P.setPixel(P.state, d, 0, 0, [255, 0, 0, 255]);
  P.setPixel(P.state, d, 1, 0, [0, 255, 0, 128]);
  const rows = P.pixelsToRows(d, 64, 32);
  check('32 строки', rows.length === 32);
  check('64 колонки', rows[0].length === 64);
  check('прозрачный = 0', rows[0][2] === '0');
  check('непрозрачный = #RRGGBBAA', rows[0][0] === '#ff0000ff');
  check('полупрозрачный = #RRGGBBAA', rows[0][1] === '#00ff0080');
  check('размер холста 64×32', P.stateW(P.state) === 64 && P.stateH(P.state) === 32);
}

console.log(failed ? `\nПРОВАЛЕНО: ${failed}` : '\nВСЕ ТЕСТЫ ПРОШЛИ');
process.exit(failed ? 1 : 0);
