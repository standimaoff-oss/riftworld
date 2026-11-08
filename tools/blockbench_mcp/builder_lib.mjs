/**
 * Core Blockbench MCP Builder Library for Riftworld Fauna.
 *
 * Builds Hytale / Blockbench-style articulated rectangular geometry (zero voxel staircases),
 * packs non-overlapping Box UVs, exports world-space texel metadata for 3D-aware pixel-art
 * painting, drives the official `jasonjgardner/blockbench-mcp-plugin` MCP server over stdio,
 * runs all geometry & animation validation gates, and exports Bedrock + Riftworld GeoModel JSON.
 */

import fs from 'node:fs';
import path from 'node:path';
import { Client } from '/tmp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js';
import { StdioClientTransport } from '/tmp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/stdio.js';

const DEG = Math.PI / 180;
const RAD = 180 / Math.PI;

// ---------------------------- Vector & Matrix Math ----------------------------

export const vAdd = (a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]];
export const vSub = (a, b) => [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
export const vScale = (a, s) => [a[0] * s, a[1] * s, a[2] * s];
export const vDot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
export const vCross = (a, b) => [
  a[1] * b[2] - a[2] * b[1],
  a[2] * b[0] - a[0] * b[2],
  a[0] * b[1] - a[1] * b[0],
];
export const vLen = (a) => Math.hypot(a[0], a[1], a[2]);
export const vNorm = (a) => {
  const l = vLen(a);
  return l > 1e-12 ? [a[0] / l, a[1] / l, a[2] / l] : [0, 1, 0];
};
export const vLerp = (a, b, t) => [
  a[0] + (b[0] - a[0]) * t,
  a[1] + (b[1] - a[1]) * t,
  a[2] + (b[2] - a[2]) * t,
];

/** Blockbench ZYX Euler matrix (`Rz * Ry * Rx`), row-major 9-array. */
export function eulerZYX(deg) {
  const x = deg[0] * DEG, y = deg[1] * DEG, z = deg[2] * DEG;
  const cx = Math.cos(x), sx = Math.sin(x);
  const cy = Math.cos(y), sy = Math.sin(y);
  const cz = Math.cos(z), sz = Math.sin(z);
  return [
    cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx,
    sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx,
    -sy,     cy * sx,                cy * cx,
  ];
}

export function matMulVec(m, v) {
  return [
    m[0] * v[0] + m[1] * v[1] + m[2] * v[2],
    m[3] * v[0] + m[4] * v[1] + m[5] * v[2],
    m[6] * v[0] + m[7] * v[1] + m[8] * v[2],
  ];
}

/**
 * Extracts Blockbench ZYX Euler angles (in degrees) from orthonormal basis columns
 * `xAxis` (local +X), `yAxis` (local +Y), `zAxis` (local +Z, must equal cross(xAxis, yAxis)).
 */
export function eulerFromBasis(xAxis, yAxis, zAxis) {
  const m = [
    xAxis[0], yAxis[0], zAxis[0],
    xAxis[1], yAxis[1], zAxis[1],
    xAxis[2], yAxis[2], zAxis[2],
  ];
  let rx, ry, rz;
  if (Math.abs(m[6]) < 0.9999999) {
    ry = Math.asin(Math.max(-1, Math.min(1, -m[6])));
    rx = Math.atan2(m[7], m[8]);
    rz = Math.atan2(m[3], m[0]);
  } else {
    ry = m[6] < 0 ? Math.PI / 2 : -Math.PI / 2;
    rz = 0;
    rx = Math.atan2(-m[5], m[4]);
  }
  const clean = (v) => {
    const d = Math.round(v * RAD * 10000) / 10000;
    return Object.is(d, -0) ? 0 : d;
  };
  return [clean(rx), clean(ry), clean(rz)];
}

/** Rotates vector `v` around unit axis `k` by `deg` degrees (Rodrigues). */
export function rotateAroundAxis(v, k, deg) {
  if (Math.abs(deg) < 1e-9) return v;
  const rad = deg * DEG;
  const c = Math.cos(rad), s = Math.sin(rad);
  const kv = vCross(k, v);
  const kd = vDot(k, v);
  return [
    v[0] * c + kv[0] * s + k[0] * kd * (1 - c),
    v[1] * c + kv[1] * s + k[1] * kd * (1 - c),
    v[2] * c + kv[2] * s + k[2] * kd * (1 - c),
  ];
}

/** Mirrors a 3D point across X = 0. */
export const mirrorPointX = (p) => [p[0] === 0 ? 0 : -p[0], p[1], p[2]];

/** Mirrors Blockbench ZYX Euler angles across X = 0. */
export const mirrorRotX = (r) => [
  r[0],
  r[1] === 0 ? 0 : -r[1],
  r[2] === 0 ? 0 : -r[2],
];

/** Swaps `_l` <-> `_r` tokens in node names, matching `blockbench-mcp-plugin` gateMirror. */
export function mirrorNodeName(name) {
  return name
    .split(/([_\-. ]+)/)
    .map((tok) => {
      const low = tok.toLowerCase();
      if (low === 'l') return tok === 'L' ? 'R' : 'r';
      if (low === 'r') return tok === 'R' ? 'L' : 'l';
      if (low === 'left') return tok[0] === 'L' ? 'Right' : 'right';
      if (low === 'right') return tok[0] === 'R' ? 'Left' : 'left';
      return tok;
    })
    .join('');
}

const r4 = (n) => {
  const v = Math.round(n * 10000) / 10000;
  return Object.is(v, -0) ? 0 : v;
};
const rVec = (v) => [r4(v[0]), r4(v[1]), r4(v[2])];

// ---------------------------- Model Builder DSL ----------------------------

export class ModelBuilder {
  constructor({ id, titleRu, biome, atlasSize = 1024, renderScale = 0.5, translucent = false }) {
    this.id = id;
    this.titleRu = titleRu;
    this.biome = biome;
    this.atlasSize = atlasSize;
    this.renderScale = renderScale;
    this.translucent = translucent;
    this.groups = [];       // { name, origin, rotation, parent, riftAnim }
    this.groupMap = new Map();
    this.cubes = [];        // { name, from, to, origin, rotation, parent, mat, meta, uv_offset }
    this.animations = [];   // { name, length, loop, snapping, tracks: [{ bone, channel, keys: [{time, value, interpolation}] }] }
    this.freeElements = [];
    this.asymmetric = [];
  }

  addGroup(name, origin = [0, 0, 0], rotation = [0, 0, 0], parent = null, riftAnim = null) {
    if (this.groupMap.has(name)) {
      throw new Error(`Duplicate group name: ${name}`);
    }
    const g = {
      name,
      origin: rVec(origin),
      rotation: rVec(rotation),
      parent,
      riftAnim,
    };
    this.groups.push(g);
    this.groupMap.set(name, g);
    return name;
  }

  /**
   * Adds a rectangular cuboid with integer dimensions `[sx, sy, sz]` >= 1.
   * `center` is in local unrotated coordinates before `rotation` about `origin`.
   */
  addCenteredBox(name, {
    center,
    size,
    origin = null,
    rotation = [0, 0, 0],
    parent = null,
    mat = 'default',
    meta = {},
    inflate = 0,
  }) {
    const sx = Math.max(1, Math.round(size[0]));
    const sy = Math.max(1, Math.round(size[1]));
    const sz = Math.max(1, Math.round(size[2]));
    const from = rVec([center[0] - sx / 2, center[1] - sy / 2, center[2] - sz / 2]);
    const to = rVec([from[0] + sx, from[1] + sy, from[2] + sz]);
    const org = rVec(origin ?? center);
    const rot = rVec(rotation);
    const cube = { name, from, to, size: [sx, sy, sz], origin: org, rotation: rot, parent, mat, meta, inflate };
    this.cubes.push(cube);
    return cube;
  }

  /**
   * Places an oriented rectangular prism spanning from `pStart` to `pEnd` along local +Y,
   * with integer cross-section `[width (X), length (Y), depth (Z)]`!
   *
   * `upHint` controls which way local +Z (or +X) faces before `twistDeg` roll around the segment axis.
   * `padStart` / `padEnd` extend the integer length slightly past `pStart`/`pEnd` for airtight joint seating.
   */
  addSegmentBox(name, {
    pStart,
    pEnd,
    width,
    depth,
    upHint = [0, 0, 1],
    twistDeg = 0,
    padStart = 0,
    padEnd = 0,
    parent = null,
    mat = 'default',
    meta = {},
  }) {
    const diff = vSub(pEnd, pStart);
    const rawLen = vLen(diff);
    const yAxis = vNorm(diff);
    let hint = vNorm(upHint);
    if (Math.abs(vDot(yAxis, hint)) > 0.985) {
      hint = Math.abs(yAxis[2]) < 0.9 ? [0, 0, 1] : [1, 0, 0];
    }
    let xAxis = vNorm(vCross(yAxis, hint));
    let zAxis = vNorm(vCross(xAxis, yAxis));
    if (Math.abs(twistDeg) > 1e-6) {
      xAxis = vNorm(rotateAroundAxis(xAxis, yAxis, twistDeg));
      zAxis = vNorm(vCross(xAxis, yAxis));
    }
    const rotation = eulerFromBasis(xAxis, yAxis, zAxis);
    const sx = Math.max(1, Math.round(width));
    const sz = Math.max(1, Math.round(depth));
    const sy = Math.max(1, Math.round(rawLen + padStart + padEnd));
    const actualStartOffset = -padStart;
    const origin = rVec(pStart);
    const from = rVec([origin[0] - sx / 2, origin[1] + actualStartOffset, origin[2] - sz / 2]);
    const to = rVec([from[0] + sx, from[1] + sy, from[2] + sz]);
    const cube = { name, from, to, size: [sx, sy, sz], origin, rotation, parent, mat, meta, inflate: 0 };
    this.cubes.push(cube);
    return cube;
  }

  /**
   * Places a rectangular plate/slab centered at `center` with orthonormal tangent axes:
   * - `uAxis` -> local +X (width `sx`)
   * - `nAxis` -> local +Y (thickness `sy`, surface normal!)
   * - `vAxis` = cross(uAxis, nAxis) -> local +Z (length `sz`)
   */
  addPlate(name, {
    center,
    width,
    thickness,
    length,
    uAxis,
    nAxis,
    origin = null,
    parent = null,
    mat = 'default',
    meta = {},
  }) {
    const n = vNorm(nAxis);
    let u = vSub(uAxis, vScale(n, vDot(uAxis, n)));
    u = vNorm(u);
    const v = vNorm(vCross(u, n));
    const rotation = eulerFromBasis(u, n, v);
    return this.addCenteredBox(name, {
      center,
      size: [width, thickness, length],
      origin: origin ?? center,
      rotation,
      parent,
      mat,
      meta,
    });
  }

  /**
   * Builds a left-side (`_l`) subtree via `fn(sideHelpers)` and automatically mirrors
   * every group and cube created inside `fn` to the right side (`_r`) across `X = 0`!
   */
  addMirrored(fn) {
    const gStart = this.groups.length;
    const cStart = this.cubes.length;
    fn();
    const gEnd = this.groups.length;
    const cEnd = this.cubes.length;

    for (let i = gStart; i < gEnd; i++) {
      const g = this.groups[i];
      const mName = mirrorNodeName(g.name);
      if (mName === g.name) {
        throw new Error(`Group "${g.name}" inside addMirrored must contain _l / left token!`);
      }
      const mParent = g.parent ? mirrorNodeName(g.parent) : null;
      const actualParent = mParent && this.groupMap.has(mParent) ? mParent : g.parent;
      let mAnim = null;
      if (g.riftAnim) {
        const flipSign = g.riftAnim.axis === 'y' || g.riftAnim.axis === 'z';
        mAnim = {
          ...g.riftAnim,
          amp: flipSign && !g.riftAnim.sameSign ? -g.riftAnim.amp : g.riftAnim.amp,
          phase: g.riftAnim.phaseOffsetR !== undefined ? g.riftAnim.phase + g.riftAnim.phaseOffsetR : g.riftAnim.phase,
        };
      }
      this.addGroup(mName, mirrorPointX(g.origin), mirrorRotX(g.rotation), actualParent, mAnim);
    }

    for (let i = cStart; i < cEnd; i++) {
      const c = this.cubes[i];
      const mName = mirrorNodeName(c.name);
      if (mName === c.name) {
        throw new Error(`Cube "${c.name}" inside addMirrored must contain _l / left token!`);
      }
      const mParent = c.parent ? mirrorNodeName(c.parent) : null;
      const actualParent = mParent && this.groupMap.has(mParent) ? mParent : c.parent;
      const mFrom = rVec([-c.to[0], c.from[1], c.from[2]]);
      const mTo = rVec([-c.from[0], c.to[1], c.to[2]]);
      this.cubes.push({
        name: mName,
        from: mFrom,
        to: mTo,
        size: [...c.size],
        origin: rVec(mirrorPointX(c.origin)),
        rotation: rVec(mirrorRotX(c.rotation)),
        parent: actualParent,
        mat: c.mat,
        meta: { ...c.meta, mirroredSide: 'r' },
        inflate: c.inflate ?? 0,
      });
    }
  }

  addAnimation(name, { length = 4.0, loop = 'loop', snapping = 24, tracks = [] }) {
    this.animations.push({ name, length, loop, snapping, tracks });
  }

  // ---------------------------- Box UV Packing ----------------------------

  packBoxUvs(padding = 2) {
    const items = this.cubes.map((c, idx) => {
      const [sx, sy, sz] = c.size;
      const fw = 2 * sz + 2 * sx;
      const fh = sz + sy;
      return { idx, c, fw, fh, pw: fw + padding * 2, ph: fh + padding * 2 };
    });
    items.sort((a, b) => b.ph - a.ph || b.pw - a.pw);

    let curX = 0;
    let shelfY = 0;
    let shelfH = 0;
    for (const it of items) {
      if (curX + it.pw > this.atlasSize) {
        shelfY += shelfH;
        curX = 0;
        shelfH = 0;
      }
      if (shelfY + it.ph > this.atlasSize) {
        throw new Error(`Atlas ${this.atlasSize}x${this.atlasSize} overflowed while packing ${this.id}! Need larger atlas.`);
      }
      it.c.uv_offset = [curX + padding, shelfY + padding];
      curX += it.pw;
      shelfH = Math.max(shelfH, it.ph);
    }
    return { usedHeight: shelfY + shelfH, atlasSize: this.atlasSize };
  }

  /**
   * Exports world-transform + UV metadata so `atlas_painter.py` knows exact 3D world
   * positions & normals of every texel on every face of every cube.
   */
  exportPainterSpec(outPath) {
    // Compute group rest transforms (normally identity when groups have [0,0,0] rest rotation)
    const groupWorld = new Map();
    const getGroupTransform = (gName) => {
      if (!gName) return { m: [1, 0, 0, 0, 1, 0, 0, 0, 1], t: [0, 0, 0] };
      if (groupWorld.has(gName)) return groupWorld.get(gName);
      const g = this.groupMap.get(gName);
      const pT = getGroupTransform(g.parent);
      const mLocal = eulerZYX(g.rotation);
      const tLocal = vSub(g.origin, matMulVec(mLocal, g.origin));
      // compose(pT, local)
      const m = [
        pT.m[0]*mLocal[0] + pT.m[1]*mLocal[3] + pT.m[2]*mLocal[6],
        pT.m[0]*mLocal[1] + pT.m[1]*mLocal[4] + pT.m[2]*mLocal[7],
        pT.m[0]*mLocal[2] + pT.m[1]*mLocal[5] + pT.m[2]*mLocal[8],
        pT.m[3]*mLocal[0] + pT.m[4]*mLocal[3] + pT.m[5]*mLocal[6],
        pT.m[3]*mLocal[1] + pT.m[4]*mLocal[4] + pT.m[5]*mLocal[7],
        pT.m[3]*mLocal[2] + pT.m[4]*mLocal[5] + pT.m[5]*mLocal[8],
        pT.m[6]*mLocal[0] + pT.m[7]*mLocal[3] + pT.m[8]*mLocal[6],
        pT.m[6]*mLocal[1] + pT.m[7]*mLocal[4] + pT.m[8]*mLocal[7],
        pT.m[6]*mLocal[2] + pT.m[7]*mLocal[5] + pT.m[8]*mLocal[8],
      ];
      const t = vAdd(matMulVec(pT.m, tLocal), pT.t);
      const res = { m, t };
      groupWorld.set(gName, res);
      return res;
    };

    const cubesSpec = this.cubes.map((c) => {
      const pT = getGroupTransform(c.parent);
      const mOwn = eulerZYX(c.rotation);
      const tOwn = vSub(c.origin, matMulVec(mOwn, c.origin));
      const m = [
        pT.m[0]*mOwn[0] + pT.m[1]*mOwn[3] + pT.m[2]*mOwn[6],
        pT.m[0]*mOwn[1] + pT.m[1]*mOwn[4] + pT.m[2]*mOwn[7],
        pT.m[0]*mOwn[2] + pT.m[1]*mOwn[5] + pT.m[2]*mOwn[8],
        pT.m[3]*mOwn[0] + pT.m[4]*mOwn[3] + pT.m[5]*mOwn[6],
        pT.m[3]*mOwn[1] + pT.m[4]*mOwn[4] + pT.m[5]*mOwn[7],
        pT.m[3]*mOwn[2] + pT.m[4]*mOwn[5] + pT.m[5]*mOwn[8],
        pT.m[6]*mOwn[0] + pT.m[7]*mOwn[3] + pT.m[8]*mOwn[6],
        pT.m[6]*mOwn[1] + pT.m[7]*mOwn[4] + pT.m[8]*mOwn[7],
        pT.m[6]*mOwn[2] + pT.m[7]*mOwn[5] + pT.m[8]*mOwn[8],
      ];
      const t = vAdd(matMulVec(pT.m, tOwn), pT.t);
      return {
        name: c.name,
        from: c.from,
        to: c.to,
        size: c.size,
        uv_offset: c.uv_offset,
        mat: c.mat,
        meta: c.meta,
        world_m: m,
        world_t: t,
      };
    });

    const payload = {
      id: this.id,
      atlas_size: this.atlasSize,
      render_scale: this.renderScale,
      cubes: cubesSpec,
    };
    fs.mkdirSync(path.dirname(outPath), { recursive: true });
    fs.writeFileSync(outPath, JSON.stringify(payload));
    return payload;
  }

  /**
   * Exports Riftworld runtime `mod/src/main/resources/assets/riftworld/geo/<id>.json`.
   * Converts Blockbench Y-up into Minecraft ModelPart Y-down:
   * - Y_mc = -Y_bb
   * - Rotated cubes become child bones under their parent bone with converted rotation.
   * Note: In Blockbench ZYX (`Rz*Ry*Rx`) vs Minecraft ModelPart ZYX (`pose.mulPose(Z).mulPose(Y).mulPose(X)`):
   * Under Y-flip `S = diag(1, -1, 1)`:
   * `S * Rx(a) * S = Rx(-a)`, `S * Ry(b) * S = Ry(b)`, `S * Rz(c) * S = Rz(-c)`!
   */
  exportRiftworldGeo(geoOutPath) {
    const bonesOut = [];
    for (const g of this.groups) {
      const directCubes = [];
      for (const c of this.cubes) {
        if (c.parent !== g.name) continue;
        const isRot = c.rotation.some((v) => Math.abs(v) > 1e-5);
        if (!isRot) {
          directCubes.push({
            origin: [r4(c.from[0]), r4(-c.to[1]), r4(c.from[2])],
            size: c.size,
            uv: c.uv_offset,
          });
        }
      }
      const entry = {
        name: g.name,
        pivot: [r4(g.origin[0]), r4(-g.origin[1]), r4(g.origin[2])],
        ...(g.parent ? { parent: g.parent } : {}),
        ...(g.rotation.some((v) => Math.abs(v) > 1e-5)
          ? { rotation: [r4(-g.rotation[0]), r4(g.rotation[1]), r4(-g.rotation[2])] }
          : {}),
        ...(g.riftAnim
          ? {
              anim: {
                axis: g.riftAnim.axis,
                amp: g.riftAnim.amp,
                speed: g.riftAnim.speed,
                phase: g.riftAnim.phase ?? 0,
              },
            }
          : {}),
        cubes: directCubes,
      };
      bonesOut.push(entry);

      // Emit rotated cubes as sub-bones
      for (const c of this.cubes) {
        if (c.parent !== g.name) continue;
        const isRot = c.rotation.some((v) => Math.abs(v) > 1e-5);
        if (!isRot) continue;
        bonesOut.push({
          name: `${c.name}_bone`,
          parent: g.name,
          pivot: [r4(c.origin[0]), r4(-c.origin[1]), r4(c.origin[2])],
          rotation: [r4(-c.rotation[0]), r4(c.rotation[1]), r4(-c.rotation[2])],
          cubes: [
            {
              origin: [r4(c.from[0]), r4(-c.to[1]), r4(c.from[2])],
              size: c.size,
              uv: c.uv_offset,
            },
          ],
        });
      }
    }

    const geo = {
      texture_width: this.atlasSize,
      texture_height: this.atlasSize,
      render_scale: this.renderScale,
      translucent: this.translucent,
      bones: bonesOut,
    };
    fs.mkdirSync(path.dirname(geoOutPath), { recursive: true });
    fs.writeFileSync(geoOutPath, JSON.stringify(geo, null, 2) + '\n');
    return geo;
  }
}

// ---------------------------- Official Blockbench MCP Client ----------------------------

export class BlockbenchMcpSession {
  constructor(workspaceRoot) {
    this.workspaceRoot = workspaceRoot;
    this.client = null;
  }

  async connect() {
    const bunBin = '/tmp/bb-tools/node_modules/.bin/bun';
    const serverEntry = '/tmp/blockbench-mcp-plugin/headless/index.ts';
    this.client = new Client({ name: 'riftworld-blockbench-mcp-agent', version: '1.0.0' });
    const transport = new StdioClientTransport({
      command: bunBin,
      args: ['run', serverEntry, '--root', this.workspaceRoot, '--no-web-links'],
      stderr: 'pipe',
    });
    await this.client.connect(transport);
    return this;
  }

  async call(toolName, args) {
    const res = await this.client.callTool({ name: toolName, arguments: args }, undefined, { timeout: 300_000 });
    const text = res.content
      .filter((p) => p.type === 'text')
      .map((p) => p.text)
      .join('\n');
    if (res.isError) {
      throw new Error(`MCP tool ${toolName} failed: ${text}`);
    }
    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }

  async buildAndValidateModel(builder, {
    bbmodelRelPath,
    textureRelPath,
    bedrockGeoRelPath,
    bedrockAnimRelPath,
  }) {
    // 1. Create empty .bbmodel via Blockbench MCP `bbmodel_create`
    let state = await this.call('bbmodel_create', {
      file: bbmodelRelPath,
      format: 'bedrock',
      name: builder.id,
      box_uv: true,
      resolution: { width: builder.atlasSize, height: builder.atlasSize },
      overwrite: true,
    });
    let revision = state.revision;

    // 2. Apply model properties + groups + cubes + animations in batches <= 400 ops via `bbmodel_edit`
    const allOps = [
      {
        op: 'set_model_properties',
        name: builder.id,
        model_identifier: `riftworld.${builder.id}`,
        resolution: { width: builder.atlasSize, height: builder.atlasSize },
      },
      ...builder.groups.map((g) => ({
        op: 'add_group',
        name: g.name,
        origin: g.origin,
        rotation: g.rotation,
        ...(g.parent ? { parent: g.parent } : {}),
      })),
      ...builder.cubes.map((c) => ({
        op: 'add_cube',
        name: c.name,
        from: c.from,
        to: c.to,
        origin: c.origin,
        rotation: c.rotation,
        ...(c.inflate ? { inflate: c.inflate } : {}),
        ...(c.parent ? { parent: c.parent } : {}),
        box_uv: true,
        uv_offset: c.uv_offset,
        mirror_uv: false,
      })),
    ];

    for (const anim of builder.animations) {
      allOps.push({
        op: 'add_animation',
        name: anim.name,
        length: anim.length,
        loop: anim.loop,
        snapping: anim.snapping,
      });
      for (const track of anim.tracks) {
        for (const key of track.keys) {
          allOps.push({
            op: 'set_keyframe',
            animation: anim.name,
            bone: track.bone,
            channel: track.channel,
            time: key.time,
            value: key.value,
            interpolation: key.interpolation ?? 'catmullrom',
          });
        }
      }
    }

    for (let i = 0; i < allOps.length; i += 350) {
      const chunk = allOps.slice(i, i + 350);
      const editRes = await this.call('bbmodel_edit', {
        file: bbmodelRelPath,
        expected_revision: revision,
        operations: chunk,
      });
      revision = editRes.revision;
    }

    // 3. Embed texture via `bbmodel_add_texture` and assign to all cubes
    const topGroupNames = builder.groups.filter((g) => !g.parent).map((g) => g.name);
    const texRes = await this.call('bbmodel_add_texture', {
      file: bbmodelRelPath,
      expected_revision: revision,
      image: textureRelPath,
      name: `${builder.id}.png`,
      assign_to: topGroupNames,
    });
    revision = texRes.revision;

    // 4. Validate geometry via `bbmodel_validate` (including self_test!)
    const geomVal = await this.call('bbmodel_validate', {
      file: bbmodelRelPath,
      free_elements: builder.freeElements,
      asymmetric: builder.asymmetric,
      material_key: 'texture',
      self_test: true,
    });

    // 5. Validate animations via `bbmodel_validate_animations`
    const animVal = await this.call('bbmodel_validate_animations', {
      file: bbmodelRelPath,
      sample_rate: 24,
      sink_tolerance: 0.8,
      detach_gap: 1.5,
    });

    // 6. Export Bedrock Geometry & Animation via Blockbench MCP
    if (bedrockGeoRelPath) {
      await this.call('bbmodel_export_bedrock_geometry', {
        file: bbmodelRelPath,
        output: bedrockGeoRelPath,
        identifier: `riftworld.${builder.id}`,
        overwrite: true,
      });
    }
    if (bedrockAnimRelPath && builder.animations.length > 0) {
      await this.call('bbmodel_export_bedrock_animation', {
        file: bbmodelRelPath,
        output: bedrockAnimRelPath,
        overwrite: true,
      });
    }

    // 7. Fetch model summary via `bbmodel_info`
    const info = await this.call('bbmodel_info', { file: bbmodelRelPath });

    return { info, geometry_validation: geomVal, animation_validation: animVal };
  }

  async close() {
    if (this.client) {
      await this.client.close();
      this.client = null;
    }
  }
}
