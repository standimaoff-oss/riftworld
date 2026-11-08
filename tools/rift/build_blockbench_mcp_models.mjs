#!/usr/bin/env node
/**
 * Builds Riftworld's high-detail Blockbench (.bbmodel) creature models via REAL MCP
 * tool calls to `blockbench-mcp-headless` (`jasonjgardner/blockbench-mcp-plugin`):
 *   1. `reports/blockbench_models/liopleurodon.bbmodel` (1024x1024, Pliosaur Leviathan)
 *   2. `reports/blockbench_models/pegasus.bbmodel`      (512x512, Celestial Winged Steed)
 *
 * Pipeline:
 *   MCP `bbmodel_create` -> MCP `bbmodel_edit` (bones, rotated cuboids, Box UVs, animations)
 *   -> `paint_blockbench_textures.py` (3D world-space pixel-art atlases)
 *   -> MCP `bbmodel_add_texture` -> MCP `bbmodel_validate` (with self_test=true)
 *   -> MCP `bbmodel_validate_animations` -> MCP `bbmodel_export_modded_entity` (Forge 1.17+ Mojmap)
 *   -> MCP `bbmodel_export_bedrock_geometry` & `bbmodel_export_bedrock_animation`
 *   -> Synchronized `mod/src/main/resources/assets/riftworld/geo/{liopleurodon,pegasus}.json`
 */

import { execFileSync } from "node:child_process";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(HERE, "..", "..");
const MCP_REPO = process.env.BLOCKBENCH_MCP_DIR || join(ROOT, ".cache/blockbench-mcp-plugin");

const req = createRequire(join(MCP_REPO, "package.json"));
const { Client } = req("@modelcontextprotocol/sdk/client/index.js");
const { StdioClientTransport } = req("@modelcontextprotocol/sdk/client/stdio.js");

// ============================================================================
// Skyline Non-Overlapping Box-UV Packer + Symmetric Rig Builder
// ============================================================================
class ModelSpec {
  constructor(name, identifier, atlasSize) {
    this.name = name;
    this.identifier = identifier;
    this.atlasSize = atlasSize;
    this.groups = [];
    this.cubes = [];
    this.animations = [];
  }

  group(name, origin, rotation = [0, 0, 0], parent = undefined) {
    this.groups.push({ name, origin, rotation, parent });
    return name;
  }

  symGroup(baseName, originL, rotationL = [0, 0, 0], parentBase = undefined) {
    const pL = parentBase ? (parentBase.endsWith("_l") ? parentBase : `${parentBase}_l`) : undefined;
    const pR = parentBase ? (parentBase.endsWith("_l") ? parentBase.replace(/_l$/, "_r") : `${parentBase}_r`) : undefined;
    this.group(`${baseName}_l`, originL, rotationL, pL ?? parentBase);
    this.group(
      `${baseName}_r`,
      [-originL[0], originL[1], originL[2]],
      [rotationL[0], -rotationL[1], -rotationL[2]],
      pR ?? parentBase,
    );
  }

  /**
   * Adds one cuboid with exact integer dimensions [sx, sy, sz] placed from `from`.
   */
  cube(parent, name, from, size, opts = {}) {
    const sx = Math.max(1, Math.round(size[0]));
    const sy = Math.max(1, Math.round(size[1]));
    const sz = Math.max(1, Math.round(size[2]));
    const to = [from[0] + sx, from[1] + sy, from[2] + sz];
    const origin = opts.origin ?? [
      +(from[0] + sx * 0.5).toFixed(3),
      +(from[1] + sy * 0.5).toFixed(3),
      +(from[2] + sz * 0.5).toFixed(3),
    ];
    const rotation = opts.rotation ?? [0, 0, 0];
    const inflate = opts.inflate ?? 0;
    this.cubes.push({
      parent,
      name,
      from: from.map((v) => +v.toFixed(3)),
      to: to.map((v) => +v.toFixed(3)),
      size: [sx, sy, sz],
      origin: origin.map((v) => +v.toFixed(3)),
      rotation: rotation.map((v) => +v.toFixed(3)),
      inflate,
      mirror_uv: Boolean(opts.mirror_uv),
    });
  }

  /**
   * Centered cuboid across X = 0 (`from[0] = -sx / 2`).
   */
  centered(parent, name, y0, z0, sx, sy, sz, opts = {}) {
    const w = Math.max(1, Math.round(sx));
    this.cube(parent, name, [-w * 0.5, y0, z0], [w, sy, sz], {
      ...opts,
      origin: opts.origin ?? [0, +(y0 + sy * 0.5).toFixed(3), +(z0 + sz * 0.5).toFixed(3)],
    });
  }

  /**
   * Exact mirror-symmetric left (+X) & right (-X) pair across X = 0.
   * `parentL` can either be a shared centerline bone (like `head`) or a `_l` bone (automatically mapped to `_r`).
   */
  pair(parentL, baseName, fromL, size, opts = {}) {
    const sx = Math.max(1, Math.round(size[0]));
    const sy = Math.max(1, Math.round(size[1]));
    const sz = Math.max(1, Math.round(size[2]));
    const originL = opts.origin ?? [
      +(fromL[0] + sx * 0.5).toFixed(3),
      +(fromL[1] + sy * 0.5).toFixed(3),
      +(fromL[2] + sz * 0.5).toFixed(3),
    ];
    const rotL = opts.rotation ?? [0, 0, 0];
    const parentR = parentL
      .replace(/_fl(?=_|$)/, "_fr")
      .replace(/_bl(?=_|$)/, "_br")
      .replace(/_l(?=_|$)/, "_r");

    this.cube(parentL, `${baseName}_l`, fromL, [sx, sy, sz], {
      ...opts,
      origin: originL,
      rotation: rotL,
      mirror_uv: false,
    });
    this.cube(parentR, `${baseName}_r`, [-(fromL[0] + sx), fromL[1], fromL[2]], [sx, sy, sz], {
      ...opts,
      origin: [-originL[0], originL[1], originL[2]],
      rotation: [rotL[0], -rotL[1], -rotL[2]],
      mirror_uv: true,
    });
  }

  packUvs(pad = 2) {
    const W = this.atlasSize;
    const H = this.atlasSize;
    const items = this.cubes.map((cb, idx) => {
      const [sx, sy, sz] = cb.size;
      const fw = 2 * sz + 2 * sx + pad * 2;
      const fh = sz + sy + pad * 2;
      return { cb, idx, fw, fh };
    });
    items.sort((a, b) => b.fh - a.fh || b.fw - a.fw || a.idx - b.idx);
    const sky = new Int32Array(W);
    for (const it of items) {
      let bestX = -1;
      let bestY = 1 << 30;
      for (let x = 0; x <= W - it.fw; x++) {
        let maxY = 0;
        for (let k = x; k < x + it.fw; k++) {
          if (sky[k] > maxY) maxY = sky[k];
        }
        if (maxY + it.fh <= H && maxY < bestY) {
          bestY = maxY;
          bestX = x;
          if (maxY === 0) break;
        }
      }
      if (bestX < 0) {
        throw new Error(`Atlas ${W}x${H} overflow packing cube ${it.cb.name} (${it.fw}x${it.fh})`);
      }
      for (let k = bestX; k < bestX + it.fw; k++) {
        sky[k] = bestY + it.fh;
      }
      it.cb.uv_offset = [bestX + pad, bestY + pad];
    }
  }
}

