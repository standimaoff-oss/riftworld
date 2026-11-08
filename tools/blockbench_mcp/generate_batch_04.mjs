#!/usr/bin/env node
/**
 * Batch 04 Generator (V2 Masterpiece) via Official Blockbench MCP (`jasonjgardner/blockbench-mcp-plugin`):
 *   10 — Гранёная сова (`faceted_owl`)
 *   11 — Костяной ворон (`bone_raven`)
 *   12 — Коралловый рак-отшельник (`coral_hermit_crab`)
 */

import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { ModelBuilder, BlockbenchMcpSession, vNorm, vAdd, vScale } from './builder_lib.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '../..');

// ============================================================================
// 10 — ГРАНЁНАЯ СОВА (`faceted_owl`)
// ============================================================================
function buildFacetedOwl() {
  const b = new ModelBuilder({
    id: 'faceted_owl',
    titleRu: 'Гранёная сова',
    biome: 'riftworld:crystal_rifts',
    atlasSize: 512,
    renderScale: 0.5,
  });

  b.addGroup('root', [0, 0, 0]);
  b.addGroup('body', [0, 15.0, -0.5], [0, 0, 0], 'root', { axis: 'x', amp: 0.9, speed: 0.9, phase: 0 });

  // 1. Plump Curved Snowy-Owl Raptor Torso (puffed chest tapering down-and-back toward tail!)
  b.addCenteredBox('torso_core', {
    center: [0, 17.2, -0.6],
    size: [11, 13, 10],
    parent: 'body',
    mat: 'owl_plumage',
  });
  b.addCenteredBox('chest_bulge', {
    center: [0, 18.2, -5.2],
    size: [9, 9, 3],
    parent: 'body',
    mat: 'owl_plumage',
  });
  b.addCenteredBox('torso_belly_taper', {
    center: [0, 10.6, 0.8],
    size: [9, 7, 9],
    rotation: [18, 0, 0],
    parent: 'body',
    mat: 'owl_plumage',
  });
  b.addCenteredBox('torso_undertail', {
    center: [0, 7.6, 3.2],
    size: [7, 5, 6],
    rotation: [24, 0, 0],
    parent: 'body',
    mat: 'owl_plumage',
  });

  // 2. 45-Degree Diamond-Pointed Faceted Breast Crystal Necklace (`orig_10_owl.png`!)
  b.addPlate('chest_diamond_center', {
    center: [0, 19.6, -6.8],
    width: 4,
    thickness: 2,
    length: 4,
    uAxis: vNorm([0.707, 0.707, 0]),
    nAxis: vNorm([0, 0.14, -0.99]),
    parent: 'body',
    mat: 'owl_diamond_breast',
    meta: { cy: 19.6 },
  });
  b.addPlate('belly_diamond_center', {
    center: [0, 15.4, -6.2],
    width: 4,
    thickness: 2,
    length: 4,
    uAxis: vNorm([0.707, 0.707, 0]),
    nAxis: vNorm([0, -0.14, -0.99]),
    parent: 'body',
    mat: 'owl_diamond_breast',
    meta: { cy: 15.4 },
  });
  b.addPlate('belly_diamond_low_center', {
    center: [0, 11.2, -4.6],
    width: 4,
    thickness: 2,
    length: 4,
    uAxis: vNorm([0.707, 0.707, 0]),
    nAxis: vNorm([0, -0.28, -0.96]),
    parent: 'body',
    mat: 'owl_plumage',
  });

  b.addMirrored(() => {
    b.addPlate('chest_diamond_inner_l', {
      center: [3.2, 20.2, -6.3],
      width: 4,
      thickness: 2,
      length: 4,
      uAxis: vNorm([0.68, 0.70, 0.20]),
      nAxis: vNorm([0.28, 0.12, -0.95]),
      parent: 'body',
      mat: 'owl_diamond_breast',
      meta: { cy: 20.2 },
    });
    b.addPlate('chest_diamond_outer_l', {
      center: [5.4, 21.0, -4.8],
      width: 3,
      thickness: 2,
      length: 3,
      uAxis: vNorm([0.60, 0.72, 0.35]),
      nAxis: vNorm([0.55, 0.14, -0.82]),
      parent: 'body',
      mat: 'owl_diamond_breast',
      meta: { cy: 21.0 },
    });
    b.addPlate('belly_diamond_side_l', {
      center: [2.8, 16.4, -5.8],
      width: 4,
      thickness: 2,
      length: 4,
      uAxis: vNorm([0.68, 0.70, 0.20]),
      nAxis: vNorm([0.30, -0.14, -0.94]),
      parent: 'body',
      mat: 'owl_plumage',
    });
    b.addPlate('belly_diamond_low_l', {
      center: [2.6, 12.4, -4.4],
      width: 4,
      thickness: 2,
      length: 4,
      uAxis: vNorm([0.68, 0.70, 0.20]),
      nAxis: vNorm([0.32, -0.26, -0.91]),
      parent: 'body',
      mat: 'owl_plumage',
    });
  });

  // 3. Folded Multi-Tiered Crystalline Wings (`wing_l` / `wing_r`) with Faceted Turquoise Gems & Cobalt Primaries!
  b.addMirrored(() => {
    b.addGroup('wing_l', [5.5, 22.2, -1.0], [0, 0, 0], 'body', { axis: 'z', amp: 1.4, speed: 0.9, phase: 0.3 });

    b.addSegmentBox('wing_core_l', {
      pStart: [5.4, 22.4, -1.8],
      pEnd: [5.6, 10.6, 4.8],
      width: 2,
      depth: 7,
      upHint: [0, 0, 1],
      padStart: 1.0,
      padEnd: 0.8,
      parent: 'wing_l',
      mat: 'owl_plumage',
    });

    // Beveled Diamond Shoulder Gem + Staggered Layered Turquoise Crystal Coverts!
    b.addPlate('wing_shoulder_diamond_l', {
      center: [6.7, 19.6, -1.2],
      width: 5,
      thickness: 2,
      length: 5,
      uAxis: vNorm([0.12, 0.76, 0.64]),
      nAxis: vNorm([0.96, 0.18, -0.20]),
      parent: 'wing_l',
      mat: 'owl_crystal_cyan',
      meta: { tipDir: 'down' },
    });

    const coverts = [
      { name: 'wing_cov_l_1', s: [6.4, 22.2,  0.4], e: [7.0, 16.6, 4.2], w: 2, d: 3, mat: 'owl_plumage' },
      { name: 'wing_cov_l_2', s: [6.7, 17.6,  0.6], e: [7.2, 12.2, 4.4], w: 2, d: 3, mat: 'owl_crystal_cyan' },
      { name: 'wing_cov_l_3', s: [6.4, 18.8,  2.8], e: [7.0, 13.4, 6.8], w: 2, d: 3, mat: 'owl_crystal_cyan' },
      { name: 'wing_cov_l_4', s: [6.5, 14.4,  2.4], e: [6.9,  9.2, 6.4], w: 2, d: 3, mat: 'owl_crystal_cyan' },
    ];
    for (const cv of coverts) {
      b.addSegmentBox(cv.name, {
        pStart: cv.s,
        pEnd: cv.e,
        width: cv.w,
        depth: cv.d,
        upHint: [0, 0, 1],
        padStart: 1.0,
        padEnd: 0.4,
        parent: 'wing_l',
        mat: cv.mat,
        meta: { tipDir: 'down' },
      });
    }

    const primaries = [
      { name: 'wing_prim_l_1', s: [6.0, 13.8, 0.8], e: [6.2, 5.8, 6.8], d: 3, mat: 'owl_crystal_cobalt' },
      { name: 'wing_prim_l_2', s: [5.8, 12.6, 3.0], e: [6.0, 5.0, 9.2], d: 3, mat: 'owl_crystal_cyan' },
      { name: 'wing_prim_l_3', s: [5.4, 11.6, 4.8], e: [5.5, 4.4, 10.8], d: 2, mat: 'owl_crystal_cobalt' },
    ];
    for (const pr of primaries) {
      b.addSegmentBox(pr.name, {
        pStart: pr.s,
        pEnd: pr.e,
        width: 2,
        depth: pr.d,
        upHint: [0, 0, 1],
        padStart: 1.0,
        padEnd: 0.3,
        parent: 'wing_l',
        mat: pr.mat,
        meta: { tipDir: 'down' },
      });
    }
  });

  // 4. Crystalline Tail Fan (`tail`)
  b.addGroup('tail', [0, 7.2, 4.4], [0, 0, 0], 'body', { axis: 'x', amp: 1.8, speed: 0.9, phase: 0.6 });
  b.addSegmentBox('tail_center', {
    pStart: [0, 7.6, 4.2],
    pEnd: [0, 2.6, 11.0],
    width: 3,
    depth: 2,
    upHint: [0, 1, 0],
    padStart: 1.2,
    padEnd: 0.3,
    parent: 'tail',
    mat: 'owl_crystal_cyan',
    meta: { tipDir: 'down' },
  });
  b.addMirrored(() => {
    b.addSegmentBox('tail_side_l', {
      pStart: [1.8, 7.8, 4.0],
      pEnd: [2.8, 3.2, 10.4],
      width: 3,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.3,
      parent: 'tail',
      mat: 'owl_crystal_cobalt',
    });
  });

  // 5. Rounded Owl Head, Twin Facial Discs, Round 4x4 Glowing Cyan Eyes, Cobalt Beak & Double Crystal Ear-Tufts!
  b.addGroup('head', [0, 23.6, -1.2], [0, 0, 0], 'body', { axis: 'y', amp: 2.8, speed: 0.8, phase: 0.4 });
  b.addCenteredBox('head_main', {
    center: [0, 28.4, -2.0],
    size: [13, 9, 9],
    parent: 'head',
    mat: 'owl_face_head',
  });
  b.addCenteredBox('head_crown_dome', {
    center: [0, 33.4, -1.8],
    size: [9, 2, 7],
    parent: 'head',
    mat: 'owl_plumage',
  });
  b.addCenteredBox('head_back_round', {
    center: [0, 28.4, 3.0],
    size: [9, 8, 2],
    parent: 'head',
    mat: 'owl_plumage',
  });

  b.addCenteredBox('beak_upper', {
    center: [0, 26.8, -7.2],
    size: [2, 3, 3],
    rotation: [16, 0, 0],
    parent: 'head',
    mat: 'owl_beak',
  });
  b.addCenteredBox('beak_hook_tip', {
    center: [0, 25.2, -7.8],
    size: [1, 2, 2],
    parent: 'head',
    mat: 'owl_beak',
  });

  b.addMirrored(() => {
    // Large Round `4x4` Luminous Glowing Cyan Owl Eye!
    b.addCenteredBox('owl_eye_l', {
      center: [3.2, 28.2, -6.0],
      size: [4, 4, 2],
      parent: 'head',
      mat: 'owl_eye',
    });

    b.addSegmentBox('brow_ridge_l', {
      pStart: [0.8, 29.4, -6.8],
      pEnd: [6.0, 32.2, -6.0],
      width: 2,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.6,
      parent: 'head',
      mat: 'owl_plumage',
    });

    b.addSegmentBox('ear_crest_main_l', {
      pStart: [4.2, 31.6, -5.4],
      pEnd: [7.6, 37.0, -3.2],
      width: 2,
      depth: 3,
      upHint: [0, 0, 1],
      padStart: 1.0,
      padEnd: 0.3,
      parent: 'head',
      mat: 'owl_crest_lavender',
    });
    b.addSegmentBox('ear_plume_cyan_l', {
      pStart: [5.4, 31.4, -3.0],
      pEnd: [9.2, 37.0, 0.6],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.3,
      parent: 'head',
      mat: 'owl_crystal_cyan',
      meta: { tipDir: 'up' },
    });
    b.addSegmentBox('ear_plume_sub_l', {
      pStart: [5.8, 29.8, -2.0],
      pEnd: [8.8, 33.4, 1.4],
      width: 1,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.2,
      parent: 'head',
      mat: 'owl_crystal_cyan',
      meta: { tipDir: 'up' },
    });

    b.addCenteredBox('cheek_ruff_l_1', {
      center: [6.8, 27.6, -2.0],
      size: [2, 6, 5],
      rotation: [8, 0, -10],
      parent: 'head',
      mat: 'owl_plumage',
    });
  });

  // 6. Crystalline Legs & Splayed Turquoise-to-Sapphire Talons on Y = 0!
  b.addMirrored(() => {
    b.addGroup('leg_l', [3.2, 8.8, -0.2], [0, 0, 0], 'root');
    b.addSegmentBox('leg_l_thigh', {
      pStart: [3.2, 9.6, -0.2],
      pEnd: [3.4, 5.2, -1.6],
      width: 3,
      depth: 4,
      upHint: [0, 0, 1],
      padStart: 1.2,
      padEnd: 0.8,
      parent: 'leg_l',
      mat: 'owl_plumage',
    });
    b.addSegmentBox('leg_l_tarsus', {
      pStart: [3.4, 5.4, -1.5],
      pEnd: [3.5, 1.6, -3.4],
      width: 2,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.4,
      parent: 'leg_l',
      mat: 'owl_tarsus',
    });
    b.addCenteredBox('leg_l_ankle', {
      center: [3.5, 1.5, -3.4],
      size: [4, 2, 3],
      parent: 'leg_l',
      mat: 'owl_tarsus',
    });

    const toeYaws = [22, 0, -22];
    toeYaws.forEach((yaw, ti) => {
      const rad = (yaw * Math.PI) / 180;
      const sx = -Math.sin(rad);
      const sz = -Math.cos(rad);
      b.addCenteredBox(`leg_l_toe_${ti + 1}`, {
        center: [3.5 + sx * 2.2, 1.5, -3.4 + sz * 2.2],
        size: [1, 1, 4],
        rotation: [0, yaw, 0],
        parent: 'leg_l',
        mat: 'owl_crystal_cyan',
        meta: { tipDir: 'up' },
      });
      b.addCenteredBox(`leg_l_claw_${ti + 1}`, {
        center: [3.5 + sx * 4.0, 0.5, -3.4 + sz * 4.0],
        size: [1, 1, 2],
        rotation: [0, yaw, 0],
        parent: 'leg_l',
        mat: 'owl_claw_dark',
      });
    });

    b.addCenteredBox('leg_l_hallux', {
      center: [3.5, 0.5, -1.4],
      size: [1, 1, 3],
      parent: 'leg_l',
      mat: 'owl_claw_dark',
    });
  });

  b.addAnimation('idle', {
    length: 4.0,
    loop: 'loop',
    snapping: 24,
    tracks: [
      { bone: 'body', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [1.0, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'head', channel: 'rotation', keys: [{ time: 0.0, value: [0, -3.0, 0] }, { time: 2.0, value: [1.5, 3.0, 0] }, { time: 4.0, value: [0, -3.0, 0] }] },
      { bone: 'wing_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [0, 0, 1.5] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'wing_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [0, 0, -1.5] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'tail', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [2.0, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// 11 — КОСТЯНОЙ ВОРОН (`bone_raven`)
// ============================================================================
function buildBoneRaven() {
  const b = new ModelBuilder({
    id: 'bone_raven',
    titleRu: 'Костяной ворон',
    biome: 'riftworld:morok_hollow',
    atlasSize: 512,
    renderScale: 0.5,
  });

  b.addGroup('root', [0, 0, 0]);
  b.addGroup('body', [0, 15.0, 1.0], [0, 0, 0], 'root', { axis: 'x', amp: 1.2, speed: 1.0, phase: 0 });

  // 1. Hollow Skeletal Avian Spine, Clavicle Collar, Pelvis & Prominent Keel Sternum!
  b.addSegmentBox('spine_front', {
    pStart: [0, 20.2, -3.8],
    pEnd: [0, 16.4, 2.0],
    width: 3,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 1.0,
    padEnd: 0.8,
    parent: 'body',
    mat: 'raven_bone',
  });
  b.addSegmentBox('spine_rear', {
    pStart: [0, 16.6, 1.8],
    pEnd: [0, 12.2, 7.2],
    width: 3,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 0.8,
    padEnd: 0.8,
    parent: 'body',
    mat: 'raven_bone',
  });
  b.addCenteredBox('clavicle_collar', {
    center: [0, 19.6, -3.6],
    size: [8, 3, 3],
    parent: 'body',
    mat: 'raven_bone',
  });
  b.addCenteredBox('pelvis_bone', {
    center: [0, 12.8, 3.0],
    size: [7, 3, 4],
    parent: 'body',
    mat: 'raven_bone',
  });
  b.addSegmentBox('sternum_front', {
    pStart: [0, 19.2, -4.4],
    pEnd: [0, 14.0, -5.0],
    width: 4,
    depth: 3,
    upHint: [0, 0, 1],
    padStart: 1.0,
    padEnd: 0.8,
    parent: 'body',
    mat: 'raven_bone',
  });
  b.addSegmentBox('sternum_keel', {
    pStart: [0, 14.2, -4.8],
    pEnd: [0, 10.6, 1.4],
    width: 3,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 1.0,
    padEnd: 0.8,
    parent: 'body',
    mat: 'raven_bone',
  });

  // 4 Curved L-Shaped Skeletal Ribs Per Side!
  b.addMirrored(() => {
    const ribs = [
      { idx: 1, sp: [1.4, 19.0, -2.0], mid: [4.4, 16.0, -2.2], st: [1.4, 14.2, -3.8] },
      { idx: 2, sp: [1.4, 18.0, -0.2], mid: [4.6, 14.8, -0.4], st: [1.4, 13.0, -2.2] },
      { idx: 3, sp: [1.4, 17.0,  1.6], mid: [4.4, 13.8,  1.4], st: [1.4, 12.0, -0.4] },
      { idx: 4, sp: [1.4, 15.8,  3.4], mid: [4.0, 13.0,  3.2], st: [1.4, 11.4,  1.2] },
    ];
    for (const rb of ribs) {
      b.addSegmentBox(`rib_up_l_${rb.idx}`, {
        pStart: rb.sp,
        pEnd: rb.mid,
        width: 1,
        depth: 1,
        upHint: [0, 0, 1],
        padStart: 1.0,
        padEnd: 0.8,
        parent: 'body',
        mat: 'raven_bone',
      });
      b.addSegmentBox(`rib_low_l_${rb.idx}`, {
        pStart: rb.mid,
        pEnd: rb.st,
        width: 1,
        depth: 1,
        upHint: [0, 1, 0],
        padStart: 0.8,
        padEnd: 1.0,
        parent: 'body',
        mat: 'raven_bone',
      });
    }
  });

  // 2. Cervical Vertebrae Neck & Elongated Corvid Skull with Smoldering Ember Eye Sockets + Recessed Nostril!
  b.addGroup('neck', [0, 20.2, -4.0], [0, 0, 0], 'body', { axis: 'x', amp: 1.8, speed: 1.0, phase: 0.3 });
  b.addSegmentBox('neck_column', {
    pStart: [0, 20.0, -4.0],
    pEnd: [0, 24.4, -7.2],
    width: 3,
    depth: 3,
    upHint: [0, 0, 1],
    padStart: 1.2,
    padEnd: 1.0,
    parent: 'neck',
    mat: 'raven_bone',
  });
  b.addCenteredBox('vert_ring_1', {
    center: [0, 21.6, -5.2],
    size: [4, 1, 4],
    rotation: [24, 0, 0],
    parent: 'neck',
    mat: 'raven_bone',
  });
  b.addCenteredBox('vert_ring_2', {
    center: [0, 23.2, -6.4],
    size: [4, 1, 4],
    rotation: [24, 0, 0],
    parent: 'neck',
    mat: 'raven_bone',
  });

  b.addGroup('head', [0, 24.4, -7.4], [0, 0, 0], 'neck', { axis: 'x', amp: -2.0, speed: 1.0, phase: 0.6 });
  b.addCenteredBox('skull_cranium', {
    center: [0, 25.4, -9.8],
    size: [7, 6, 7],
    rotation: [8, 0, 0],
    parent: 'head',
    mat: 'raven_bone',
  });

  // Solid Sculpted Corvid Upper Beak Wedge + Parted Lower Mandible (`orig_11_raven.png`!)
  b.addSegmentBox('beak_upper_base', {
    pStart: [0, 25.2, -12.6],
    pEnd: [0, 23.4, -17.0],
    width: 4,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 1.2,
    padEnd: 0.8,
    parent: 'head',
    mat: 'raven_beak',
  });
  b.addSegmentBox('beak_upper_tip', {
    pStart: [0, 23.5, -16.8],
    pEnd: [0, 21.8, -20.8],
    width: 3,
    depth: 2,
    upHint: [0, 1, 0],
    padStart: 0.8,
    padEnd: 0.3,
    parent: 'head',
    mat: 'raven_beak',
  });
  b.addSegmentBox('jaw_lower', {
    pStart: [0, 22.8, -12.4],
    pEnd: [0, 19.8, -19.2],
    width: 3,
    depth: 1,
    upHint: [0, 1, 0],
    padStart: 1.2,
    padEnd: 0.3,
    parent: 'head',
    mat: 'raven_bone',
  });

  b.addMirrored(() => {
    b.addCenteredBox('eye_socket_l', {
      center: [2.8, 25.4, -10.4],
      size: [2, 3, 4],
      rotation: [8, 0, 0],
      parent: 'head',
      mat: 'raven_eye_ember',
    });
  });

  // 3. Wide Arched Skeletal Wings: Clean Aged-Bone Frame Above + Thin `1x3` Ragged Soot-Black Feathers Mounted Underneath!
  b.addMirrored(() => {
    b.addGroup('wing_l', [4.0, 19.6, -3.6], [0, 0, 0], 'body', { axis: 'z', amp: 3.5, speed: 1.0, phase: 0.2 });

    b.addSegmentBox('wing_humerus_l', {
      pStart: [3.6, 19.8, -3.8],
      pEnd: [11.2, 20.4, -3.8],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.8,
      parent: 'wing_l',
      mat: 'raven_bone',
    });
    b.addSegmentBox('wing_ulna_l', {
      pStart: [11.0, 20.2, -3.8],
      pEnd: [14.8, 28.6, -5.2],
      width: 2,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 1.0,
      padEnd: 0.8,
      parent: 'wing_l',
      mat: 'raven_bone',
    });
    b.addCenteredBox('wing_carpal_l', {
      center: [14.8, 29.0, -5.4],
      size: [3, 3, 3],
      rotation: [12, -10, -24],
      parent: 'wing_l',
      mat: 'raven_bone',
    });

    b.addSegmentBox('wing_finger_l_1', {
      pStart: [14.8, 28.8, -5.2],
      pEnd: [28.8, 22.0, -4.2],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.3,
      parent: 'wing_l',
      mat: 'raven_bone',
    });
    b.addSegmentBox('wing_finger_l_2', {
      pStart: [14.8, 27.8, -4.6],
      pEnd: [25.6, 16.8, -1.0],
      width: 1,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.3,
      parent: 'wing_l',
      mat: 'raven_bone',
    });
    b.addSegmentBox('wing_finger_l_3', {
      pStart: [14.0, 26.2, -4.0],
      pEnd: [21.4, 14.8, 1.4],
      width: 1,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.3,
      parent: 'wing_l',
      mat: 'raven_bone',
    });

    // 12 Thin `width: 1` Ragged Charcoal-Black Feather Blades Mounted Underneath the Bone Frame (`padStart: 0.8` -> ZERO poke-through on bone top!)
    const raggedFeathers = [
      // Inner tattered secondaries under humerus
      { s: [ 5.6, 19.0, -3.0], e: [ 8.8, 10.6, 5.2], d: 2 },
      { s: [ 7.8, 19.2, -3.0], e: [12.4,  8.8, 6.8], d: 3 },
      { s: [10.0, 19.4, -3.2], e: [15.8, 10.4, 6.6], d: 2 },
      // Mid tattered secondaries under ulna
      { s: [12.0, 21.6, -3.4], e: [19.2,  9.6, 6.8], d: 3 },
      { s: [13.2, 24.0, -3.8], e: [22.6, 11.4, 5.8], d: 2 },
      { s: [14.2, 26.2, -4.2], e: [25.8, 10.2, 5.0], d: 3 },
      // Jagged alternating outer primaries under `wing_finger_l_1`!
      { s: [16.8, 26.8, -4.0], e: [29.6, 12.2, 3.6], d: 2 },
      { s: [19.2, 25.6, -3.8], e: [33.4, 12.8, 2.4], d: 3 },
      { s: [21.6, 24.4, -3.6], e: [35.4, 15.0, 1.0], d: 2 },
      { s: [24.0, 23.2, -3.5], e: [38.0, 15.8, -0.2], d: 2 },
      { s: [26.2, 22.2, -3.4], e: [36.2, 18.2, -1.4], d: 2 },
      { s: [27.8, 21.4, -3.4], e: [33.8, 19.8, -2.4], d: 2 },
    ];
    raggedFeathers.forEach((rf, idx) => {
      b.addSegmentBox(`wing_feather_l_${idx + 1}`, {
        pStart: rf.s,
        pEnd: rf.e,
        width: 1,
        depth: rf.d,
        upHint: [0, 1, 0],
        padStart: 0.9,
        padEnd: 0.2,
        parent: 'wing_l',
        mat: 'raven_feather_dark',
      });
    });
  });

  // 4. Ragged Black Tail Fan (`tail`)
  b.addGroup('tail', [0, 12.2, 7.0], [0, 0, 0], 'body', { axis: 'x', amp: 2.0, speed: 1.0, phase: 0.5 });
  b.addSegmentBox('tail_pygostyle', {
    pStart: [0, 12.4, 6.8],
    pEnd: [0, 10.6, 10.2],
    width: 2,
    depth: 2,
    upHint: [0, 1, 0],
    padStart: 1.0,
    padEnd: 0.6,
    parent: 'tail',
    mat: 'raven_bone',
  });
  b.addSegmentBox('tail_feather_center', {
    pStart: [0, 10.8, 9.4],
    pEnd: [0, 5.8, 18.8],
    width: 2,
    depth: 1,
    upHint: [0, 1, 0],
    padStart: 1.2,
    padEnd: 0.2,
    parent: 'tail',
    mat: 'raven_feather_dark',
  });
  b.addMirrored(() => {
    b.addSegmentBox('tail_feather_l_1', {
      pStart: [0.8, 11.0, 9.2],
      pEnd: [2.6, 6.2, 17.8],
      width: 2,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.2,
      parent: 'tail',
      mat: 'raven_feather_dark',
    });
    b.addSegmentBox('tail_feather_l_2', {
      pStart: [1.2, 11.2, 8.8],
      pEnd: [4.8, 7.2, 16.0],
      width: 2,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.2,
      parent: 'tail',
      mat: 'raven_feather_dark',
    });
  });

  // 5. Skeletal Bird Legs & 4-Toe Talons Resting Cleanly on Y = 0!
  b.addMirrored(() => {
    b.addGroup('leg_l', [3.0, 12.2, 3.0], [0, 0, 0], 'root');
    b.addSegmentBox('leg_l_femur', {
      pStart: [3.0, 12.4, 2.8],
      pEnd: [3.6, 7.0, 4.4],
      width: 2,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 1.0,
      padEnd: 0.8,
      parent: 'leg_l',
      mat: 'raven_bone',
    });
    b.addSegmentBox('leg_l_tarsus', {
      pStart: [3.6, 7.2, 4.2],
      pEnd: [3.6, 1.4, 0.4],
      width: 2,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.2,
      parent: 'leg_l',
      mat: 'raven_bone',
    });
    b.addCenteredBox('leg_l_ankle', {
      center: [3.6, 1.0, 0.2],
      size: [3, 2, 2],
      parent: 'leg_l',
      mat: 'raven_bone',
    });

    const toeYaws = [25, 0, -25];
    toeYaws.forEach((yaw, ti) => {
      const rad = (yaw * Math.PI) / 180;
      const tx = 3.6 - Math.sin(rad) * 3.0;
      const tz = 0.2 - Math.cos(rad) * 3.0;
      b.addCenteredBox(`leg_l_toe_${ti + 1}`, {
        center: [tx, 0.5, tz],
        size: [1, 1, 6],
        rotation: [0, yaw, 0],
        parent: 'leg_l',
        mat: 'raven_bone',
      });
    });
    b.addCenteredBox('leg_l_hallux', {
      center: [3.6, 0.5, 2.2],
      size: [1, 1, 3],
      parent: 'leg_l',
      mat: 'raven_bone',
    });
  });

  b.addAnimation('idle', {
    length: 3.6,
    loop: 'loop',
    snapping: 24,
    tracks: [
      { bone: 'body', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [1.5, 0, 0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'head', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [-2.5, 2.5, 0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'wing_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [1.5, -2.0, 4.0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'wing_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [1.5, 2.0, -4.0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'tail', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [2.5, 0, 0] }, { time: 3.6, value: [0, 0, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// 12 — КОРАЛЛОВЫЙ РАК-ОТШЕЛЬНИК (`coral_hermit_crab`)
// ============================================================================
function addCoralTuft(b, prefix, basePos, outDir, mat, parent) {
  const n = vNorm(outDir);
  const stemEnd = vAdd(basePos, vScale(n, 2.2));
  b.addSegmentBox(`${prefix}_stem`, {
    pStart: basePos,
    pEnd: stemEnd,
    width: 1,
    depth: 1,
    upHint: [0, 1, 0],
    padStart: 1.4,
    padEnd: 0.6,
    parent,
    mat,
  });

  const ref = Math.abs(n[1]) < 0.85 ? [0, 1, 0] : [1, 0, 0];
  const t1 = vNorm([
    n[1] * ref[2] - n[2] * ref[1],
    n[2] * ref[0] - n[0] * ref[2],
    n[0] * ref[1] - n[1] * ref[0],
  ]);
  const t2 = vNorm([
    n[1] * t1[2] - n[2] * t1[1],
    n[2] * t1[0] - n[0] * t1[2],
    n[0] * t1[1] - n[1] * t1[0],
  ]);

  const dirs = [
    vAdd(vScale(n, 0.72), vScale(t1, 0.69)),
    vAdd(vScale(n, 0.72), vAdd(vScale(t1, -0.45), vScale(t2, 0.52))),
    vAdd(vScale(n, 0.74), vAdd(vScale(t1, -0.35), vScale(t2, -0.58))),
  ];
  dirs.forEach((d, i) => {
    const tip = vAdd(stemEnd, vScale(vNorm(d), 2.4));
    b.addSegmentBox(`${prefix}_br_${i + 1}`, {
      pStart: stemEnd,
      pEnd: tip,
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.9,
      padEnd: 0.2,
      parent,
      mat,
    });
  });
}

function buildCoralHermitCrab() {
  const b = new ModelBuilder({
    id: 'coral_hermit_crab',
    titleRu: 'Коралловый рак-отшельник',
    biome: 'riftworld:shallow_reef',
    atlasSize: 512,
    renderScale: 0.5,
  });

  b.addGroup('root', [0, 0, 0]);
  b.addGroup('body', [0, 9.0, -5.5], [0, 0, 0], 'root', { axis: 'z', amp: 0.9, speed: 1.0, phase: 0 });

  b.addCenteredBox('carapace_main', {
    center: [0, 9.6, -6.2],
    size: [11, 8, 9],
    parent: 'body',
    mat: 'crab_carapace',
  });

  b.addMirrored(() => {
    b.addGroup('eyestalk_l', [2.6, 13.0, -9.2], [0, 0, 0], 'body', { axis: 'z', amp: 2.2, speed: 1.1, phase: 0.3 });
    b.addSegmentBox('eyestalk_shaft_l', {
      pStart: [2.6, 12.8, -9.2],
      pEnd: [2.8, 17.6, -10.2],
      width: 2,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 1.0,
      padEnd: 0.6,
      parent: 'eyestalk_l',
      mat: 'crab_orange',
    });
    b.addCenteredBox('crab_eye_l', {
      center: [2.8, 18.6, -10.4],
      size: [3, 3, 3],
      parent: 'eyestalk_l',
      mat: 'crab_eye',
    });
  });

  // Asymmetric Heterochelous Claws (`orig_12_crab.png`)!
  b.addGroup('claw_major_r', [-4.6, 8.8, -8.2], [0, 0, 0], 'body', { axis: 'y', amp: 2.5, speed: 1.0, phase: 0.2 });
  b.addSegmentBox('claw_major_arm', {
    pStart: [-4.4, 8.8, -8.0],
    pEnd: [-6.4, 7.6, -11.6],
    width: 3,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 1.2,
    padEnd: 1.0,
    parent: 'claw_major_r',
    mat: 'crab_pink',
  });
  b.addCenteredBox('claw_major_palm', {
    center: [-6.2, 6.2, -14.8],
    size: [6, 6, 9],
    rotation: [22, 16, -10],
    parent: 'claw_major_r',
    mat: 'crab_pink',
  });
  b.addCenteredBox('claw_major_thumb', {
    center: [-3.4, 6.6, -15.4],
    size: [2, 3, 5],
    rotation: [22, 16, -10],
    parent: 'claw_major_r',
    mat: 'crab_orange',
  });

  b.addGroup('claw_minor_l', [4.6, 9.0, -8.2], [0, 0, 0], 'body', { axis: 'y', amp: -2.5, speed: 1.0, phase: 0.5 });
  b.addSegmentBox('claw_minor_arm', {
    pStart: [4.4, 9.0, -8.0],
    pEnd: [9.2, 8.2, -11.4],
    width: 3,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 1.2,
    padEnd: 1.0,
    parent: 'claw_minor_l',
    mat: 'crab_pink',
  });
  b.addCenteredBox('claw_minor_palm', {
    center: [12.0, 7.2, -14.2],
    size: [5, 5, 9],
    rotation: [16, -28, 8],
    parent: 'claw_minor_l',
    mat: 'crab_pink',
  });
  b.addCenteredBox('claw_minor_pincer', {
    center: [9.4, 7.0, -14.0],
    size: [2, 2, 5],
    rotation: [16, -28, 8],
    parent: 'claw_minor_l',
    mat: 'crab_orange',
  });

  // Six 3-Segment Articulated Walking Legs on Y = 0!
  b.addMirrored(() => {
    const walkLegs = [
      { idx: 1, hip: [4.8, 8.2, -5.6], knee: [ 9.4, 6.6, -6.4], ank: [11.2, 2.4, -7.0] },
      { idx: 2, hip: [5.0, 8.2, -3.2], knee: [10.2, 6.6, -2.4], ank: [12.2, 2.4, -2.0] },
      { idx: 3, hip: [5.0, 8.4, -0.8], knee: [10.0, 6.8,  1.6], ank: [11.8, 2.4,  2.8] },
    ];
    for (const wl of walkLegs) {
      b.addGroup(`leg_l_${wl.idx}`, wl.hip, [0, 0, 0], 'root');
      b.addSegmentBox(`leg_l_${wl.idx}_merus`, {
        pStart: wl.hip,
        pEnd: wl.knee,
        width: 2,
        depth: 3,
        upHint: [0, 1, 0],
        padStart: 1.2,
        padEnd: 0.8,
        parent: `leg_l_${wl.idx}`,
        mat: 'crab_pink',
      });
      b.addSegmentBox(`leg_l_${wl.idx}_carpus`, {
        pStart: wl.knee,
        pEnd: wl.ank,
        width: 2,
        depth: 2,
        upHint: [0, 0, 1],
        padStart: 0.8,
        padEnd: 0.6,
        parent: `leg_l_${wl.idx}`,
        mat: 'crab_orange',
      });
      b.addCenteredBox(`leg_l_${wl.idx}_tip`, {
        center: [wl.ank[0], 1.5, wl.ank[2]],
        size: [1, 3, 2],
        parent: `leg_l_${wl.idx}`,
        mat: 'crab_orange_tip',
      });
    }
  });

  // 4-Tier Stair-Stepped Cream-Ivory Spiral Conch Shell (`rotation: [24, 0, 0]` showing crisp stepped spiral terraces!)
  b.addGroup('shell', [0, 11.0, -1.5], [0, 0, 0], 'body', { axis: 'x', amp: 1.0, speed: 1.0, phase: 0.4 });
  const shellTiers = [
    { name: 'shell_tier_1', c: [0, 12.8,  0.4], sz: [16, 12, 10] },
    { name: 'shell_tier_2', c: [0, 16.8,  5.2], sz: [13, 11,  9] },
    { name: 'shell_tier_3', c: [0, 20.4,  9.6], sz: [ 9,  9,  7] },
    { name: 'shell_tier_4', c: [0, 23.4, 13.0], sz: [ 5,  6,  5] },
  ];
  for (const st of shellTiers) {
    b.addCenteredBox(st.name, {
      center: st.c,
      size: st.sz,
      rotation: [24, 0, 0],
      parent: 'shell',
      mat: 'crab_shell_cream',
    });
  }

  // Vibrant Branching Reef Corals + Sea-Green Tube Anemone!
  const coralColonies = [
    { name: 'coral_pink_1', pos: [-3.6, 16.8, -3.6], dir: [-0.30, 0.78, -0.55], mat: 'crab_coral_pink' },
    { name: 'coral_pink_2', pos: [ 3.4, 16.8, -3.6], dir: [ 0.32, 0.78, -0.55], mat: 'crab_coral_pink' },
    { name: 'coral_mint_1', pos: [-2.2, 20.8,  1.2], dir: [-0.22, 0.88, -0.42], mat: 'crab_coral_mint' },
    { name: 'coral_mint_2', pos: [ 3.8, 20.2,  1.4], dir: [ 0.52, 0.78, -0.35], mat: 'crab_coral_mint' },
    { name: 'coral_lav_1',  pos: [-6.2, 16.8,  3.8], dir: [-0.84, 0.46, -0.28], mat: 'crab_coral_lavender' },
    { name: 'coral_mint_3', pos: [-5.6, 19.6,  6.8], dir: [-0.78, 0.58,  0.22], mat: 'crab_coral_mint' },
    { name: 'coral_pink_3', pos: [-5.2, 16.2,  8.6], dir: [-0.84, 0.35,  0.40], mat: 'crab_coral_pink' },
    { name: 'coral_pink_4', pos: [ 1.6, 23.8,  6.6], dir: [ 0.25, 0.90, -0.35], mat: 'crab_coral_pink' },
    { name: 'coral_lav_2',  pos: [-1.4, 25.4, 10.6], dir: [-0.28, 0.92,  0.28], mat: 'crab_coral_lavender' },
    { name: 'coral_lav_3',  pos: [-3.8, 21.8, 11.2], dir: [-0.72, 0.55,  0.42], mat: 'crab_coral_lavender' },
    { name: 'coral_lav_4',  pos: [ 6.2, 16.6,  3.6], dir: [ 0.85, 0.45, -0.25], mat: 'crab_coral_lavender' },
    { name: 'coral_pink_5', pos: [ 5.0, 18.4,  7.8], dir: [ 0.82, 0.48,  0.30], mat: 'crab_coral_pink' },
    { name: 'coral_pink_6', pos: [-4.4,  9.8,  7.2], dir: [-0.65, -0.22, 0.72], mat: 'crab_coral_pink' },
  ];
  for (const cc of coralColonies) {
    addCoralTuft(b, cc.name, cc.pos, cc.dir, cc.mat, 'shell');
  }

  // Fleshy Sea-Green Tube Anemone on Foreground `-X` Shell Flank!
  const anemCenter = [-8.0, 11.6, 2.2];
  const anemNormal = vNorm([-0.92, 0.20, -0.34]);
  const anemU = vNorm([-0.34, 0.08, 0.94]);
  b.addPlate('anemone_column', {
    center: anemCenter,
    width: 5,
    thickness: 4,
    length: 5,
    uAxis: anemU,
    nAxis: anemNormal,
    parent: 'shell',
    mat: 'crab_anemone_body',
  });

  const anemV = vNorm([
    anemU[1] * anemNormal[2] - anemU[2] * anemNormal[1],
    anemU[2] * anemNormal[0] - anemU[0] * anemNormal[2],
    anemU[0] * anemNormal[1] - anemU[1] * anemNormal[0],
  ]);
  const rimCenter = vAdd(anemCenter, vScale(anemNormal, 1.4));
  for (let i = 0; i < 8; i++) {
    const ang = (i * Math.PI * 2) / 8;
    const radVec = vAdd(vScale(anemU, Math.cos(ang)), vScale(anemV, Math.sin(ang)));
    const tStart = vAdd(rimCenter, vScale(radVec, 1.5));
    const tDir = vNorm(vAdd(vScale(anemNormal, 0.48), vScale(radVec, 0.88)));
    const tEnd = vAdd(tStart, vScale(tDir, 2.3));
    b.addSegmentBox(`anemone_tent_${i + 1}`, {
      pStart: tStart,
      pEnd: tEnd,
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 1.0,
      padEnd: 0.2,
      parent: 'shell',
      mat: 'crab_anemone_tentacle',
    });
  }

  b.addAnimation('idle', {
    length: 3.6,
    loop: 'loop',
    snapping: 24,
    tracks: [
      { bone: 'body', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [1.0, 0, 0.9] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'eyestalk_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [2.5, 0, 2.5] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'eyestalk_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [2.5, 0, -2.5] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'claw_major_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [-2.0, 3.0, 0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'claw_minor_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [-2.0, -3.5, 0] }, { time: 3.6, value: [0, 0, 0] }] },
      { bone: 'shell', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 1.8, value: [-1.2, 0, -0.8] }, { time: 3.6, value: [0, 0, 0] }] },
    ],
  });

  b.asymmetric = [
    'claw_major_r', 'claw_minor_l',
    ...b.cubes
      .map((c) => c.name)
      .filter((nm) => nm.startsWith('claw_') || nm.startsWith('coral_') || nm.startsWith('anemone_')),
  ];

  return b;
}

// ============================================================================
// MAIN PIPELINE
// ============================================================================
async function main() {
  const builders = [
    buildFacetedOwl(),
    buildBoneRaven(),
    buildCoralHermitCrab(),
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

  const reportDir = path.join(repoRoot, 'reports/blockbench_mcp/batch_04');
  fs.mkdirSync(reportDir, { recursive: true });
  fs.writeFileSync(
    path.join(reportDir, 'validation_report.json'),
    JSON.stringify({ generated_via: 'blockbench-mcp-plugin (official MCP server)', models: validationSummary }, null, 2) + '\n'
  );
  console.log('\n[done] All 3 Batch 04 V2 models built & validated via Blockbench MCP!');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
