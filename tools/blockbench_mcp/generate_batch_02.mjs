#!/usr/bin/env node
/**
 * Batch 02 Generator (V2 Master Polish) via Official Blockbench MCP (`jasonjgardner/blockbench-mcp-plugin`):
 *   04 — Аметистовый лис (`amethyst_fox`)
 *   05 — Сизый листокрыл (`sizo_leafwing`)
 *   06 — Лазурный древесный лягуш (`azure_frog`)
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
// 04 — АМЕТИСТОВЫЙ ЛИС (`amethyst_fox`)
// ============================================================================
function buildAmethystFox() {
  const b = new ModelBuilder({
    id: 'amethyst_fox',
    titleRu: 'Аметистовый лис',
    biome: 'riftworld:violet_hills',
    atlasSize: 512,
    renderScale: 0.5,
  });

  b.addGroup('root', [0, 0, 0]);
  b.addGroup('body', [0, 19.0, 1.0], [0, 0, 0], 'root', { axis: 'x', amp: 0.9, speed: 1.0, phase: 0 });

  // Lithe Articulated Fox Torso + Proud Upright Neck & Smooth Flush Cream-White Chest Bib!
  b.addCenteredBox('torso_front', {
    center: [0, 19.8, -3.2],
    size: [10, 9, 12],
    rotation: [2, 0, 0],
    parent: 'body',
    mat: 'fox_fur_body',
  });
  b.addCenteredBox('torso_rear', {
    center: [0, 20.0, 6.8],
    size: [9, 9, 11],
    rotation: [-2, 0, 0],
    parent: 'body',
    mat: 'fox_fur_body',
  });
  b.addSegmentBox('neck_column', {
    pStart: [0, 19.8, -6.8],
    pEnd: [0, 27.6, -11.2],
    width: 8,
    depth: 8,
    upHint: [0, 0, 1],
    padStart: 1.4,
    padEnd: 1.2,
    parent: 'body',
    mat: 'fox_fur_body',
  });
  b.addSegmentBox('chest_bib', {
    pStart: [0, 15.8, -7.8],
    pEnd: [0, 25.8, -12.6],
    width: 7,
    depth: 3,
    upHint: [0, 0, 1],
    padStart: 1.0,
    padEnd: 0.8,
    parent: 'body',
    mat: 'fox_chest_white',
  });

  // Dorsal Faceted Amethyst Crystal Clusters erupting along the spine!
  const spineCrystals = [
    { name: 'dorsal_crys_1', s: [0, 23.4, -3.6], e: [0, 28.8, -1.8], w: 3, d: 3 },
    { name: 'dorsal_crys_2', s: [0, 23.6,  0.5], e: [0, 31.6,  4.6], w: 4, d: 4 },
    { name: 'dorsal_crys_3', s: [0, 23.6,  5.0], e: [0, 29.0,  8.2], w: 3, d: 3 },
  ];
  for (const cr of spineCrystals) {
    b.addSegmentBox(cr.name, {
      pStart: cr.s,
      pEnd: cr.e,
      width: cr.w,
      depth: cr.d,
      upHint: [0, 0, 1],
      padStart: 1.2,
      padEnd: 0.2,
      parent: 'body',
      mat: 'amethyst_crystal',
      meta: { crysT0: 0.05, crysT1: 1.0 },
    });
  }

  b.addMirrored(() => {
    b.addSegmentBox('dorsal_shard_l_1', {
      pStart: [2.8, 22.8, 0.4],
      pEnd: [5.8, 27.0, 2.8],
      width: 3,
      depth: 3,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.2,
      parent: 'body',
      mat: 'amethyst_crystal',
      meta: { crysT0: 0.1, crysT1: 0.95 },
    });
    b.addSegmentBox('dorsal_shard_l_2', {
      pStart: [2.4, 23.0, 5.4],
      pEnd: [4.8, 26.6, 7.8],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.2,
      parent: 'body',
      mat: 'amethyst_crystal',
      meta: { crysT0: 0.15, crysT1: 0.95 },
    });
  });

  // Cute Expressive Fox Head, Glowing Lavender-White Eyes, Splayed Fox Ears & Crystal Ear Tips!
  b.addGroup('head', [0, 28.5, -11.5], [0, 0, 0], 'body', { axis: 'x', amp: 1.6, speed: 1.0, phase: 0.4 });
  b.addCenteredBox('head_cranium', {
    center: [0, 29.8, -13.8],
    size: [11, 8, 9],
    parent: 'head',
    mat: 'fox_head',
  });
  b.addMirrored(() => {
    b.addCenteredBox('cheek_ruff_l', {
      center: [5.2, 27.2, -13.4],
      size: [3, 4, 6],
      rotation: [0, -4, -8],
      parent: 'head',
      mat: 'fox_head',
    });
    b.addCenteredBox('eye_l', {
      center: [3.2, 29.0, -17.9],
      size: [3, 3, 2],
      parent: 'head',
      mat: 'fox_eye',
    });
  });

  // Sleek Tapered Fox Snout (Lilac bridge + dark plum nose button + creamy-white lower jaw!)
  b.addCenteredBox('snout_upper', {
    center: [0, 27.6, -19.8],
    size: [5, 2, 5],
    parent: 'head',
    mat: 'fox_snout',
  });
  b.addCenteredBox('snout_lower', {
    center: [0, 26.0, -19.6],
    size: [5, 2, 5],
    parent: 'head',
    mat: 'fox_chest_white',
  });

  // Wide Triangular Splayed Fox Ears with Crystalline Amethyst Shard Tips!
  b.addMirrored(() => {
    b.addGroup('ear_l', [4.0, 33.4, -13.2], [0, 0, 0], 'head', { axis: 'z', amp: 2.0, speed: 1.2, phase: 0.2 });
    b.addSegmentBox('ear_base_l', {
      pStart: [3.6, 33.0, -13.2],
      pEnd: [5.4, 37.8, -13.0],
      width: 5,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 1.2,
      padEnd: 0.7,
      parent: 'ear_l',
      mat: 'fox_ear',
    });
    b.addSegmentBox('ear_mid_l', {
      pStart: [5.2, 37.4, -13.0],
      pEnd: [6.5, 40.8, -12.8],
      width: 4,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.6,
      parent: 'ear_l',
      mat: 'fox_ear',
    });
    b.addSegmentBox('ear_crystal_l', {
      pStart: [6.3, 40.4, -12.8],
      pEnd: [7.5, 43.8, -12.6],
      width: 2,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 0.9,
      padEnd: 0.2,
      parent: 'ear_l',
      mat: 'amethyst_crystal',
      meta: { crysT0: 0.25, crysT1: 1.0 },
    });
  });

  // Four Slender Articulated Fox Legs on Y = 0
  b.addMirrored(() => {
    b.addGroup('leg_front_l', [3.2, 16.5, -6.2], [0, 0, 0], 'root');
    b.addCenteredBox('leg_front_l_shoulder', {
      center: [3.4, 15.4, -6.2],
      size: [4, 7, 5],
      parent: 'leg_front_l',
      mat: 'fox_fur_body',
    });
    b.addCenteredBox('leg_front_l_shank', {
      center: [3.2, 7.0, -6.4],
      size: [3, 11, 3],
      parent: 'leg_front_l',
      mat: 'fox_leg',
    });
    b.addCenteredBox('leg_front_l_paw', {
      center: [3.2, 1.0, -7.2],
      size: [3, 2, 5],
      parent: 'leg_front_l',
      mat: 'fox_paw',
      meta: { toeTipZ: -9.7 },
    });

    b.addGroup('leg_back_l', [3.1, 17.0, 8.2], [0, 0, 0], 'root');
    b.addSegmentBox('leg_back_l_thigh', {
      pStart: [3.2, 18.2, 7.4],
      pEnd: [3.2, 11.2, 9.2],
      width: 4,
      depth: 6,
      upHint: [0, 0, 1],
      padStart: 1.0,
      padEnd: 0.8,
      parent: 'leg_back_l',
      mat: 'fox_fur_body',
    });
    b.addCenteredBox('leg_back_l_shank', {
      center: [3.0, 6.5, 9.0],
      size: [3, 9, 3],
      parent: 'leg_back_l',
      mat: 'fox_leg',
    });
    b.addCenteredBox('leg_back_l_paw', {
      center: [3.0, 1.0, 8.2],
      size: [3, 2, 5],
      parent: 'leg_back_l',
      mat: 'fox_paw',
      meta: { toeTipZ: 5.7 },
    });
  });

  // Huge Bushy Fox Tail Ending in a Blossoming Amethyst Geode Rosette!
  b.addGroup('tail_base', [0, 20.6, 11.2], [0, 0, 0], 'body', { axis: 'y', amp: 4.5, speed: 1.0, phase: 0.5 });

  b.addSegmentBox('tail_seg_1', {
    pStart: [0, 20.5, 10.8],
    pEnd: [0, 16.8, 17.6],
    width: 8,
    depth: 8,
    upHint: [0, 1, 0],
    padStart: 1.4,
    padEnd: 1.0,
    parent: 'tail_base',
    mat: 'fox_tail',
  });
  b.addSegmentBox('tail_seg_2', {
    pStart: [0, 16.8, 17.2],
    pEnd: [0, 13.0, 24.0],
    width: 9,
    depth: 9,
    upHint: [0, 1, 0],
    padStart: 1.2,
    padEnd: 1.0,
    parent: 'tail_base',
    mat: 'fox_tail',
  });
  b.addSegmentBox('tail_seg_3', {
    pStart: [0, 13.0, 23.6],
    pEnd: [0, 9.8, 29.2],
    width: 8,
    depth: 8,
    upHint: [0, 1, 0],
    padStart: 1.2,
    padEnd: 0.8,
    parent: 'tail_base',
    mat: 'fox_tail',
  });

  b.addSegmentBox('tail_top_crys_1', {
    pStart: [0, 19.6, 15.2],
    pEnd: [0, 24.4, 18.8],
    width: 3,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 1.2,
    padEnd: 0.2,
    parent: 'tail_base',
    mat: 'amethyst_crystal',
    meta: { crysT0: 0.1, crysT1: 1.0 },
  });
  b.addSegmentBox('tail_top_crys_2', {
    pStart: [0, 16.2, 20.8],
    pEnd: [0, 20.6, 24.6],
    width: 3,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 1.2,
    padEnd: 0.2,
    parent: 'tail_base',
    mat: 'amethyst_crystal',
    meta: { crysT0: 0.1, crysT1: 1.0 },
  });

  // Spectacular Blooming Amethyst Geode Tail Tip Bouquet!
  b.addSegmentBox('tail_geode_center', {
    pStart: [0, 9.8, 28.4],
    pEnd: [0, 8.2, 37.6],
    width: 4,
    depth: 4,
    upHint: [0, 1, 0],
    padStart: 1.5,
    padEnd: 0.2,
    parent: 'tail_base',
    mat: 'amethyst_crystal',
    meta: { crysT0: 0.0, crysT1: 1.0 },
  });
  b.addSegmentBox('tail_geode_up', {
    pStart: [0, 10.6, 28.0],
    pEnd: [0, 16.2, 36.0],
    width: 3,
    depth: 4,
    upHint: [0, 1, 0],
    padStart: 1.5,
    padEnd: 0.2,
    parent: 'tail_base',
    mat: 'amethyst_crystal',
    meta: { crysT0: 0.0, crysT1: 1.0 },
  });
  b.addSegmentBox('tail_geode_down', {
    pStart: [0, 8.8, 28.0],
    pEnd: [0, 4.2, 34.8],
    width: 3,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 1.5,
    padEnd: 0.2,
    parent: 'tail_base',
    mat: 'amethyst_crystal',
    meta: { crysT0: 0.0, crysT1: 0.95 },
  });

  b.addMirrored(() => {
    const sideSpikes = [
      { name: 'tail_geode_l_1', s: [1.6, 10.8, 27.8], e: [5.8, 16.0, 35.2], w: 3, d: 3 },
      { name: 'tail_geode_l_2', s: [2.0,  9.8, 28.0], e: [7.6, 10.6, 36.2], w: 3, d: 4 },
      { name: 'tail_geode_l_3', s: [1.8,  8.8, 27.8], e: [6.0,  5.4, 34.6], w: 3, d: 3 },
      { name: 'tail_geode_l_4', s: [2.4, 11.4, 27.2], e: [5.8, 16.6, 31.2], w: 3, d: 3 },
    ];
    for (const sp of sideSpikes) {
      b.addSegmentBox(sp.name, {
        pStart: sp.s,
        pEnd: sp.e,
        width: sp.w,
        depth: sp.d,
        upHint: [0, 1, 0],
        padStart: 1.4,
        padEnd: 0.2,
        parent: 'tail_base',
        mat: 'amethyst_crystal',
        meta: { crysT0: 0.0, crysT1: 1.0 },
      });
    }
  });

  b.addAnimation('idle', {
    length: 3.6,
    loop: 'loop',
    snapping: 24,
    tracks: [
      { bone: 'body', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [1.0, 0, 0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'head', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [-1.8, 2.5, 0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'ear_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [0, 0, 3.0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'ear_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [0, 0, -3.0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'tail_base', channel: 'rotation', keys: [{ time: 0.0, value: [0, -4.0, 0] }, { time: 1.8, value: [1.5, 4.0, 0] }, { time: 3.6, value: [0, -4.0, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// 05 — СИЗЫЙ ЛИСТОКРЫЛ (`sizo_leafwing`)
// ============================================================================
function buildSizoLeafwing() {
  const b = new ModelBuilder({
    id: 'sizo_leafwing',
    titleRu: 'Сизый листокрыл',
    biome: 'riftworld:sizo_grove',
    atlasSize: 512,
    renderScale: 0.5,
  });

  b.addGroup('root', [0, 0, 0]);
  b.addGroup('body', [0, 17.5, 1.5], [0, 0, 0], 'root', { axis: 'x', amp: 1.2, speed: 1.0, phase: 0 });

  // S-Curved Agile Arboreal Dragon Torso & Proud Upright Neck
  const bodySpine = [
    { name: 'pelvis_core', p0: [0, 15.5,  5.5], p1: [0, 18.2, -0.5], w: 9,  d: 8 },
    { name: 'waist_core',  p0: [0, 18.0, -0.2], p1: [0, 22.8, -5.8], w: 9,  d: 9 },
    { name: 'chest_core',  p0: [0, 22.5, -5.5], p1: [0, 28.4, -8.6], w: 10, d: 9 },
    { name: 'neck_lower',  p0: [0, 28.0, -8.3], p1: [0, 32.8, -9.8], w: 7,  d: 7 },
    { name: 'neck_upper',  p0: [0, 32.2, -9.6], p1: [0, 35.4, -10.4], w: 6, d: 6 },
  ];
  for (const seg of bodySpine) {
    b.addSegmentBox(seg.name, {
      pStart: seg.p0,
      pEnd: seg.p1,
      width: seg.w,
      depth: seg.d,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 1.0,
      parent: 'body',
      mat: 'leafwing_body',
    });
  }

  // Smooth Flush Cream-White Tufted Chest Mane!
  b.addSegmentBox('chest_mane_upper', {
    pStart: [0, 32.4, -11.4],
    pEnd: [0, 24.8, -11.8],
    width: 7,
    depth: 3,
    upHint: [0, 0, 1],
    padStart: 0.8,
    padEnd: 0.8,
    parent: 'body',
    mat: 'leafwing_chest_fur',
  });
  b.addSegmentBox('chest_mane_lower', {
    pStart: [0, 25.4, -11.6],
    pEnd: [0, 18.4, -8.8],
    width: 8,
    depth: 3,
    upHint: [0, 0, 1],
    padStart: 0.8,
    padEnd: 0.4,
    parent: 'body',
    mat: 'leafwing_chest_fur',
  });

  // Cornflower-Blue Forest Blossoms & Vine Stems along the Neck!
  b.addMirrored(() => {
    const flowers = [
      { name: 'neck_flower_l_1', c: [3.8, 31.0, -8.8], r: [12, 24, -18] },
      { name: 'neck_flower_l_2', c: [4.5, 28.0, -9.4], r: [16, 18, -22] },
      { name: 'neck_flower_l_3', c: [4.2, 25.2, -9.8], r: [20, 12, -16] },
    ];
    for (const fl of flowers) {
      b.addCenteredBox(fl.name, {
        center: fl.c,
        size: [2, 2, 2],
        rotation: fl.r,
        parent: 'body',
        mat: 'leafwing_flower',
      });
    }
    b.addSegmentBox('neck_vine_l', {
      pStart: [3.5, 31.4, -8.6],
      pEnd: [4.1, 25.0, -9.6],
      width: 1,
      depth: 1,
      upHint: [1, 0, 0],
      padStart: 0.6,
      padEnd: 0.6,
      parent: 'body',
      mat: 'leafwing_vine',
    });
  });

  // Sculpted Dragon Head, Large Honey-Amber Eyes, Leaf-Ears & Graceful Swept Wooden Antlers!
  b.addGroup('head', [0, 35.2, -10.4], [0, 0, 0], 'body', { axis: 'x', amp: -1.6, speed: 1.0, phase: 0.4 });
  b.addCenteredBox('head_cranium', {
    center: [0, 35.6, -12.0],
    size: [8, 6, 8],
    rotation: [6, 0, 0],
    parent: 'head',
    mat: 'leafwing_head',
  });
  b.addCenteredBox('snout_upper', {
    center: [0, 34.4, -16.8],
    size: [5, 3, 6],
    rotation: [10, 0, 0],
    parent: 'head',
    mat: 'leafwing_head',
  });
  b.addCenteredBox('snout_lower', {
    center: [0, 32.8, -16.0],
    size: [4, 2, 5],
    rotation: [6, 0, 0],
    parent: 'head',
    mat: 'leafwing_head',
  });

  b.addMirrored(() => {
    // Large Expressive Honey-Amber Dragon Eye visible cleanly in both 3/4 and side views!
    b.addCenteredBox('eye_l', {
      center: [3.4, 35.5, -13.6],
      size: [2, 3, 4],
      rotation: [6, -18, 0],
      parent: 'head',
      mat: 'leafwing_eye',
    });

    b.addSegmentBox('cheek_tuft_l_1', {
      pStart: [3.6, 34.0, -11.2],
      pEnd: [5.8, 33.2, -7.8],
      width: 2,
      depth: 3,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.2,
      parent: 'head',
      mat: 'leafwing_chest_fur',
    });

    // Long Swept-Back Leaf-Ears
    b.addSegmentBox('ear_main_l', {
      pStart: [3.6, 37.0, -9.8],
      pEnd: [10.0, 41.2, -3.6],
      width: 2,
      depth: 4,
      upHint: [0.3, 0.95, 0],
      padStart: 1.2,
      padEnd: 0.2,
      parent: 'head',
      mat: 'leafwing_body',
    });
    b.addSegmentBox('ear_lower_l', {
      pStart: [3.8, 35.8, -9.6],
      pEnd: [8.6, 37.8, -4.4],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.2,
      parent: 'head',
      mat: 'leafwing_chest_fur',
    });

    b.addSegmentBox('horn_bud_l', {
      pStart: [1.8, 38.0, -10.2],
      pEnd: [2.2, 41.6, -8.4],
      width: 2,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 1.0,
      padEnd: 0.2,
      parent: 'head',
      mat: 'leafwing_body',
    });

    // Slender Swept-Back Branched Bark-Brown Antlers!
    b.addGroup('antler_l', [2.6, 38.2, -11.4], [0, 0, 0], 'head');
    b.addSegmentBox('antler_beam_l_1', {
      pStart: [2.6, 37.8, -11.4],
      pEnd: [4.6, 43.8, -8.2],
      width: 2,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 1.0,
      padEnd: 0.6,
      parent: 'antler_l',
      mat: 'leafwing_antler',
    });
    b.addSegmentBox('antler_beam_l_2', {
      pStart: [4.5, 43.4, -8.4],
      pEnd: [6.8, 49.8, -4.4],
      width: 1,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.2,
      parent: 'antler_l',
      mat: 'leafwing_antler',
    });
    b.addSegmentBox('antler_tine_front_l', {
      pStart: [3.8, 41.4, -9.5],
      pEnd: [4.6, 46.0, -11.2],
      width: 1,
      depth: 1,
      upHint: [1, 0, 0],
      padStart: 0.8,
      padEnd: 0.2,
      parent: 'antler_l',
      mat: 'leafwing_antler',
    });
    b.addSegmentBox('antler_tine_upper_l', {
      pStart: [5.6, 46.2, -6.6],
      pEnd: [6.2, 50.6, -8.0],
      width: 1,
      depth: 1,
      upHint: [1, 0, 0],
      padStart: 0.8,
      padEnd: 0.2,
      parent: 'antler_l',
      mat: 'leafwing_antler',
    });
  });

  // Raised Agile Forelimbs (`arm_l/r`) & Strong Digitigrade Hind Legs (`leg_back_l/r`) on Y = 0!
  b.addMirrored(() => {
    b.addGroup('arm_l', [4.8, 24.0, -6.8], [0, 0, 0], 'body', { axis: 'x', amp: 3.0, speed: 1.0, phase: 0.3 });
    b.addSegmentBox('arm_l_upper', {
      pStart: [4.6, 24.2, -6.8],
      pEnd: [5.6, 17.5, -4.6],
      width: 3,
      depth: 4,
      upHint: [0, 0, 1],
      padStart: 1.2,
      padEnd: 0.8,
      parent: 'arm_l',
      mat: 'leafwing_body',
    });
    b.addSegmentBox('arm_l_fore', {
      pStart: [5.5, 17.8, -4.8],
      pEnd: [4.6, 13.0, -11.8],
      width: 3,
      depth: 3,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.8,
      parent: 'arm_l',
      mat: 'leafwing_body',
    });
    b.addSegmentBox('arm_l_hand', {
      pStart: [4.6, 13.2, -11.6],
      pEnd: [4.4, 9.6, -12.8],
      width: 3,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.4,
      parent: 'arm_l',
      mat: 'leafwing_body',
    });
    for (let c = -1; c <= 1; c++) {
      b.addCenteredBox(`arm_l_claw_${c + 2}`, {
        center: [4.4 + c * 0.9, 8.6, -13.2],
        size: [1, 2, 2],
        rotation: [22, 0, 0],
        parent: 'arm_l',
        mat: 'leafwing_claw',
      });
    }

    b.addGroup('leg_back_l', [4.4, 16.5, 4.2], [0, 0, 0], 'root');
    b.addSegmentBox('leg_back_l_thigh', {
      pStart: [4.4, 16.8, 4.2],
      pEnd: [5.2, 10.6, -1.2],
      width: 4,
      depth: 6,
      upHint: [0, 1, 0],
      padStart: 1.4,
      padEnd: 1.0,
      parent: 'leg_back_l',
      mat: 'leafwing_body',
    });
    b.addSegmentBox('leg_back_l_shin', {
      pStart: [5.2, 10.8, -1.0],
      pEnd: [5.0, 6.2, 4.2],
      width: 3,
      depth: 4,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.9,
      parent: 'leg_back_l',
      mat: 'leafwing_body',
    });
    b.addSegmentBox('leg_back_l_ankle', {
      pStart: [5.0, 6.4, 4.0],
      pEnd: [4.8, 1.8, 0.8],
      width: 3,
      depth: 3,
      upHint: [0, 0, 1],
      padStart: 0.9,
      padEnd: 0.8,
      parent: 'leg_back_l',
      mat: 'leafwing_body',
    });
    b.addCenteredBox('leg_back_l_foot', {
      center: [4.8, 1.0, -1.2],
      size: [4, 2, 5],
      parent: 'leg_back_l',
      mat: 'leafwing_body',
    });
    for (let c = -1; c <= 1; c++) {
      b.addCenteredBox(`leg_back_l_claw_${c + 2}`, {
        center: [4.8 + c * 1.2, 0.5, -4.0],
        size: [1, 1, 2],
        parent: 'leg_back_l',
        mat: 'leafwing_claw',
      });
    }
  });

  // High-Swept Coplanar Leaf-Membrane Dragon Wings (`wing_l/r`) — soared high so curling leaf-tail shines unobstructed!
  const uWing = vNorm([0.90, 0.41, 0.14]);
  const rawDown = [0.06, -0.95, 0.30];
  const nWing = vNorm(vCross(rawDown, uWing));
  const vWing = vNorm(vCross(uWing, nWing));

  b.addMirrored(() => {
    b.addGroup('wing_l', [4.6, 26.2, -4.5], [0, 0, 0], 'body', { axis: 'z', amp: 4.5, speed: 1.0, phase: 0.2 });

    b.addSegmentBox('wing_humerus_l', {
      pStart: [4.4, 26.2, -4.5],
      pEnd: [12.0, 26.5, -2.8],
      width: 3,
      depth: 4,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 1.0,
      parent: 'wing_l',
      mat: 'leafwing_body',
    });
    b.addSegmentBox('wing_forearm_l', {
      pStart: [11.6, 26.3, -2.9],
      pEnd: [20.4, 41.8, -3.5],
      width: 3,
      depth: 3,
      upHint: [0, 0, 1],
      padStart: 1.2,
      padEnd: 1.0,
      parent: 'wing_l',
      mat: 'leafwing_body',
    });
    b.addCenteredBox('wing_claw_l', {
      center: [20.6, 43.6, -4.2],
      size: [2, 3, 2],
      rotation: [22, 0, -18],
      parent: 'wing_l',
      mat: 'leafwing_claw',
    });
    b.addSegmentBox('wing_costa_l_1', {
      pStart: [20.2, 41.6, -3.5],
      pEnd: [35.5, 46.8, -0.8],
      width: 2,
      depth: 3,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.8,
      parent: 'wing_l',
      mat: 'leafwing_body',
    });
    b.addSegmentBox('wing_costa_l_2', {
      pStart: [35.0, 46.6, -0.9],
      pEnd: [51.0, 51.2, 1.8],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.2,
      parent: 'wing_l',
      mat: 'leafwing_body',
    });

    const wAnchor = [10.0, 35.0, -3.2];
    const wPt = (s, c, n = 0) => vAdd(vAdd(wAnchor, vScale(uWing, s)), vAdd(vScale(vWing, c), vScale(nWing, n)));

    const leafPanels = [
      { name: 'wing_leaf_l_1', center: wPt(26.0,  0.5,  0.00), w: 32, l:  9 },
      { name: 'wing_leaf_l_2', center: wPt(19.5,  5.6, -0.04), w: 29, l: 10 },
      { name: 'wing_leaf_l_3', center: wPt(14.5, 10.8, -0.08), w: 22, l:  8 },
      { name: 'wing_leaf_l_4', center: wPt(4.8,   6.8, -0.06), w: 12, l:  8 },
    ];
    for (const lp of leafPanels) {
      b.addPlate(lp.name, {
        center: lp.center,
        width: lp.w,
        thickness: 2,
        length: lp.l,
        uAxis: uWing,
        nAxis: nWing,
        parent: 'wing_l',
        mat: 'leafwing_wing_blade',
      });
    }
  });

  // Long Prehensile S-Curling Tail Ending in a Broad Veined Mint Leaf Blade (extended back cleanly into full view!)
  b.addGroup('tail_base', [0, 15.8, 6.0], [0, 0, 0], 'body', { axis: 'y', amp: 3.5, speed: 1.0, phase: 0.6 });
  const tailPts = [
    [0, 15.6,  5.6],
    [0, 10.0, 12.0],
    [0,  6.2, 19.8],
    [0,  6.0, 28.0],
    [0,  9.8, 34.8],
    [0, 16.5, 37.5],
    [0, 22.8, 34.5],
    [0, 24.2, 28.5],
  ];
  const tailThickness = [6, 5, 5, 4, 4, 3, 3];
  for (let i = 0; i < tailThickness.length; i++) {
    const th = tailThickness[i];
    b.addSegmentBox(`tail_seg_${i + 1}`, {
      pStart: tailPts[i],
      pEnd: tailPts[i + 1],
      width: th,
      depth: th,
      upHint: [1, 0, 0],
      padStart: 1.2,
      padEnd: 1.0,
      parent: 'tail_base',
      mat: 'leafwing_tail',
    });
  }

  // Broad Coplanar Mint Leaf Paddle curling downward inside the tail loop (tilted slightly around Y so 3/4 camera sees full broad leaf!)
  const uLeaf = vNorm([0, -0.64, -0.77]);
  const nLeaf = vNorm([0.92, 0.10, -0.38]);
  b.addPlate('tail_leaf_main', {
    center: [0, 21.0, 24.6],
    width: 11,
    thickness: 2,
    length: 8,
    uAxis: uLeaf,
    nAxis: nLeaf,
    parent: 'tail_base',
    mat: 'leafwing_tail_leaf',
  });
  b.addPlate('tail_leaf_tip', {
    center: [0, 17.5, 20.8],
    width: 6,
    thickness: 2,
    length: 5,
    uAxis: uLeaf,
    nAxis: nLeaf,
    parent: 'tail_base',
    mat: 'leafwing_tail_leaf',
  });

  b.addAnimation('idle', {
    length: 3.6,
    loop: 'loop',
    snapping: 24,
    tracks: [
      { bone: 'body', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [1.4, 0, 0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'head', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [-2.0, 2.0, 0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'wing_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [0, -3.0, 4.5] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'wing_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [0, 3.0, -4.5] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'arm_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [3.0, 0, 0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'arm_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [3.0, 0, 0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'tail_base', channel: 'rotation', keys: [{ time: 0.0, value: [0, -3.0, 0] }, { time: 1.8, value: [2.0, 3.0, 0] }, { time: 3.6, value: [0, -3.0, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// 06 — ЛАЗУРНЫЙ ДРЕВЕСНЫЙ ЛЯГУШ (`azure_frog`)
// ============================================================================
function buildAzureFrog() {
  const b = new ModelBuilder({
    id: 'azure_frog',
    titleRu: 'Лазурный древесный лягуш',
    biome: 'riftworld:waterfall_gardens',
    atlasSize: 512,
    renderScale: 0.5,
  });

  b.addGroup('root', [0, 0, 0]);
  b.addGroup('body', [0, 9.5, 1.5], [0, 0, 0], 'root', { axis: 'x', amp: 1.0, speed: 1.1, phase: 0 });

  // Plump Chubby Squatting Amphibian Body
  b.addCenteredBox('torso_core', {
    center: [0, 10.8, 1.6],
    size: [17, 13, 17],
    rotation: [-22, 0, 0],
    parent: 'body',
    mat: 'frog_skin_body',
  });
  b.addCenteredBox('dorsal_hump', {
    center: [0, 15.8, 2.8],
    size: [14, 5, 13],
    rotation: [-26, 0, 0],
    parent: 'body',
    mat: 'frog_skin_body',
  });
  b.addCenteredBox('rump_round', {
    center: [0, 7.6, 9.6],
    size: [14, 10, 7],
    rotation: [-16, 0, 0],
    parent: 'body',
    mat: 'frog_skin_body',
  });
  b.addCenteredBox('vocal_chest', {
    center: [0, 10.2, -5.8],
    size: [13, 11, 6],
    rotation: [-18, 0, 0],
    parent: 'body',
    mat: 'frog_belly',
  });

  // Lush Continuous Overlapping Sakura-Pink & Mint-Sepal Lotus Petal Collar + Delicate Flower Buds!
  // Note: `-Rx` pitches `-Z` tip DOWNWARD over chest/shoulders so the broad pink-and-mint upper face shines toward the viewer!
  b.addGroup('collar', [0, 17.2, -2.8], [0, 0, 0], 'body', { axis: 'x', amp: 1.4, speed: 1.1, phase: 0.3 });

  b.addCenteredBox('petal_front_center', {
    center: [0, 14.8, -10.6],
    size: [6, 2, 7],
    rotation: [-32, 0, 0],
    parent: 'collar',
    mat: 'frog_petal_collar',
  });
  b.addCenteredBox('petal_back_center', {
    center: [0, 21.2, 5.2],
    size: [6, 2, 7],
    rotation: [-22, 180, 0],
    parent: 'collar',
    mat: 'frog_petal_collar',
  });

  b.addMirrored(() => {
    const sideAnglesDeg = [28, 58, 90, 122, 152];
    sideAnglesDeg.forEach((deg, idx) => {
      const rad = (deg * Math.PI) / 180;
      const r = 8.8;
      const px = Math.sin(rad) * r;
      const pz = -2.8 - Math.cos(rad) * r;
      const py = 17.6 + (pz - (-2.8)) * 0.36;
      // Front petals droop gracefully down over chest (`-30`), side/back petals flare outward & slightly up (`-18` to `-10`)!
      const pitch = idx <= 1 ? -30 : idx === 2 ? -22 : -14;
      b.addCenteredBox(`petal_l_${idx + 1}`, {
        center: [px, py, pz],
        size: [6, 2, 7],
        rotation: [pitch, -deg, 0],
        parent: 'collar',
        mat: 'frog_petal_collar',
      });
    });

    // Delicate tiny 2x2x2 pink flower buds on thin 1x1 green stems peeking right between outer petal tips!
    const budAnglesDeg = [16, 44, 76, 108, 138];
    budAnglesDeg.forEach((deg, idx) => {
      const rad = (deg * Math.PI) / 180;
      const rIn = 9.0;
      const rOut = 12.8;
      const sx = Math.sin(rad) * rIn;
      const sz = -2.8 - Math.cos(rad) * rIn;
      const sy = 17.4 + (sz - (-2.8)) * 0.36;
      const tx = Math.sin(rad) * rOut;
      const tz = -2.8 - Math.cos(rad) * rOut;
      const ty = sy + (idx >= 3 ? 1.8 : -2.2);

      b.addSegmentBox(`bud_stem_l_${idx + 1}`, {
        pStart: [sx, sy, sz],
        pEnd: [tx, ty, tz],
        width: 1,
        depth: 1,
        upHint: [0, 1, 0],
        padStart: 0.8,
        padEnd: 0.5,
        parent: 'collar',
        mat: 'frog_bud_stem',
      });
      b.addCenteredBox(`bud_flower_l_${idx + 1}`, {
        center: [tx * 1.05, ty + (idx >= 3 ? 0.4 : -0.5), -2.8 + (tz + 2.8) * 1.05],
        size: [2, 2, 2],
        rotation: [-18, -deg, 0],
        parent: 'collar',
        mat: 'frog_bud_pink',
      });
    });
  });

  // Wide Happy Smiling Frog Head & Prominent Bulging Golden-Yellow Turret Eyes!
  b.addGroup('head', [0, 18.8, -3.5], [0, 0, 0], 'body', { axis: 'x', amp: -1.4, speed: 1.1, phase: 0.5 });
  b.addCenteredBox('head_cranium', {
    center: [0, 20.8, -5.6],
    size: [15, 6, 11],
    rotation: [6, 0, 0],
    parent: 'head',
    mat: 'frog_head',
  });
  b.addCenteredBox('snout_mid', {
    center: [0, 20.4, -9.8],
    size: [13, 5, 5],
    rotation: [6, 0, 0],
    parent: 'head',
    mat: 'frog_head',
  });
  b.addCenteredBox('snout_tip', {
    center: [0, 20.2, -11.4],
    size: [10, 4, 3],
    rotation: [6, 0, 0],
    parent: 'head',
    mat: 'frog_head',
  });
  b.addCenteredBox('jaw_lower', {
    center: [0, 17.8, -7.8],
    size: [12, 3, 9],
    rotation: [4, 0, 0],
    parent: 'head',
    mat: 'frog_belly',
  });

  b.addMirrored(() => {
    // High Turret Eyelid Hood + Full Unobstructed 4x5x4 Golden Frog Eye staring happily forward-3/4!
    b.addCenteredBox('eye_hood_l', {
      center: [5.4, 25.4, -5.8],
      size: [5, 5, 5],
      rotation: [6, -24, -6],
      parent: 'head',
      mat: 'frog_skin_body',
    });
    b.addCenteredBox('eye_globe_l', {
      center: [5.8, 25.2, -7.2],
      size: [5, 5, 4],
      rotation: [4, -26, -3],
      parent: 'head',
      mat: 'frog_eye_gold',
    });
  });

  // Four Sturdy Squatting Tree-Frog Limbs with Plump Mint-Green Sticky Toe Pads on Y = 0!
  b.addMirrored(() => {
    b.addGroup('leg_front_l', [7.2, 12.2, -5.0], [0, 0, 0], 'root');
    b.addSegmentBox('leg_front_l_upper', {
      pStart: [7.0, 12.6, -4.8],
      pEnd: [8.0, 7.6, -7.2],
      width: 5,
      depth: 5,
      upHint: [0, 0, 1],
      padStart: 1.4,
      padEnd: 1.0,
      parent: 'leg_front_l',
      mat: 'frog_skin_body',
    });
    b.addSegmentBox('leg_front_l_fore', {
      pStart: [7.9, 7.8, -7.0],
      pEnd: [6.4, 1.8, -9.8],
      width: 5,
      depth: 5,
      upHint: [0, 0, 1],
      padStart: 1.0,
      padEnd: 0.8,
      parent: 'leg_front_l',
      mat: 'frog_skin_body',
    });
    const frontToes = [
      [4.2, 1.0, -12.0],
      [6.8, 1.0, -12.6],
      [9.4, 1.0, -11.6],
    ];
    frontToes.forEach((tc, ti) => {
      b.addCenteredBox(`leg_front_l_pad_${ti + 1}`, {
        center: tc,
        size: [2, 2, 3],
        parent: 'leg_front_l',
        mat: 'frog_toe_pad',
      });
    });
    b.addCenteredBox('leg_front_l_palm', {
      center: [6.6, 1.0, -10.2],
      size: [6, 2, 4],
      parent: 'leg_front_l',
      mat: 'frog_skin_body',
    });

    b.addGroup('leg_back_l', [8.0, 8.8, 4.5], [0, 0, 0], 'root');
    b.addCenteredBox('leg_back_l_thigh', {
      center: [8.8, 8.2, 3.8],
      size: [5, 7, 10],
      rotation: [28, 10, -8],
      parent: 'leg_back_l',
      mat: 'frog_skin_body',
    });
    b.addCenteredBox('leg_back_l_calf', {
      center: [9.4, 4.4, 5.2],
      size: [5, 5, 10],
      rotation: [-12, 8, 0],
      parent: 'leg_back_l',
      mat: 'frog_skin_body',
    });
    b.addCenteredBox('leg_back_l_foot', {
      center: [8.8, 1.0, 1.4],
      size: [6, 2, 7],
      parent: 'leg_back_l',
      mat: 'frog_skin_body',
    });
    const backToes = [
      [6.6,  1.0, -2.6],
      [9.0,  1.0, -3.0],
      [11.4, 1.0, -2.2],
    ];
    backToes.forEach((tc, ti) => {
      b.addCenteredBox(`leg_back_l_pad_${ti + 1}`, {
        center: tc,
        size: [2, 2, 3],
        parent: 'leg_back_l',
        mat: 'frog_toe_pad',
      });
    });
  });

  b.addAnimation('idle', {
    length: 3.2,
    loop: 'loop',
    snapping: 24,
    tracks: [
      { bone: 'body', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.6, value: [-1.4, 0, 0] }, { time: 3.2, value: [0, 0, 0] }] },
      { bone: 'head', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.6, value: [2.2, 0, 0] }, { time: 3.2, value: [0, 0, 0] }] },
      { bone: 'collar', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.6, value: [1.8, 0, 0] }, { time: 3.2, value: [0, 0, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// MAIN PIPELINE
// ============================================================================
async function main() {
  const builders = [
    buildAmethystFox(),
    buildSizoLeafwing(),
    buildAzureFrog(),
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

  const reportDir = path.join(repoRoot, 'reports/blockbench_mcp/batch_02');
  fs.mkdirSync(reportDir, { recursive: true });
  fs.writeFileSync(
    path.join(reportDir, 'validation_report.json'),
    JSON.stringify({ generated_via: 'blockbench-mcp-plugin (official MCP server)', models: validationSummary }, null, 2) + '\n'
  );
  console.log('\n[done] All 3 Batch 02 V2 models built & validated via Blockbench MCP!');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