// ============================================================================
// 1. LIOPLEURODON — Abyssal Pliosaur Leviathan (196 units = 12.25 blocks)
// ============================================================================
function designLiopleurodon() {
  const m = new ModelSpec("liopleurodon", "liopleurodon", 1024);

  // Bone hierarchy (21 articulated bones)
  m.group("root", [0, 19, 0]);
  m.group("body_front", [0, 19, -12], [0, 0, 0], "root");
  m.group("body_mid", [0, 19, -4], [0, 0, 0], "body_front");
  m.group("body_rear", [0, 19, 18], [0, 0, 0], "body_mid");

  m.group("neck_base", [0, 19.5, -28], [0, 0, 0], "body_front");
  m.group("neck_front", [0, 20, -39], [0, 0, 0], "neck_base");
  m.group("head", [0, 20.5, -49], [0, 0, 0], "neck_front");
  m.group("jaw", [0, 17.5, -50], [-6, 0, 0], "head"); // -6 deg around X drops -Z chin DOWN (menacing gape!)

  // 4 x 3-joint swept hydrofoil megapaddles (_l / _r symmetry for Blockbench mirror gate!)
  m.group("fin_front_1_l", [17, 13, -18], [4, -18, -15], "body_front");
  m.group("fin_front_2_l", [34, 13, -18], [2, -14, -6], "fin_front_1_l");
  m.group("fin_front_3_l", [50, 13, -18], [0, -16, -4], "fin_front_2_l");

  m.group("fin_front_1_r", [-17, 13, -18], [4, 18, 15], "body_front");
  m.group("fin_front_2_r", [-34, 13, -18], [2, 14, 6], "fin_front_1_r");
  m.group("fin_front_3_r", [-50, 13, -18], [0, 16, 4], "fin_front_2_r");

  m.group("fin_back_1_l", [13, 13, 25], [5, -22, -14], "body_rear");
  m.group("fin_back_2_l", [26, 13, 25], [2, -15, -5], "fin_back_1_l");
  m.group("fin_back_3_l", [38, 13, 25], [0, -16, -3], "fin_back_2_l");

  m.group("fin_back_1_r", [-13, 13, 25], [5, 22, 14], "body_rear");
  m.group("fin_back_2_r", [-26, 13, 25], [2, 15, 5], "fin_back_1_r");
  m.group("fin_back_3_r", [-38, 13, 25], [0, 16, 3], "fin_back_2_r");

  // 4-stage muscular tail + vertical marine-reptile caudal fluke
  m.group("tail_1", [0, 19, 38], [0, 0, 0], "body_rear");
  m.group("tail_2", [0, 19, 55], [0, 0, 0], "tail_1");
  m.group("tail_3", [0, 19, 71], [0, 0, 0], "tail_2");
  m.group("tail_fluke", [0, 19, 86], [0, 0, 0], "tail_3");

  // ---------------- TORSO: 5-tier sculpted barrel cross-section ----------------
  // Front chest barrel (Z = -29 .. -3)
  m.centered("body_front", "chest_core", 11, -29, 36, 15, 26);
  m.centered("body_front", "chest_upper", 25, -29, 30, 5, 26, { inflate: 0.05 });
  m.centered("body_front", "chest_dorsal", 29, -28, 20, 3, 25);
  m.centered("body_front", "chest_lower", 7.5, -28, 28, 4, 25, { inflate: 0.04 });
  m.centered("body_front", "chest_plastron", 5.5, -26, 20, 3, 23);
  // Muscular pectoral shoulder girdles where megapaddles socket into chest
  m.pair("body_front", "pectoral_girdle", [14, 9, -25], [7, 9, 15], { rotation: [0, -6, -12], inflate: 0.12 });
  // Dorsal median osteoderm scutes & paired lateral armor ridges
  for (let i = 0; i < 4; i++) {
    const z = -26 + i * 6;
    m.centered("body_front", `chest_scute_m${i}`, 31.2, z, 4, 4, 5, { rotation: [-14, 0, 0] });
    m.pair("body_front", `chest_scute_s${i}`, [9, 28.5, z + 0.5], [3, 3, 5], { rotation: [-10, 0, -24] });
    m.pair("body_front", `chest_lat_keel${i}`, [16.8, 19, z], [2, 3, 5], { rotation: [0, 0, -18] });
  }

  // Mid barrel (Z = -4 .. +19)
  m.centered("body_mid", "belly_core", 11, -4, 34, 15, 23, { inflate: 0.02 });
  m.centered("body_mid", "belly_upper", 25, -4, 28, 5, 23);
  m.centered("body_mid", "belly_dorsal", 29, -4, 18, 3, 23);
  m.centered("body_mid", "belly_lower", 7.5, -4, 26, 4, 23);
  m.centered("body_mid", "belly_plastron", 5.5, -4, 18, 3, 22);
  for (let i = 0; i < 4; i++) {
    const z = -2 + i * 5.5;
    m.centered("body_mid", `mid_scute_m${i}`, 31.0, z, 4, 4, 5, { rotation: [-14, 0, 0] });
    m.pair("body_mid", `mid_scute_s${i}`, [8, 28.2, z], [3, 3, 5], { rotation: [-10, 0, -24] });
    m.pair("body_mid", `mid_lat_keel${i}`, [15.8, 19, z], [2, 3, 5], { rotation: [0, 0, -18] });
  }

  // Rear pelvic trunk (Z = +18 .. +39)
  m.centered("body_rear", "pelvis_core", 12, 18, 28, 14, 21);
  m.centered("body_rear", "pelvis_upper", 25, 18, 22, 4, 21);
  m.centered("body_rear", "pelvis_dorsal", 28, 18, 14, 3, 20);
  m.centered("body_rear", "pelvis_lower", 9, 18, 22, 4, 20);
  m.centered("body_rear", "pelvis_belly", 7, 18, 14, 3, 19);
  m.pair("body_rear", "pelvic_girdle", [11, 10, 20], [6, 8, 12], { rotation: [0, -8, -14], inflate: 0.1 });
  for (let i = 0; i < 3; i++) {
    const z = 19 + i * 6;
    m.centered("body_rear", `pelvis_scute_m${i}`, 30.0, z, 3, 4, 5, { rotation: [-16, 0, 0] });
    m.pair("body_rear", `pelvis_scute_s${i}`, [6.2, 27.2, z], [2, 3, 5], { rotation: [-10, 0, -22] });
  }

  // ---------------- SHORT, POWERFUL PLIOSAUR NECK ----------------
  // Neck base (Z = -40 .. -28)
  m.centered("neck_base", "neck_b_core", 13, -40, 28, 14, 13, { inflate: 0.04 });
  m.centered("neck_base", "neck_b_upper", 26, -40, 22, 4, 13);
  m.centered("neck_base", "neck_b_crest", 29, -39, 14, 2, 12);
  m.centered("neck_base", "neck_b_throat", 9.5, -40, 22, 4, 13);
  m.pair("neck_base", "neck_b_muscle", [12, 15, -39], [4, 10, 12], { rotation: [0, 4, -8] });
  for (let i = 0; i < 2; i++) {
    m.centered("neck_base", `neck_b_scute${i}`, 30.2, -38 + i * 6, 3, 3, 5, { rotation: [-12, 0, 0] });
  }

  // Neck front (Z = -50 .. -39)
  m.centered("neck_front", "neck_f_core", 14, -50, 24, 13, 12, { inflate: 0.03 });
  m.centered("neck_front", "neck_f_upper", 26, -50, 18, 3, 12);
  m.centered("neck_front", "neck_f_throat", 11, -50, 18, 4, 12);
  m.centered("neck_front", "neck_f_scute", 28.4, -47, 3, 3, 5, { rotation: [-10, 0, 0] });

  // ---------------- MASSIVE APEX PLIOSAUR SKULL & JAWS ----------------
  // Temporal & Occipital Skull (Z = -62 .. -49)
  m.centered("head", "skull_occiput", 18, -62, 22, 9, 14, { inflate: 0.06 });
  m.centered("head", "skull_crown", 26, -61, 16, 3, 13, { rotation: [4, 0, 0] });
  m.centered("head", "sagittal_crest", 28, -59, 6, 3, 11, { rotation: [5, 0, 0] });
  m.pair("head", "temporal_cheek", [9, 17.5, -61], [4, 8, 13], { rotation: [0, 5, 0], inflate: 0.08 });
  m.pair("head", "jugal_bar", [9.2, 17.5, -69], [3, 5, 9], { rotation: [0, 8, 0] });

  // Recessed Orbits, Armored Supraorbital Brow Ridges & Predatory Eyes (Z = -68 .. -59)
  m.pair("head", "orbit_socket", [7.6, 21.0, -67], [3, 5, 8]);
  m.pair("head", "brow_crest", [7.8, 24.8, -68], [4, 3, 10], {
    rotation: [6, 7, -14],
    inflate: 0.16,
  });
  m.pair("head", "postorbital_horn", [8.8, 25.5, -60], [3, 3, 5], {
    rotation: [14, 10, -18],
  });
  // Eyes: crisp golden-amber predator eyes angled slightly forward & up
  m.pair("head", "eye_globe", [8.6, 21.8, -66], [2, 3, 5], {
    rotation: [0, 6, -6],
    inflate: 0.12,
  });

  // Mid-Rostrum / Maxillary Snout (Z = -80 .. -61)
  m.centered("head", "maxilla_rear", 18, -72, 18, 6, 11);
  m.centered("head", "nasal_bridge_rear", 23.2, -73, 12, 4, 12, { rotation: [5, 0, 0] });
  // Retracted dorsal Pliosaur external nostrils ahead of eyes (sleek recessed marine-reptile narial grooves!)
  m.pair("head", "nostril_rim", [2.2, 24.1, -71], [2, 1, 4], { rotation: [5, 0, -6], inflate: 0.14 });
  m.pair("head", "nostril_pit", [2.5, 24.4, -70.5], [1, 1, 3], { inflate: 0.04 });

  // Slight pre-rosette constriction (Z = -80 .. -71) — authentic Liopleurodon skull waist!
  m.centered("head", "maxilla_waist", 18, -80, 14, 5, 9);
  m.centered("head", "nasal_bridge_front", 22.0, -81, 10, 3, 9, { rotation: [4, 0, 0] });
  m.pair("head", "maxilla_festoon", [6.2, 17.2, -76], [3, 3, 12], { rotation: [-3, 6, 0], inflate: 0.05 });

  // Expanded Spoon-Shaped Premaxillary Terminal Rosette (Z = -90 .. -79)
  m.centered("head", "rosette_upper_main", 18, -88, 16, 5, 9, { inflate: 0.06 });
  m.centered("head", "rosette_upper_tip", 18.2, -90.5, 12, 4, 4);
  m.centered("head", "rosette_upper_dome", 22.0, -88.5, 10, 2, 9, { rotation: [7, 0, 0] });

  // Wet Crimson Palate & Upper Gum Margin
  m.centered("head", "palate_rear", 17.2, -73, 14, 2, 16, { inflate: -0.04 });
  m.centered("head", "palate_front", 17.2, -89, 12, 2, 16, { inflate: -0.04 });

  // 28 Individual Heterodont Upper Fangs (Rosette Giant Tusks + Maxillary Fangs)
  const upperRosetteFangs = [
    // [xL, yTop, z, h, rot]
    [4.2, 18.2, -90.0, 5, [-8, 0, -9]],
    [6.4, 18.2, -87.0, 6, [-6, 0, -11]],
    [6.8, 18.2, -83.5, 5, [-5, 0, -10]],
    [6.0, 18.2, -80.2, 4, [-4, 0, -8]],
  ];
  upperRosetteFangs.forEach(([x, yTop, z, h, rot], idx) => {
    m.pair("head", `tusk_rosette_u${idx}`, [x, yTop - h, z], [1, h, 2], {
      origin: [x + 0.5, yTop, z + 1],
      rotation: rot,
      inflate: 0.12,
    });
  });

  const upperMaxillaFangs = [
    [5.8, 18.0, -77.0, 3],
    [6.6, 17.8, -74.0, 5],
    [7.2, 17.8, -70.8, 4],
    [7.6, 17.8, -67.6, 5],
    [8.0, 18.0, -64.4, 4],
    [8.2, 18.0, -61.4, 3],
    [8.4, 18.2, -58.5, 2],
  ];
  upperMaxillaFangs.forEach(([x, yTop, z, h], idx) => {
    m.pair("head", `fang_upper_${idx}`, [x, yTop - h, z], [1, h, 2], {
      origin: [x + 0.5, yTop, z + 1],
      rotation: [-5, 0, -7],
      inflate: 0.08,
    });
  });
  // Anterior median pair of premaxillary fangs right at snout tip
  m.pair("head", "tusk_rosette_u_tip", [1.6, 14.2, -90.6], [1, 4, 1], {
    origin: [2.1, 18.2, -90.1],
    rotation: [-10, 0, -5],
    inflate: 0.1,
  });

  // ---------------- ARTICULATED LOWER JAW (`jaw`) ----------------
  m.pair("jaw", "jaw_ramus", [7.5, 13.0, -62], [4, 5, 13], { rotation: [0, 5, 0], inflate: 0.06 });
  m.centered("jaw", "jaw_throat_sac", 11.2, -63, 18, 5, 14, { rotation: [-5, 0, 0] });
  m.centered("jaw", "mandible_rear", 13.0, -73, 16, 4, 12);
  m.centered("jaw", "mandible_waist", 13.4, -80, 12, 4, 8);
  m.centered("jaw", "chin_rosette", 13.4, -88, 14, 4, 9, { inflate: 0.05 });
  m.centered("jaw", "chin_tip", 13.8, -90.0, 10, 3, 3);
  m.centered("jaw", "chin_keel", 12.0, -86, 8, 2, 18, { rotation: [-3, 0, 0] });

  // Lower Gums & Fleshy Marine-Reptile Tongue
  m.centered("jaw", "lower_gum_rear", 16.2, -73, 13, 2, 14, { inflate: -0.05 });
  m.centered("jaw", "lower_gum_front", 16.2, -88, 11, 2, 15, { inflate: -0.05 });
  m.centered("jaw", "tongue_base", 16.8, -71, 9, 2, 14, { inflate: 0.04 });
  m.centered("jaw", "tongue_tip", 16.8, -82, 6, 2, 12, { rotation: [2, 0, 0] });

  // 22 Interlocking Lower Fangs
  const lowerFangs = [
    [1.8, 16.5, -89.5, 4, [8, 0, 5]],
    [4.8, 16.5, -88.2, 5, [7, 0, 9]],
    [5.8, 16.5, -85.0, 4, [5, 0, 10]],
    [5.2, 16.5, -81.5, 3, [4, 0, 8]],
    [5.0, 16.5, -78.2, 4, [4, 0, 7]],
    [5.8, 16.5, -75.0, 3, [4, 0, 7]],
    [6.2, 16.5, -71.8, 4, [4, 0, 7]],
    [6.6, 16.5, -68.6, 3, [4, 0, 6]],
    [7.0, 16.5, -65.4, 3, [4, 0, 6]],
    [7.2, 16.5, -62.2, 2, [3, 0, 5]],
  ];
  lowerFangs.forEach(([x, yBase, z, h, rot], idx) => {
    m.pair("jaw", `fang_lower_${idx}`, [x, yBase, z], [1, h, 2], {
      origin: [x + 0.5, yBase, z + 1],
      rotation: rot,
      inflate: 0.07,
    });
  });

  // ---------------- FOUR SWEPT HYDROFOIL MEGAPADDLES ----------------
  // Front Pectoral Megapaddles (3 articulated segments per side, hydrofoil profile)
  // Segment 1: Humerus & Inner Foil
  m.pair("fin_front_1_l", "ff_humeral_spar", [16, 11.5, -24], [19, 4, 9], { inflate: 0.08 });
  m.pair("fin_front_1_l", "ff_shoulder_cap", [15.5, 14.5, -23], [11, 2, 9], { rotation: [0, 0, -8], inflate: 0.12 });
  m.pair("fin_front_1_l", "ff_mid_foil1", [17, 12.0, -16], [18, 3, 8]);
  m.pair("fin_front_1_l", "ff_trail_blade1", [18, 12.5, -9], [16, 2, 6]);

  // Segment 2: Fore-paddle & Swept Camber
  m.pair("fin_front_2_l", "ff_lead_spar2", [33, 12.0, -23], [18, 3, 8], { inflate: 0.05 });
  m.pair("fin_front_2_l", "ff_mid_foil2", [33, 12.2, -16], [17, 3, 7]);
  m.pair("fin_front_2_l", "ff_trail_blade2", [34, 12.6, -10], [15, 2, 6]);
  // Scalloped hydrodynamic trailing lobes
  m.pair("fin_front_2_l", "ff_scallop2a", [35, 12.6, -5], [6, 1, 3], { inflate: 0.05 });
  m.pair("fin_front_2_l", "ff_scallop2b", [42, 12.6, -5.5], [6, 1, 3], { inflate: 0.05 });

  // Segment 3: Falcate Distal Wingtip
  m.pair("fin_front_3_l", "ff_tip_lead", [49, 12.2, -22], [15, 3, 7], { inflate: 0.04 });
  m.pair("fin_front_3_l", "ff_tip_trail", [49, 12.6, -16], [13, 2, 7]);
  m.pair("fin_front_3_l", "ff_tip_apex", [62, 12.5, -20.5], [9, 2, 6], { rotation: [0, -12, 0] });
  m.pair("fin_front_3_l", "ff_tip_point", [69, 12.7, -18.5], [5, 1, 4], { rotation: [0, -18, 0], inflate: 0.08 });

  // Rear Pelvic Paddles (3 articulated segments per side)
  m.pair("fin_back_1_l", "fb_femoral_spar", [12, 11.8, 20], [15, 4, 8], { inflate: 0.06 });
  m.pair("fin_back_1_l", "fb_hip_cap", [11.5, 14.5, 20.5], [9, 2, 8], { rotation: [0, 0, -8], inflate: 0.1 });
  m.pair("fin_back_1_l", "fb_mid_foil1", [13, 12.2, 27], [14, 3, 7]);
  m.pair("fin_back_1_l", "fb_trail_blade1", [14, 12.6, 33], [12, 2, 5]);

  m.pair("fin_back_2_l", "fb_lead_spar2", [25, 12.2, 20.5], [14, 3, 7], { inflate: 0.04 });
  m.pair("fin_back_2_l", "fb_mid_foil2", [25, 12.5, 26.5], [13, 2, 7]);
  m.pair("fin_back_2_l", "fb_trail_blade2", [26, 12.7, 32.5], [11, 2, 5]);

  m.pair("fin_back_3_l", "fb_tip_lead", [37, 12.4, 21], [11, 2, 6], { inflate: 0.04 });
  m.pair("fin_back_3_l", "fb_tip_trail", [37, 12.6, 26], [9, 2, 6]);
  m.pair("fin_back_3_l", "fb_tip_apex", [46, 12.6, 22.5], [7, 1, 5], { rotation: [0, -15, 0], inflate: 0.1 });

  // ---------------- MUSCULAR TAIL & SWEPT VERTICAL CAUDAL FLUKE ----------------
  // Tail 1 (Z = 37 .. 56)
  m.centered("tail_1", "tail1_core", 13, 37, 20, 12, 19);
  m.centered("tail_1", "tail1_upper", 24, 37, 14, 3, 19);
  m.centered("tail_1", "tail1_lower", 10.5, 37, 14, 3, 18);
  for (let i = 0; i < 3; i++) {
    m.centered("tail_1", `tail1_scute${i}`, 26.2, 39 + i * 5.5, 3, 3, 4, { rotation: [-18, 0, 0] });
  }

  // Tail 2 (Z = 54 .. 72)
  m.centered("tail_2", "tail2_core", 14, 54, 14, 10, 18);
  m.centered("tail_2", "tail2_upper", 23, 54, 10, 3, 18);
  m.centered("tail_2", "tail2_lower", 12, 54, 10, 3, 17);
  for (let i = 0; i < 2; i++) {
    m.centered("tail_2", `tail2_keel${i}`, 25.2, 56 + i * 7, 2, 3, 6, { rotation: [-20, 0, 0] });
  }

  // Tail 3 / Peduncle (Z = 70 .. 87)
  m.centered("tail_3", "tail3_core", 15, 70, 9, 8, 17);
  m.centered("tail_3", "tail3_dorsal_keel", 22, 71, 4, 3, 16);
  m.pair("tail_3", "peduncle_stabilizer", [3.5, 18, 73], [4, 2, 13], { inflate: 0.05 });

  // Tail Fluke (Z = 85 .. 107) — Continuous Crescent Marine-Reptile Caudal Fin
  m.centered("tail_fluke", "fluke_peduncle", 16, 85, 6, 6, 13);
  m.centered("tail_fluke", "fluke_tip_cone", 17, 96, 4, 4, 7);
  // Swept dorsal fluke lobes aligned seamlessly along the 34-deg sweep spine
  m.centered("tail_fluke", "fluke_dorsal_base", 20, 87, 3, 10, 9, {
    origin: [0, 20, 88],
    rotation: [32, 0, 0],
    inflate: 0.06,
  });
  m.centered("tail_fluke", "fluke_dorsal_mid", 26.5, 91.5, 2, 10, 8, {
    origin: [0, 26.5, 92.5],
    rotation: [36, 0, 0],
    inflate: 0.05,
  });
  m.centered("tail_fluke", "fluke_dorsal_tip", 33.5, 96.5, 2, 8, 6, {
    origin: [0, 33.5, 97.5],
    rotation: [40, 0, 0],
  });
  // Swept ventral hypocercal fluke lobes (`-rx` sweeps `+Y_down` backward into `+Z`!)
  m.centered("tail_fluke", "fluke_ventral_base", 9.5, 88, 3, 8, 8, {
    origin: [0, 17.5, 89],
    rotation: [-30, 0, 0],
    inflate: 0.06,
  });
  m.centered("tail_fluke", "fluke_ventral_tip", 4.5, 92.5, 2, 7, 6, {
    origin: [0, 11.5, 93.5],
    rotation: [-36, 0, 0],
  });

  // Keyframe Animations inside .bbmodel (validated by bbmodel_validate_animations!)
  m.animations.push({
    name: "animation.liopleurodon.swim",
    length: 4.0,
    loop: "loop",
    snapping: 20,
    tracks: [
      { bone: "neck_base", channel: "rotation", keys: [[0, [0, 2.5, 0]], [2, [0, -2.5, 0]], [4, [0, 2.5, 0]]] },
      { bone: "head", channel: "rotation", keys: [[0, [1.5, 3, 0]], [2, [-1.5, -3, 0]], [4, [1.5, 3, 0]]] },
      { bone: "jaw", channel: "rotation", keys: [[0, [0, 0, 0]], [2, [-6, 0, 0]], [4, [0, 0, 0]]] },
      { bone: "tail_1", channel: "rotation", keys: [[0, [0, -6, 0]], [2, [0, 6, 0]], [4, [0, -6, 0]]] },
      { bone: "tail_2", channel: "rotation", keys: [[0, [0, -9, 0]], [2, [0, 9, 0]], [4, [0, -9, 0]]] },
      { bone: "tail_3", channel: "rotation", keys: [[0, [0, 11, 0]], [2, [0, -11, 0]], [4, [0, 11, 0]]] },
      { bone: "tail_fluke", channel: "rotation", keys: [[0, [0, 14, 0]], [2, [0, -14, 0]], [4, [0, 14, 0]]] },
      { bone: "fin_front_1_l", channel: "rotation", keys: [[0, [6, 0, -10]], [2, [-6, 0, 12]], [4, [6, 0, -10]]] },
      { bone: "fin_front_1_r", channel: "rotation", keys: [[0, [6, 0, 10]], [2, [-6, 0, -12]], [4, [6, 0, 10]]] },
      { bone: "fin_back_1_l", channel: "rotation", keys: [[0, [-5, 0, 8]], [2, [5, 0, -10]], [4, [-5, 0, 8]]] },
      { bone: "fin_back_1_r", channel: "rotation", keys: [[0, [-5, 0, -8]], [2, [5, 0, 10]], [4, [-5, 0, -8]]] },
    ],
  });

  m.packUvs(2);
  return m;
}

