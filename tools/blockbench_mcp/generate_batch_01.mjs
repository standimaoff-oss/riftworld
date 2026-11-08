#!/usr/bin/env node
/**
 * Batch 01 Generator (V3 Master Polish) via Official Blockbench MCP (`jasonjgardner/blockbench-mcp-plugin`):
 *   01 — Фарфоровый журавль (`porcelain_crane`)
 *   02 — Базальтовый жук-углеед (`basalt_beetle`)
 *   03 — Пепельная моль (`ash_moth`)
 */

import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { ModelBuilder, BlockbenchMcpSession, vAdd, vScale, vNorm, vCross } from './builder_lib.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '../..');

// ============================================================================
// 01 — ФАРФОРОВЫЙ ЖУРАВЛЬ (`porcelain_crane`)
// ============================================================================
function buildPorcelainCrane() {
  const b = new ModelBuilder({
    id: 'porcelain_crane',
    titleRu: 'Фарфоровый журавль',
    biome: 'riftworld:elisar_city',
    atlasSize: 512,
    renderScale: 0.5,
  });

  b.addGroup('root', [0, 0, 0]);
  b.addGroup('body', [0, 36.0, 0.5], [0, 0, 0], 'root', { axis: 'x', amp: 1.2, speed: 0.9, phase: 0 });

  // Elongated, graceful avian porcelain hull (matches full horizontal length & proud curve of orig_01_crane.png!)
  b.addCenteredBox('torso_core', {
    center: [0, 37.0, 0.5],
    size: [13, 13, 22],
    rotation: [6, 0, 0],
    parent: 'body',
    mat: 'porcelain_kintsugi',
  });
  b.addCenteredBox('breast_top', {
    center: [0, 41.4, -8.6],
    size: [10, 7, 8],
    rotation: [-16, 0, 0],
    parent: 'body',
    mat: 'porcelain_kintsugi',
  });
  b.addCenteredBox('breast_mid', {
    center: [0, 37.0, -10.4],
    size: [11, 7, 7],
    rotation: [4, 0, 0],
    parent: 'body',
    mat: 'porcelain_kintsugi',
  });
  b.addCenteredBox('breast_lower', {
    center: [0, 32.6, -8.2],
    size: [10, 6, 9],
    rotation: [24, 0, 0],
    parent: 'body',
    mat: 'porcelain_kintsugi',
  });
  b.addCenteredBox('dorsal_front', {
    center: [0, 43.4, -3.2],
    size: [10, 4, 11],
    rotation: [-3, 0, 0],
    parent: 'body',
    mat: 'porcelain_kintsugi',
  });
  b.addCenteredBox('dorsal_rear', {
    center: [0, 41.6, 6.5],
    size: [9, 4, 12],
    rotation: [-15, 0, 0],
    parent: 'body',
    mat: 'porcelain_kintsugi',
  });
  b.addCenteredBox('belly_under', {
    center: [0, 30.6, 0.5],
    size: [9, 5, 16],
    rotation: [8, 0, 0],
    parent: 'body',
    mat: 'porcelain_kintsugi',
  });
  b.addCenteredBox('rump_cone', {
    center: [0, 36.4, 11.8],
    size: [8, 8, 7],
    rotation: [-16, 0, 0],
    parent: 'body',
    mat: 'porcelain_kintsugi',
  });

  b.addMirrored(() => {
    b.addCenteredBox('torso_flank_l', {
      center: [6.2, 37.2, 0.5],
      size: [3, 10, 18],
      rotation: [6, 3, -6],
      parent: 'body',
      mat: 'porcelain_kintsugi',
    });
  });

  // Slender S-Curved Porcelain Neck
  b.addGroup('neck_base', [0, 43.0, -8.2], [0, 0, 0], 'body', { axis: 'x', amp: 2.0, speed: 0.9, phase: 0.6 });
  const neckPts = [
    [0, 42.2, -8.2],
    [0, 47.4, -10.8],
    [0, 53.2, -10.2],
    [0, 59.2, -7.0],
    [0, 64.8, -5.4],
    [0, 69.2, -7.0],
    [0, 71.6, -9.4],
  ];
  const neckSizes = [
    [7, 7],
    [6, 6],
    [5, 6],
    [5, 5],
    [4, 5],
    [4, 5],
  ];
  for (let i = 0; i < neckSizes.length; i++) {
    b.addSegmentBox(`neck_seg_${i + 1}`, {
      pStart: neckPts[i],
      pEnd: neckPts[i + 1],
      width: neckSizes[i][0],
      depth: neckSizes[i][1],
      upHint: [0, 0, 1],
      padStart: 1.3,
      padEnd: 1.1,
      parent: 'neck_base',
      mat: 'porcelain_kintsugi',
    });
  }

  // Sculpted Porcelain Head & Tapered Golden Beak
  b.addGroup('head', [0, 71.6, -9.4], [0, 0, 0], 'neck_base', { axis: 'x', amp: -1.8, speed: 0.9, phase: 1.1 });
  b.addCenteredBox('head_cranium', {
    center: [0, 72.2, -11.4],
    size: [5, 5, 7],
    rotation: [4, 0, 0],
    parent: 'head',
    mat: 'porcelain_kintsugi',
  });
  b.addCenteredBox('head_crown', {
    center: [0, 74.4, -11.0],
    size: [4, 2, 6],
    rotation: [-6, 0, 0],
    parent: 'head',
    mat: 'porcelain_kintsugi',
  });
  b.addCenteredBox('head_lore', {
    center: [0, 71.8, -14.8],
    size: [4, 3, 3],
    rotation: [6, 0, 0],
    parent: 'head',
    mat: 'porcelain_kintsugi',
  });

  b.addMirrored(() => {
    b.addCenteredBox('eye_l', {
      center: [2.25, 72.5, -12.4],
      size: [1, 2, 2],
      parent: 'head',
      mat: 'obsidian_eye',
    });
  });

  b.addSegmentBox('beak_upper_base', {
    pStart: [0, 71.8, -15.4],
    pEnd: [0, 71.2, -20.4],
    width: 3,
    depth: 2,
    upHint: [0, 1, 0],
    padStart: 0.8,
    padEnd: 0.6,
    parent: 'head',
    mat: 'imperial_gold',
  });
  b.addSegmentBox('beak_upper_mid', {
    pStart: [0, 71.2, -20.1],
    pEnd: [0, 70.6, -25.1],
    width: 2,
    depth: 2,
    upHint: [0, 1, 0],
    padStart: 0.6,
    padEnd: 0.6,
    parent: 'head',
    mat: 'imperial_gold',
  });
  b.addSegmentBox('beak_upper_tip', {
    pStart: [0, 70.6, -24.8],
    pEnd: [0, 70.1, -28.6],
    width: 2,
    depth: 1,
    upHint: [0, 1, 0],
    padStart: 0.6,
    padEnd: 0.2,
    parent: 'head',
    mat: 'imperial_gold_bright',
  });
  b.addSegmentBox('beak_lower', {
    pStart: [0, 70.6, -15.4],
    pEnd: [0, 70.1, -24.4],
    width: 2,
    depth: 1,
    upHint: [0, 1, 0],
    padStart: 0.6,
    padEnd: 0.2,
    parent: 'head',
    mat: 'imperial_gold_dark',
  });

  // Smooth Unified Celadon Wing Mantle + Long Sleek Flight Feathers
  b.addMirrored(() => {
    b.addGroup('wing_l', [6.8, 40.5, -4.0], [0, 0, 0], 'body', { axis: 'z', amp: 1.6, speed: 0.9, phase: 0.3 });

    // Cohesive, coplanar celadon shoulder shield (flush surface normal = zero internal step lines!)
    const nShield = vNorm([0.985, 0.14, 0.08]);
    const uShield = vNorm([-0.10, 0.42, 0.90]);
    b.addPlate('wing_mantle_l_1', {
      center: [7.4, 39.2, 0.2],
      width: 15,
      thickness: 2,
      length: 9,
      uAxis: uShield,
      nAxis: nShield,
      parent: 'wing_l',
      mat: 'celadon_kintsugi',
    });
    b.addPlate('wing_mantle_l_2', {
      center: [7.37, 36.2, -1.2],
      width: 12,
      thickness: 2,
      length: 7,
      uAxis: uShield,
      nAxis: nShield,
      parent: 'wing_l',
      mat: 'celadon_kintsugi',
    });

    // Long pure-celadon primary flight feathers sweeping gracefully down-and-back
    const primaries = [
      { s: [7.25, 38.0, 3.5], e: [6.8, 31.2, 18.2], d: 5 },
      { s: [7.35, 36.5, 4.0], e: [6.6, 28.8, 19.6], d: 5 },
      { s: [7.15, 35.0, 4.2], e: [6.3, 26.8, 19.5], d: 5 },
      { s: [6.85, 33.6, 4.0], e: [5.9, 25.4, 17.8], d: 4 },
    ];
    primaries.forEach((pr, idx) => {
      b.addSegmentBox(`wing_prim_l_${idx + 1}`, {
        pStart: pr.s,
        pEnd: pr.e,
        width: 2,
        depth: pr.d,
        upHint: [1, 0.12, 0],
        padStart: 1.2,
        padEnd: 0.4,
        parent: 'wing_l',
        mat: 'celadon_feather_long',
      });
    });
  });

  // Lush Lotus-Petal Tail Plume Fan (tilted feather vanes so broad porcelain/jade faces show in 3/4 & side views!)
  b.addGroup('tail_base', [0, 36.2, 12.5], [0, 0, 0], 'body', { axis: 'x', amp: 2.0, speed: 0.9, phase: 1.4 });
  b.addCenteredBox('tail_pedestal', {
    center: [0, 36.0, 13.2],
    size: [7, 6, 5],
    rotation: [-14, 0, 0],
    parent: 'tail_base',
    mat: 'porcelain_kintsugi',
  });

  b.addSegmentBox('tail_up_center', {
    pStart: [0, 37.6, 13.2],
    pEnd: [0, 44.2, 21.2],
    width: 5,
    depth: 2,
    upHint: [0, 1, 0],
    padStart: 1.4,
    parent: 'tail_base',
    mat: 'tail_plume_white',
  });
  b.addSegmentBox('tail_mid_center', {
    pStart: [0, 36.0, 13.5],
    pEnd: [0, 33.8, 24.5],
    width: 5,
    depth: 2,
    upHint: [0, 1, 0],
    padStart: 1.4,
    parent: 'tail_base',
    mat: 'tail_plume_mint',
  });
  b.addSegmentBox('tail_low_center', {
    pStart: [0, 33.8, 12.8],
    pEnd: [0, 20.8, 19.5],
    width: 5,
    depth: 2,
    upHint: [0, 0, 1],
    padStart: 1.4,
    parent: 'tail_base',
    mat: 'tail_plume_deep',
  });

  b.addMirrored(() => {
    const lateralPetals = [
      { name: 'tail_up_l_1',  s: [1.4, 37.4, 13.2], e: [4.2, 43.0, 21.0], w: 2, d: 5, mat: 'tail_plume_white', hint: [0.65, 0.75, 0] },
      { name: 'tail_up_l_2',  s: [2.4, 36.8, 13.0], e: [7.4, 40.5, 20.2], w: 2, d: 5, mat: 'tail_plume_white', hint: [0.75, 0.65, 0] },
      { name: 'tail_mid_l_1', s: [1.6, 35.8, 13.5], e: [4.5, 32.8, 24.2], w: 2, d: 5, mat: 'tail_plume_mint',  hint: [0.70, 0.70, 0] },
      { name: 'tail_mid_l_2', s: [2.6, 35.2, 13.0], e: [7.8, 30.6, 22.5], w: 2, d: 5, mat: 'tail_plume_mint',  hint: [0.80, 0.60, 0] },
      { name: 'tail_low_l_1', s: [1.6, 33.8, 12.6], e: [3.6, 20.8, 19.2], w: 2, d: 5, mat: 'tail_plume_deep',  hint: [0.80, 0.20, 0.55] },
      { name: 'tail_low_l_2', s: [2.8, 33.6, 12.2], e: [6.4, 22.4, 17.8], w: 2, d: 5, mat: 'tail_plume_deep',  hint: [0.85, 0.20, 0.48] },
    ];
    for (const pl of lateralPetals) {
      b.addSegmentBox(pl.name, {
        pStart: pl.s,
        pEnd: pl.e,
        width: pl.w,
        depth: pl.d,
        upHint: pl.hint,
        padStart: 1.4,
        parent: 'tail_base',
        mat: pl.mat,
      });
    }
  });

  // Slender Articulated Ceramic Legs & Splayed Toes on Y = 0
  b.addMirrored(() => {
    b.addGroup('leg_l', [3.6, 30.5, 0.0], [0, 0, 0], 'root');

    b.addSegmentBox('thigh_cuff_l', {
      pStart: [3.6, 32.0, -0.2],
      pEnd: [3.6, 25.8, 0.8],
      width: 5,
      depth: 6,
      upHint: [0, 0, 1],
      padStart: 1.5,
      padEnd: 0.6,
      parent: 'leg_l',
      mat: 'porcelain_kintsugi',
    });
    b.addSegmentBox('tibia_upper_l', {
      pStart: [3.6, 26.2, 0.7],
      pEnd: [3.6, 20.5, 1.0],
      width: 3,
      depth: 3,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.8,
      parent: 'leg_l',
      mat: 'crane_leg_ivory',
    });
    b.addCenteredBox('hock_joint_l', {
      center: [3.6, 19.5, 1.0],
      size: [4, 4, 4],
      parent: 'leg_l',
      mat: 'crane_leg_joint',
    });
    b.addCenteredBox('tarsus_shaft_l', {
      center: [3.6, 10.0, 1.0],
      size: [2, 17, 2],
      parent: 'leg_l',
      mat: 'crane_leg_ivory',
    });
    b.addCenteredBox('ankle_base_l', {
      center: [3.6, 1.5, 0.8],
      size: [3, 2, 3],
      parent: 'leg_l',
      mat: 'crane_leg_joint',
    });
    b.addCenteredBox('toe_mid_l', {
      center: [3.6, 0.5, -2.6],
      size: [1, 1, 6],
      parent: 'leg_l',
      mat: 'crane_leg_ivory',
    });
    b.addCenteredBox('toe_inner_l', {
      center: [2.3, 0.5, -2.0],
      size: [1, 1, 5],
      origin: [3.2, 0.5, 0.2],
      rotation: [0, 24, 0],
      parent: 'leg_l',
      mat: 'crane_leg_ivory',
    });
    b.addCenteredBox('toe_outer_l', {
      center: [4.9, 0.5, -2.0],
      size: [1, 1, 5],
      origin: [4.0, 0.5, 0.2],
      rotation: [0, -24, 0],
      parent: 'leg_l',
      mat: 'crane_leg_ivory',
    });
    b.addCenteredBox('toe_back_l', {
      center: [3.6, 0.5, 2.8],
      size: [1, 1, 3],
      parent: 'leg_l',
      mat: 'crane_leg_ivory',
    });
  });

  b.addAnimation('idle', {
    length: 4.0,
    loop: 'loop',
    snapping: 24,
    tracks: [
      { bone: 'body', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [1.2, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'neck_base', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [2.2, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'head', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [-2.8, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'wing_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [0, 0, 1.4] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'wing_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [0, 0, -1.4] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'tail_base', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [2.0, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// 02 — БАЗАЛЬТОВЫЙ ЖУК-УГЛЕЕД (`basalt_beetle`)
// ============================================================================
function buildBasaltBeetle() {
  const b = new ModelBuilder({
    id: 'basalt_beetle',
    titleRu: 'Базальтовый жук-углеед',
    biome: 'riftworld:ashen_lands',
    atlasSize: 512,
    renderScale: 0.5,
  });

  b.addGroup('root', [0, 0, 0]);
  b.addGroup('body', [0, 18.0, 2.0], [0, 0, 0], 'root', { axis: 'x', amp: 0.9, speed: 0.85, phase: 0 });

  // 1. Inner Blazing Molten Magma Core
  b.addCenteredBox('magma_core_main', {
    center: [0, 20.5, 3.0],
    size: [27, 15, 33],
    parent: 'body',
    mat: 'molten_magma',
  });
  b.addCenteredBox('magma_core_crown', {
    center: [0, 29.0, 3.0],
    size: [21, 6, 26],
    parent: 'body',
    mat: 'molten_magma',
  });
  b.addCenteredBox('magma_core_neck', {
    center: [0, 20.0, -14.8],
    size: [21, 12, 9],
    rotation: [18, 0, 0],
    parent: 'body',
    mat: 'molten_magma',
  });
  b.addCenteredBox('magma_core_rear', {
    center: [0, 20.0, 20.5],
    size: [21, 12, 8],
    rotation: [-22, 0, 0],
    parent: 'body',
    mat: 'molten_magma',
  });
  b.addMirrored(() => {
    b.addCenteredBox('magma_core_flank_l', {
      center: [14.5, 20.0, 3.0],
      size: [4, 12, 27],
      parent: 'body',
      mat: 'molten_magma',
    });
  });

  b.addCenteredBox('sternum_belly', {
    center: [0, 12.0, 1.5],
    size: [23, 3, 33],
    parent: 'body',
    mat: 'basalt_underbelly',
  });

  // 2. Closely-Fitted Heavy Tectonic Basalt Armor Slabs (leaving crisp ~1.5-unit glowing magma cracks!)
  b.addCenteredBox('pronotum_top', {
    center: [0, 28.0, -13.0],
    size: [15, 4, 10],
    rotation: [22, 0, 0],
    parent: 'body',
    mat: 'basalt_plate',
  });
  b.addMirrored(() => {
    b.addCenteredBox('pronotum_shoulder_l', {
      center: [10.6, 25.0, -12.6],
      size: [9, 4, 10],
      rotation: [18, -6, -36],
      parent: 'body',
      mat: 'basalt_plate',
    });
    b.addCenteredBox('pronotum_cheek_l', {
      center: [14.6, 18.8, -12.8],
      size: [4, 9, 10],
      rotation: [5, -5, -10],
      parent: 'body',
      mat: 'basalt_plate',
    });
  });

  const dorsalPlates = [
    { name: 'elytra_dorsal_1', c: [0, 31.8, -4.6], s: [14, 4, 8], r: [15, 0, 0] },
    { name: 'elytra_dorsal_2', c: [0, 33.2,  3.6], s: [15, 4, 8], r: [2, 0, 0] },
    { name: 'elytra_dorsal_3', c: [0, 31.8, 11.8], s: [14, 4, 8], r: [-14, 0, 0] },
    { name: 'elytra_dorsal_4', c: [0, 28.2, 19.2], s: [12, 4, 7], r: [-32, 0, 0] },
  ];
  for (const dp of dorsalPlates) {
    b.addCenteredBox(dp.name, {
      center: dp.c,
      size: dp.s,
      rotation: dp.r,
      parent: 'body',
      mat: 'basalt_plate',
    });
  }

  b.addCenteredBox('rear_pygidium_center', {
    center: [0, 21.2, 23.8],
    size: [11, 9, 4],
    rotation: [-22, 0, 0],
    parent: 'body',
    mat: 'basalt_plate',
  });

  b.addMirrored(() => {
    const midDome = [
      { name: 'elytra_mid_l_1', c: [11.4, 28.4, -4.6], s: [10, 4, 8], r: [14, -4, -36] },
      { name: 'elytra_mid_l_2', c: [12.2, 29.6,  3.6], s: [10, 4, 8], r: [2, 0, -38] },
      { name: 'elytra_mid_l_3', c: [11.4, 28.4, 11.8], s: [10, 4, 8], r: [-13, 4, -36] },
      { name: 'elytra_mid_l_4', c: [9.6,  25.2, 19.0], s: [9,  4, 7], r: [-28, 9, -34] },
    ];
    for (const mp of midDome) {
      b.addCenteredBox(mp.name, {
        center: mp.c,
        size: mp.s,
        rotation: mp.r,
        parent: 'body',
        mat: 'basalt_plate',
      });
    }

    const lowerSkirt = [
      { name: 'elytra_skirt_l_1', c: [16.2, 20.0, -4.8], s: [4, 11, 8], r: [4, -3, -10] },
      { name: 'elytra_skirt_l_2', c: [16.8, 19.6,  3.6], s: [4, 10, 8], r: [0, 0, -10] },
      { name: 'elytra_skirt_l_3', c: [16.2, 20.2, 11.8], s: [4, 11, 8], r: [-4, 3, -10] },
      { name: 'elytra_skirt_l_4', c: [14.4, 19.0, 19.0], s: [4, 10, 7], r: [-8, 12, -10] },
      { name: 'rear_pygidium_l',  c: [8.6,  20.2, 23.2], s: [8,  9, 4], r: [-18, 22, -8] },
    ];
    for (const sk of lowerSkirt) {
      b.addCenteredBox(sk.name, {
        center: sk.c,
        size: sk.s,
        rotation: sk.r,
        parent: 'body',
        mat: 'basalt_plate',
      });
    }
  });

  // 3. Armored Volcanic Head, Glowing Eyes, Rhino Horn, Molten Cheek Horns, Mandibles & Antennae
  b.addGroup('head', [0, 17.5, -18.2], [0, 0, 0], 'body', { axis: 'x', amp: 1.8, speed: 0.85, phase: 0.5 });

  b.addCenteredBox('head_cranium', {
    center: [0, 17.8, -21.8],
    size: [14, 9, 9],
    parent: 'head',
    mat: 'basalt_plate',
  });
  b.addCenteredBox('head_frons_shield', {
    center: [0, 22.0, -21.2],
    size: [11, 4, 8],
    rotation: [24, 0, 0],
    parent: 'head',
    mat: 'basalt_plate',
  });
  b.addCenteredBox('snout_bridge', {
    center: [0, 16.8, -26.8],
    size: [8, 6, 5],
    rotation: [12, 0, 0],
    parent: 'head',
    mat: 'basalt_horn',
  });

  b.addSegmentBox('rhino_horn_1', {
    pStart: [0, 16.4, -28.0],
    pEnd: [0, 17.8, -32.5],
    width: 6,
    depth: 5,
    upHint: [0, 1, 0],
    padStart: 1.0,
    padEnd: 0.8,
    parent: 'head',
    mat: 'basalt_horn',
  });
  b.addSegmentBox('rhino_horn_2', {
    pStart: [0, 17.6, -32.0],
    pEnd: [0, 20.5, -35.5],
    width: 5,
    depth: 4,
    upHint: [0, 1, 0],
    padStart: 0.8,
    padEnd: 0.6,
    parent: 'head',
    mat: 'basalt_horn',
  });
  b.addSegmentBox('rhino_horn_3', {
    pStart: [0, 20.2, -35.0],
    pEnd: [0, 23.6, -37.0],
    width: 4,
    depth: 4,
    upHint: [0, 0, 1],
    padStart: 0.8,
    padEnd: 0.2,
    parent: 'head',
    mat: 'basalt_horn',
  });

  b.addMirrored(() => {
    b.addCenteredBox('brow_ridge_l', {
      center: [6.4, 20.4, -23.0],
      size: [4, 3, 6],
      rotation: [8, -8, -16],
      parent: 'head',
      mat: 'basalt_horn',
    });
    // Prominent blazing orange magma eye beneath brow ridge!
    b.addCenteredBox('eye_l', {
      center: [6.9, 17.8, -24.2],
      size: [2, 3, 3],
      parent: 'head',
      mat: 'magma_eye',
    });

    const hornPts = [
      [6.4,  19.4, -21.0],
      [9.8,  20.8, -24.6],
      [11.6, 24.6, -27.4],
      [11.4, 30.0, -28.2],
    ];
    const hornSizes = [
      { w: 5, d: 5, t0: 0.0,  t1: 0.36 },
      { w: 4, d: 4, t0: 0.34, t1: 0.70 },
      { w: 4, d: 4, t0: 0.68, t1: 1.00 },
    ];
    for (let i = 0; i < hornSizes.length; i++) {
      b.addSegmentBox(`molten_horn_l_${i + 1}`, {
        pStart: hornPts[i],
        pEnd: hornPts[i + 1],
        width: hornSizes[i].w,
        depth: hornSizes[i].d,
        upHint: [0, 0, 1],
        padStart: 1.0,
        padEnd: i === hornSizes.length - 1 ? 0.2 : 0.8,
        parent: 'head',
        mat: 'molten_horn',
        meta: { hornT0: hornSizes[i].t0, hornT1: hornSizes[i].t1 },
      });
    }

    b.addGroup('mandible_l', [5.2, 14.2, -24.8], [0, 0, 0], 'head', { axis: 'y', amp: 3.5, speed: 1.1, phase: 0 });
    b.addSegmentBox('mandible_base_l', {
      pStart: [5.2, 14.2, -24.5],
      pEnd: [6.5, 13.2, -29.2],
      width: 3,
      depth: 3,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.6,
      parent: 'mandible_l',
      mat: 'basalt_mandible',
    });
    b.addSegmentBox('mandible_tip_l', {
      pStart: [6.3, 13.2, -28.8],
      pEnd: [3.2, 12.8, -32.4],
      width: 3,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.2,
      parent: 'mandible_l',
      mat: 'basalt_mandible',
    });

    b.addGroup('antenna_l', [4.6, 23.2, -19.0], [0, 0, 0], 'head', { axis: 'x', amp: 2.5, speed: 1.2, phase: 0.8 });
    const antPts = [
      [4.6,  23.0, -19.0],
      [6.2,  26.8, -21.2],
      [7.8,  30.4, -23.8],
      [9.4,  33.6, -26.4],
      [10.6, 36.2, -28.8],
    ];
    for (let i = 0; i < 4; i++) {
      const isTip = i === 3;
      b.addSegmentBox(`antenna_seg_l_${i + 1}`, {
        pStart: antPts[i],
        pEnd: antPts[i + 1],
        width: isTip ? 4 : 3,
        depth: isTip ? 4 : 3,
        upHint: [0, 0, 1],
        padStart: 0.9,
        padEnd: isTip ? 0.2 : 0.7,
        parent: 'antenna_l',
        mat: 'basalt_horn',
      });
    }
  });

  // 4. Six Heavy Articulated Basalt Legs with 2-Toed Cleft Feet on Y = 0
  b.addMirrored(() => {
    const legSpecs = [
      { name: 'leg_front_l', hip: [12.0, 13.2, -11.8], knee: [18.0, 11.4, -15.5], ank: [21.4, 2.5, -18.8], footZ: -20.8 },
      { name: 'leg_mid_l',   hip: [12.8, 13.2,   1.5], knee: [19.4, 11.4,   1.5], ank: [22.6, 2.5,   1.5], footZ:  -0.5 },
      { name: 'leg_back_l',  hip: [12.0, 13.2,  13.8], knee: [18.2, 11.4,  17.2], ank: [21.8, 2.5,  20.8], footZ:  19.0 },
    ];
    for (const lg of legSpecs) {
      b.addGroup(lg.name, lg.hip, [0, 0, 0], 'root');
      b.addCenteredBox(`${lg.name}_coxa`, { center: lg.hip, size: [5, 5, 5], parent: lg.name, mat: 'basalt_leg' });
      b.addSegmentBox(`${lg.name}_femur`, { pStart: lg.hip, pEnd: lg.knee, width: 5, depth: 5, upHint: [0, 1, 0], padStart: 1.2, padEnd: 1.0, parent: lg.name, mat: 'basalt_leg' });
      b.addSegmentBox(`${lg.name}_tibia`, { pStart: lg.knee, pEnd: lg.ank, width: 7, depth: 7, upHint: [0, 0, 1], padStart: 1.4, padEnd: 0.8, parent: lg.name, mat: 'basalt_leg' });
      b.addCenteredBox(`${lg.name}_heel`, { center: [lg.ank[0], 1.5, lg.ank[2]], size: [7, 3, 6], parent: lg.name, mat: 'basalt_leg' });
      b.addCenteredBox(`${lg.name}_toe_in`, { center: [lg.ank[0] - 1.9, 1.5, lg.footZ], size: [3, 3, 4], parent: lg.name, mat: 'basalt_horn' });
      b.addCenteredBox(`${lg.name}_toe_out`, { center: [lg.ank[0] + 1.9, 1.5, lg.footZ], size: [3, 3, 4], parent: lg.name, mat: 'basalt_horn' });
    }
  });

  b.addAnimation('idle', {
    length: 4.0,
    loop: 'loop',
    snapping: 24,
    tracks: [
      { bone: 'body', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [1.0, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'head', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [-2.0, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'mandible_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [0, 4.0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'mandible_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [0, -4.0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'antenna_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [3.0, 0, -2.0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'antenna_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [3.0, 0, 2.0] }, { time: 4.0, value: [0, 0, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// 03 — ПЕПЕЛЬНАЯ МОЛЬ (`ash_moth`)
// ============================================================================
function buildAshMoth() {
  const b = new ModelBuilder({
    id: 'ash_moth',
    titleRu: 'Пепельная моль',
    biome: 'riftworld:ashen_lands',
    atlasSize: 512,
    renderScale: 0.5,
  });

  b.addGroup('root', [0, 24.0, 0]);
  b.addGroup('thorax', [0, 25.5, -3.5], [0, 0, 0], 'root', { axis: 'x', amp: 1.5, speed: 1.4, phase: 0 });

  // Plush woolly thorax & thick pronotum ruff collar pitched ~50 deg in hovering flight!
  b.addCenteredBox('thorax_core', {
    center: [0, 25.5, -3.5],
    size: [12, 11, 11],
    rotation: [48, 0, 0],
    parent: 'thorax',
    mat: 'moth_wool_mid',
  });
  b.addCenteredBox('collar_dorsal', {
    center: [0, 30.2, -6.8],
    size: [13, 6, 8],
    rotation: [38, 0, 0],
    parent: 'thorax',
    mat: 'moth_wool_light',
  });
  b.addCenteredBox('collar_chest', {
    center: [0, 24.0, -9.2],
    size: [11, 6, 7],
    rotation: [48, 0, 0],
    parent: 'thorax',
    mat: 'moth_wool_light',
  });
  b.addMirrored(() => {
    b.addCenteredBox('collar_fluff_l', {
      center: [6.0, 27.4, -6.5],
      size: [4, 8, 8],
      rotation: [42, -8, -10],
      parent: 'thorax',
      mat: 'moth_wool_light',
    });
  });

  // Cute Fuzzy Head, Prominent Front-Facing Dark Eyes & Bipectinate Feathered Antennae!
  b.addGroup('head', [0, 29.6, -10.2], [0, 0, 0], 'thorax', { axis: 'x', amp: -1.6, speed: 1.4, phase: 0.4 });
  b.addCenteredBox('head_main', {
    center: [0, 30.0, -11.8],
    size: [9, 7, 7],
    rotation: [26, 0, 0],
    parent: 'head',
    mat: 'moth_wool_light',
  });
  b.addCenteredBox('head_frons_ridge', {
    center: [0, 29.4, -15.0],
    size: [3, 5, 2],
    rotation: [26, 0, 0],
    parent: 'head',
    mat: 'moth_wool_light',
  });

  b.addMirrored(() => {
    b.addCenteredBox('eye_l', {
      center: [2.9, 29.5, -14.8],
      size: [3, 3, 3],
      rotation: [26, 0, 0],
      parent: 'head',
      mat: 'moth_eye',
    });

    b.addGroup('antenna_l', [2.4, 33.2, -12.4], [0, 0, 0], 'head', { axis: 'z', amp: 2.8, speed: 1.6, phase: 0.3 });
    const antPts = [
      [2.4,  33.0, -12.4],
      [4.8,  36.8, -13.6],
      [7.5,  40.4, -14.6],
      [10.0, 43.4, -15.4],
    ];
    for (let i = 0; i < 3; i++) {
      b.addSegmentBox(`antenna_shaft_l_${i + 1}`, {
        pStart: antPts[i],
        pEnd: antPts[i + 1],
        width: 2,
        depth: 2,
        upHint: [0, 0, 1],
        padStart: 0.8,
        padEnd: i === 2 ? 0.2 : 0.6,
        parent: 'antenna_l',
        mat: 'moth_antenna_shaft',
      });
    }

    for (let k = 0; k < 6; k++) {
      const frac = (k + 0.65) / 6.2;
      const segIdx = Math.min(2, Math.floor(frac * 3));
      const localT = frac * 3 - segIdx;
      const pA = antPts[segIdx];
      const pB = antPts[segIdx + 1];
      const cx = pA[0] + (pB[0] - pA[0]) * localT;
      const cy = pA[1] + (pB[1] - pA[1]) * localT;
      const cz = pA[2] + (pB[2] - pA[2]) * localT;
      const barWidth = k === 0 || k === 5 ? 4 : 6;
      b.addCenteredBox(`antenna_comb_l_${k + 1}`, {
        center: [cx, cy, cz],
        size: [barWidth, 1, 2],
        rotation: [18, -8, -32],
        parent: 'antenna_l',
        mat: 'moth_antenna_comb',
      });
    }
  });

  // 6 Tucked Thoracic Legs
  b.addMirrored(() => {
    const legRows = [
      { name: 'moth_leg_front_l', s: [3.2, 23.6, -9.2], m: [3.6, 20.4, -9.8], e: [3.2, 17.8, -10.0] },
      { name: 'moth_leg_mid_l',   s: [3.6, 20.6, -6.2], m: [4.0, 17.2, -6.6], e: [3.6, 14.6,  -6.8] },
      { name: 'moth_leg_back_l',  s: [3.4, 17.6, -3.2], m: [3.8, 14.2, -3.5], e: [3.4, 11.6,  -3.6] },
    ];
    for (const lg of legRows) {
      b.addSegmentBox(`${lg.name}_femur`, { pStart: lg.s, pEnd: lg.m, width: 2, depth: 2, upHint: [0, 0, 1], padStart: 1.0, padEnd: 0.6, parent: 'thorax', mat: 'moth_wool_mid' });
      b.addSegmentBox(`${lg.name}_paw`, { pStart: lg.m, pEnd: lg.e, width: 2, depth: 2, upHint: [0, 0, 1], padStart: 0.8, padEnd: 0.2, parent: 'thorax', mat: 'moth_leg_dark' });
    }
  });

  // Prominent Segmented Woolly Abdomen hanging clearly visible below thorax between hindwings!
  b.addGroup('abdomen_base', [0, 20.5, -0.2], [0, 0, 0], 'thorax', { axis: 'x', amp: 2.2, speed: 1.4, phase: 0.8 });
  const abdSegs = [
    { name: 'abdomen_1', c: [0, 18.6, 1.2], s: [11, 8, 8], r: [56, 0, 0] },
    { name: 'abdomen_2', c: [0, 14.2, 4.0], s: [10, 7, 7], r: [58, 0, 0] },
    { name: 'abdomen_3', c: [0, 10.2, 6.4], s: [8,  6, 6], r: [60, 0, 0] },
    { name: 'abdomen_4', c: [0,  6.8, 8.4], s: [6,  5, 5], r: [62, 0, 0] },
  ];
  for (const ab of abdSegs) {
    b.addCenteredBox(ab.name, {
      center: ab.c,
      size: ab.s,
      rotation: ab.r,
      parent: 'abdomen_base',
      mat: 'moth_abdomen',
    });
  }

  // COPLANAR BROAD SCULPTED WINGS (~55 deg pitch facing forward & upward — zero rib stripes, smooth Hytale canvas!)
  // Orthonormal wing basis for left wing (`+X` side):
  // - `uSpan`: sweeps outward (+X), slightly upward (+Y), and slightly forward (-Z) -> `[0.955, 0.275, -0.110]`
  // - `vChord`: sweeps steeply down (-Y) and back (+Z) -> matching `atlas_painter.py` `[0, -0.82, 0.57]` orthogonalized!
  const uSpan = vNorm([0.955, 0.275, -0.115]);
  const rawChord = [0.0, -0.82, 0.57];
  const nWing = vNorm(vCross(rawChord, uSpan)); // Points forward (-Z) and upward (+Y)!
  const vChord = vNorm(vCross(uSpan, nWing));   // Exact in-plane chord vector!

  const wingAnchor = [5.2, 30.0, -7.0];
  const wPos = (s, c, n = 0) => vAdd(vAdd(wingAnchor, vScale(uSpan, s)), vAdd(vScale(vChord, c), vScale(nWing, n)));

  b.addMirrored(() => {
    b.addGroup('forewing_l', wingAnchor, [0, 0, 0], 'thorax', { axis: 'z', amp: 6.5, speed: 1.4, phase: 0 });

    // Leading-edge thickened costal spar (sweeping along leading edge `c ~ 0 -> -4.5` out to glowing ember tip `s = 42`!)
    const costaPts = [
      wPos(0.0,   1.0, 0.2),
      wPos(14.5, -1.2, 0.2),
      wPos(28.5, -3.5, 0.2),
      wPos(42.0, -5.6, 0.2),
    ];
    for (let i = 0; i < 3; i++) {
      b.addSegmentBox(`fw_costa_l_${i + 1}`, {
        pStart: costaPts[i],
        pEnd: costaPts[i + 1],
        width: 3,
        depth: 5,
        upHint: vChord,
        padStart: 1.0,
        padEnd: i === 2 ? 0.3 : 0.8,
        parent: 'forewing_l',
        mat: i === 2 ? 'moth_forewing_ember' : 'moth_forewing_costa',
      });
    }

    // 4 broad coplanar rectangular wing panels forming a smooth triangular forewing with scalloped outer margin!
    // Because ALL 4 panels use `addPlate` with exact `uAxis = uSpan, nAxis = nWing`, their faces are 100% COPLANAR (no rib shadows)!
    const fwPanels = [
      // Main broad inner-to-apical panel
      { name: 'fw_panel_l_1', center: wPos(20.5, 1.2,  0.00), w: 40, l: 10, mat: 'moth_forewing_panel' },
      // Mid-chord broad panel (steps in at outer margin for natural Saturniid scallop!)
      { name: 'fw_panel_l_2', center: wPos(18.5, 6.2, -0.04), w: 34, l:  9, mat: 'moth_forewing_panel' },
      // Trailing-chord panel
      { name: 'fw_panel_l_3', center: wPos(15.5, 10.2, -0.08), w: 26, l:  7, mat: 'moth_forewing_panel' },
      // Outer apical ember tip extension
      { name: 'fw_panel_l_4', center: wPos(37.5, -2.5,  0.04), w: 10, l:  7, mat: 'moth_forewing_ember' },
    ];
    for (const fp of fwPanels) {
      b.addPlate(fp.name, {
        center: fp.center,
        width: fp.w,
        thickness: 2,
        length: fp.l,
        uAxis: uSpan,
        nAxis: nWing,
        parent: 'forewing_l',
        mat: fp.mat,
      });
    }

    // Tucked Broad Rounded Coplanar Hindwing (displaying an unbroken, razor-sharp round Saturniid Eyespot!)
    const hwOrigin = wPos(2.0, 11.5, -1.2);
    b.addGroup('hindwing_l', hwOrigin, [0, 0, 0], 'thorax', { axis: 'z', amp: 5.0, speed: 1.4, phase: 0.2 });

    // Slightly less swept span vector for rounded hindwing fan, positioned cleanly below forewing so 100% of eyespot is visible!
    const uHind = vNorm([0.985, 0.06, -0.16]);
    const nHind = vNorm(vCross(rawChord, uHind));
    const hwPos = (s, c, n = 0) => vAdd(vAdd([4.8, 22.8, -0.6], vScale(uHind, s)), vAdd(vScale(vChord, c), vScale(nHind, n)));

    const hwPanels = [
      // Central broad eyespot canvas (`19 x 12` unbroken coplanar plate!)
      { name: 'hw_panel_l_main',  center: hwPos(10.8, 7.2,  0.00), w: 19, l: 12 },
      // Rounded outer lobe
      { name: 'hw_panel_l_outer', center: hwPos(13.8, 6.2, -0.03), w: 16, l:  9 },
      // Rounded lower/trailing margin lobe
      { name: 'hw_panel_l_low',   center: hwPos(9.8, 11.2, -0.06), w: 14, l:  7 },
    ];
    for (const hp of hwPanels) {
      b.addPlate(hp.name, {
        center: hp.center,
        width: hp.w,
        thickness: 2,
        length: hp.l,
        uAxis: uHind,
        nAxis: nHind,
        parent: 'hindwing_l',
        mat: 'moth_hindwing_eye',
      });
    }

    // Floating volcanic ash motes trailing beneath hindwings & abdomen!
    b.addGroup('ash_motes_l', [12.0, 8.0, 7.0], [0, 0, 0], 'root');
    const motes = [
      { name: 'ash_mote_l_1', c: [7.2,   7.5,  4.5], r: [15, 22, -10] },
      { name: 'ash_mote_l_2', c: [13.5,  6.2,  9.2], r: [-18, 14, 20] },
      { name: 'ash_mote_l_3', c: [19.2,  8.6, 11.4], r: [24, -16, 12] },
    ];
    for (const mt of motes) {
      b.addCenteredBox(mt.name, {
        center: mt.c,
        size: [2, 2, 2],
        rotation: mt.r,
        parent: 'ash_motes_l',
        mat: 'ash_mote',
      });
      b.freeElements.push(mt.name, mt.name.replace('_l_', '_r_'));
    }
  });

  b.addAnimation('idle', {
    length: 2.4,
    loop: 'loop',
    snapping: 24,
    tracks: [
      { bone: 'root', channel: 'position', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.2, value: [0, 1.8, 0] }, { time: 2.4, value: [0, 0, 0] }] },
      { bone: 'forewing_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.2, value: [0, 0, 8.0] }, { time: 2.4, value: [0, 0, 0] }] },
      { bone: 'forewing_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.2, value: [0, 0, -8.0] }, { time: 2.4, value: [0, 0, 0] }] },
      { bone: 'hindwing_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.2, value: [0, 0, 6.0] }, { time: 2.4, value: [0, 0, 0] }] },
      { bone: 'hindwing_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.2, value: [0, 0, -6.0] }, { time: 2.4, value: [0, 0, 0] }] },
      { bone: 'abdomen_base', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.2, value: [-3.0, 0, 0] }, { time: 2.4, value: [0, 0, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// MAIN PIPELINE
// ============================================================================
async function main() {
  const builders = [
    buildPorcelainCrane(),
    buildBasaltBeetle(),
    buildAshMoth(),
  ];

  const tmpSpecDir = path.join(os.tmpdir(), 'riftworld-bb-mcp-specs');
  fs.mkdirSync(tmpSpecDir, { recursive: true });

  const mcp = await new BlockbenchMcpSession(repoRoot).connect();
  const validationSummary = [];

  try {
    for (const builder of builders) {
      console.log(`\n=== Building ${builder.id} (${builder.titleRu}) via Blockbench MCP ===`);

      const packInfo = builder.packBoxUvs(2);
      console.log(`[uv] Packed ${builder.cubes.length} rectangular cuboids into ${builder.atlasSize}x${packInfo.usedHeight}px`);

      const specPath = path.join(tmpSpecDir, `${builder.id}_spec.json`);
      builder.exportPainterSpec(specPath);

      const texRel = `mod/src/main/resources/assets/riftworld/textures/entity/${builder.id}.png`;
      const texAbs = path.join(repoRoot, texRel);
      execFileSync('python3', [path.join(__dirname, 'atlas_painter.py'), specPath, texAbs], { stdio: 'inherit' });

      const bbRel = `blockbench/riftworld_fauna/${builder.id}.bbmodel`;
      const bedrockGeoRel = `blockbench/riftworld_fauna/bedrock/${builder.id}.geo.json`;
      const bedrockAnimRel = `blockbench/riftworld_fauna/bedrock/${builder.id}.animation.json`;

      const mcpRes = await mcp.buildAndValidateModel(builder, {
        bbmodelRelPath: bbRel,
        textureRelPath: texRel,
        bedrockGeoRelPath: bedrockGeoRel,
        bedrockAnimRelPath: bedrockAnimRel,
      });

      const riftGeoAbs = path.join(repoRoot, `mod/src/main/resources/assets/riftworld/geo/${builder.id}.json`);
      builder.exportRiftworldGeo(riftGeoAbs);

      const gSum = mcpRes.geometry_validation.summary;
      const aSum = mcpRes.animation_validation.summary;
      console.log(`[mcp-validate] ${builder.id} geometry:`, JSON.stringify(gSum));
      console.log(`[mcp-validate] ${builder.id} animations:`, JSON.stringify(aSum));

      if (gSum.failed > 0 || gSum.warnings > 0) {
        console.error('GEOMETRY VALIDATION DETAILS:', JSON.stringify(mcpRes.geometry_validation.gates, null, 2));
        if (gSum.failed > 0) throw new Error(`Geometry validation failed on ${builder.id}`);
      }
      if (aSum.failed > 0 || aSum.warnings > 0) {
        console.error('ANIMATION VALIDATION DETAILS:', JSON.stringify(mcpRes.animation_validation.gates, null, 2));
        if (aSum.failed > 0) throw new Error(`Animation validation failed on ${builder.id}`);
      }

      validationSummary.push({
        id: builder.id,
        title_ru: builder.titleRu,
        biome: builder.biome,
        bbmodel: bbRel,
        bedrock_geo: bedrockGeoRel,
        bedrock_animation: bedrockAnimRel,
        riftworld_geo: `mod/src/main/resources/assets/riftworld/geo/${builder.id}.json`,
        texture: texRel,
        info: mcpRes.info,
        geometry_validation: mcpRes.geometry_validation.summary,
        geometry_self_test: mcpRes.geometry_validation.self_test,
        animation_validation: mcpRes.animation_validation.summary,
      });
    }
  } finally {
    await mcp.close();
  }

  const reportDir = path.join(repoRoot, 'reports/blockbench_mcp/batch_01');
  fs.mkdirSync(reportDir, { recursive: true });
  fs.writeFileSync(
    path.join(reportDir, 'validation_report.json'),
    JSON.stringify({ generated_via: 'blockbench-mcp-plugin (official MCP server)', models: validationSummary }, null, 2) + '\n'
  );
  console.log('\n[done] All 3 V3 models built & validated via Blockbench MCP!');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
