#!/usr/bin/env node
/**
 * Batch 03 Generator (V2 Master Polish) via Official Blockbench MCP (`jasonjgardner/blockbench-mcp-plugin`):
 *   07 — Цветочная улитка (`blossom_snail`)
 *   08 — Туманный волк-призрак (`phantom_wolf`)
 *   09 — Кристальный ящер (`crystal_gecko`)
 */

import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { ModelBuilder, BlockbenchMcpSession, vNorm } from './builder_lib.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '../..');

// ============================================================================
// 07 — ЦВЕТОЧНАЯ УЛИТКА (`blossom_snail`)
// ============================================================================
function buildBlossomSnail() {
  const b = new ModelBuilder({
    id: 'blossom_snail',
    titleRu: 'Цветочная улитка',
    biome: 'riftworld:waterfall_gardens',
    atlasSize: 512,
    renderScale: 0.5,
  });

  b.addGroup('root', [0, 0, 0]);
  b.addGroup('body', [0, 4.0, 0.0], [0, 0, 0], 'root', { axis: 'z', amp: 0.6, speed: 0.8, phase: 0 });

  // Soft Mint-Turquoise Gliding Snail Foot on Y = 0
  b.addCenteredBox('foot_sole', {
    center: [0, 1.5, 1.0],
    size: [10, 3, 27],
    parent: 'body',
    mat: 'snail_body',
  });
  b.addCenteredBox('foot_body_mid', {
    center: [0, 4.6, 0.5],
    size: [8, 4, 21],
    parent: 'body',
    mat: 'snail_body',
  });
  b.addCenteredBox('foot_tail_tip', {
    center: [0, 1.5, 16.5],
    size: [7, 3, 5],
    parent: 'body',
    mat: 'snail_body',
  });
  b.addCenteredBox('tail_moss_patch', {
    center: [0, 3.6, 15.0],
    size: [5, 2, 5],
    rotation: [-8, 0, 0],
    parent: 'body',
    mat: 'snail_moss',
  });

  // Slender Cute Upright Snail Neck & Segmented Seafoam Throat Plates!
  b.addGroup('neck', [0, 5.8, -9.2], [0, 0, 0], 'body', { axis: 'x', amp: 1.5, speed: 0.9, phase: 0.3 });
  b.addSegmentBox('neck_column', {
    pStart: [0, 4.8, -8.8],
    pEnd: [0, 13.6, -11.4],
    width: 6,
    depth: 6,
    upHint: [0, 0, 1],
    padStart: 1.4,
    padEnd: 1.2,
    parent: 'neck',
    mat: 'snail_body',
  });
  b.addSegmentBox('neck_belly_plate', {
    pStart: [0, 3.2, -11.6],
    pEnd: [0, 12.8, -13.8],
    width: 5,
    depth: 2,
    upHint: [0, 0, 1],
    padStart: 0.8,
    padEnd: 0.8,
    parent: 'neck',
    mat: 'snail_belly',
  });

  // Cute Rounded Petite Snail Head, Cheek Feelers & Slender Eye Stalks!
  b.addGroup('head', [0, 13.8, -11.6], [0, 0, 0], 'neck', { axis: 'x', amp: -1.8, speed: 0.9, phase: 0.7 });
  b.addCenteredBox('head_main', {
    center: [0, 15.2, -12.6],
    size: [7, 5, 6],
    parent: 'head',
    mat: 'snail_head',
  });
  b.addCenteredBox('head_crown', {
    center: [0, 18.0, -12.5],
    size: [5, 2, 5],
    parent: 'head',
    mat: 'snail_body',
  });

  b.addMirrored(() => {
    b.addSegmentBox('feeler_low_l', {
      pStart: [3.0, 14.4, -14.6],
      pEnd: [4.8, 13.8, -16.2],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.2,
      parent: 'head',
      mat: 'snail_body',
    });

    b.addGroup('eyestalk_l', [2.3, 18.2, -12.8], [0, 0, 0], 'head', { axis: 'z', amp: 2.8, speed: 1.1, phase: 0.2 });
    b.addSegmentBox('eyestalk_shaft_l', {
      pStart: [2.2, 17.8, -12.8],
      pEnd: [2.4, 23.0, -13.4],
      width: 1,
      depth: 1,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.6,
      parent: 'eyestalk_l',
      mat: 'snail_body',
    });
    b.addCenteredBox('eye_bulb_l', {
      center: [2.4, 23.8, -13.5],
      size: [3, 3, 2],
      parent: 'eyestalk_l',
      mat: 'snail_eye_bulb',
    });

    b.addCenteredBox('neck_moss_l', {
      center: [3.6, 9.8, -6.2],
      size: [2, 4, 3],
      rotation: [18, 0, -14],
      parent: 'body',
      mat: 'snail_moss',
    });
  });

  // Spectacular Blooming Rose-Blossom Shell with Wide Open Glowing Warm Golden Core!
  const scY = 18.0;
  const scZ = 3.5;
  b.addGroup('shell', [0, scY, scZ], [0, 0, 0], 'body', { axis: 'x', amp: 0.8, speed: 0.8, phase: 0.5 });

  b.addCenteredBox('shell_core_drum', {
    center: [0, scY, scZ],
    size: [11, 18, 18],
    parent: 'shell',
    mat: 'snail_shell_outer',
  });

  // 12 Smooth Polygonal Outer Pink Shell Arch Segments (`r = 10.8`) + Cute Domed Crown Cap (`orig_07_snail.png`)!
  for (let i = 0; i < 12; i++) {
    const deg = i * 30;
    const rad = (deg * Math.PI) / 180;
    const py = scY + Math.cos(rad) * 10.8;
    const pz = scZ + Math.sin(rad) * 10.8;
    b.addCenteredBox(`shell_arch_${i + 1}`, {
      center: [0, py, pz],
      size: [10, 4, 7],
      rotation: [-deg, 0, 0],
      parent: 'shell',
      mat: 'snail_shell_outer',
    });
  }
  b.addCenteredBox('shell_crown_cap', {
    center: [0, scY + 12.6, scZ],
    size: [7, 2, 7],
    parent: 'shell',
    mat: 'snail_shell_outer',
  });

  // Sprouting Green Moss Leaves on front-top & rear-top of shell!
  b.addSegmentBox('shell_sprout_front_1', {
    pStart: [0, 26.8, -3.8],
    pEnd: [0, 30.8, -5.8],
    width: 2,
    depth: 1,
    upHint: [0, 0, 1],
    padStart: 1.0,
    padEnd: 0.2,
    parent: 'shell',
    mat: 'snail_moss',
  });
  b.addSegmentBox('shell_sprout_back_1', {
    pStart: [0, 26.0, 11.4],
    pEnd: [0, 30.4, 14.4],
    width: 2,
    depth: 1,
    upHint: [0, 0, 1],
    padStart: 1.0,
    padEnd: 0.2,
    parent: 'shell',
    mat: 'snail_moss',
  });
  b.addSegmentBox('shell_sprout_back_2', {
    pStart: [0, 25.0, 12.0],
    pEnd: [0, 27.2, 16.4],
    width: 2,
    depth: 1,
    upHint: [0, 1, 0],
    padStart: 1.0,
    padEnd: 0.2,
    parent: 'shell',
    mat: 'snail_moss',
  });

  // Symmetrical Lateral Blooming Rose Petals (`addPlate` exact radial-cupped geometry!) + Wide Open Glowing Core!
  b.addMirrored(() => {
    // 1. Woody-tan calyx sepal ring (`X = 5.4`, `r = 10.2`)
    for (let i = 0; i < 10; i++) {
      const deg = i * 36 + 18;
      const rad = (deg * Math.PI) / 180;
      const py = scY + Math.cos(rad) * 10.2;
      const pz = scZ + Math.sin(rad) * 10.2;
      b.addCenteredBox(`calyx_seg_l_${i + 1}`, {
        center: [5.4, py, pz],
        size: [2, 3, 7],
        rotation: [-deg, 0, 0],
        parent: 'shell',
        mat: 'snail_calyx',
      });
    }

    // 2. WIDE UNOBSTRUCTED RADIANT GOLDEN-PEACH ROSE HEART (`X = 5.8 .. 6.8`, `12x12` glowing canvas + center bud)!
    b.addCenteredBox('rose_core_disc_l', {
      center: [5.8, scY, scZ],
      size: [2, 13, 13],
      parent: 'shell',
      mat: 'snail_rose_core',
    });
    b.addCenteredBox('rose_core_heart_l', {
      center: [6.4, scY, scZ],
      size: [1, 5, 5],
      parent: 'shell',
      mat: 'snail_rose_core',
    });

    // 3. Outer Scalloped Cupped Rose Petal Ring (`r = 9.7`, `X = 6.9`, 9 individual cupped petals!)
    for (let i = 0; i < 9; i++) {
      const rad = ((i * 40 + 6) * Math.PI) / 180;
      const cy = Math.cos(rad);
      const cz = Math.sin(rad);
      const py = scY + cy * 9.7;
      const pz = scZ + cz * 9.7;
      const uTangent = vNorm([0, -cz * 0.96 + cy * 0.28, cy * 0.96 + cz * 0.28]);
      const nCupped = vNorm([0.84, -cy * 0.54, -cz * 0.54]);
      b.addPlate(`rose_outer_l_${i + 1}`, {
        center: [6.9, py, pz],
        width: 5,
        thickness: 1,
        length: 4,
        uAxis: uTangent,
        nAxis: nCupped,
        parent: 'shell',
        mat: 'snail_rose_petal',
      });
    }

    // 4. Middle Staggered Scalloped Rose Petal Ring (`r = 7.7`, `X = 7.4`, 8 cupped petals!)
    for (let i = 0; i < 8; i++) {
      const rad = ((i * 45 + 26) * Math.PI) / 180;
      const cy = Math.cos(rad);
      const cz = Math.sin(rad);
      const py = scY + cy * 7.7;
      const pz = scZ + cz * 7.7;
      const uTangent = vNorm([0, -cz * 0.96 + cy * 0.28, cy * 0.96 + cz * 0.28]);
      const nCupped = vNorm([0.80, -cy * 0.60, -cz * 0.60]);
      b.addPlate(`rose_mid_l_${i + 1}`, {
        center: [7.4, py, pz],
        width: 5,
        thickness: 1,
        length: 3,
        uAxis: uTangent,
        nAxis: nCupped,
        parent: 'shell',
        mat: 'snail_rose_petal',
      });
    }

    // 5. Inner Cupped Rose Petal Ring (`r = 5.8`, `X = 7.9`, 7 delicate petals framing the glowing golden heart!)
    for (let i = 0; i < 7; i++) {
      const rad = ((i * (360 / 7) + 12) * Math.PI) / 180;
      const cy = Math.cos(rad);
      const cz = Math.sin(rad);
      const py = scY + cy * 5.8;
      const pz = scZ + cz * 5.8;
      const uTangent = vNorm([0, -cz * 0.96 + cy * 0.28, cy * 0.96 + cz * 0.28]);
      const nCupped = vNorm([0.76, -cy * 0.65, -cz * 0.65]);
      b.addPlate(`rose_inner_l_${i + 1}`, {
        center: [7.9, py, pz],
        width: 4,
        thickness: 1,
        length: 3,
        uAxis: uTangent,
        nAxis: nCupped,
        parent: 'shell',
        mat: 'snail_rose_petal',
      });
    }

    b.addCenteredBox('calyx_moss_l_1', {
      center: [6.0, 25.4, -2.4],
      size: [2, 3, 3],
      rotation: [24, 12, -18],
      parent: 'shell',
      mat: 'snail_moss',
    });

    const motes = [
      { name: 'pollen_mote_l_1', c: [9.0, 28.5,  8.5] },
      { name: 'pollen_mote_l_2', c: [9.6, 21.0, 15.8] },
      { name: 'pollen_mote_l_3', c: [8.8, 13.0, 12.4] },
    ];
    for (const mt of motes) {
      b.addCenteredBox(mt.name, {
        center: mt.c,
        size: [1, 1, 1],
        parent: 'shell',
        mat: 'snail_pollen',
      });
      b.freeElements.push(mt.name, mt.name.replace('_l_', '_r_'));
    }
  });

  b.addAnimation('idle', {
    length: 4.0,
    loop: 'loop',
    snapping: 24,
    tracks: [
      { bone: 'neck', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [2.2, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'head', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [-2.0, 2.5, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'eyestalk_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [3.5, 0, 3.0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'eyestalk_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [3.5, 0, -3.0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'shell', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [-1.0, 0, 0.8] }, { time: 4.0, value: [0, 0, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// 08 — ТУМАННЫЙ ВОЛК-ПРИЗРАК (`phantom_wolf`)
// ============================================================================
function buildPhantomWolf() {
  const b = new ModelBuilder({
    id: 'phantom_wolf',
    titleRu: 'Туманный волк-призрак',
    biome: 'riftworld:mist_keep',
    atlasSize: 512,
    renderScale: 0.5,
  });

  b.addGroup('root', [0, 0, 0]);
  b.addGroup('body', [0, 22.0, 1.5], [0, 0, 0], 'root', { axis: 'x', amp: 0.8, speed: 0.9, phase: 0 });

  // 1. Hollow Spectral Ribcage Over Glowing Ice-Cyan Spirit Core (elevated on tall gaunt lupine legs!)
  b.addCenteredBox('spirit_core', {
    center: [0, 21.8, 1.2],
    size: [5, 7, 13],
    parent: 'body',
    mat: 'wolf_spirit_core',
  });
  b.addCenteredBox('spine_ridge', {
    center: [0, 25.6, 1.5],
    size: [8, 3, 21],
    parent: 'body',
    mat: 'wolf_fur',
  });
  b.addCenteredBox('chest_front_block', {
    center: [0, 21.6, -6.8],
    size: [9, 8, 5],
    parent: 'body',
    mat: 'wolf_fur',
  });
  b.addCenteredBox('pelvis_rear_block', {
    center: [0, 22.2, 9.8],
    size: [8, 7, 5],
    parent: 'body',
    mat: 'wolf_fur',
  });
  b.addCenteredBox('sternum_keel', {
    center: [0, 17.6, 0.2],
    size: [5, 2, 15],
    rotation: [-4, 0, 0],
    parent: 'body',
    mat: 'wolf_rib_bone',
  });

  b.addMirrored(() => {
    const ribZs = [-3.6, -1.0, 1.6, 4.2, 6.8];
    ribZs.forEach((rz, idx) => {
      const ribH = idx <= 2 ? 8 : 7;
      const ribY = idx <= 2 ? 21.6 : 22.1;
      b.addCenteredBox(`rib_l_${idx + 1}`, {
        center: [3.8, ribY, rz],
        size: [2, ribH, 1],
        rotation: [4, 0, -6],
        parent: 'body',
        mat: 'wolf_rib_bone',
      });
    });

    // Subtle hugging dorsal mist wisp along back!
    b.addSegmentBox('dorsal_wisp_l_1', {
      pStart: [2.4, 26.8, -3.2],
      pEnd: [3.0, 27.6, 4.2],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.2,
      parent: 'body',
      mat: 'wolf_wisp',
    });
  });

  // 2. Spectral Wolf Neck, Flowing Mist-Fur Collar & Fierce Glowing-Eyed Lupine Head!
  b.addGroup('neck', [0, 23.8, -8.2], [0, 0, 0], 'body', { axis: 'x', amp: 1.4, speed: 0.9, phase: 0.3 });
  b.addSegmentBox('neck_core', {
    pStart: [0, 22.4, -7.8],
    pEnd: [0, 29.8, -12.6],
    width: 8,
    depth: 8,
    upHint: [0, 0, 1],
    padStart: 1.4,
    padEnd: 1.2,
    parent: 'neck',
    mat: 'wolf_fur',
  });
  b.addMirrored(() => {
    b.addSegmentBox('neck_mane_wisp_l_1', {
      pStart: [3.6, 28.4, -10.8],
      pEnd: [4.8, 30.2, -6.8],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.6,
      parent: 'neck',
      mat: 'wolf_wisp',
    });
    b.addSegmentBox('neck_mane_wisp_l_2', {
      pStart: [4.7, 30.0, -7.0],
      pEnd: [5.4, 31.8, -3.6],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.2,
      parent: 'neck',
      mat: 'wolf_wisp',
    });
  });

  b.addGroup('head', [0, 29.8, -12.6], [0, 0, 0], 'neck', { axis: 'x', amp: -1.5, speed: 0.9, phase: 0.6 });
  b.addCenteredBox('head_cranium', {
    center: [0, 31.0, -15.0],
    size: [9, 8, 8],
    parent: 'head',
    mat: 'wolf_fur',
  });
  b.addCenteredBox('snout_upper', {
    center: [0, 29.0, -21.4],
    size: [5, 4, 7],
    rotation: [4, 0, 0],
    parent: 'head',
    mat: 'wolf_fur',
  });
  b.addCenteredBox('snout_lower', {
    center: [0, 27.0, -20.8],
    size: [4, 2, 6],
    parent: 'head',
    mat: 'wolf_fur',
  });
  b.addCenteredBox('nose_tip', {
    center: [0, 29.8, -24.4],
    size: [3, 2, 2],
    parent: 'head',
    mat: 'wolf_nose',
  });

  b.addMirrored(() => {
    // Piercing Luminous Ice-Cyan Spectral Wolf Eyes (`2x2x2` cleanly above snout!)
    b.addCenteredBox('eye_l', {
      center: [2.7, 31.6, -18.4],
      size: [2, 2, 2],
      parent: 'head',
      mat: 'wolf_eye_cyan',
    });

    b.addSegmentBox('ear_base_l', {
      pStart: [3.0, 34.6, -14.0],
      pEnd: [3.4, 38.4, -13.6],
      width: 3,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 1.0,
      padEnd: 0.5,
      parent: 'head',
      mat: 'wolf_fur',
    });
    b.addSegmentBox('ear_tip_l', {
      pStart: [3.3, 38.0, -13.6],
      pEnd: [3.6, 40.2, -13.4],
      width: 2,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 0.7,
      padEnd: 0.2,
      parent: 'head',
      mat: 'wolf_fur',
    });

    b.addSegmentBox('cheek_wisp_l_1', {
      pStart: [4.2, 30.8, -13.2],
      pEnd: [5.4, 32.2, -9.2],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.6,
      parent: 'head',
      mat: 'wolf_wisp',
    });
    b.addSegmentBox('cheek_wisp_l_2', {
      pStart: [5.3, 32.0, -9.4],
      pEnd: [5.8, 33.8, -6.0],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.2,
      parent: 'head',
      mat: 'wolf_wisp',
    });
  });

  // 3. Four Tall Gaunt Lupine Legs on Y = 0 with Delicate Ghostly Ankle Mist Wisps!
  b.addMirrored(() => {
    b.addGroup('leg_front_l', [3.6, 20.0, -6.8], [0, 0, 0], 'root');
    b.addCenteredBox('leg_front_l_shoulder', {
      center: [3.8, 19.6, -6.8],
      size: [4, 8, 5],
      parent: 'leg_front_l',
      mat: 'wolf_fur',
    });
    b.addCenteredBox('leg_front_l_shank', {
      center: [3.5, 9.0, -7.0],
      size: [3, 15, 3],
      parent: 'leg_front_l',
      mat: 'wolf_leg',
    });
    b.addCenteredBox('leg_front_l_paw', {
      center: [3.5, 1.0, -8.0],
      size: [3, 2, 5],
      parent: 'leg_front_l',
      mat: 'wolf_leg',
    });
    b.addSegmentBox('leg_front_l_wisp_1', {
      pStart: [3.5, 2.8, -5.8],
      pEnd: [4.0, 6.8, -2.8],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.2,
      parent: 'leg_front_l',
      mat: 'wolf_wisp',
    });

    b.addGroup('leg_back_l', [3.5, 21.0, 9.6], [0, 0, 0], 'root');
    b.addSegmentBox('leg_back_l_thigh', {
      pStart: [3.6, 21.6, 9.0],
      pEnd: [3.6, 13.6, 11.4],
      width: 4,
      depth: 5,
      upHint: [0, 0, 1],
      padStart: 1.2,
      padEnd: 0.8,
      parent: 'leg_back_l',
      mat: 'wolf_fur',
    });
    b.addSegmentBox('leg_back_l_hock', {
      pStart: [3.5, 14.0, 11.2],
      pEnd: [3.4, 8.6, 13.8],
      width: 3,
      depth: 3,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.8,
      parent: 'leg_back_l',
      mat: 'wolf_leg',
    });
    b.addSegmentBox('leg_back_l_tarsus', {
      pStart: [3.4, 9.0, 13.6],
      pEnd: [3.4, 1.6, 11.8],
      width: 3,
      depth: 3,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.6,
      parent: 'leg_back_l',
      mat: 'wolf_leg',
    });
    b.addCenteredBox('leg_back_l_paw', {
      center: [3.4, 1.0, 10.6],
      size: [3, 2, 5],
      parent: 'leg_back_l',
      mat: 'wolf_leg',
    });
    b.addSegmentBox('leg_back_l_wisp_1', {
      pStart: [3.4, 3.2, 12.8],
      pEnd: [4.0, 7.2, 16.4],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.2,
      parent: 'leg_back_l',
      mat: 'wolf_wisp',
    });

    const motes = [
      { name: 'wolf_mote_l_1', c: [5.0, 12.5, -2.5] },
      { name: 'wolf_mote_l_2', c: [4.6, 28.2,  5.8] },
      { name: 'wolf_mote_l_3', c: [5.0,  9.2, 16.5] },
    ];
    for (const mt of motes) {
      b.addCenteredBox(mt.name, {
        center: mt.c,
        size: [1, 1, 1],
        parent: 'root',
        mat: 'wolf_wisp',
      });
      b.freeElements.push(mt.name, mt.name.replace('_l_', '_r_'));
    }
  });

  // 4. Sinuous Flowing S-Curved Spectral Plume Tail (`tail_base`)!
  b.addGroup('tail_base', [0, 23.4, 12.0], [0, 0, 0], 'body', { axis: 'y', amp: 4.5, speed: 0.9, phase: 0.5 });
  const tailSpine = [
    { name: 'tail_core_1', p0: [0, 23.4, 11.6], p1: [0, 18.2, 18.4], w: 5, d: 5, mat: 'wolf_fur' },
    { name: 'tail_core_2', p0: [0, 18.4, 18.0], p1: [0, 13.0, 25.0], w: 5, d: 5, mat: 'wolf_wisp' },
    { name: 'tail_core_3', p0: [0, 13.2, 24.5], p1: [0, 11.6, 31.0], w: 4, d: 4, mat: 'wolf_wisp' },
    { name: 'tail_core_4', p0: [0, 11.6, 30.5], p1: [0, 14.8, 35.6], w: 2, d: 3, mat: 'wolf_wisp' },
  ];
  for (const ts of tailSpine) {
    b.addSegmentBox(ts.name, {
      pStart: ts.p0,
      pEnd: ts.p1,
      width: ts.w,
      depth: ts.d,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.8,
      parent: 'tail_base',
      mat: ts.mat,
    });
  }

  b.addMirrored(() => {
    b.addSegmentBox('tail_plume_up_l', {
      pStart: [1.8, 21.0, 15.2],
      pEnd: [2.8, 16.8, 24.8],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.6,
      parent: 'tail_base',
      mat: 'wolf_wisp',
    });
    b.addSegmentBox('tail_plume_curl_l', {
      pStart: [2.6, 16.8, 24.4],
      pEnd: [3.2, 18.2, 31.8],
      width: 1,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.2,
      parent: 'tail_base',
      mat: 'wolf_wisp',
    });
    b.addSegmentBox('tail_plume_low_l', {
      pStart: [1.6, 15.4, 21.0],
      pEnd: [2.4, 10.2, 29.8],
      width: 1,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.2,
      parent: 'tail_base',
      mat: 'wolf_wisp',
    });
  });

  b.addAnimation('idle', {
    length: 4.0,
    loop: 'loop',
    snapping: 24,
    tracks: [
      { bone: 'body', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [0.9, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'neck', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [1.5, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'head', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [-2.0, 2.0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'tail_base', channel: 'rotation', keys: [{ time: 0.0, value: [0, -4.5, 0] }, { time: 2.0, value: [2.0, 4.5, 0] }, { time: 4.0, value: [0, -4.5, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// 09 — КРИСТАЛЬНЫЙ ЯЩЕР (`crystal_gecko`)
// ============================================================================
function buildCrystalGecko() {
  const b = new ModelBuilder({
    id: 'crystal_gecko',
    titleRu: 'Кристальный ящер',
    biome: 'riftworld:crystal_rifts',
    atlasSize: 512,
    renderScale: 0.5,
  });

  b.addGroup('root', [0, 0, 0]);
  b.addGroup('body', [0, 9.5, 1.0], [0, 0, 0], 'root', { axis: 'y', amp: 1.5, speed: 1.0, phase: 0 });

  b.addCenteredBox('torso_main', {
    center: [0, 9.8, 0.8],
    size: [9, 7, 18],
    parent: 'body',
    mat: 'gecko_scales_body',
  });
  b.addCenteredBox('pelvis_taper', {
    center: [0, 9.6, 10.6],
    size: [7, 6, 5],
    rotation: [-6, 0, 0],
    parent: 'body',
    mat: 'gecko_tail',
  });
  b.addCenteredBox('neck_joint', {
    center: [0, 10.6, -9.2],
    size: [8, 6, 5],
    rotation: [12, 0, 0],
    parent: 'body',
    mat: 'gecko_tail',
  });

  // 6 Graduated Tilted Quartz-Ice Dorsal Crystal Spines + Pointed Crystal Tips!
  const spineCrys = [
    { name: 'gecko_crys_1', s: [0, 12.8, -5.8], e: [0, 16.6, -4.4], w: 2, d: 3, tip: false },
    { name: 'gecko_crys_2', s: [0, 12.8, -2.6], e: [0, 18.0, -0.8], w: 2, d: 3, tip: true  },
    { name: 'gecko_crys_3', s: [0, 12.8,  0.8], e: [0, 19.2,  2.8], w: 3, d: 3, tip: true  },
    { name: 'gecko_crys_4', s: [0, 12.8,  4.2], e: [0, 18.2,  6.2], w: 2, d: 3, tip: true  },
    { name: 'gecko_crys_5', s: [0, 12.6,  7.4], e: [0, 16.8,  9.2], w: 2, d: 3, tip: false },
    { name: 'gecko_crys_6', s: [0, 12.0, 10.4], e: [0, 15.0, 11.8], w: 2, d: 2, tip: false },
  ];
  for (const cr of spineCrys) {
    b.addSegmentBox(cr.name, {
      pStart: cr.s,
      pEnd: cr.e,
      width: cr.w,
      depth: cr.d,
      upHint: [0, 0, 1],
      padStart: 1.0,
      padEnd: 0.4,
      parent: 'body',
      mat: 'gecko_dorsal_crystal',
      meta: { crysT0: 0.0, crysT1: 0.9 },
    });
    if (cr.tip) {
      b.addSegmentBox(`${cr.name}_peak`, {
        pStart: cr.e,
        pEnd: [cr.e[0], cr.e[1] + 1.8, cr.e[2] + 0.6],
        width: 1,
        depth: 2,
        upHint: [0, 0, 1],
        padStart: 0.8,
        padEnd: 0.1,
        parent: 'body',
        mat: 'gecko_dorsal_crystal',
        meta: { crysT0: 0.75, crysT1: 1.0 },
      });
    }
  }

  b.addMirrored(() => {
    b.addCenteredBox('neck_frill_l', {
      center: [5.0, 11.2, -9.2],
      size: [2, 6, 5],
      rotation: [18, -14, -22],
      parent: 'body',
      mat: 'gecko_frill',
    });
  });

  // Sculpted Crystalline Lizard Head, Bright Glowing Ice-Lantern Gecko Eyes & Faceted Ear-Horns!
  b.addGroup('head', [0, 11.5, -11.0], [0, 0, 0], 'body', { axis: 'y', amp: -2.2, speed: 1.0, phase: 0.4 });
  b.addCenteredBox('head_cranium', {
    center: [0, 12.4, -14.2],
    size: [10, 7, 8],
    parent: 'head',
    mat: 'gecko_head',
  });
  b.addSegmentBox('snout_bridge', {
    pStart: [0, 14.2, -16.8],
    pEnd: [0, 11.6, -21.4],
    width: 6,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 1.2,
    padEnd: 0.6,
    parent: 'head',
    mat: 'gecko_head',
  });
  b.addCenteredBox('snout_upper', {
    center: [0, 11.2, -19.6],
    size: [7, 3, 5],
    parent: 'head',
    mat: 'gecko_head',
  });
  b.addCenteredBox('jaw_lower', {
    center: [0, 9.2, -16.8],
    size: [7, 2, 10],
    parent: 'head',
    mat: 'gecko_head',
  });

  b.addMirrored(() => {
    b.addCenteredBox('brow_ridge_l', {
      center: [3.8, 15.8, -14.8],
      size: [3, 2, 6],
      rotation: [6, 0, -8],
      parent: 'head',
      mat: 'gecko_head',
    });
    // Large Bright Glowing Ice-Cyan Gecko Eye (`[2, 4, 5]` protruding slightly from skull!)
    b.addCenteredBox('eye_l', {
      center: [4.4, 12.8, -14.8],
      size: [2, 4, 5],
      parent: 'head',
      mat: 'gecko_eye',
    });
    b.addSegmentBox('head_horn_l', {
      pStart: [3.8, 15.0, -11.2],
      pEnd: [5.4, 17.6, -8.8],
      width: 2,
      depth: 3,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.2,
      parent: 'head',
      mat: 'gecko_dorsal_crystal',
      meta: { crysT0: 0.2, crysT1: 1.0 },
    });
  });

  // Four Sprawling Articulated Reptilian Legs + 4 LONG Radially-Splayed Crystal Toes Per Foot on Y = 0!
  b.addMirrored(() => {
    const sprawlingLegs = [
      {
        name: 'leg_front_l',
        hip: [4.2, 8.8, -5.8],
        elb: [8.8, 6.8, -4.4],
        wri: [7.8, 1.5, -8.2],
        palm: [7.8, 1.0, -8.8],
        toeAngles: [52, 18, -16, -50],
      },
      {
        name: 'leg_back_l',
        hip: [4.0, 8.8, 7.6],
        elb: [8.6, 7.2, 6.2],
        wri: [8.8, 1.5, 9.2],
        palm: [8.8, 1.0, 8.6],
        toeAngles: [46, 12, -22, -56],
      },
    ];

    for (const lg of sprawlingLegs) {
      b.addGroup(lg.name, lg.hip, [0, 0, 0], 'root');
      b.addSegmentBox(`${lg.name}_upper`, {
        pStart: lg.hip,
        pEnd: lg.elb,
        width: 4,
        depth: 4,
        upHint: [0, 1, 0],
        padStart: 1.4,
        padEnd: 1.0,
        parent: lg.name,
        mat: 'gecko_tail',
      });
      b.addSegmentBox(`${lg.name}_lower`, {
        pStart: lg.elb,
        pEnd: lg.wri,
        width: 4,
        depth: 4,
        upHint: [0, 0, 1],
        padStart: 1.0,
        padEnd: 0.8,
        parent: lg.name,
        mat: 'gecko_tail',
      });
      b.addCenteredBox(`${lg.name}_palm`, {
        center: lg.palm,
        size: [4, 2, 4],
        parent: lg.name,
        mat: 'gecko_tail',
      });

      // 4 Long Radially-Splayed Crystal Gecko Fingers (`size: [2, 2, 6]` extending 5 units beyond palm!)
      lg.toeAngles.forEach((yaw, ti) => {
        const rad = (yaw * Math.PI) / 180;
        const offsetDist = 4.2;
        // Note: Three.js `+Ry` rotates `(0, 0, -1)` to `(-sin(yaw), 0, -cos(yaw))`? Wait:
        // R_y(+yaw) * [0, 0, -1] = [-sin(yaw), 0, -cos(yaw)]!
        // Let's check: for left foot (`X > 0`), negative `yaw` (`-50`) gives `-sin(-50) = +0.766` (+X outward!),
        // positive `yaw` (`+52`) gives `-sin(+52) = -0.788` (-X inward toward midline!).
        const tx = lg.palm[0] - Math.sin(rad) * offsetDist;
        const tz = lg.palm[2] - Math.cos(rad) * offsetDist;
        b.addCenteredBox(`${lg.name}_toe_${ti + 1}`, {
          center: [tx, 1.0, tz],
          size: [2, 2, 6],
          rotation: [0, yaw, 0],
          parent: lg.name,
          mat: 'gecko_toe',
        });
      });
    }
  });

  // Iconic 12-Segment Vertically Coiled Spiral Chameleon/Gecko Tail!
  b.addGroup('tail_base', [0, 9.4, 12.2], [0, 0, 0], 'body', { axis: 'x', amp: 2.5, speed: 1.0, phase: 0.5 });
  const spiralPts = [
    [0,  9.4, 12.0],
    [0,  8.0, 16.5],
    [0,  7.4, 21.2],
    [0,  8.2, 25.8],
    [0, 10.6, 29.6],
    [0, 14.4, 31.8],
    [0, 18.6, 32.0],
    [0, 22.4, 30.0],
    [0, 24.6, 26.4],
    [0, 24.4, 22.4],
    [0, 21.8, 19.6],
    [0, 18.4, 19.4],
    [0, 16.2, 21.6],
  ];
  const spiralSizes = [
    [6, 6],
    [6, 5],
    [5, 5],
    [5, 5],
    [5, 4],
    [4, 4],
    [4, 4],
    [4, 4],
    [3, 4],
    [3, 3],
    [3, 3],
    [2, 3],
  ];
  for (let i = 0; i < spiralSizes.length; i++) {
    b.addSegmentBox(`tail_coil_${i + 1}`, {
      pStart: spiralPts[i],
      pEnd: spiralPts[i + 1],
      width: spiralSizes[i][0],
      depth: spiralSizes[i][1],
      upHint: [1, 0, 0],
      padStart: 1.0,
      padEnd: i === spiralSizes.length - 1 ? 0.3 : 0.9,
      parent: 'tail_base',
      mat: 'gecko_tail',
    });
  }

  b.addAnimation('idle', {
    length: 3.6,
    loop: 'loop',
    snapping: 24,
    tracks: [
      { bone: 'body', channel: 'rotation', keys: [{ time: 0.0, value: [0, -1.5, 0] }, { time: 1.8, value: [0, 1.5, 0] }, { time: 3.6, value: [0, -1.5, 0] }] },
      { bone: 'head', channel: 'rotation', keys: [{ time: 0.0, value: [0, 2.5, 0] }, { time: 1.8, value: [-1.5, -2.5, 0] }, { time: 3.6, value: [0, 2.5, 0] }] },
      { bone: 'tail_base', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [3.0, 2.0, 0] }, { time: 3.6, value: [0, 0, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// MAIN PIPELINE
// ============================================================================
async function main() {
  const builders = [
    buildBlossomSnail(),
    buildPhantomWolf(),
    buildCrystalGecko(),
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

  const reportDir = path.join(repoRoot, 'reports/blockbench_mcp/batch_03');
  fs.mkdirSync(reportDir, { recursive: true });
  fs.writeFileSync(
    path.join(reportDir, 'validation_report.json'),
    JSON.stringify({ generated_via: 'blockbench-mcp-plugin (official MCP server)', models: validationSummary }, null, 2) + '\n'
  );
  console.log('\n[done] All 3 Batch 03 V2 models built & validated via Blockbench MCP!');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