// ============================================================================
// 2. PEGASUS — Celestial Winged Steed of Elisar (512x512)
// ============================================================================
function designPegasus() {
  const m = new ModelSpec("pegasus", "pegasus", 512);

  // Bone hierarchy (31 articulated bones)
  m.group("root", [0, 20, 0]);
  m.group("body", [0, 20, 0], [0, 0, 0], "root");
  m.group("saddle_tack", [0, 24, -1], [0, 0, 0], "body");

  m.group("neck_lower", [0, 22.5, -10.5], [-24, 0, 0], "body");
  m.group("neck_upper", [0, 29.0, -10.8], [16, 0, 0], "neck_lower");
  m.group("head", [0, 35.0, -11.2], [-15, 0, 0], "neck_upper");
  m.group("head_tack", [0, 35.0, -11.2], [0, 0, 0], "head");
  m.group("jaw", [0, 33.2, -15.5], [0, 0, 0], "head");
  m.group("ear_l", [2.1, 37.4, -11.2], [12, 16, 16], "head");
  m.group("ear_r", [-2.1, 37.4, -11.2], [12, -16, -16], "head");

  // 4 x 3-joint equine legs (hooves rest flush at Y = 0.0!)
  m.group("leg_front_upper_l", [3.8, 19.5, -9.8], [0, 0, 0], "body");
  m.group("leg_front_cannon_l", [3.8, 9.8, -9.8], [0, 0, 0], "leg_front_upper_l");
  m.group("leg_front_hoof_l", [3.8, 3.8, -9.8], [0, 0, 0], "leg_front_cannon_l");

  m.group("leg_front_upper_r", [-3.8, 19.5, -9.8], [0, 0, 0], "body");
  m.group("leg_front_cannon_r", [-3.8, 9.8, -9.8], [0, 0, 0], "leg_front_upper_r");
  m.group("leg_front_hoof_r", [-3.8, 3.8, -9.8], [0, 0, 0], "leg_front_cannon_r");

  m.group("leg_back_thigh_l", [4.0, 20.0, 10.0], [0, 0, 0], "body");
  m.group("leg_back_cannon_l", [4.0, 10.4, 11.2], [0, 0, 0], "leg_back_thigh_l");
  m.group("leg_back_hoof_l", [4.0, 3.8, 10.8], [0, 0, 0], "leg_back_cannon_l");

  m.group("leg_back_thigh_r", [-4.0, 20.0, 10.0], [0, 0, 0], "body");
  m.group("leg_back_cannon_r", [-4.0, 10.4, 11.2], [0, 0, 0], "leg_back_thigh_r");
  m.group("leg_back_hoof_r", [-4.0, 3.8, 10.8], [0, 0, 0], "leg_back_cannon_r");

  // 2 x 3-joint heraldic feathered wings + 3 feather-fan scissoring sub-bones per wing (37 bones total!)
  // Cleanly rooted at the outer shoulder socket (X = ±6.5) so folded wings NEVER clip into torso or saddle!
  m.group("wing_1_l", [6.5, 22.4, -7.2], [4, -8, 14], "body");
  m.group("wing_1_feathers_l", [9.5, 22.0, -5.8], [0, 0, 0], "wing_1_l");
  m.group("wing_2_l", [18.0, 22.4, -7.2], [2, -6, 6], "wing_1_l");
  m.group("wing_2_feathers_l", [22.5, 22.0, -5.8], [0, 0, 0], "wing_2_l");
  m.group("wing_3_l", [31.2, 22.4, -7.2], [0, -8, 4], "wing_2_l");
  m.group("wing_3_fan_l", [33.5, 22.0, -4.2], [0, 0, 0], "wing_3_l");

  m.group("wing_1_r", [-6.5, 22.4, -7.2], [4, 8, -14], "body");
  m.group("wing_1_feathers_r", [-9.5, 22.0, -5.8], [0, 0, 0], "wing_1_r");
  m.group("wing_2_r", [-18.0, 22.4, -7.2], [2, 6, -6], "wing_1_r");
  m.group("wing_2_feathers_r", [-22.5, 22.0, -5.8], [0, 0, 0], "wing_2_r");
  m.group("wing_3_r", [-31.2, 22.4, -7.2], [0, 8, -4], "wing_2_r");
  m.group("wing_3_fan_r", [-33.5, 22.0, -4.2], [0, 0, 0], "wing_3_r");

  // 3-segment flowing Arabian plume tail (`-rx` lifts & sweeps `+Z` dock gracefully backward-up!)
  m.group("tail_1", [0, 23.2, 13.5], [-18, 0, 0], "body");
  m.group("tail_2", [0, 20.5, 17.2], [12, 0, 0], "tail_1");
  m.group("tail_3", [0, 12.5, 18.8], [6, 0, 0], "tail_2");

  // ---------------- EQUINE TORSO (`body`) ----------------
  m.centered("body", "ribcage_core", 16.5, -10.5, 11, 8, 20);
  m.centered("body", "back_loin", 23.5, -7.5, 9, 2, 17, { inflate: 0.08 });
  m.centered("body", "withers_arch", 23.8, -11.5, 8, 3, 6, { rotation: [-10, 0, 0], inflate: 0.1 });
  m.centered("body", "sternum_keel", 15.2, -11.5, 9, 3, 13, { inflate: 0.05 });
  m.centered("body", "tucked_flank", 16.0, 1.0, 8, 3, 10, { rotation: [7, 0, 0] });
  m.centered("body", "chest_front", 16.8, -13.0, 10, 7, 4, { rotation: [9, 0, 0], inflate: 0.08 });
  m.pair("body", "pectoral_muscle", [1.0, 16.5, -13.4], [4, 6, 4], { rotation: [6, 7, 0], inflate: 0.14 });
  m.pair("body", "scapula_shoulder", [4.0, 18.0, -11.8], [2, 7, 6], { rotation: [-16, 0, -4], inflate: 0.12 });
  m.pair("body", "covert_wing_socket", [5.0, 21.3, -8.8], [2, 3, 4], { rotation: [-8, 0, -6], inflate: 0.08 });
  m.centered("body", "croup_core", 17.5, 8.5, 10, 8, 6, { rotation: [-8, 0, 0], inflate: 0.08 });
  m.pair("body", "haunch_muscle", [2.0, 16.5, 7.5], [4, 8, 6], { rotation: [-4, 0, 3], inflate: 0.14 });

  // ---------------- ROYAL ELISAR SADDLE & HARNESS (`saddle_tack`) ----------------
  m.centered("saddle_tack", "saddle_cloth_top", 24.4, -6.0, 10, 2, 12, { inflate: 0.18 });
  m.pair("saddle_tack", "saddle_cloth_skirt", [4.2, 19.5, -5.5], [2, 6, 11], {
    rotation: [0, 0, -4],
    inflate: 0.08,
  });
  m.centered("saddle_tack", "saddle_seat", 25.0, -4.5, 9, 2, 9, { inflate: 0.12 });
  m.centered("saddle_tack", "saddle_pommel", 25.5, -6.0, 7, 3, 2, { rotation: [-16, 0, 0], inflate: 0.12 });
  m.centered("saddle_tack", "saddle_cantle", 25.5, 3.5, 8, 3, 2, { rotation: [20, 0, 0], inflate: 0.12 });
  m.centered("saddle_tack", "girth_belly_band", 14.9, -4.5, 9, 2, 3, { inflate: 0.22 });
  m.pair("saddle_tack", "girth_side_strap", [4.5, 15.5, -4.5], [1, 9, 3], { inflate: 0.2 });
  m.pair("saddle_tack", "stirrup_strap", [5.2, 16.5, -2.5], [1, 8, 1], { inflate: 0.08 });
  m.pair("saddle_tack", "stirrup_iron", [5.0, 14.5, -3.0], [1, 2, 2], { inflate: 0.14 });
  m.pair("saddle_tack", "breastcollar", [1.5, 19.2, -13.6], [4, 2, 6], {
    rotation: [16, 22, 0],
    inflate: 0.18,
  });
  m.centered("saddle_tack", "chest_medallion", 18.5, -13.9, 3, 3, 1, { rotation: [9, 0, 0], inflate: 0.2 });
  m.centered("saddle_tack", "chest_gem", 19.0, -14.1, 2, 2, 1, { rotation: [9, 0, 0], inflate: 0.24 });

  // ---------------- PROUD STALLION NECK, HEAD, EARS & MANE ----------------
  // Lower Neck
  m.centered("neck_lower", "neck_low_core", 21.0, -13.5, 8, 9, 6, { inflate: 0.08 });
  m.centered("neck_lower", "neck_low_crest", 22.0, -12.5, 6, 9, 5, { inflate: 0.04 });
  m.centered("neck_lower", "neck_low_windpipe", 20.8, -14.2, 5, 8, 3);
  // Cascading Lower Silver-Pearl Mane Locks
  m.centered("neck_lower", "mane_low_ridge", 22.0, -8.2, 2, 9, 3, { inflate: 0.12 });
  for (let i = 0; i < 3; i++) {
    m.pair("neck_lower", `mane_low_lock${i}`, [0.8, 21.5 + i * 2.4, -8.8], [2, 4, 3], {
      rotation: [-8, 10, -14 - i * 3],
      inflate: 0.08,
    });
  }

  // Upper Neck
  m.centered("neck_upper", "neck_up_core", 28.5, -13.5, 6, 8, 5, { inflate: 0.1 });
  m.centered("neck_upper", "neck_up_poll", 34.5, -13.2, 5, 3, 5, { inflate: 0.06 });
  m.centered("neck_upper", "mane_up_ridge", 28.5, -9.0, 2, 9, 3, { inflate: 0.12 });
  for (let i = 0; i < 3; i++) {
    m.pair("neck_upper", `mane_up_lock${i}`, [0.6, 28.8 + i * 2.4, -9.4], [2, 4, 3], {
      rotation: [-6, 12, -16],
      inflate: 0.08,
    });
  }

  // Sculpted Noble Equine Head
  m.centered("head", "cranium", 33.2, -16.2, 6, 5, 6, { inflate: 0.12 });
  m.pair("head", "cheek_jowl", [2.0, 32.0, -15.2], [2, 4, 5], { inflate: 0.1 });
  m.centered("head", "nasal_bridge", 33.6, -21.2, 4, 3, 6, { rotation: [-4, 0, 0], inflate: 0.22 });
  m.centered("head", "muzzle_upper", 33.0, -24.2, 4, 3, 4, { inflate: 0.08 });
  m.pair("head", "nostril_flare", [1.2, 33.6, -24.0], [1, 2, 2], { inflate: 0.16 });
  // Large Dark-Sapphire Equine Eyes + Subtle Upper Eyelid
  m.pair("head", "eye_equine", [2.2, 34.8, -16.2], [1, 2, 3], { inflate: 0.16 });
  m.pair("head", "brow_lash", [2.2, 36.4, -16.3], [1, 1, 3], { rotation: [0, 0, -8], inflate: 0.12 });
  // Forelock falling gracefully between the eyes
  m.centered("head", "forelock_top", 36.8, -16.8, 2, 2, 5, { rotation: [-12, 0, 0], inflate: 0.12 });
  m.centered("head", "forelock_tip", 35.8, -19.2, 2, 2, 3, { rotation: [-22, 0, 0], inflate: 0.06 });
  // Fine Golden Elisar Bridle / Halter (in `head_tack` so wild Pegasus is 100% pure celestial white!)
  m.centered("head_tack", "bridle_noseband", 33.0, -21.4, 4, 3, 1, { inflate: 0.3 });
  m.centered("head_tack", "bridle_browband", 36.4, -15.6, 6, 1, 1, { inflate: 0.26 });
  m.centered("head_tack", "bridle_gem", 36.4, -16.3, 2, 1, 1, { inflate: 0.28 });
  m.pair("head_tack", "bridle_ring", [2.1, 33.0, -21.5], [1, 1, 1], { inflate: 0.28 });

  // Lower Jaw
  m.centered("jaw", "mandible_equine", 31.6, -23.6, 3, 2, 8, { rotation: [2, 0, 0], inflate: 0.12 });

  // Alert Sculpted Equine Ears
  m.cube("ear_l", "ear_base_l", [1.5, 37.0, -11.8], [2, 3, 1], { inflate: 0.06 });
  m.cube("ear_l", "ear_tip_l", [1.8, 39.6, -11.7], [1, 2, 1], { inflate: 0.08 });
  m.cube("ear_r", "ear_base_r", [-3.5, 37.0, -11.8], [2, 3, 1], { inflate: 0.06, mirror_uv: true });
  m.cube("ear_r", "ear_tip_r", [-2.8, 39.6, -11.7], [1, 2, 1], { inflate: 0.08, mirror_uv: true });

  // ---------------- FOUR MULTI-JOINTED EQUINE LEGS (Hooves at Y = 0.0!) ----------------
  // Forelegs (`leg_front_*_l/r`)
  m.pair("leg_front_upper_l", "fore_arm", [2.3, 10.5, -11.3], [3, 10, 3], { rotation: [3, 0, 0], inflate: 0.22 });
  m.pair("leg_front_upper_l", "fore_knee", [2.3, 9.2, -11.3], [3, 2, 3], { inflate: 0.28 });
  m.pair("leg_front_cannon_l", "fore_cannon", [2.8, 3.8, -10.8], [2, 6, 2], { inflate: 0.18 });
  m.pair("leg_front_hoof_l", "fore_fetlock", [2.8, 2.4, -10.9], [2, 2, 2], { inflate: 0.26 });
  m.pair("leg_front_hoof_l", "fetlock_feather_f", [2.8, 1.0, -9.4], [2, 3, 3], {
    origin: [3.8, 3.5, -9.0],
    rotation: [26, 0, 0],
    inflate: 0.08,
  });
  m.pair("leg_front_hoof_l", "fore_pastern", [2.8, 1.2, -11.3], [2, 2, 2], {
    origin: [3.8, 2.5, -10.3],
    rotation: [-14, 0, 0],
    inflate: 0.16,
  });
  m.pair("leg_front_hoof_l", "fore_hoof", [2.3, 0.0, -11.6], [3, 2, 3], { inflate: 0.04 });

  // Hindlegs (`leg_back_*_l/r` — true equine gaskin + hock + cannon!)
  m.pair("leg_back_thigh_l", "hind_gaskin", [2.2, 11.2, 8.4], [4, 10, 4], { rotation: [-7, 0, 0], inflate: 0.1 });
  m.pair("leg_back_thigh_l", "hind_hock", [2.5, 9.6, 9.8], [3, 3, 3], { inflate: 0.2 });
  m.pair("leg_back_cannon_l", "hind_cannon", [3.0, 3.8, 10.0], [2, 7, 2], { rotation: [4, 0, 0], inflate: 0.18 });
  m.pair("leg_back_hoof_l", "hind_fetlock", [3.0, 2.4, 9.7], [2, 2, 2], { inflate: 0.26 });
  m.pair("leg_back_hoof_l", "fetlock_feather_b", [3.0, 1.0, 11.2], [2, 3, 3], {
    origin: [4.0, 3.5, 11.5],
    rotation: [28, 0, 0],
    inflate: 0.08,
  });
  m.pair("leg_back_hoof_l", "hind_pastern", [3.0, 1.2, 9.4], [2, 2, 2], {
    origin: [4.0, 2.5, 10.4],
    rotation: [-14, 0, 0],
    inflate: 0.16,
  });
  m.pair("leg_back_hoof_l", "hind_hoof", [2.5, 0.0, 9.0], [3, 2, 3], { inflate: 0.04 });

  // ---------------- MAGNIFICENT 3-JOINT FEATHERED WINGS (56 Wing Cuboids + Feather-Fan Sub-Bones!) ----------------
  // Segment 1: Inner Humerus, Shoulder Coverts & Articulated Inner Feather Fan (`wing_1_feathers_l/r`)
  m.pair("wing_1_l", "wing_spar_1", [6.5, 21.6, -8.4], [12, 2, 3], { inflate: 0.10 });
  m.pair("wing_1_l", "covert_shoulder_cap", [6.5, 22.0, -8.1], [7, 2, 4], { rotation: [-6, 0, 0], inflate: 0.10 });
  for (let i = 0; i < 4; i++) {
    const x = 6.9 + i * 2.7;
    m.pair("wing_1_feathers_l", `covert_inner_${i}`, [x, 21.6, -6.4], [3, 2, 6 + (i % 2)], {
      origin: [x + 1.3, 22.5, -3.5],
      rotation: [-5, -3, 0],
      inflate: 0.06,
    });
    m.pair("wing_1_feathers_l", `secondary_inner_${i}`, [x, 21.2, -2.5], [3, 1, 9 + i], {
      origin: [x + 1.3, 21.6, 2.0],
      rotation: [-4, -4, 0],
      inflate: 0.05,
    });
  }

  // Segment 2: Forearm Spar, Alula Thumb Tuft & Articulated Forearm Feather Fan (`wing_2_feathers_l/r`)
  m.pair("wing_2_l", "wing_spar_2", [17.6, 21.6, -8.4], [14, 2, 3], { inflate: 0.10 });
  m.pair("wing_2_l", "alula_tuft", [28.8, 22.0, -9.8], [5, 1, 3], {
    origin: [29.3, 22.5, -8.5],
    rotation: [4, 14, 6],
    inflate: 0.06,
  });
  for (let i = 0; i < 5; i++) {
    const x = 18.2 + i * 2.6;
    m.pair("wing_2_feathers_l", `covert_mid_${i}`, [x, 21.6, -6.4], [3, 2, 7], {
      origin: [x + 1.3, 22.5, -3.0],
      rotation: [-4, -4, 0],
      inflate: 0.06,
    });
  }
  for (let i = 0; i < 5; i++) {
    const x = 18.2 + i * 2.6;
    const len = 11 + (i < 3 ? i : 4 - i);
    m.pair("wing_2_feathers_l", `secondary_mid_${i}`, [x, 21.2, -2.0], [3, 1, len], {
      origin: [x + 1.3, 21.6, 3.5],
      rotation: [-3, -5 - i * 1.5, 0],
      inflate: 0.05,
    });
  }

  // Segment 3: Outer Hand Spar, Primary Coverts & 8 Slotted Eagle/Swan Primary Flight Feathers!
  m.pair("wing_3_l", "wing_spar_3", [30.8, 21.6, -8.3], [11, 2, 2], {
    origin: [36.3, 22.6, -7.3],
    rotation: [0, -8, 0],
    inflate: 0.08,
  });
  for (let i = 0; i < 3; i++) {
    const x = 31.3 + i * 3.1;
    m.pair("wing_3_l", `covert_outer_${i}`, [x, 21.5, -7.0], [4, 1, 6], {
      origin: [x + 2.0, 22.0, -4.0],
      rotation: [-3, -10 - i * 4, 0],
      inflate: 0.08,
    });
  }
  // 8 Graduated & Fanned Primary Remiges (outer 4 on `wing_3_l`, inner 4 on `wing_3_fan_l` so they scissor neatly when folded!)
  const primaries = [
    // [xPivot, zPivot, widthX, lenZ, ryDeg, rzDeg]
    [40.8, -7.7, 13, 3, -6, 2],
    [40.0, -6.9, 15, 3, -15, 1],
    [38.8, -6.1, 16, 3, -25, 0],
    [37.3, -5.3, 16, 3, -36, -1],
    [35.6, -4.7, 15, 3, -47, -2],
    [33.8, -4.3, 14, 3, -58, -2],
    [32.2, -3.9, 13, 3, -69, -3],
    [31.0, -3.5, 12, 3, -79, -3],
  ];
  primaries.forEach(([px, pz, wx, lz, ry, rz], idx) => {
    const bone = idx < 4 ? "wing_3_l" : "wing_3_fan_l";
    m.pair(bone, `primary_${idx}`, [px - 1.0, 21.2, pz - 1.0], [wx, 1, lz], {
      origin: [px, 21.7, pz],
      rotation: [-2, ry, rz],
      inflate: 0.06,
    });
  });

  // ---------------- LUXURIOUS CASCADING TAIL PLUME (`tail_1..3`) ----------------
  m.centered("tail_1", "tail_dock", 21.5, 12.8, 3, 3, 5, { inflate: 0.1 });
  m.pair("tail_1", "tail_dock_lock", [1.0, 21.0, 13.5], [2, 3, 5], { rotation: [-6, 10, -8] });

  m.centered("tail_2", "tail_plume_core", 12.2, 16.0, 4, 9, 4, { inflate: 0.12 });
  m.pair("tail_2", "tail_plume_splay", [1.2, 12.5, 16.2], [2, 8, 3], { rotation: [4, 8, -10], inflate: 0.08 });

  m.centered("tail_3", "tail_tip_core", 6.0, 17.2, 3, 7, 3, { inflate: 0.08 });
  m.pair("tail_3", "tail_tip_wisp", [0.8, 6.8, 17.5], [1, 6, 2], { rotation: [4, 6, -6], inflate: 0.06 });

  // ============================================================================
  // COMPLETE BIOMECHANICAL ANIMATION SUITE (9 Keyframe Animations in .bbmodel!)
  // ============================================================================
  // Exact zero-penetration avian Z-fold helper tracks (100% outside torso & saddle!)
  const foldedWingTracks = (breathZ = 1.2) => [
    { bone: "wing_1_l", channel: "position", keys: [[0, [1.48, -0.3, 0.4]], [1.5, [1.48, -0.1, 0.4]], [3.0, [1.48, -0.3, 0.4]]] },
    { bone: "wing_1_r", channel: "position", keys: [[0, [-1.48, -0.3, 0.4]], [1.5, [-1.48, -0.1, 0.4]], [3.0, [-1.48, -0.3, 0.4]]] },
    { bone: "wing_1_l", channel: "rotation", keys: [[0, [-9.2, -69.1, -97.2]], [1.5, [-9.2, -69.1, -97.2 + breathZ]], [3.0, [-9.2, -69.1, -97.2]]] },
    { bone: "wing_1_r", channel: "rotation", keys: [[0, [-9.2, 69.1, 97.2]], [1.5, [-9.2, 69.1, 97.2 - breathZ]], [3.0, [-9.2, 69.1, 97.2]]] },
    { bone: "wing_1_feathers_l", channel: "rotation", keys: [[0, [0, 78, 0]], [3.0, [0, 78, 0]]] },
    { bone: "wing_1_feathers_r", channel: "rotation", keys: [[0, [0, -78, 0]], [3.0, [0, -78, 0]]] },
    { bone: "wing_2_l", channel: "rotation", keys: [[0, [0, 178, -6]], [3.0, [0, 178, -6]]] },
    { bone: "wing_2_r", channel: "rotation", keys: [[0, [0, -178, 6]], [3.0, [0, -178, 6]]] },
    { bone: "wing_2_feathers_l", channel: "rotation", keys: [[0, [0, -115, 0]], [3.0, [0, -115, 0]]] },
    { bone: "wing_2_feathers_r", channel: "rotation", keys: [[0, [0, 115, 0]], [3.0, [0, 115, 0]]] },
    { bone: "wing_3_l", channel: "rotation", keys: [[0, [-2, -157, -4]], [3.0, [-2, -157, -4]]] },
    { bone: "wing_3_r", channel: "rotation", keys: [[0, [-2, 157, 4]], [3.0, [-2, 157, 4]]] },
    { bone: "wing_3_fan_l", channel: "rotation", keys: [[0, [0, 54, 0]], [3.0, [0, 54, 0]]] },
    { bone: "wing_3_fan_r", channel: "rotation", keys: [[0, [0, -54, 0]], [3.0, [0, -54, 0]]] },
  ];

  // 1) IDLE: Standing calm breathing, ear flicks, Arabian tail sway, zero-clipping folded wings
  m.animations.push({
    name: "animation.pegasus.idle",
    length: 3.0,
    loop: "loop",
    snapping: 20,
    tracks: [
      ...foldedWingTracks(1.4),
      { bone: "neck_lower", channel: "rotation", keys: [[0, [-1.5, 0, 0]], [1.5, [1.8, 0, 0]], [3.0, [-1.5, 0, 0]]] },
      { bone: "neck_upper", channel: "rotation", keys: [[0, [1.0, 0, 0]], [1.5, [-1.2, 0, 0]], [3.0, [1.0, 0, 0]]] },
      { bone: "head", channel: "rotation", keys: [[0, [0.5, -1.5, 0]], [1.5, [-1.0, 1.5, 0]], [3.0, [0.5, -1.5, 0]]] },
      { bone: "ear_l", channel: "rotation", keys: [[0, [0, 0, 0]], [0.8, [-8, 6, 0]], [1.6, [2, 0, 0]], [3.0, [0, 0, 0]]] },
      { bone: "ear_r", channel: "rotation", keys: [[0, [0, 0, 0]], [1.4, [3, 0, 0]], [2.2, [-7, -6, 0]], [3.0, [0, 0, 0]]] },
      { bone: "tail_1", channel: "rotation", keys: [[0, [0, -4, 0]], [1.5, [2, 4, 0]], [3.0, [0, -4, 0]]] },
      { bone: "tail_2", channel: "rotation", keys: [[0, [0, 5, 0]], [1.5, [0, -5, 0]], [3.0, [0, 5, 0]]] },
      { bone: "tail_3", channel: "rotation", keys: [[0, [0, 6, 0]], [1.5, [0, -6, 0]], [3.0, [0, 6, 0]]] },
    ],
  });

  // 2) GRAZE: Lowering arched neck to nibble Elisar grass with rhythmic jaw chewing
  m.animations.push({
    name: "animation.pegasus.graze",
    length: 3.0,
    loop: "loop",
    snapping: 20,
    tracks: [
      ...foldedWingTracks(0.8),
      { bone: "neck_lower", channel: "rotation", keys: [[0, [0, 0, 0]], [0.8, [-38, 0, 0]], [2.2, [-40, 2, 0]], [3.0, [0, 0, 0]]] },
      { bone: "neck_upper", channel: "rotation", keys: [[0, [0, 0, 0]], [0.8, [-24, 0, 0]], [2.2, [-26, -2, 0]], [3.0, [0, 0, 0]]] },
      { bone: "head", channel: "rotation", keys: [[0, [0, 0, 0]], [0.8, [22, 0, 0]], [1.5, [18, 0, 0]], [2.2, [22, 0, 0]], [3.0, [0, 0, 0]]] },
      { bone: "jaw", channel: "rotation", keys: [[0, [0, 0, 0]], [1.0, [-8, 0, 0]], [1.3, [-1, 0, 0]], [1.6, [-9, 0, 0]], [1.9, [-1, 0, 0]], [3.0, [0, 0, 0]]] },
      { bone: "tail_1", channel: "rotation", keys: [[0, [0, -5, 0]], [1.5, [4, 6, 0]], [3.0, [0, -5, 0]]] },
    ],
  });

  // 3) WALK: Authentic 4-beat equine walk with knee/cannon/fetlock flexion & 2x head-neck bob
  const foldStatic1 = (tEnd) => [
    { bone: "wing_1_l", channel: "position", keys: [[0, [1.48, -0.3, 0.4]], [tEnd, [1.48, -0.3, 0.4]]] },
    { bone: "wing_1_r", channel: "position", keys: [[0, [-1.48, -0.3, 0.4]], [tEnd, [-1.48, -0.3, 0.4]]] },
    { bone: "wing_1_l", channel: "rotation", keys: [[0, [-9.2, -69.1, -97.2]], [tEnd / 2, [-9.2, -69.1, -95.0]], [tEnd, [-9.2, -69.1, -97.2]]] },
    { bone: "wing_1_r", channel: "rotation", keys: [[0, [-9.2, 69.1, 97.2]], [tEnd / 2, [-9.2, 69.1, 95.0]], [tEnd, [-9.2, 69.1, 97.2]]] },
    { bone: "wing_1_feathers_l", channel: "rotation", keys: [[0, [0, 78, 0]], [tEnd, [0, 78, 0]]] },
    { bone: "wing_1_feathers_r", channel: "rotation", keys: [[0, [0, -78, 0]], [tEnd, [0, -78, 0]]] },
    { bone: "wing_2_l", channel: "rotation", keys: [[0, [0, 178, -6]], [tEnd, [0, 178, -6]]] },
    { bone: "wing_2_r", channel: "rotation", keys: [[0, [0, -178, 6]], [tEnd, [0, -178, 6]]] },
    { bone: "wing_2_feathers_l", channel: "rotation", keys: [[0, [0, -115, 0]], [tEnd, [0, -115, 0]]] },
    { bone: "wing_2_feathers_r", channel: "rotation", keys: [[0, [0, 115, 0]], [tEnd, [0, 115, 0]]] },
    { bone: "wing_3_l", channel: "rotation", keys: [[0, [-2, -157, -4]], [tEnd, [-2, -157, -4]]] },
    { bone: "wing_3_r", channel: "rotation", keys: [[0, [-2, 157, 4]], [tEnd, [-2, 157, 4]]] },
    { bone: "wing_3_fan_l", channel: "rotation", keys: [[0, [0, 54, 0]], [tEnd, [0, 54, 0]]] },
    { bone: "wing_3_fan_r", channel: "rotation", keys: [[0, [0, -54, 0]], [tEnd, [0, -54, 0]]] },
  ];

  m.animations.push({
    name: "animation.pegasus.walk",
    length: 1.6,
    loop: "loop",
    snapping: 20,
    tracks: [
      ...foldStatic1(1.6),
      { bone: "neck_lower", channel: "rotation", keys: [[0, [-3, 0, 0]], [0.4, [3, 0, 0]], [0.8, [-3, 0, 0]], [1.2, [3, 0, 0]], [1.6, [-3, 0, 0]]] },
      { bone: "head", channel: "rotation", keys: [[0, [3, 0, 0]], [0.4, [-3, 0, 0]], [0.8, [3, 0, 0]], [1.2, [-3, 0, 0]], [1.6, [3, 0, 0]]] },
      { bone: "leg_front_upper_l", channel: "rotation", keys: [[0, [26, 0, 0]], [0.4, [0, 0, 0]], [0.8, [-26, 0, 0]], [1.2, [0, 0, 0]], [1.6, [26, 0, 0]]] },
      { bone: "leg_front_cannon_l", channel: "rotation", keys: [[0, [0, 0, 0]], [0.8, [0, 0, 0]], [1.2, [28, 0, 0]], [1.6, [0, 0, 0]]] },
      { bone: "leg_front_hoof_l", channel: "rotation", keys: [[0, [-8, 0, 0]], [0.8, [10, 0, 0]], [1.2, [16, 0, 0]], [1.6, [-8, 0, 0]]] },
      { bone: "leg_front_upper_r", channel: "rotation", keys: [[0, [-26, 0, 0]], [0.4, [0, 0, 0]], [0.8, [26, 0, 0]], [1.2, [0, 0, 0]], [1.6, [-26, 0, 0]]] },
      { bone: "leg_front_cannon_r", channel: "rotation", keys: [[0, [0, 0, 0]], [0.4, [28, 0, 0]], [0.8, [0, 0, 0]], [1.6, [0, 0, 0]]] },
      { bone: "leg_front_hoof_r", channel: "rotation", keys: [[0, [10, 0, 0]], [0.4, [16, 0, 0]], [0.8, [-8, 0, 0]], [1.6, [10, 0, 0]]] },
      { bone: "leg_back_thigh_l", channel: "rotation", keys: [[0, [-24, 0, 0]], [0.4, [0, 0, 0]], [0.8, [24, 0, 0]], [1.2, [0, 0, 0]], [1.6, [-24, 0, 0]]] },
      { bone: "leg_back_cannon_l", channel: "rotation", keys: [[0, [0, 0, 0]], [0.4, [-22, 0, 0]], [0.8, [0, 0, 0]], [1.6, [0, 0, 0]]] },
      { bone: "leg_back_thigh_r", channel: "rotation", keys: [[0, [24, 0, 0]], [0.4, [0, 0, 0]], [0.8, [-24, 0, 0]], [1.2, [0, 0, 0]], [1.6, [24, 0, 0]]] },
      { bone: "leg_back_cannon_r", channel: "rotation", keys: [[0, [0, 0, 0]], [0.8, [0, 0, 0]], [1.2, [-22, 0, 0]], [1.6, [0, 0, 0]]] },
      { bone: "tail_1", channel: "rotation", keys: [[0, [-6, -5, 0]], [0.8, [-4, 5, 0]], [1.6, [-6, -5, 0]]] },
      { bone: "tail_2", channel: "rotation", keys: [[0, [4, 7, 0]], [0.8, [4, -7, 0]], [1.6, [4, 7, 0]]] },
    ],
  });

  // 4) GALLOP: Asymmetric 3-beat equine canter/gallop with suspension phase & streaming tail
  m.animations.push({
    name: "animation.pegasus.gallop",
    length: 0.9,
    loop: "loop",
    snapping: 20,
    tracks: [
      ...foldStatic1(0.9),
      { bone: "body", channel: "position", keys: [[0, [0, 1.0, 0]], [0.45, [0, -0.5, 0]], [0.9, [0, 1.0, 0]]] },
      { bone: "body", channel: "rotation", keys: [[0, [-5, 0, 0]], [0.45, [5, 0, 0]], [0.9, [-5, 0, 0]]] },
      { bone: "neck_lower", channel: "rotation", keys: [[0, [-10, 0, 0]], [0.45, [8, 0, 0]], [0.9, [-10, 0, 0]]] },
      { bone: "head", channel: "rotation", keys: [[0, [8, 0, 0]], [0.45, [-6, 0, 0]], [0.9, [8, 0, 0]]] },
      { bone: "leg_front_upper_l", channel: "rotation", keys: [[0, [38, 0, 0]], [0.45, [-34, 0, 0]], [0.9, [38, 0, 0]]] },
      { bone: "leg_front_cannon_l", channel: "rotation", keys: [[0, [8, 0, 0]], [0.65, [42, 0, 0]], [0.9, [8, 0, 0]]] },
      { bone: "leg_front_upper_r", channel: "rotation", keys: [[0, [22, 0, 0]], [0.45, [-38, 0, 0]], [0.65, [10, 0, 0]], [0.9, [22, 0, 0]]] },
      { bone: "leg_front_cannon_r", channel: "rotation", keys: [[0, [18, 0, 0]], [0.45, [36, 0, 0]], [0.9, [18, 0, 0]]] },
      { bone: "leg_back_thigh_l", channel: "rotation", keys: [[0, [-34, 0, 0]], [0.45, [34, 0, 0]], [0.9, [-34, 0, 0]]] },
      { bone: "leg_back_cannon_l", channel: "rotation", keys: [[0, [-26, 0, 0]], [0.45, [4, 0, 0]], [0.9, [-26, 0, 0]]] },
      { bone: "leg_back_thigh_r", channel: "rotation", keys: [[0, [-20, 0, 0]], [0.45, [38, 0, 0]], [0.9, [-20, 0, 0]]] },
      { bone: "leg_back_cannon_r", channel: "rotation", keys: [[0, [-18, 0, 0]], [0.45, [6, 0, 0]], [0.9, [-18, 0, 0]]] },
      { bone: "tail_1", channel: "rotation", keys: [[0, [-22, 0, 0]], [0.45, [-12, 0, 0]], [0.9, [-22, 0, 0]]] },
      { bone: "tail_2", channel: "rotation", keys: [[0, [-12, 0, 0]], [0.45, [4, 0, 0]], [0.9, [-12, 0, 0]]] },
    ],
  });

  // 5) REAR: Heroic stallion rearing up on hindlegs, forelegs pawing the air, heraldic half-spread wings!
  m.animations.push({
    name: "animation.pegasus.rear",
    length: 1.8,
    loop: "once",
    snapping: 20,
    tracks: [
      { bone: "body", channel: "position", keys: [[0, [0, 0, 0]], [0.6, [0, 3.0, 2.5]], [1.2, [0, 2.8, 2.5]], [1.8, [0, 0, 0]]] },
      { bone: "body", channel: "rotation", keys: [[0, [0, 0, 0]], [0.6, [38, 0, 0]], [1.2, [35, 0, 0]], [1.8, [0, 0, 0]]] },
      { bone: "neck_lower", channel: "rotation", keys: [[0, [0, 0, 0]], [0.6, [-18, 0, 0]], [1.2, [-14, 0, 0]], [1.8, [0, 0, 0]]] },
      { bone: "head", channel: "rotation", keys: [[0, [0, 0, 0]], [0.6, [-16, 0, 0]], [1.2, [-12, 0, 0]], [1.8, [0, 0, 0]]] },
      { bone: "jaw", channel: "rotation", keys: [[0, [0, 0, 0]], [0.6, [-14, 0, 0]], [1.1, [-10, 0, 0]], [1.8, [0, 0, 0]]] },
      { bone: "leg_front_upper_l", channel: "rotation", keys: [[0, [0, 0, 0]], [0.6, [48, 0, 0]], [0.9, [28, 0, 0]], [1.2, [46, 0, 0]], [1.8, [0, 0, 0]]] },
      { bone: "leg_front_cannon_l", channel: "rotation", keys: [[0, [0, 0, 0]], [0.6, [52, 0, 0]], [0.9, [30, 0, 0]], [1.2, [48, 0, 0]], [1.8, [0, 0, 0]]] },
      { bone: "leg_front_upper_r", channel: "rotation", keys: [[0, [0, 0, 0]], [0.6, [28, 0, 0]], [0.9, [50, 0, 0]], [1.2, [30, 0, 0]], [1.8, [0, 0, 0]]] },
      { bone: "leg_front_cannon_r", channel: "rotation", keys: [[0, [0, 0, 0]], [0.6, [34, 0, 0]], [0.9, [54, 0, 0]], [1.2, [36, 0, 0]], [1.8, [0, 0, 0]]] },
      { bone: "leg_back_thigh_l", channel: "rotation", keys: [[0, [0, 0, 0]], [0.6, [-36, 0, 0]], [1.2, [-34, 0, 0]], [1.8, [0, 0, 0]]] },
      { bone: "leg_back_thigh_r", channel: "rotation", keys: [[0, [0, 0, 0]], [0.6, [-36, 0, 0]], [1.2, [-34, 0, 0]], [1.8, [0, 0, 0]]] },
      { bone: "wing_1_l", channel: "rotation", keys: [[0, [-9.2, -69.1, -97.2]], [0.7, [10, -10, 22]], [1.2, [8, -12, 18]], [1.8, [-9.2, -69.1, -97.2]]] },
      { bone: "wing_1_r", channel: "rotation", keys: [[0, [-9.2, 69.1, 97.2]], [0.7, [10, 10, -22]], [1.2, [8, 12, -18]], [1.8, [-9.2, 69.1, 97.2]]] },
      { bone: "wing_2_l", channel: "rotation", keys: [[0, [0, 178, -6]], [0.7, [4, 8, 14]], [1.2, [4, 8, 12]], [1.8, [0, 178, -6]]] },
      { bone: "wing_2_r", channel: "rotation", keys: [[0, [0, -178, 6]], [0.7, [4, -8, -14]], [1.2, [4, -8, -12]], [1.8, [0, -178, 6]]] },
      { bone: "wing_3_l", channel: "rotation", keys: [[0, [-2, -157, -4]], [0.7, [0, 4, 10]], [1.2, [0, 4, 8]], [1.8, [-2, -157, -4]]] },
      { bone: "wing_3_r", channel: "rotation", keys: [[0, [-2, 157, 4]], [0.7, [0, -4, -10]], [1.2, [0, -4, -8]], [1.8, [-2, 157, 4]]] },
    ],
  });

  // 6) TAKEOFF: Smooth 3-stage wing unfurling & leap into flight
  m.animations.push({
    name: "animation.pegasus.takeoff",
    length: 1.0,
    loop: "once",
    snapping: 20,
    tracks: [
      { bone: "wing_1_l", channel: "position", keys: [[0, [1.48, -0.3, 0.4]], [0.6, [0, 0, 0]], [1.0, [0, 0, 0]]] },
      { bone: "wing_1_r", channel: "position", keys: [[0, [-1.48, -0.3, 0.4]], [0.6, [0, 0, 0]], [1.0, [0, 0, 0]]] },
      { bone: "wing_1_l", channel: "rotation", keys: [[0, [-9.2, -69.1, -97.2]], [0.5, [8, 4, 32]], [1.0, [-4, -4, -18]]] },
      { bone: "wing_1_r", channel: "rotation", keys: [[0, [-9.2, 69.1, 97.2]], [0.5, [8, -4, -32]], [1.0, [-4, 4, 18]]] },
      { bone: "wing_1_feathers_l", channel: "rotation", keys: [[0, [0, 78, 0]], [0.5, [0, 0, 0]], [1.0, [0, 0, 0]]] },
      { bone: "wing_1_feathers_r", channel: "rotation", keys: [[0, [0, -78, 0]], [0.5, [0, 0, 0]], [1.0, [0, 0, 0]]] },
      { bone: "wing_2_l", channel: "rotation", keys: [[0, [0, 178, -6]], [0.5, [4, 0, 18]], [1.0, [-2, 0, -14]]] },
      { bone: "wing_2_r", channel: "rotation", keys: [[0, [0, -178, 6]], [0.5, [4, 0, -18]], [1.0, [-2, 0, 14]]] },
      { bone: "wing_2_feathers_l", channel: "rotation", keys: [[0, [0, -115, 0]], [0.5, [0, 0, 0]], [1.0, [0, 0, 0]]] },
      { bone: "wing_2_feathers_r", channel: "rotation", keys: [[0, [0, 115, 0]], [0.5, [0, 0, 0]], [1.0, [0, 0, 0]]] },
      { bone: "wing_3_l", channel: "rotation", keys: [[0, [-2, -157, -4]], [0.5, [0, 0, 12]], [1.0, [0, 0, -12]]] },
      { bone: "wing_3_r", channel: "rotation", keys: [[0, [-2, 157, 4]], [0.5, [0, 0, -12]], [1.0, [0, 0, 12]]] },
      { bone: "wing_3_fan_l", channel: "rotation", keys: [[0, [0, 54, 0]], [0.5, [0, 0, 0]], [1.0, [0, 0, 0]]] },
      { bone: "wing_3_fan_r", channel: "rotation", keys: [[0, [0, -54, 0]], [0.5, [0, 0, 0]], [1.0, [0, 0, 0]]] },
    ],
  });

  // 7) FLY_FLAP: Powered avian figure-8 wingbeat with wrist-flexed upstroke & tucked legs
  m.animations.push({
    name: "animation.pegasus.fly_flap",
    length: 1.6,
    loop: "loop",
    snapping: 20,
    tracks: [
      { bone: "body", channel: "position", keys: [[0, [0, -0.6, 0]], [0.8, [0, 0.8, 0]], [1.6, [0, -0.6, 0]]] },
      { bone: "wing_1_l", channel: "rotation", keys: [[0, [-4, 4, 28]], [0.8, [8, -6, -24]], [1.6, [-4, 4, 28]]] },
      { bone: "wing_2_l", channel: "rotation", keys: [[0, [-2, 8, 16]], [0.4, [4, 0, 4]], [0.8, [6, -12, -20]], [1.2, [0, 18, 8]], [1.6, [-2, 8, 16]]] },
      { bone: "wing_3_l", channel: "rotation", keys: [[0, [0, 4, 12]], [0.8, [4, -8, -18]], [1.2, [0, 16, 6]], [1.6, [0, 4, 12]]] },
      { bone: "wing_1_r", channel: "rotation", keys: [[0, [-4, -4, -28]], [0.8, [8, 6, 24]], [1.6, [-4, -4, -28]]] },
      { bone: "wing_2_r", channel: "rotation", keys: [[0, [-2, -8, -16]], [0.4, [4, 0, -4]], [0.8, [6, 12, 20]], [1.2, [0, -18, -8]], [1.6, [-2, -8, -16]]] },
      { bone: "wing_3_r", channel: "rotation", keys: [[0, [0, -4, -12]], [0.8, [4, 8, 18]], [1.2, [0, -16, -6]], [1.6, [0, -4, -12]]] },
      { bone: "neck_lower", channel: "rotation", keys: [[0, [-4, 0, 0]], [0.8, [3, 0, 0]], [1.6, [-4, 0, 0]]] },
      { bone: "leg_front_upper_l", channel: "rotation", keys: [[0, [-28, 0, 0]], [0.8, [-22, 0, 0]], [1.6, [-28, 0, 0]]] },
      { bone: "leg_front_cannon_l", channel: "rotation", keys: [[0, [40, 0, 0]], [0.8, [34, 0, 0]], [1.6, [40, 0, 0]]] },
      { bone: "leg_front_upper_r", channel: "rotation", keys: [[0, [-22, 0, 0]], [0.8, [-28, 0, 0]], [1.6, [-22, 0, 0]]] },
      { bone: "leg_front_cannon_r", channel: "rotation", keys: [[0, [34, 0, 0]], [0.8, [40, 0, 0]], [1.6, [34, 0, 0]]] },
      { bone: "leg_back_thigh_l", channel: "rotation", keys: [[0, [28, 0, 0]], [0.8, [34, 0, 0]], [1.6, [28, 0, 0]]] },
      { bone: "leg_back_cannon_l", channel: "rotation", keys: [[0, [22, 0, 0]], [0.8, [26, 0, 0]], [1.6, [22, 0, 0]]] },
      { bone: "leg_back_thigh_r", channel: "rotation", keys: [[0, [34, 0, 0]], [0.8, [28, 0, 0]], [1.6, [34, 0, 0]]] },
      { bone: "leg_back_cannon_r", channel: "rotation", keys: [[0, [26, 0, 0]], [0.8, [22, 0, 0]], [1.6, [26, 0, 0]]] },
      { bone: "tail_1", channel: "rotation", keys: [[0, [-14, -3, 0]], [0.8, [-8, 3, 0]], [1.6, [-14, -3, 0]]] },
      { bone: "tail_2", channel: "rotation", keys: [[0, [-8, 4, 0]], [0.8, [-4, -4, 0]], [1.6, [-8, 4, 0]]] },
    ],
  });

  // 8) FLY_GLIDE: Majestic outstretched soaring dihedral glide
  m.animations.push({
    name: "animation.pegasus.fly_glide",
    length: 3.0,
    loop: "loop",
    snapping: 20,
    tracks: [
      { bone: "wing_1_l", channel: "rotation", keys: [[0, [3, 5, 14]], [1.5, [4, 6, 18]], [3.0, [3, 5, 14]]] },
      { bone: "wing_2_l", channel: "rotation", keys: [[0, [2, 3, 8]], [1.5, [2, 4, 11]], [3.0, [2, 3, 8]]] },
      { bone: "wing_3_l", channel: "rotation", keys: [[0, [0, 2, 6]], [1.5, [1, 2, 10]], [3.0, [0, 2, 6]]] },
      { bone: "wing_1_r", channel: "rotation", keys: [[0, [3, -5, -14]], [1.5, [4, -6, -18]], [3.0, [3, -5, -14]]] },
      { bone: "wing_2_r", channel: "rotation", keys: [[0, [2, -3, -8]], [1.5, [2, -4, -11]], [3.0, [2, -3, -8]]] },
      { bone: "wing_3_r", channel: "rotation", keys: [[0, [0, -2, -6]], [1.5, [1, -2, -10]], [3.0, [0, -2, -6]]] },
      { bone: "leg_front_upper_l", channel: "rotation", keys: [[0, [-30, 0, 0]], [3.0, [-30, 0, 0]]] },
      { bone: "leg_front_cannon_l", channel: "rotation", keys: [[0, [42, 0, 0]], [3.0, [42, 0, 0]]] },
      { bone: "leg_front_upper_r", channel: "rotation", keys: [[0, [-26, 0, 0]], [3.0, [-26, 0, 0]]] },
      { bone: "leg_front_cannon_r", channel: "rotation", keys: [[0, [38, 0, 0]], [3.0, [38, 0, 0]]] },
      { bone: "leg_back_thigh_l", channel: "rotation", keys: [[0, [32, 0, 0]], [3.0, [32, 0, 0]]] },
      { bone: "leg_back_cannon_l", channel: "rotation", keys: [[0, [24, 0, 0]], [3.0, [24, 0, 0]]] },
      { bone: "leg_back_thigh_r", channel: "rotation", keys: [[0, [36, 0, 0]], [3.0, [36, 0, 0]]] },
      { bone: "leg_back_cannon_r", channel: "rotation", keys: [[0, [26, 0, 0]], [3.0, [26, 0, 0]]] },
      { bone: "tail_1", channel: "rotation", keys: [[0, [-16, -3, 0]], [1.5, [-14, 3, 0]], [3.0, [-16, -3, 0]]] },
    ],
  });

  // 9) FLY_HOVER: Upright aerial hover with deep sculling wingbeats & balance tail
  m.animations.push({
    name: "animation.pegasus.fly_hover",
    length: 1.2,
    loop: "loop",
    snapping: 20,
    tracks: [
      { bone: "body", channel: "rotation", keys: [[0, [14, 0, 0]], [0.6, [18, 0, 0]], [1.2, [14, 0, 0]]] },
      { bone: "wing_1_l", channel: "rotation", keys: [[0, [-14, 16, 30]], [0.6, [16, -18, -22]], [1.2, [-14, 16, 30]]] },
      { bone: "wing_2_l", channel: "rotation", keys: [[0, [-8, 10, 18]], [0.6, [10, -14, -18]], [1.2, [-8, 10, 18]]] },
      { bone: "wing_3_l", channel: "rotation", keys: [[0, [-4, 6, 14]], [0.6, [6, -10, -14]], [1.2, [-4, 6, 14]]] },
      { bone: "wing_1_r", channel: "rotation", keys: [[0, [-14, -16, -30]], [0.6, [16, 18, 22]], [1.2, [-14, -16, -30]]] },
      { bone: "wing_2_r", channel: "rotation", keys: [[0, [-8, -10, -18]], [0.6, [10, 14, 18]], [1.2, [-8, -10, -18]]] },
      { bone: "wing_3_r", channel: "rotation", keys: [[0, [-4, -6, -14]], [0.6, [6, 10, 14]], [1.2, [-4, -6, -14]]] },
      { bone: "leg_front_upper_l", channel: "rotation", keys: [[0, [-12, 0, 0]], [0.6, [-18, 0, 0]], [1.2, [-12, 0, 0]]] },
      { bone: "leg_front_cannon_l", channel: "rotation", keys: [[0, [26, 0, 0]], [0.6, [34, 0, 0]], [1.2, [26, 0, 0]]] },
      { bone: "leg_front_upper_r", channel: "rotation", keys: [[0, [-18, 0, 0]], [0.6, [-12, 0, 0]], [1.2, [-18, 0, 0]]] },
      { bone: "leg_front_cannon_r", channel: "rotation", keys: [[0, [34, 0, 0]], [0.6, [26, 0, 0]], [1.2, [34, 0, 0]]] },
      { bone: "tail_1", channel: "rotation", keys: [[0, [6, -4, 0]], [0.6, [12, 4, 0]], [1.2, [6, -4, 0]]] },
    ],
  });

  m.packUvs(2);
  return m;
}

