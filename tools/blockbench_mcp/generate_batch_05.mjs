#!/usr/bin/env node
/**
 * Batch 05 Generator (V2 Masterpiece) via Official Blockbench MCP (`jasonjgardner/blockbench-mcp-plugin`):
 *   13 — Рифовый морской конёк (`reef_seahorse`)
 *   14 — Глубоководный кальмар-призрак (`ghost_squid`)
 *   15 — Серебряный морской змей (`silver_sea_serpent`)
 */

import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { ModelBuilder, BlockbenchMcpSession } from './builder_lib.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '../..');

// ============================================================================
// MODEL 13: REEF SEAHORSE (`reef_seahorse` — Рифовый морской конёк)
// Biome: riftworld:shallow_reef
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
  b.freeElements = ['bubble_mote_1', 'bubble_mote_2', 'bubble_mote_3'];
  b.asymmetric = ['bubble_mote_1', 'bubble_mote_2', 'bubble_mote_3'];

  // Bone Hierarchy + Riftworld Runtime Animations
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

  // Stepped Seafoam-Teal Dorsal Spine Plates along Back (`+Z`)
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

  // Prominent Scalloped Fan Dorsal Fin (`dorsal_fin`) (`orig_13_seahorse.png`!)
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

  // Flush `3x3` Tubular Seafoam Snout + Coral-Peach Tip (zero coplanar Z-seam!)
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

    // Branching Coral-Orange & Pastel-Pink Crown Antlers (2 pairs + forks!)
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
  // Wider open spiral window (`Z = -5.5 .. +5.5`, `Y = 0 .. 10.5`) matching `orig_13_seahorse.png`!
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

  // --- 4. THREE FLOATING WATER BUBBLES IN FRONT OF SNOUT (`freeElements`) ---
  b.addCenteredBox('bubble_mote_1', {
    center: [-0.2, 23.5, -10.2],
    size: [1, 1, 1],
    parent: 'head',
    mat: 'seahorse_bubble',
  });
  b.addCenteredBox('bubble_mote_2', {
    center: [0.3, 26.5, -10.8],
    size: [2, 2, 2],
    parent: 'head',
    mat: 'seahorse_bubble',
  });
  b.addCenteredBox('bubble_mote_3', {
    center: [-0.4, 30.5, -9.8],
    size: [3, 3, 3],
    parent: 'head',
    mat: 'seahorse_bubble',
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
// Biome: riftworld:abyss
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

  // Smooth 4-Sided Converging Torpedo Cone (ZERO blunt telescope steps!)
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

  // Mirrored Cone Flank Panels & Translucent Lateral Stabilizer Fins
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

  // --- 2. RECESSED HEAD COLLAR & LARGE GLOWING PALE-LAVENDER LATERAL EYES ---
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

  // --- 3. EIGHT LONG STREAMING ARMS + TWO EXTRA-LONG CLUBBED TENTACLES (`orig_14_squid.png`!) ---
  b.addMirrored(() => {
    // Upper 2 pairs (`arms_upper`)
    b.addSegmentBox('arm_dorsal_l_base', {
      pStart: [-1.3, 15.8, 7.2],
      pEnd: [-2.2, 13.8, 17.5],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.8,
      parent: 'arms_upper',
      mat: 'squid_tentacle',
    });
    b.addSegmentBox('arm_dorsal_l_tip', {
      pStart: [-2.2, 13.8, 17.5],
      pEnd: [-3.4, 12.2, 27.8],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.4,
      parent: 'arms_upper',
      mat: 'squid_tentacle',
    });

    b.addSegmentBox('arm_upper_lat_l_base', {
      pStart: [-2.5, 14.8, 7.2],
      pEnd: [-5.2, 12.0, 17.2],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.8,
      parent: 'arms_upper',
      mat: 'squid_tentacle',
    });
    b.addSegmentBox('arm_upper_lat_l_tip', {
      pStart: [-5.2, 12.0, 17.2],
      pEnd: [-7.8, 9.8, 26.8],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.4,
      parent: 'arms_upper',
      mat: 'squid_tentacle',
    });

    // Lower 2 pairs (`arms_lower`) — deepest ventral tip touches `Y = 0` cleanly!
    b.addSegmentBox('arm_lower_lat_l_base', {
      pStart: [-2.4, 12.4, 7.2],
      pEnd: [-4.6, 7.0, 16.5],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.8,
      parent: 'arms_lower',
      mat: 'squid_tentacle',
    });
    b.addSegmentBox('arm_lower_lat_l_tip', {
      pStart: [-4.6, 7.0, 16.5],
      pEnd: [-6.4, 2.2, 24.8],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.4,
      parent: 'arms_lower',
      mat: 'squid_tentacle',
    });

    b.addSegmentBox('arm_ventral_l_base', {
      pStart: [-1.2, 11.4, 7.2],
      pEnd: [-2.0, 5.4, 15.8],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.8,
      parent: 'arms_lower',
      mat: 'squid_tentacle',
    });
    b.addSegmentBox('arm_ventral_l_tip', {
      pStart: [-2.0, 5.4, 15.8],
      pEnd: [-2.8, 0.9, 23.2],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.0,
      parent: 'arms_lower',
      mat: 'squid_tentacle',
    });

    // Extra-Long Feeding Tentacle with Enlarged Bioluminescent Club (`tentacle_long_l`)
    b.addGroup('tentacle_long_l', [-2.4, 13.0, 7.0], [0, 0, 0], 'head_collar', {
      wave: 'cos', axis: 'y', speed: 0.14, amp: 0.12, phase: 1.1,
    });
    b.addSegmentBox('tentacle_l_stalk_1', {
      pStart: [-2.2, 13.4, 7.2],
      pEnd: [-4.0, 9.6, 17.8],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.8,
      parent: 'tentacle_long_l',
      mat: 'squid_tentacle',
    });
    b.addSegmentBox('tentacle_l_stalk_2', {
      pStart: [-4.0, 9.6, 17.8],
      pEnd: [-5.4, 6.2, 26.2],
      width: 2,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.8,
      parent: 'tentacle_long_l',
      mat: 'squid_tentacle',
    });
    b.addSegmentBox('tentacle_l_club', {
      pStart: [-5.3, 6.4, 25.8],
      pEnd: [-6.4, 4.4, 31.2],
      width: 3,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.6,
      padEnd: 0.4,
      parent: 'tentacle_long_l',
      mat: 'squid_club',
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
      { bone: 'arms_upper', channel: 'rotation', keys: [{ time: 0.0, value: [-4, 0, 0] }, { time: 2.0, value: [5, 0, 0] }, { time: 4.0, value: [-4, 0, 0] }] },
      { bone: 'arms_lower', channel: 'rotation', keys: [{ time: 0.0, value: [3, 0, 0] }, { time: 2.0, value: [-2, 0, 0] }, { time: 4.0, value: [3, 0, 0] }] },
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
// MODEL 15: SILVER SEA SERPENT (`silver_sea_serpent` — Серебряный морской змей)
// Biome: riftworld:maja_ocean
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

  b.addGroup('serpent_root', [0, 5, -7], [0, 0, 0], null, null);
  b.addGroup('neck', [0, 6.0, -9.0], [0, 0, 0], 'serpent_root', {
    wave: 'sin', axis: 'x', speed: 0.10, amp: 0.06, phase: 0,
  });
  b.addGroup('head', [0, 18.0, -13.5], [0, 0, 0], 'neck', {
    wave: 'sin', axis: 'x', speed: 0.10, amp: -0.06, phase: 0,
  });
  b.addGroup('body_arch', [0, 5.0, -3.5], [0, 0, 0], 'serpent_root', {
    wave: 'sin', axis: 'x', speed: 0.12, amp: 0.08, phase: 0.9,
  });
  b.addGroup('tail_dip', [0, 18.0, 12.5], [0, 0, 0], 'body_arch', {
    wave: 'sin', axis: 'x', speed: 0.12, amp: -0.10, phase: 1.8,
  });
  b.addGroup('tail_tip', [0, 7.5, 22.0], [0, 0, 0], 'tail_dip', {
    wave: 'sin', axis: 'x', speed: 0.12, amp: 0.14, phase: 2.7,
  });

  // --- 1. SCULPTED DRACONIC HEAD, SWEPT SILVER HORNS, CHEEK FINS & CHIN BARBELS ---
  b.addCenteredBox('head_skull', {
    center: [0, 18.5, -16.0],
    size: [7, 5, 7],
    parent: 'head',
    mat: 'serpent_head',
  });
  b.addCenteredBox('snout_upper', {
    center: [0, 18.0, -22.0],
    size: [5, 3, 6],
    parent: 'head',
    mat: 'serpent_snout',
  });
  b.addCenteredBox('jaw_lower', {
    center: [0, 15.8, -20.8],
    size: [4, 2, 7],
    parent: 'head',
    mat: 'serpent_head',
  });

  b.addMirrored(() => {
    b.addCenteredBox('serpent_eye_l', {
      center: [-2.8, 18.5, -17.0],
      size: [2, 2, 3],
      parent: 'head',
      mat: 'serpent_eye',
    });
    b.addSegmentBox('brow_horn_l', {
      pStart: [-2.2, 20.2, -16.8],
      pEnd: [-3.5, 24.4, -10.8],
      width: 1,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.4,
      parent: 'head',
      mat: 'serpent_horn',
    });
    b.addSegmentBox('cheek_whisker_l', {
      pStart: [-3.0, 17.8, -14.8],
      pEnd: [-4.8, 19.4, -9.8],
      width: 1,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.4,
      parent: 'head',
      mat: 'serpent_horn',
    });
    b.addSegmentBox('chin_barbel_l', {
      pStart: [-1.4, 15.4, -22.2],
      pEnd: [-1.9, 11.4, -20.5],
      width: 1,
      depth: 1,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.3,
      parent: 'head',
      mat: 'serpent_horn',
    });
  });

  // --- 2. NECK ARCH (`head` -> `pectoral trough`) + TALL SCALLOPED NECK CREST ---
  // `pStart` = TOP, `pEnd` = BOTTOM + `upHint` along dorsal normal so `south` = dorsal ridge & `north` = ribbed belly!
  b.addSegmentBox('neck_upper_arch', {
    pStart: [0, 18.5, -13.8],
    pEnd: [0, 14.5, -10.5],
    width: 6,
    depth: 6,
    upHint: [0, 0.75, 0.66],
    padStart: 1.4,
    padEnd: 1.2,
    parent: 'neck',
    mat: 'serpent_body',
  });
  b.addSegmentBox('neck_column', {
    pStart: [0, 14.5, -10.5],
    pEnd: [0, 6.2, -9.2],
    width: 6,
    depth: 6,
    upHint: [0, 0.16, 0.99],
    padStart: 1.4,
    padEnd: 1.4,
    parent: 'neck',
    mat: 'serpent_body',
  });

  // Tall Scalloped Translucent Seafoam Neck Fin Crest (`orig_15_serpent.png`!)
  b.addSegmentBox('fin_crest_nape_1', {
    pStart: [0, 21.8, -14.2],
    pEnd: [0, 17.8, -8.8],
    width: 1,
    depth: 4,
    upHint: [0, 0.80, 0.60],
    padStart: 0.8,
    padEnd: 0.8,
    parent: 'neck',
    mat: 'serpent_fin',
  });
  b.addSegmentBox('fin_crest_nape_2', {
    pStart: [0, 18.2, -9.2],
    pEnd: [0, 12.2, -6.4],
    width: 1,
    depth: 4,
    upHint: [0, 0.35, 0.94],
    padStart: 0.8,
    padEnd: 0.8,
    parent: 'neck',
    mat: 'serpent_fin',
  });
  b.addSegmentBox('fin_crest_neck_low', {
    pStart: [0, 12.5, -6.8],
    pEnd: [0, 6.8, -5.2],
    width: 1,
    depth: 4,
    upHint: [0, 0.25, 0.97],
    padStart: 0.8,
    padEnd: 0.8,
    parent: 'neck',
    mat: 'serpent_fin',
  });

  // --- 3. PECTORAL TROUGH & SPLAYED PADDLE FLIPPERS (`Y = 0` ground touch!) ---
  b.addSegmentBox('body_trough_1', {
    pStart: [0, 6.2, -9.2],
    pEnd: [0, 4.2, -4.0],
    width: 6,
    depth: 6,
    upHint: [0, 1, 0],
    padStart: 1.5,
    padEnd: 1.5,
    parent: 'serpent_root',
    mat: 'serpent_body',
  });

  b.addMirrored(() => {
    b.addGroup('flipper_l', [-3.0, 4.0, -5.5], [0, 0, 0], 'serpent_root', {
      wave: 'sin', axis: 'z', speed: 0.16, amp: 0.22, phase: 0,
    });
    b.addSegmentBox('flipper_l_arm', {
      pStart: [-2.5, 4.0, -6.0],
      pEnd: [-6.8, 1.2, -3.5],
      width: 3,
      depth: 2,
      upHint: [0, 1, 0],
      padStart: 1.2,
      padEnd: 0.6,
      parent: 'flipper_l',
      mat: 'serpent_flipper',
    });
    b.addSegmentBox('flipper_l_blade_outer', {
      pStart: [-5.6, 1.8, -4.2],
      pEnd: [-9.6, 0.8, -1.5],
      width: 2,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.3,
      parent: 'flipper_l',
      mat: 'serpent_flipper',
    });
    b.addSegmentBox('flipper_l_blade_inner', {
      pStart: [-5.2, 1.5, -3.2],
      pEnd: [-8.4, 0.5, 0.2],
      width: 2,
      depth: 1,
      upHint: [0, 1, 0],
      padStart: 0.8,
      padEnd: 0.0,
      parent: 'flipper_l',
      mat: 'serpent_flipper',
    });
  });

  // --- 4. HIGH MID-BODY S-WAVE ARCH (`body_arch`) + TALL SCALLOPED FIN CREST ---
  b.addSegmentBox('body_climb_1', {
    pStart: [0, 4.2, -4.0],
    pEnd: [0, 12.2, 1.8],
    width: 6,
    depth: 6,
    upHint: [0, 0.65, -0.76],
    padStart: 1.5,
    padEnd: 1.4,
    parent: 'body_arch',
    mat: 'serpent_body',
  });
  b.addSegmentBox('body_climb_2', {
    pStart: [0, 12.2, 1.8],
    pEnd: [0, 19.2, 7.6],
    width: 5,
    depth: 5,
    upHint: [0, 0.68, -0.73],
    padStart: 1.4,
    padEnd: 1.4,
    parent: 'body_arch',
    mat: 'serpent_body',
  });
  b.addSegmentBox('body_crest_peak', {
    pStart: [0, 19.2, 7.6],
    pEnd: [0, 18.2, 13.0],
    width: 5,
    depth: 5,
    upHint: [0, 1, 0],
    padStart: 1.4,
    padEnd: 1.4,
    parent: 'body_arch',
    mat: 'serpent_body',
  });

  // Tall Scalloped Translucent Seafoam Dorsal Crest along Mid-Arch (`orig_15_serpent.png`!)
  b.addSegmentBox('fin_crest_arch_1', {
    pStart: [0, 7.2, -4.6],
    pEnd: [0, 15.2, 0.6],
    width: 1,
    depth: 4,
    upHint: [0, 0.65, -0.76],
    padStart: 0.8,
    padEnd: 0.8,
    parent: 'body_arch',
    mat: 'serpent_fin',
  });
  b.addSegmentBox('fin_crest_arch_2', {
    pStart: [0, 15.2, 0.6],
    pEnd: [0, 22.4, 6.8],
    width: 1,
    depth: 5,
    upHint: [0, 0.68, -0.73],
    padStart: 0.8,
    padEnd: 0.8,
    parent: 'body_arch',
    mat: 'serpent_fin',
  });
  b.addSegmentBox('fin_crest_arch_3', {
    pStart: [0, 22.4, 6.8],
    pEnd: [0, 21.4, 13.4],
    width: 1,
    depth: 5,
    upHint: [0, 1, 0],
    padStart: 0.8,
    padEnd: 0.8,
    parent: 'body_arch',
    mat: 'serpent_fin',
  });
  b.addSegmentBox('fin_crest_arch_4', {
    pStart: [0, 21.0, 12.8],
    pEnd: [0, 15.2, 18.0],
    width: 1,
    depth: 4,
    upHint: [0, 0.82, 0.57],
    padStart: 0.8,
    padEnd: 0.8,
    parent: 'body_arch',
    mat: 'serpent_fin',
  });

  // Scalloped Dorsal Fin Spines jutting out along Mid-Arch & Neck (`orig_15_serpent.png`!)
  for (const [nm, ps, pe, par] of [
    ['fin_spike_nape_1', [0, 20.5, -13.0], [0, 23.8, -11.0], 'neck'],
    ['fin_spike_nape_2', [0, 16.5, -9.0], [0, 19.5, -6.5], 'neck'],
    ['fin_spike_arch_1', [0, 11.5, -1.8], [0, 15.8, -1.2], 'body_arch'],
    ['fin_spike_arch_2', [0, 17.5, 2.8], [0, 22.2, 3.8], 'body_arch'],
    ['fin_spike_arch_3', [0, 21.0, 7.8], [0, 25.4, 9.2], 'body_arch'],
    ['fin_spike_arch_4', [0, 20.2, 11.8], [0, 24.2, 13.8], 'body_arch'],
    ['fin_spike_tail_1', [0, 12.2, 24.8], [0, 16.2, 26.2], 'tail_tip'],
    ['fin_spike_tail_2', [0, 16.2, 29.2], [0, 20.5, 31.2], 'tail_tip'],
  ]) {
    b.addSegmentBox(nm, {
      pStart: ps,
      pEnd: pe,
      width: 1,
      depth: 2,
      upHint: [0, 0, 1],
      padStart: 0.8,
      padEnd: 0.3,
      parent: par,
      mat: 'serpent_fin',
    });
  }

  // --- 5. SECOND WAVE DIP (`tail_dip`) & RISING TAPERED TAIL (`tail_tip`) ---
  b.addSegmentBox('tail_descent_1', {
    pStart: [0, 18.2, 13.0],
    pEnd: [0, 11.2, 18.2],
    width: 5,
    depth: 5,
    upHint: [0, 0.72, 0.69],
    padStart: 1.4,
    padEnd: 1.4,
    parent: 'tail_dip',
    mat: 'serpent_body',
  });
  b.addSegmentBox('tail_trough_2', {
    pStart: [0, 11.2, 18.2],
    pEnd: [0, 7.6, 22.8],
    width: 4,
    depth: 4,
    upHint: [0, 0.85, 0.52],
    padStart: 1.4,
    padEnd: 1.2,
    parent: 'tail_dip',
    mat: 'serpent_body',
  });

  b.addSegmentBox('tail_rise_1', {
    pStart: [0, 7.6, 22.8],
    pEnd: [0, 12.2, 27.8],
    width: 4,
    depth: 4,
    upHint: [0, 0.76, -0.65],
    padStart: 1.2,
    padEnd: 1.0,
    parent: 'tail_tip',
    mat: 'serpent_body',
  });
  b.addSegmentBox('tail_rise_tip', {
    pStart: [0, 12.2, 27.8],
    pEnd: [0, 17.2, 32.5],
    width: 3,
    depth: 3,
    upHint: [0, 0.76, -0.65],
    padStart: 1.0,
    padEnd: 0.4,
    parent: 'tail_tip',
    mat: 'serpent_body',
  });

  // Tall Scalloped Translucent Seafoam Tail Fin Crest
  b.addSegmentBox('fin_crest_tail_1', {
    pStart: [0, 9.8, 22.2],
    pEnd: [0, 15.0, 27.4],
    width: 1,
    depth: 4,
    upHint: [0, 0.76, -0.65],
    padStart: 0.8,
    padEnd: 0.8,
    parent: 'tail_tip',
    mat: 'serpent_fin',
  });
  b.addSegmentBox('fin_crest_tail_2', {
    pStart: [0, 15.0, 27.4],
    pEnd: [0, 20.0, 32.8],
    width: 1,
    depth: 4,
    upHint: [0, 0.76, -0.65],
    padStart: 0.8,
    padEnd: 0.6,
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
      { bone: 'flipper_l', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, -6] }, { time: 2.0, value: [0, 0, 8] }, { time: 4.0, value: [0, 0, -6] }] },
      { bone: 'flipper_r', channel: 'rotation', keys: [{ time: 0.0, value: [0, 0, 6] }, { time: 2.0, value: [0, 0, -8] }, { time: 4.0, value: [0, 0, 6] }] },
      { bone: 'body_arch', channel: 'rotation', keys: [{ time: 0.0, value: [3, 0, 0] }, { time: 2.0, value: [-3, 0, 0] }, { time: 4.0, value: [3, 0, 0] }] },
      { bone: 'tail_tip', channel: 'rotation', keys: [{ time: 0.0, value: [-6, 0, 0] }, { time: 2.0, value: [6, 0, 0] }, { time: 4.0, value: [-6, 0, 0] }] },
    ],
  });

  b.addAnimation('swim', {
    length: 2.0,
    loop: 'loop',
    tracks: [
      { bone: 'neck', channel: 'rotation', keys: [{ time: 0.0, value: [-5, 0, 0] }, { time: 1.0, value: [5, 0, 0] }, { time: 2.0, value: [-5, 0, 0] }] },
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

  const reportDir = path.join(repoRoot, 'reports/blockbench_mcp/batch_05');
  fs.mkdirSync(reportDir, { recursive: true });
  fs.writeFileSync(
    path.join(reportDir, 'validation_report.json'),
    JSON.stringify({ generated_via: 'blockbench-mcp-plugin (official MCP server)', models: validationSummary }, null, 2) + '\n'
  );
  console.log('\n[done] All 3 Batch 05 V2 models built & validated via Blockbench MCP!');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
