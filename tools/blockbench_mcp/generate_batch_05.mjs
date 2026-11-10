#!/usr/bin/env node
/**
 * Batch 05 Generator (V5 Masterpiece) via Official Blockbench MCP (`jasonjgardner/blockbench-mcp-plugin`):
 *   13 — Рифовый морской конёк (`reef_seahorse` — без пузырьков, рельефные панцирные щитки)
 *   14 — Глубоководный кальмар-призрак (`ghost_squid` — без контурных рамок, 3-сегментные щупальца)
 *   15 — Серебряный морской змей (`silver_sea_serpent` — детализированный морской дракон-левиафан, ~92 элемента)
 */

import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { ModelBuilder, BlockbenchMcpSession, vSub, vNorm, vAdd, vScale } from './builder_lib.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '../..');

// ============================================================================
// MODEL 13: REEF SEAHORSE (`reef_seahorse` — Рифовый морской конёк)
// Biome: riftworld:shallow_reef — ZERO BUBBLES!
// ============================================================================
function buildReefSeahorse() {
  const b = new ModelBuilder({
    id: 'reef_seahorse',
    titleRu: 'Рифовый морской конёк',
    biome: 'riftworld:shallow_reef',
    atlasSize: 512,
    renderScale: 0.90,
    translucent: true,
  });
  b.freeElements = [];
  b.asymmetric = [];

  b.addGroup('root', [0, 14, 0], [0, 0, 0], null, {
    wave: 'cos', axis: 'y', speed: 0.08, amp: 0.05, phase: 0,
  });
  b.addGroup('trunk', [0, 15, 0], [0, 0, 0], 'root', {
    wave: 'sin', axis: 'x', speed: 0.10, amp: 0.05, phase: 0,
  });
  b.addGroup('dorsal_fin', [0, 16, 3.5], [0, 0, 0], 'trunk', {
    wave: 'sin', axis: 'y', speed: 0.45, amp: 0.24, phase: 0,
  });
  b.addGroup('head', [0, 22.5, -0.5], [0, 0, 0], 'trunk', {
    wave: 'cos', axis: 'x', speed: 0.10, amp: 0.06, phase: 0.4,
  });
  b.addGroup('tail_curl', [0, 10.5, 2.5], [0, 0, 0], 'root', {
    wave: 'sin', axis: 'x', speed: 0.10, amp: 0.08, phase: 1.0,
  });

  // --- 1. PUFFED TRUNK (`trunk`) ---
  b.addCenteredBox('trunk_lower_core', {
    center: [0, 11.8, 0.8],
    size: [5, 4, 5],
    parent: 'trunk',
    mat: 'seahorse_trunk_flank',
  });
  b.addCenteredBox('trunk_mid_core', {
    center: [0, 16.2, 0.2],
    size: [6, 7, 6],
    parent: 'trunk',
    mat: 'seahorse_trunk_flank',
  });
  b.addCenteredBox('trunk_upper_core', {
    center: [0, 21.0, 0.2],
    size: [5, 4, 5],
    parent: 'trunk',
    mat: 'seahorse_trunk_flank',
  });

  // Stepped Seafoam-Teal Dorsal Spine Plates + Sculpted Angled Ridge Spines along Back (`+Z`)
  b.addCenteredBox('dorsal_plate_low', {
    center: [0, 12.0, 3.0],
    size: [4, 4, 2],
    parent: 'trunk',
    mat: 'seahorse_teal',
  });
  b.addCenteredBox('dorsal_plate_mid', {
    center: [0, 16.5, 3.0],
    size: [5, 6, 2],
    parent: 'trunk',
    mat: 'seahorse_teal',
  });
  b.addCenteredBox('dorsal_plate_upper', {
    center: [0, 21.2, 2.5],
    size: [4, 5, 2],
    parent: 'trunk',
    mat: 'seahorse_teal',
  });
  b.addCenteredBox('dorsal_ridge_nape', {
    center: [0, 21.6, 3.3],
    size: [2, 3, 2],
    rotation: [-18, 0, 0],
    parent: 'trunk',
    mat: 'seahorse_teal',
  });
  b.addCenteredBox('dorsal_ridge_waist', {
    center: [0, 11.6, 3.8],
    size: [2, 3, 2],
    rotation: [16, 0, 0],
    parent: 'trunk',
    mat: 'seahorse_teal',
  });

  // Puffed Pastel-Pink & Cream Segmented Belly Scutes (`-Z`)
  b.addCenteredBox('belly_plate_lower', {
    center: [0, 12.2, -2.0],
    size: [4, 3, 2],
    parent: 'trunk',
    mat: 'seahorse_belly',
  });
  b.addCenteredBox('belly_plate_main', {
    center: [0, 16.5, -3.0],
    size: [4, 6, 2],
    parent: 'trunk',
    mat: 'seahorse_belly',
  });
  b.addCenteredBox('belly_plate_throat', {
    center: [0, 20.8, -2.2],
    size: [3, 4, 2],
    parent: 'trunk',
    mat: 'seahorse_belly',
  });

  // Prominent Scalloped Fan Dorsal Fin (`dorsal_fin`)
  b.addCenteredBox('dorsal_fin_stem', {
    center: [0, 16.0, 4.5],
    size: [1, 6, 3],
    parent: 'dorsal_fin',
    mat: 'seahorse_fin',
  });
  b.addCenteredBox('dorsal_fin_fan_mid', {
    center: [0, 16.2, 6.5],
    size: [1, 10, 3],
    parent: 'dorsal_fin',
    mat: 'seahorse_fin',
  });
  b.addCenteredBox('dorsal_fin_fan_outer', {
    center: [0, 16.2, 8.5],
    size: [1, 8, 3],
    parent: 'dorsal_fin',
    mat: 'seahorse_fin',
  });
  b.addCenteredBox('dorsal_fin_fan_tip', {
    center: [0, 16.2, 10.2],
    size: [1, 5, 2],
    parent: 'dorsal_fin',
    mat: 'seahorse_fin',
  });

  // Paired Mid-Trunk Lateral Fins
  b.addMirrored(() => {
    b.addGroup('trunk_fin_l', [-2.8, 16.0, 1.5], [0, 0, 0], 'trunk', {
      wave: 'sin', axis: 'y', speed: 0.42, amp: 0.22, phase: 0,
    });
    b.addPlate('trunk_fin_l_blade', {
      center: [-3.4, 15.8, 2.8],
      width: 4,
      thickness: 1,
      length: 4,
      uAxis: [-0.42, 0.0, 0.91],
      nAxis: [-0.91, 0.0, -0.42],
      parent: 'trunk_fin_l',
      mat: 'seahorse_fin',
    });
  });

  // --- 2. EQUINE HEAD, FLUSH `3x3` TUBULAR SNOUT, TURRET EYES & CORAL ANTLERS ---
  b.addCenteredBox('head_cranium', {
    center: [0, 25.0, 0.0],
    size: [6, 5, 5],
    parent: 'head',
    mat: 'seahorse_teal',
  });
  b.addCenteredBox('head_crown_cap', {
    center: [0, 27.8, 0.0],
    size: [4, 2, 3],
    parent: 'head',
    mat: 'seahorse_teal',
  });
  b.addCenteredBox('crown_coral_nub', {
    center: [0, 29.2, 0.0],
    size: [2, 2, 2],
    parent: 'head',
    mat: 'seahorse_coral',
  });

  // Flush `3x3` Tubular Seafoam Snout + Coral-Peach Tip
  b.addSegmentBox('snout_tube', {
    pStart: [0, 24.5, -2.0],
    pEnd: [0, 22.3, -6.6],
    width: 3,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 1.0,
    padEnd: 0.1,
    parent: 'head',
    mat: 'seahorse_teal',
  });
  b.addSegmentBox('snout_coral_tip', {
    pStart: [0, 22.3, -6.6],
    pEnd: [0, 21.3, -8.7],
    width: 3,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 0.1,
    padEnd: 0.4,
    parent: 'head',
    mat: 'seahorse_snout_tip',
  });

  // Mirrored Turret Eyes, Cheek Pectoral Fins, and Branching Coral Antlers
  b.addMirrored(() => {
    b.addCenteredBox('eye_turret_l', {
      center: [-2.8, 25.0, -0.8],
      size: [2, 3, 2],
      parent: 'head',
      mat: 'seahorse_eye',
    });

    b.addGroup('cheek_fin_l', [-2.8, 24.5, 2.0], [0, 0, 0], 'head', {
      wave: 'cos', axis: 'y', speed: 0.48, amp: 0.22, phase: 0,
    });
    b.addPlate('cheek_fin_l_blade', {
      center: [-3.4, 24.3, 3.2],
      width: 4,
      thickness: 1,
      length: 4,
      uAxis: [-0.48, -0.10, 0.87],
      nAxis: [-0.87, 0.0, -0.49],
      parent: 'cheek_fin_l',
      mat: 'seahorse_fin',
    });

    b.addSegmentBox('antler_upper_l_main', {
      pStart: [-1.1, 28.0, 0.2],
      pEnd: [-2.3, 33.2, 2.4],
      width: 1,
      depth: 1,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.4,
      parent: 'head',
      mat: 'seahorse_antler',
    });
    b.addSegmentBox('antler_upper_l_tine', {
      pStart: [-1.6, 30.2, 1.1],
      pEnd: [-2.7, 33.0, -0.4],
      width: 1,
      depth: 1,
      upHint: [0, 0, 1],
      padStart: 0.7,
      padEnd: 0.3,
      parent: 'head',
      mat: 'seahorse_antler',
    });
    b.addSegmentBox('antler_rear_l_main', {
      pStart: [-1.4, 26.8, 1.8],
      pEnd: [-2.9, 29.8, 5.4],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.4,
      parent: 'head',
      mat: 'seahorse_antler',
    });
    b.addSegmentBox('antler_rear_l_tine', {
      pStart: [-2.1, 28.2, 3.4],
      pEnd: [-3.3, 30.8, 3.6],
      width: 1,
      depth: 1,
      upHint: [0, 0, 1],
      padStart: 0.7,
      padEnd: 0.3,
      parent: 'head',
      mat: 'seahorse_antler',
    });
  });

  // --- 3. PREHENSILE FORWARD-CURLING SPIRAL TAIL (`tail_curl`, resting on `Y = 0`) ---
  b.addCenteredBox('tail_stem_outer', {
    center: [0, 6.5, 4.5],
    size: [4, 9, 2],
    parent: 'tail_curl',
    mat: 'seahorse_teal',
  });
  b.addCenteredBox('tail_stem_inner_coral', {
    center: [0, 6.8, 2.5],
    size: [3, 8, 2],
    parent: 'tail_curl',
    mat: 'seahorse_coral',
  });
  b.addCenteredBox('tail_base_bottom_teal', {
    center: [0, 1.0, 0.0],
    size: [4, 2, 11],
    parent: 'tail_curl',
    mat: 'seahorse_teal',
  });
  b.addCenteredBox('tail_base_inner_coral', {
    center: [0, 2.5, -0.2],
    size: [3, 2, 8],
    parent: 'tail_curl',
    mat: 'seahorse_coral',
  });
  b.addCenteredBox('tail_front_wall_teal', {
    center: [0, 5.5, -4.5],
    size: [3, 7, 2],
    parent: 'tail_curl',
    mat: 'seahorse_teal',
  });
  b.addCenteredBox('tail_front_inner_coral', {
    center: [0, 5.5, -3.0],
    size: [2, 5, 1],
    parent: 'tail_curl',
    mat: 'seahorse_coral',
  });
  b.addCenteredBox('tail_top_arch_teal', {
    center: [0, 8.0, -1.8],
    size: [3, 2, 5],
    parent: 'tail_curl',
    mat: 'seahorse_teal',
  });
  b.addCenteredBox('tail_spiral_tip_coral', {
    center: [0, 6.0, -0.2],
    size: [2, 3, 2],
    parent: 'tail_curl',
    mat: 'seahorse_coral',
  });
  b.addCenteredBox('tail_spiral_core_nub', {
    center: [0, 5.2, -1.5],
    size: [2, 2, 1],
    parent: 'tail_curl',
    mat: 'seahorse_coral',
  });

  // Blockbench Keyframe Animations
  b.addAnimation('idle', {
    length: 4.0,
    loop: 'loop',
    tracks: [
      { bone: 'root', channel: 'position', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [0, 0.7, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'dorsal_fin', channel: 'rotation', keys: [{ time: 0.0, value: [0, -14, 0] }, { time: 0.5, value: [0, 14, 0] }, { time: 1.0, value: [0, -14, 0] }, { time: 1.5, value: [0, 14, 0] }, { time: 2.0, value: [0, -14, 0] }, { time: 2.5, value: [0, 14, 0] }, { time: 3.0, value: [0, -14, 0] }, { time: 3.5, value: [0, 14, 0] }, { time: 4.0, value: [0, -14, 0] }] },
      { bone: 'head', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [3.5, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'tail_curl', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [4.5, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
    ],
  });

  b.addAnimation('swim', {
    length: 1.6,
    loop: 'loop',
    tracks: [
      { bone: 'trunk', channel: 'rotation', keys: [{ time: 0.0, value: [4, 0, 0] }, { time: 0.8, value: [8, 0, 0] }, { time: 1.6, value: [4, 0, 0] }] },
      { bone: 'dorsal_fin', channel: 'rotation', keys: [{ time: 0.0, value: [0, -18, 0] }, { time: 0.4, value: [0, 18, 0] }, { time: 0.8, value: [0, -18, 0] }, { time: 1.2, value: [0, 18, 0] }, { time: 1.6, value: [0, -18, 0] }] },
      { bone: 'tail_curl', channel: 'rotation', keys: [{ time: 0.0, value: [-4, 0, 0] }, { time: 0.8, value: [5, 0, 0] }, { time: 1.6, value: [-4, 0, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// MODEL 14: GHOST SQUID (`ghost_squid` — Глубоководный кальмар-призрак)
// Biome: riftworld:abyss — ZERO CONTOUR OUTLINES + 3-SEGMENT FLOWING TENTACLES!
// ============================================================================
function buildGhostSquid() {
  const b = new ModelBuilder({
    id: 'ghost_squid',
    titleRu: 'Глубоководный кальмар-призрак',
    biome: 'riftworld:abyss',
    atlasSize: 512,
    renderScale: 0.96,
    translucent: true,
    asymmetric: [],
  });

  b.addGroup('mantle_root', [0, 17, -4], [0, 0, 0], null, {
    wave: 'sin', axis: 'x', speed: 0.09, amp: 0.05, phase: 0,
  });
  b.addGroup('head_collar', [0, 15.0, 2.0], [0, 0, 0], 'mantle_root', null);
  b.addGroup('arms_upper', [0, 14.8, 7.0], [0, 0, 0], 'head_collar', {
    wave: 'sin', axis: 'x', speed: 0.14, amp: 0.10, phase: 0.6,
  });
  b.addGroup('arms_lower', [0, 11.8, 7.0], [0, 0, 0], 'head_collar', {
    wave: 'sin', axis: 'x', speed: 0.14, amp: -0.10, phase: 0.6,
  });

  // --- 1. SMOOTH TAPERED TORPEDO MANTLE + MAIN MANTLE BOX ---
  b.addSegmentBox('mantle_main_box', {
    pStart: [0, 20.8, -11.2],
    pEnd: [0, 16.0, 2.0],
    width: 10,
    depth: 10,
    upHint: [0, 1, 0],
    padStart: 0.8,
    padEnd: 0.8,
    parent: 'mantle_root',
    mat: 'squid_mantle',
  });

  b.addSegmentBox('mantle_cone_core', {
    pStart: [0, 25.4, -23.8],
    pEnd: [0, 20.5, -10.5],
    width: 4,
    depth: 4,
    upHint: [0, 1, 0],
    padStart: 0.4,
    padEnd: 1.2,
    parent: 'mantle_root',
    mat: 'squid_mantle',
  });
  b.addSegmentBox('mantle_cone_dorsal', {
    pStart: [0, 26.0, -23.2],
    pEnd: [0, 23.6, -10.8],
    width: 6,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 0.4,
    padEnd: 1.2,
    parent: 'mantle_root',
    mat: 'squid_mantle',
  });
  b.addSegmentBox('mantle_cone_ventral', {
    pStart: [0, 24.6, -23.2],
    pEnd: [0, 18.2, -10.8],
    width: 6,
    depth: 3,
    upHint: [0, 1, 0],
    padStart: 0.4,
    padEnd: 1.2,
    parent: 'mantle_root',
    mat: 'squid_mantle',
  });

  b.addMirrored(() => {
    b.addSegmentBox('mantle_cone_flank_l', {
      pStart: [-1.4, 25.3, -23.2],
      pEnd: [-3.6, 20.9, -10.8],
      width: 3,
      depth: 6,
      upHint: [0, 1, 0],
      padStart: 0.4,
      padEnd: 1.2,
      parent: 'mantle_root',
      mat: 'squid_mantle',
    });

    b.addGroup('fin_l', [-4.5, 22.5, -16.0], [0, 0, 0], 'mantle_root', {
      wave: 'sin', axis: 'z', speed: 0.24, amp: 0.22, phase: 0,
    });
    b.addPlate('fin_l_wing', {
      center: [-5.2, 23.4, -16.8],
      width: 7,
      thickness: 1,
      length: 10,
      uAxis: [-0.86, 0.16, 0.48],
      nAxis: [0.18, 0.98, 0.06],
      parent: 'fin_l',
      mat: 'squid_fin',
    });
  });

  // --- 2. RECESSED HEAD COLLAR & LARGE LUMINOUS LAVENDER EYES ---
  b.addSegmentBox('head_box', {
    pStart: [0, 16.2, 1.5],
    pEnd: [0, 13.8, 8.0],
    width: 8,
    depth: 8,
    upHint: [0, 1, 0],
    padStart: 1.2,
    padEnd: 0.6,
    parent: 'head_collar',
    mat: 'squid_head',
  });

  b.addMirrored(() => {
    b.addSegmentBox('squid_eye_l', {
      pStart: [-3.6, 15.6, 2.8],
      pEnd: [-3.6, 14.3, 6.4],
      width: 2,
      depth: 5,
      upHint: [0, 1, 0],
      padStart: 0.4,
      padEnd: 0.4,
      parent: 'head_collar',
      mat: 'squid_eye',
    });
  });

  // --- 3. GRACEFUL 3-SEGMENT CURVING ARMS + EXTRA-LONG CLUBBED TENTACLES ---
  b.addMirrored(() => {
    // Dorsal-inner curving 3-segment arm
    b.addSegmentBox('arm_dorsal_l_1', {
      pStart: [-1.3, 15.8, 7.2],
      pEnd: [-2.1, 14.8, 14.2],
      width: 2, depth: 2, upHint: [0, 1, 0], padStart: 1.2, padEnd: 0.8,
      parent: 'arms_upper', mat: 'squid_tentacle',
    });
    b.addSegmentBox('arm_dorsal_l_2', {
      pStart: [-2.1, 14.8, 14.2],
      pEnd: [-3.2, 13.8, 21.4],
      width: 2, depth: 2, upHint: [0, 1, 0], padStart: 0.8, padEnd: 0.8,
      parent: 'arms_upper', mat: 'squid_tentacle',
    });
    b.addSegmentBox('arm_dorsal_l_3', {
      pStart: [-3.2, 13.8, 21.4],
      pEnd: [-4.0, 13.2, 28.0],
      width: 1, depth: 1, upHint: [0, 1, 0], padStart: 0.8, padEnd: 0.4,
      parent: 'arms_upper', mat: 'squid_tentacle',
    });

    // Upper-lateral wide curving 3-segment arm
    b.addSegmentBox('arm_upper_lat_l_1', {
      pStart: [-2.5, 14.8, 7.2],
      pEnd: [-4.6, 12.8, 14.0],
      width: 2, depth: 2, upHint: [0, 1, 0], padStart: 1.2, padEnd: 0.8,
      parent: 'arms_upper', mat: 'squid_tentacle',
    });
    b.addSegmentBox('arm_upper_lat_l_2', {
      pStart: [-4.6, 12.8, 14.0],
      pEnd: [-6.8, 10.8, 20.8],
      width: 2, depth: 2, upHint: [0, 1, 0], padStart: 0.8, padEnd: 0.8,
      parent: 'arms_upper', mat: 'squid_tentacle',
    });
    b.addSegmentBox('arm_upper_lat_l_3', {
      pStart: [-6.8, 10.8, 20.8],
      pEnd: [-8.4, 9.6, 27.0],
      width: 1, depth: 1, upHint: [0, 1, 0], padStart: 0.8, padEnd: 0.4,
      parent: 'arms_upper', mat: 'squid_tentacle',
    });

    // Lower-lateral sweeping 3-segment arm
    b.addSegmentBox('arm_lower_lat_l_1', {
      pStart: [-2.4, 12.4, 7.2],
      pEnd: [-4.2, 8.2, 13.8],
      width: 2, depth: 2, upHint: [0, 1, 0], padStart: 1.2, padEnd: 0.8,
      parent: 'arms_lower', mat: 'squid_tentacle',
    });
    b.addSegmentBox('arm_lower_lat_l_2', {
      pStart: [-4.2, 8.2, 13.8],
      pEnd: [-5.8, 4.2, 19.8],
      width: 2, depth: 2, upHint: [0, 1, 0], padStart: 0.8, padEnd: 0.8,
      parent: 'arms_lower', mat: 'squid_tentacle',
    });
    b.addSegmentBox('arm_lower_lat_l_3', {
      pStart: [-5.8, 4.2, 19.8],
      pEnd: [-6.8, 1.4, 25.2],
      width: 1, depth: 1, upHint: [0, 1, 0], padStart: 0.8, padEnd: 0.3,
      parent: 'arms_lower', mat: 'squid_tentacle',
    });

    // Ventral-inner deepest arm touching `Y = 0` cleanly
    b.addSegmentBox('arm_ventral_l_1', {
      pStart: [-1.2, 11.4, 7.2],
      pEnd: [-1.8, 6.8, 13.4],
      width: 2, depth: 2, upHint: [0, 1, 0], padStart: 1.2, padEnd: 0.8,
      parent: 'arms_lower', mat: 'squid_tentacle',
    });
    b.addSegmentBox('arm_ventral_l_2', {
      pStart: [-1.8, 6.8, 13.4],
      pEnd: [-2.5, 2.4, 19.2],
      width: 2, depth: 2, upHint: [0, 1, 0], padStart: 0.8, padEnd: 0.8,
      parent: 'arms_lower', mat: 'squid_tentacle',
    });
    b.addSegmentBox('arm_ventral_l_3', {
      pStart: [-2.5, 2.4, 19.2],
      pEnd: [-3.0, 0.8, 24.0],
      width: 2, depth: 2, upHint: [0, 1, 0], padStart: 0.8, padEnd: 0.0,
      parent: 'arms_lower', mat: 'squid_tentacle',
    });

    // Extra-Long Feeding Tentacle with Bioluminescent Club
    b.addGroup('tentacle_long_l', [-2.4, 13.0, 7.0], [0, 0, 0], 'head_collar', {
      wave: 'cos', axis: 'y', speed: 0.14, amp: 0.12, phase: 1.1,
    });
    b.addSegmentBox('tentacle_l_stalk_1', {
      pStart: [-2.2, 13.4, 7.2],
      pEnd: [-3.8, 9.8, 16.8],
      width: 2, depth: 2, upHint: [0, 1, 0], padStart: 1.2, padEnd: 0.8,
      parent: 'tentacle_long_l', mat: 'squid_tentacle',
    });
    b.addSegmentBox('tentacle_l_stalk_2', {
      pStart: [-3.8, 9.8, 16.8],
      pEnd: [-5.2, 6.4, 25.8],
      width: 2, depth: 2, upHint: [0, 1, 0], padStart: 0.8, padEnd: 0.8,
      parent: 'tentacle_long_l', mat: 'squid_tentacle',
    });
    b.addSegmentBox('tentacle_l_club', {
      pStart: [-5.1, 6.5, 25.4],
      pEnd: [-6.2, 4.6, 31.2],
      width: 3, depth: 2, upHint: [0, 1, 0], padStart: 0.6, padEnd: 0.4,
      parent: 'tentacle_long_l', mat: 'squid_club',
    });
  });

  // Blockbench Keyframe Animations
  b.addAnimation('idle', {
    length: 4.0,
    loop: 'loop',
    tracks: [
      { bone: 'mantle_root', channel: 'position', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [0, 0.7, -0.5] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'fin_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, -11] }, { time: 1.0, value: [0, 0, 11] }, { time: 2.0, value: [0, 0, -11] }, { time: 3.0, value: [0, 0, 11] }, { time: 4.0, value: [0, 0, -11] }] },
      { bone: 'fin_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 11] }, { time: 1.0, value: [0, 0, -11] }, { time: 2.0, value: [0, 0, 11] }, { time: 3.0, value: [0, 0, -11] }, { time: 4.0, value: [0, 0, 11] }] },
      { bone: 'arms_upper', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [5, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'arms_lower', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [-5, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
    ],
  });

  b.addAnimation('swim', {
    length: 2.0,
    loop: 'loop',
    tracks: [
      { bone: 'mantle_root', channel: 'position', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 0.8, value: [0, 0.8, -1.4] }, { time: 2.0, value: [0, 0, 0] }] },
      { bone: 'arms_upper', channel: 'rotation', keys: [{ time: 0.0, value: [6, 0, 0] }, { time: 0.8, value: [-5, 0, 0] }, { time: 2.0, value: [6, 0, 0] }] },
      { bone: 'tentacle_long_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, -8, 0] }, { time: 0.8, value: [0, 6, 0] }, { time: 2.0, value: [0, -8, 0] }] },
      { bone: 'tentacle_long_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 8, 0] }, { time: 0.8, value: [0, -6, 0] }, { time: 2.0, value: [0, 8, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// MODEL 15: SILVER SEA SERPENT / LEVIATHAN (`silver_sea_serpent` — Серебряный морской змей)
// Biome: riftworld:maja_ocean — HIGH-DETAIL HYTALE LEVIATHAN SEA DRAGON!
// ============================================================================
function buildSilverSeaSerpent() {
  const b = new ModelBuilder({
    id: 'silver_sea_serpent',
    titleRu: 'Серебряный морской змей',
    biome: 'riftworld:maja_ocean',
    atlasSize: 512,
    renderScale: 1.05,
    translucent: true,
    asymmetric: [],
  });

  // Bone Hierarchy for Traveling Leviathan S-Wave + Hinged Jaw + Fore/Hind Flippers
  b.addGroup('serpent_root', [0, 5.2, -5.0], [0, 0, 0], null, null);
  b.addGroup('neck', [0, 6.2, -7.2], [0, 0, 0], 'serpent_root', {
    wave: 'sin', axis: 'x', speed: 0.10, amp: 0.06, phase: 0,
  });
  b.addGroup('head', [0, 19.2, -14.2], [0, 0, 0], 'neck', {
    wave: 'sin', axis: 'x', speed: 0.10, amp: -0.05, phase: 0,
  });
  b.addGroup('jaw', [0, 17.2, -17.5], [0, 0, 0], 'head', {
    wave: 'cos', axis: 'x', speed: 0.14, amp: 0.07, phase: 0.5,
  });
  b.addGroup('body_arch', [0, 6.2, 0.4], [0, 0, 0], 'serpent_root', {
    wave: 'sin', axis: 'x', speed: 0.12, amp: 0.08, phase: 0.9,
  });
  b.addGroup('tail_dip', [0, 15.2, 17.0], [0, 0, 0], 'body_arch', {
    wave: 'sin', axis: 'x', speed: 0.12, amp: -0.10, phase: 1.8,
  });
  b.addGroup('tail_tip', [0, 8.2, 23.2], [0, 0, 0], 'tail_dip', {
    wave: 'sin', axis: 'x', speed: 0.12, amp: 0.14, phase: 2.7,
  });

  // =========================================================================
  // 1. REALISTIC SCULPTED SEA-DRAGON HEAD, OPEN FANGED MAW, HORNS & GILL-FRILLS
  // =========================================================================
  // Armored Cranium & Occipital Crown
  b.addCenteredBox('head_cranium', {
    center: [0, 19.6, -16.8],
    size: [7, 4, 7],
    parent: 'head',
    mat: 'serpent_head',
  });
  b.addCenteredBox('head_occiput_crest', {
    center: [0, 21.4, -15.0],
    size: [5, 2, 4],
    rotation: [14, 0, 0],
    parent: 'head',
    mat: 'serpent_dorsal_scute',
  });

  // Sloped Forehead-to-Snout Nasal Bridge Crest + Tapered Upper Maxilla
  b.addSegmentBox('head_nasal_bridge', {
    pStart: [0, 21.2, -17.8],
    pEnd: [0, 19.6, -24.8],
    width: 3,
    depth: 2,
    upHint: [0, 1, 0],
    padStart: 0.6,
    padEnd: 0.4,
    parent: 'head',
    mat: 'serpent_dorsal_scute',
  });
  b.addCenteredBox('snout_maxilla', {
    center: [0, 18.7, -22.5],
    size: [5, 3, 6],
    parent: 'head',
    mat: 'serpent_snout',
  });
  b.addCenteredBox('snout_rostrum_tip', {
    center: [0, 18.8, -25.8],
    size: [3, 2, 2],
    parent: 'head',
    mat: 'serpent_snout',
  });
  b.addCenteredBox('mouth_palate_upper', {
    center: [0, 17.5, -21.2],
    size: [4, 1, 6],
    parent: 'head',
    mat: 'serpent_mouth',
  });

  // Articulated Hinged Lower Jaw (`jaw` bone — slightly open draconic roar!)
  b.addSegmentBox('jaw_mandible', {
    pStart: [0, 17.0, -17.2],
    pEnd: [0, 15.6, -24.6],
    width: 4,
    depth: 2,
    upHint: [0, 1, 0],
    padStart: 0.6,
    padEnd: 0.4,
    parent: 'jaw',
    mat: 'serpent_head',
  });
  b.addSegmentBox('jaw_chin_keel', {
    pStart: [0, 16.0, -18.0],
    pEnd: [0, 14.9, -23.2],
    width: 2,
    depth: 1,
    upHint: [0, 1, 0],
    padStart: 0.4,
    padEnd: 0.3,
    parent: 'jaw',
    mat: 'serpent_dorsal_scute',
  });

  // Mirrored Facial Armor, Brow Ridges, Slit Eyes, Fangs, 2-Seg Horns, 3-Ray Gill Frills & Barbels
  b.addMirrored(() => {
    // Sculpted Cheekbone / Jowl Armor (tucked behind eye so full glowing dragon eye shines!)
    b.addCenteredBox('cheek_armor_l', {
      center: [-3.3, 18.0, -15.6],
      size: [2, 2, 4],
      rotation: [0, -8, 0],
      parent: 'head',
      mat: 'serpent_head',
    });

    // Raised Nostril Boss on Snout
    b.addCenteredBox('nostril_boss_l', {
      center: [-1.4, 20.0, -24.2],
      size: [2, 1, 2],
      parent: 'head',
      mat: 'serpent_snout',
    });

    // Fierce Angled Supraorbital Brow Ridge casting real 3D shadow over Eye
    b.addSegmentBox('brow_ridge_l', {
      pStart: [-2.4, 20.6, -20.0],
      pEnd: [-3.6, 21.4, -15.6],
      width: 2,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.4,
      padEnd: 0.4,
      parent: 'head',
      mat: 'serpent_brow',
    });

    // Prominent Glowing Cyan-Aqua Dragon Eye with Vertical Slit Pupil
    b.addCenteredBox('serpent_eye_l', {
      center: [-3.0, 19.4, -18.4],
      size: [2, 2, 3],
      parent: 'head',
      mat: 'serpent_eye',
    });

    // Sharp Gleaming Ivory-Silver Upper & Lower Fangs (`parent: head` & `parent: jaw`)
    b.addCenteredBox('fang_upper_front_l', {
      center: [-1.7, 17.0, -24.0],
      size: [1, 2, 1],
      parent: 'head',
      mat: 'serpent_fang',
    });
    b.addCenteredBox('fang_upper_mid_l', {
      center: [-1.8, 17.1, -21.5],
      size: [1, 2, 1],
      parent: 'head',
      mat: 'serpent_fang',
    });
    b.addCenteredBox('fang_lower_front_l', {
      center: [-1.3, 16.4, -23.4],
      size: [1, 2, 1],
      parent: 'jaw',
      mat: 'serpent_fang',
    });

    // Swept 2-Segment Curved Frost-Silver Dragon Horns (`horn_base` -> `horn_tip`)
    b.addSegmentBox('horn_l_base', {
      pStart: [-2.4, 21.0, -16.2],
      pEnd: [-3.8, 23.8, -12.0],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.6,
      parent: 'head',
      mat: 'serpent_horn',
    });
    b.addSegmentBox('horn_l_tip', {
      pStart: [-3.8, 23.8, -12.0],
      pEnd: [-4.9, 26.8, -7.2],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.4,
      parent: 'head',
      mat: 'serpent_horn',
    });

    // 3-Rayed Aquatic Sea-Dragon Gill-Frill Fan behind Cheek (`gill_web` + 3 fin spines!)
    b.addPlate('gill_web_l', {
      center: [-4.6, 18.8, -12.8],
      width: 5,
      thickness: 1,
      length: 4,
      uAxis: [-0.46, 0.16, 0.87],
      nAxis: [-0.88, 0.0, -0.47],
      parent: 'head',
      mat: 'serpent_gill_web',
    });
    b.addSegmentBox('gill_ray_upper_l', {
      pStart: [-3.4, 19.6, -14.8],
      pEnd: [-6.0, 21.4, -10.0],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.6,
      padEnd: 0.3,
      parent: 'head',
      mat: 'serpent_fin_ray',
    });
    b.addSegmentBox('gill_ray_mid_l', {
      pStart: [-3.5, 18.6, -14.8],
      pEnd: [-6.4, 18.9, -9.8],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.6,
      padEnd: 0.3,
      parent: 'head',
      mat: 'serpent_fin_ray',
    });
    b.addSegmentBox('gill_ray_lower_l', {
      pStart: [-3.3, 17.6, -14.8],
      pEnd: [-5.6, 16.6, -10.4],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.6,
      padEnd: 0.3,
      parent: 'head',
      mat: 'serpent_fin_ray',
    });

    // Flowing 2-Segment Sinuous Sensory Barbels trailing from Upper Snout
    b.addSegmentBox('barbel_l_base', {
      pStart: [-1.9, 17.8, -23.2],
      pEnd: [-2.8, 14.4, -21.2],
      width: 1,
      depth: 1,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.6,
      parent: 'head',
      mat: 'serpent_horn',
    });
    b.addSegmentBox('barbel_l_tip', {
      pStart: [-2.8, 14.4, -21.2],
      pEnd: [-3.5, 11.4, -18.2],
      width: 1,
      depth: 1,
      upHint: [0, 0, 1],
      padStart: 0.6,
      padEnd: 0.3,
      parent: 'head',
      mat: 'serpent_horn',
    });
  });

  // =========================================================================
  // 2. MUSCULAR 16-SEGMENT SINUOUS S-WAVE SPINE + ARMORED SCUTES + SAIL FIN
  // =========================================================================
  // Smooth monotonically-increasing-Z S-curve (`dz > 0` on ALL 16 segments -> zero twist, zero kinks!):
  const stations = [
    { p: [0, 19.0, -14.2], w: 6, d: 5, group: 'neck', finH: 4, ray: true },      // 0 -> 1
    { p: [0, 16.6, -12.0], w: 6, d: 6, group: 'neck', finH: 4, ray: true },      // 1 -> 2
    { p: [0, 12.6, -10.4], w: 6, d: 6, group: 'neck', finH: 4, ray: true },      // 2 -> 3
    { p: [0, 8.4, -8.8],   w: 7, d: 6, group: 'neck', finH: 3, ray: false },     // 3 -> 4
    { p: [0, 5.4, -6.6],   w: 7, d: 7, group: 'serpent_root', finH: 3, ray: true },  // 4 -> 5 (pectoral chest)
    { p: [0, 4.4, -3.0],   w: 7, d: 7, group: 'serpent_root', finH: 3, ray: false }, // 5 -> 6 (sternum trough)
    { p: [0, 6.2, 0.6],    w: 7, d: 6, group: 'body_arch', finH: 4, ray: true },     // 6 -> 7 (climb start)
    { p: [0, 10.2, 3.8],   w: 6, d: 6, group: 'body_arch', finH: 5, ray: true },     // 7 -> 8 (mid climb)
    { p: [0, 14.8, 6.8],   w: 6, d: 6, group: 'body_arch', finH: 5, ray: true },     // 8 -> 9 (upper climb)
    { p: [0, 18.0, 10.0],  w: 6, d: 6, group: 'body_arch', finH: 5, ray: true },     // 9 -> 10 (arch crown 1)
    { p: [0, 18.2, 13.6],  w: 5, d: 5, group: 'body_arch', finH: 5, ray: true },     // 10 -> 11 (arch crown 2)
    { p: [0, 15.2, 17.0],  w: 5, d: 5, group: 'tail_dip', finH: 4, ray: true },      // 11 -> 12 (descent 1)
    { p: [0, 11.0, 20.0],  w: 5, d: 5, group: 'tail_dip', finH: 3, ray: false },     // 12 -> 13 (descent 2)
    { p: [0, 8.2, 23.4],   w: 4, d: 4, group: 'tail_tip', finH: 3, ray: true },      // 13 -> 14 (pelvic dip)
    { p: [0, 9.0, 27.0],   w: 4, d: 4, group: 'tail_tip', finH: 4, ray: true },      // 14 -> 15 (caudal rise 1)
    { p: [0, 12.2, 30.4],  w: 3, d: 3, group: 'tail_tip', finH: 4, ray: true },      // 15 -> 16 (caudal rise 2)
    { p: [0, 16.2, 33.6],  w: 2, d: 2, group: 'tail_tip', finH: 0, ray: false },     // 16 (tail tip)
  ];

  for (let i = 0; i < stations.length - 1; i++) {
    const s0 = stations[i];
    const s1 = stations[i + 1];
    const prevGroup = i > 0 ? stations[i - 1].group : 'head';
    const nextGroup = i + 2 < stations.length ? stations[i + 1].group : s0.group;

    const tang = vNorm(vSub(s1.p, s0.p)); // (0, ty, tz) with tz > 0 always!
    const dorsalN = [0, tang[2], -tang[1]]; // exact outward dorsal normal in YZ plane!

    // Keep cross-group joint pads tight (`0.55`) so `gateInterpenetration` is 0 warnings,
    // and intra-group pads generous (`1.15`) so smooth curves have zero gaps!
    const padS = s0.group !== prevGroup ? 0.55 : 1.15;
    const padE = s0.group !== nextGroup ? 0.55 : 1.15;

    const idxStr = String(i + 1).padStart(2, '0');
    b.addSegmentBox(`spine_seg_${idxStr}`, {
      pStart: s0.p,
      pEnd: s1.p,
      width: s0.w,
      depth: s0.d,
      upHint: dorsalN,
      padStart: padS,
      padEnd: padE,
      parent: s0.group,
      mat: 'serpent_body',
    });

    // Raised Frost-Silver & Deep-Teal Armored Dorsal Spine Scute on each segment
    const midP = vScale(vAdd(s0.p, s1.p), 0.5);
    const scuteBase = vAdd(midP, vScale(dorsalN, s0.d * 0.45));
    const scuteW = Math.max(2, s0.w - 3);
    b.addPlate(`dorsal_scute_${idxStr}`, {
      center: scuteBase,
      width: scuteW,
      thickness: 1,
      length: 3,
      uAxis: [1, 0, 0],
      nAxis: dorsalN,
      parent: s0.group,
      mat: 'serpent_dorsal_scute',
    });

    // Continuous Translucent Seafoam Dorsal Sail Fin Membrane + Angled Fin Spine Rays
    if (s0.finH > 0) {
      const finOffset = s0.d * 0.5 + s0.finH * 0.42 - 0.6;
      const finS = vAdd(s0.p, vScale(dorsalN, finOffset));
      const finE = vAdd(s1.p, vScale(dorsalN, finOffset));
      b.addSegmentBox(`dorsal_sail_${idxStr}`, {
        pStart: finS,
        pEnd: finE,
        width: 1,
        depth: s0.finH,
        upHint: dorsalN,
        padStart: s0.group !== prevGroup ? 0.2 : 0.7,
        padEnd: s0.group !== nextGroup ? 0.2 : 0.7,
        parent: s0.group,
        mat: 'serpent_fin',
      });

      if (s0.ray) {
        const rayStart = vAdd(midP, vScale(dorsalN, s0.d * 0.38));
        const rayDir = vNorm(vAdd(vScale(dorsalN, 0.84), vScale(tang, 0.54)));
        const rayEnd = vAdd(rayStart, vScale(rayDir, s0.finH + 1.8));
        b.addSegmentBox(`dorsal_ray_${idxStr}`, {
          pStart: rayStart,
          pEnd: rayEnd,
          width: 1,
          depth: 1,
          upHint: [1, 0, 0],
          padStart: 0.4,
          padEnd: 0.3,
          parent: s0.group,
          mat: 'serpent_fin_ray',
        });
      }
    }
  }

  // =========================================================================
  // 3. WEBBED MULTI-RAYED FORE-FLIPPERS (`Y = 0` GROUND CONTACT) & HIND-FLIPPERS
  // =========================================================================
  b.addMirrored(() => {
    // Large Pectoral Fore-Flipper (`flipper_l` attached to `serpent_root` at chest station)
    b.addGroup('flipper_l', [-3.4, 4.5, -4.4], [0, 0, 0], 'serpent_root', {
      wave: 'sin', axis: 'z', speed: 0.16, amp: 0.22, phase: 0,
    });
    b.addCenteredBox('flipper_l_shoulder', {
      center: [-4.0, 4.2, -4.4],
      size: [3, 3, 4],
      parent: 'flipper_l',
      mat: 'serpent_flipper',
    });
    b.addSegmentBox('flipper_l_arm', {
      pStart: [-4.5, 4.0, -4.6],
      pEnd: [-8.6, 1.4, -1.8],
      width: 3,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.6,
      parent: 'flipper_l',
      mat: 'serpent_flipper',
    });
    // Translucent Aquamarine Flipper Webbing Plate + 3 Splayed Fin Rays (touching `Y = 0`!)
    b.addPlate('flipper_l_web', {
      center: [-8.8, 1.2, -0.4],
      width: 6,
      thickness: 1,
      length: 6,
      uAxis: [-0.80, -0.22, 0.56],
      nAxis: [-0.18, 0.97, 0.14],
      parent: 'flipper_l',
      mat: 'serpent_fin',
    });
    b.addSegmentBox('flipper_l_ray_1', {
      pStart: [-7.2, 2.0, -2.8],
      pEnd: [-12.2, 0.8, 0.2],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.3,
      parent: 'flipper_l',
      mat: 'serpent_fin_ray',
    });
    b.addSegmentBox('flipper_l_ray_2', {
      pStart: [-6.8, 1.8, -2.0],
      pEnd: [-10.8, 0.5, 2.2],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.0,
      parent: 'flipper_l',
      mat: 'serpent_fin_ray',
    });
    b.addSegmentBox('flipper_l_ray_3', {
      pStart: [-6.0, 1.8, -1.4],
      pEnd: [-8.6, 0.8, 3.4],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.2,
      parent: 'flipper_l',
      mat: 'serpent_fin_ray',
    });

    // Sleek Pelvic Hind-Flipper (`hind_flipper_l` tucked under `tail_dip` at Station 12-13)
    b.addGroup('hind_flipper_l', [-2.4, 9.6, 20.8], [0, 0, 0], 'tail_dip', {
      wave: 'sin', axis: 'z', speed: 0.16, amp: 0.18, phase: 1.4,
    });
    b.addSegmentBox('hind_flipper_l_arm', {
      pStart: [-2.2, 9.6, 20.6],
      pEnd: [-5.4, 7.2, 23.6],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.6,
      padEnd: 0.5,
      parent: 'hind_flipper_l',
      mat: 'serpent_flipper',
    });
    b.addPlate('hind_flipper_l_web', {
      center: [-5.8, 6.8, 24.8],
      width: 4,
      thickness: 1,
      length: 5,
      uAxis: [-0.74, -0.32, 0.59],
      nAxis: [-0.24, 0.94, 0.22],
      parent: 'hind_flipper_l',
      mat: 'serpent_fin',
    });
    b.addSegmentBox('hind_flipper_l_ray', {
      pStart: [-4.6, 7.8, 22.8],
      pEnd: [-8.2, 5.8, 26.8],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.6,
      padEnd: 0.3,
      parent: 'hind_flipper_l',
      mat: 'serpent_fin_ray',
    });
  });

  // =========================================================================
  // 4. FLARED 3-LOBED SEA-DRAGON CAUDAL TAIL FAN (`tail_tip`)
  // =========================================================================
  b.addSegmentBox('tail_fan_spine_center', {
    pStart: [0, 15.6, 33.0],
    pEnd: [0, 20.8, 38.4],
    width: 1,
    depth: 2,
    upHint: [0, 0.72, -0.69],
    padStart: 0.8,
    padEnd: 0.4,
    parent: 'tail_tip',
    mat: 'serpent_fin_ray',
  });
  b.addSegmentBox('tail_fan_lobe_upper', {
    pStart: [0, 16.2, 32.8],
    pEnd: [0, 22.8, 37.2],
    width: 1,
    depth: 3,
    upHint: [0, 0.62, -0.78],
    padStart: 0.8,
    padEnd: 0.4,
    parent: 'tail_tip',
    mat: 'serpent_fin',
  });
  b.addSegmentBox('tail_fan_lobe_lower', {
    pStart: [0, 15.2, 33.2],
    pEnd: [0, 18.2, 39.2],
    width: 1,
    depth: 3,
    upHint: [0, 0.85, -0.52],
    padStart: 0.8,
    padEnd: 0.4,
    parent: 'tail_tip',
    mat: 'serpent_fin',
  });

  // Blockbench Keyframe Animations
  b.addAnimation('idle', {
    length: 4.0,
    loop: 'loop',
    tracks: [
      { bone: 'neck', channel: 'rotation', keys: [{ time: 0.0, value: [-3, 0, 0] }, { time: 2.0, value: [3, 0, 0] }, { time: 4.0, value: [-3, 0, 0] }] },
      { bone: 'head', channel: 'rotation', keys: [{ time: 0.0, value: [3, 0, 0] }, { time: 2.0, value: [-3, 0, 0] }, { time: 4.0, value: [3, 0, 0] }] },
      { bone: 'jaw', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [5, 0, 0] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'flipper_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [0, 0, -8] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'flipper_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 0] }, { time: 2.0, value: [0, 0, 8] }, { time: 4.0, value: [0, 0, 0] }] },
      { bone: 'body_arch', channel: 'rotation', keys: [{ time: 0.0, value: [3, 0, 0] }, { time: 2.0, value: [-3, 0, 0] }, { time: 4.0, value: [3, 0, 0] }] },
      { bone: 'tail_tip', channel: 'rotation', keys: [{ time: 0.0, value: [-6, 0, 0] }, { time: 2.0, value: [6, 0, 0] }, { time: 4.0, value: [-6, 0, 0] }] },
    ],
  });

  b.addAnimation('swim', {
    length: 2.0,
    loop: 'loop',
    tracks: [
      { bone: 'neck', channel: 'rotation', keys: [{ time: 0.0, value: [-5, 0, 0] }, { time: 1.0, value: [5, 0, 0] }, { time: 2.0, value: [-5, 0, 0] }] },
      { bone: 'jaw', channel: 'rotation', keys: [{ time: 0.0, value: [2, 0, 0] }, { time: 1.0, value: [7, 0, 0] }, { time: 2.0, value: [2, 0, 0] }] },
      { bone: 'body_arch', channel: 'rotation', keys: [{ time: 0.0, value: [6, 0, 0] }, { time: 1.0, value: [-6, 0, 0] }, { time: 2.0, value: [6, 0, 0] }] },
      { bone: 'tail_dip', channel: 'rotation', keys: [{ time: 0.0, value: [-7, 0, 0] }, { time: 1.0, value: [7, 0, 0] }, { time: 2.0, value: [-7, 0, 0] }] },
      { bone: 'tail_tip', channel: 'rotation', keys: [{ time: 0.0, value: [10, 0, 0] }, { time: 1.0, value: [-10, 0, 0] }, { time: 2.0, value: [10, 0, 0] }] },
    ],
  });

  return b;
}

// ============================================================================
// MAIN PIPELINE
// ============================================================================
async function main() {
  const builders = [
    buildReefSeahorse(),
    buildGhostSquid(),
    buildSilverSeaSerpent(),
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

      if (gSum.errors > 0 || gSum.warnings > 0) {
        console.error('GEOMETRY VALIDATION DETAILS:', JSON.stringify(mcpRes.geometry_validation.gates, null, 2));
        if (gSum.errors > 0) throw new Error(`Geometry validation failed on ${builder.id}`);
      }
      if (aSum.errors > 0 || aSum.warnings > 0) {
        console.error('ANIMATION VALIDATION DETAILS:', JSON.stringify(mcpRes.animation_validation.gates, null, 2));
        if (aSum.errors > 0) throw new Error(`Animation validation failed on ${builder.id}`);
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

  const reportDir = path.join(repoRoot, 'reports/blockbench_mcp/batch_05');
  fs.mkdirSync(reportDir, { recursive: true });
  fs.writeFileSync(
    path.join(reportDir, 'validation_report.json'),
    JSON.stringify({ generated_via: 'blockbench-mcp-plugin (official MCP server)', models: validationSummary }, null, 2) + '\n'
  );
  console.log('\n[done] All 3 Batch 05 V5 models built & validated via Blockbench MCP!');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