// ============================================================================
// Synchronized Riftworld `geo/*.json` Exporter (with full rotated cubes & inflate!)
// ============================================================================
function writeRiftworldGeoJson(spec, outPath, renderScale = 1.0) {
  const cubesByGroup = new Map();
  for (const g of spec.groups) cubesByGroup.set(g.name, []);
  for (const cb of spec.cubes) {
    cubesByGroup.get(cb.parent).push(cb);
  }

  // Convert Blockbench Y-up -> Riftworld GeoModel Y-down so `GeoModel.of(...)` & `SeaProbeCommand.geoStats(...)` match 1:1
  const bones = spec.groups.map((g) => {
    const ordered = [...(cubesByGroup.get(g.name) || [])].sort((a, b) => Number(a.mirror_uv) - Number(b.mirror_uv));
    const cubes = ordered.map((cb) => {
      const [sx, sy, sz] = cb.size;
      const entry = {
        origin: [cb.from[0], -(cb.from[1] + sy), cb.from[2]],
        size: [sx, sy, sz],
        uv: cb.uv_offset,
      };
      if (cb.mirror_uv) entry.mirror = true;
      if (cb.inflate) entry.inflate = cb.inflate;
      if (cb.rotation.some((v) => Math.abs(v) > 1e-4)) {
        entry.pivot = [cb.origin[0], -cb.origin[1], cb.origin[2]];
        entry.rotation = [-cb.rotation[0], -cb.rotation[1], cb.rotation[2]];
      }
      return entry;
    });
    const boneObj = {
      name: g.name,
      pivot: [g.origin[0], -g.origin[1], g.origin[2]],
      cubes,
    };
    if (g.parent) boneObj.parent = g.parent;
    if (g.rotation.some((v) => Math.abs(v) > 1e-4)) {
      boneObj.rotation = [-g.rotation[0], -g.rotation[1], g.rotation[2]];
    }
    return boneObj;
  });

  const geo = {
    texture_width: spec.atlasSize,
    texture_height: spec.atlasSize,
    render_scale: renderScale,
    translucent: false,
    bones,
  };
  mkdirSync(dirname(outPath), { recursive: true });
  writeFileSync(outPath, JSON.stringify(geo, null, 1) + "\n", "utf-8");
}

// ============================================================================
// MCP Client Driver (`blockbench-mcp-headless` over StdioClientTransport)
// ============================================================================
async function callMcp(client, name, args) {
  const res = await client.callTool({ name, arguments: args });
  if (res.isError) {
    const txt = (res.content || []).map((c) => c.text || JSON.stringify(c)).join("\n");
    throw new Error(`MCP ${name} failed:\n${txt}`);
  }
  const txt = (res.content || []).map((c) => c.text || "").join("\n");
  try {
    return JSON.parse(txt);
  } catch {
    return txt;
  }
}

async function authorViaBlockbenchMcp(client, spec, relBbmodelPath, relTexturePath) {
  console.log(`\n=== Authoring ${spec.name} via Blockbench MCP ===`);
  let created = await callMcp(client, "bbmodel_create", {
    file: relBbmodelPath,
    format: "modded_entity",
    name: spec.name,
    box_uv: true,
    resolution: { width: spec.atlasSize, height: spec.atlasSize },
    overwrite: true,
  });
  let rev = created.revision;

  // Build operations list: groups -> cubes -> animations -> keyframes
  const ops = [
    { op: "set_model_properties", name: spec.name, model_identifier: spec.identifier },
    ...spec.groups.map((g) => ({
      op: "add_group",
      name: g.name,
      origin: g.origin,
      rotation: g.rotation,
      ...(g.parent ? { parent: g.parent } : {}),
    })),
    ...spec.cubes.map((cb) => ({
      op: "add_cube",
      name: cb.name,
      parent: cb.parent,
      from: cb.from,
      to: cb.to,
      origin: cb.origin,
      rotation: cb.rotation,
      inflate: cb.inflate,
      box_uv: true,
      uv_offset: cb.uv_offset,
      mirror_uv: cb.mirror_uv,
    })),
  ];

  for (const anim of spec.animations) {
    ops.push({
      op: "add_animation",
      name: anim.name,
      length: anim.length,
      loop: anim.loop,
      snapping: anim.snapping,
    });
    for (const tr of anim.tracks) {
      for (const [time, value] of tr.keys) {
        ops.push({
          op: "set_keyframe",
          animation: anim.name,
          bone: tr.bone,
          channel: tr.channel,
          time,
          value,
          interpolation: "catmullrom",
        });
      }
    }
  }

  // Apply in atomic chunks <= 350 operations per MCP call
  for (let i = 0; i < ops.length; i += 350) {
    const chunk = ops.slice(i, i + 350);
    const edited = await callMcp(client, "bbmodel_edit", {
      file: relBbmodelPath,
      expected_revision: rev,
      operations: chunk,
    });
    rev = edited.revision;
    if (edited.format_warnings?.length) {
      console.warn(`  format_warnings:`, edited.format_warnings);
    }
  }

  // Paint 3D surface-mapped pixel-art texture PNG from the authored .bbmodel
  execFileSync("python3", [join(HERE, "paint_blockbench_textures.py"), spec.name], {
    cwd: ROOT,
    stdio: "inherit",
  });

  // Embed painted PNG texture into the .bbmodel and assign to root group via MCP!
  const texAdded = await callMcp(client, "bbmodel_add_texture", {
    file: relBbmodelPath,
    expected_revision: rev,
    image: relTexturePath,
    name: `${spec.name}.png`,
    assign_to: ["root"],
  });
  rev = texAdded.revision;

  // Validate geometry (with self_test=true!) & animations via Blockbench MCP
  const geomVal = await callMcp(client, "bbmodel_validate", {
    file: relBbmodelPath,
    self_test: true,
    interpenetration_depth: 6.0,
    material_key: "texture",
  });
  const animVal = await callMcp(client, "bbmodel_validate_animations", {
    file: relBbmodelPath,
    sink_tolerance: 3.0,
    detach_gap: 4.0,
  });
  const info = await callMcp(client, "bbmodel_info", { file: relBbmodelPath });

  // Export Bedrock .geo.json & .animation.json via Blockbench MCP
  await callMcp(client, "bbmodel_export_bedrock_geometry", {
    file: relBbmodelPath,
    output: `reports/blockbench_models/${spec.name}.geo.json`,
    identifier: `geometry.riftworld.${spec.name}`,
    overwrite: true,
  });
  await callMcp(client, "bbmodel_export_bedrock_animation", {
    file: relBbmodelPath,
    output: `reports/blockbench_models/${spec.name}.animation.json`,
    overwrite: true,
  });

  // Export Forge 1.17+ (1.20.1 Mojmap) Java EntityModel via Blockbench MCP!
  const javaExport = await callMcp(client, "bbmodel_export_modded_entity", {
    file: relBbmodelPath,
    template: "1.17",
    model_name: `${spec.name[0].toUpperCase()}${spec.name.slice(1)}BlockbenchModel`,
    entity_class: spec.name === "liopleurodon" ? "LiopleurodonEntity" : "PegasusEntity",
    flip_y: true,
  });

  console.log(`  MCP info: groups=${info.counts?.groups ?? spec.groups.length}, cubes=${info.counts?.elements ?? spec.cubes.length}`);
  console.log(`  MCP validate summary:`, JSON.stringify(geomVal.summary));
  for (const g of geomVal.gates || []) {
    if (g.violations?.length) {
      console.log(`    gate [${g.id}] (${g.severity}): ${g.violations.length} violations ->`, g.violations.slice(0, 3));
    }
  }
  console.log(`  MCP animation summary:`, JSON.stringify(animVal.summary));
  console.log(`  MCP Java export: ${javaExport.class_name} (${javaExport.code.split("\n").length} lines, notes=${javaExport.notes.length})`);

  // Also write synchronized Riftworld GeoModel JSON
  writeRiftworldGeoJson(spec, join(ROOT, `mod/src/main/resources/assets/riftworld/geo/${spec.name}.json`), 1.0);

  return { info, geomVal, animVal, javaExport };
}

async function main() {
  mkdirSync(join(ROOT, "reports/blockbench_models"), { recursive: true });

  const transport = new StdioClientTransport({
    command: "node",
    args: [
      join(MCP_REPO, "bin/blockbench-mcp-headless.mjs"),
      "--root",
      ROOT,
      "--no-web-links",
    ],
  });
  const client = new Client({ name: "riftworld-blockbench-architect", version: "1.0.0" });
  await client.connect(transport);

  try {
    const lioSpec = designLiopleurodon();
    const lioRes = await authorViaBlockbenchMcp(
      client,
      lioSpec,
      "reports/blockbench_models/liopleurodon.bbmodel",
      "mod/src/main/resources/assets/riftworld/textures/entity/liopleurodon.png",
    );

    const pegSpec = designPegasus();
    const pegRes = await authorViaBlockbenchMcp(
      client,
      pegSpec,
      "reports/blockbench_models/pegasus.bbmodel",
      "mod/src/main/resources/assets/riftworld/textures/entity/pegasus.png",
    );

    // Save raw Blockbench MCP Java exports & validation report for inspection and Java integration
    writeFileSync(
      join(ROOT, "reports/blockbench_models/LiopleurodonBlockbenchRaw.java.txt"),
      lioRes.javaExport.code,
      "utf-8",
    );
    writeFileSync(
      join(ROOT, "reports/blockbench_models/PegasusBlockbenchRaw.java.txt"),
      pegRes.javaExport.code,
      "utf-8",
    );
    writeFileSync(
      join(ROOT, "reports/blockbench_models/mcp_validation_report.json"),
      JSON.stringify(
        {
          mcp_server: "blockbench-mcp-headless 1.10.1 (jasonjgardner/blockbench-mcp-plugin)",
          liopleurodon: {
            groups: lioSpec.groups.length,
            cubes: lioSpec.cubes.length,
            atlas: lioSpec.atlasSize,
            geometry_validation: lioRes.geomVal.summary,
            self_test: lioRes.geomVal.self_test,
            animation_validation: lioRes.animVal.summary,
            java_export_notes: lioRes.javaExport.notes,
          },
          pegasus: {
            groups: pegSpec.groups.length,
            cubes: pegSpec.cubes.length,
            atlas: pegSpec.atlasSize,
            geometry_validation: pegRes.geomVal.summary,
            self_test: pegRes.geomVal.self_test,
            animation_validation: pegRes.animVal.summary,
            java_export_notes: pegRes.javaExport.notes,
          },
        },
        null,
        2,
      ) + "\n",
      "utf-8",
    );
  } finally {
    await client.close();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
