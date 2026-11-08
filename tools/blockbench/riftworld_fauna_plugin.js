/* Riftworld Fauna — articulated plate Blockbench-API batch, revision 3.
 *
 * Builds editable Bedrock-entity projects through the public Blockbench API
 * (Group / Cube / Texture / Animation / Codecs). Geometry is authored at 64
 * units per Minecraft block and exported with render_scale 0.25, so the custom
 * Riftworld integer-cube loader keeps working while the visible voxel step is
 * 1/32 block instead of the previous 1/16 block.
 *
 * Authoring space: Blockbench internal, Y up, ground y=0, nose toward -Z.
 * Runtime space: Riftworld GeoModel, Y down, ground y=0, nose toward -Z.
 */
(function (global) {
  'use strict';

  const ATLAS = 2048;
  const LOOP = 2.0;
  const UNITS_PER_BLOCK = 64;
  const RENDER_SCALE = 16 / UNITS_PER_BLOCK;

  const MATERIALS = {
    porcelain:    { base: [243, 240, 233], noise: 3,  edge: -4, pattern: 'ceramic' },
    porcelain_hi: { base: [252, 250, 246], noise: 2,  edge: -3, pattern: 'ceramic' },
    porcelain_lo: { base: [219, 216, 208], noise: 3,  edge: -4, pattern: 'ceramic' },
    celadon:      { base: [151, 203, 188], noise: 4,  edge: -5, pattern: 'feather' },
    celadon_hi:   { base: [185, 224, 211], noise: 3,  edge: -4, pattern: 'feather' },
    celadon_lo:   { base: [112, 169, 156], noise: 4,  edge: -5, pattern: 'feather' },
    gold:         { base: [196, 157, 66],  noise: 5,  edge: -6, pattern: 'metal' },
    gold_hi:      { base: [232, 197, 106], noise: 4,  edge: -4, pattern: 'metal' },
    ivory:        { base: [232, 226, 211], noise: 3,  edge: -4, pattern: 'ceramic' },
    basalt:       { base: [42, 40, 43],    noise: 6,  edge: -6, pattern: 'basalt' },
    basalt_hi:    { base: [61, 58, 62],    noise: 6,  edge: -6, pattern: 'basalt' },
    basalt_lo:    { base: [28, 27, 30],    noise: 5,  edge: -5, pattern: 'basalt' },
    lava:         { base: [255, 121, 22],  noise: 12, edge: -4, pattern: 'lava' },
    lava_hi:      { base: [255, 190, 64],  noise: 10, edge: -3, pattern: 'lava' },
    ember:        { base: [255, 145, 38],  noise: 9,  edge: -4, pattern: 'lava' },
    ash:          { base: [158, 156, 151], noise: 6,  edge: -5, pattern: 'fuzz' },
    ash_hi:       { base: [184, 181, 175], noise: 5,  edge: -4, pattern: 'fuzz' },
    ash_dark:     { base: [105, 103, 100], noise: 5,  edge: -5, pattern: 'fuzz' },
    wing:         { base: [172, 169, 163], noise: 5,  edge: -4, pattern: 'wing' },
    wing_hi:      { base: [193, 190, 183], noise: 4,  edge: -3, pattern: 'wing' },
    wing_lo:      { base: [135, 132, 127], noise: 5,  edge: -4, pattern: 'wing' },
    eyespot:      { base: [164, 161, 155], noise: 4,  edge: -4, pattern: 'eyespot' },
    violet:       { base: [137, 96, 184],  noise: 6,  edge: -5, pattern: 'fur' },
    violet_hi:    { base: [172, 132, 214], noise: 5,  edge: -4, pattern: 'fur' },
    violet_lo:    { base: [104, 68, 150],  noise: 5,  edge: -5, pattern: 'fur' },
    lavender:     { base: [190, 160, 224], noise: 5,  edge: -4, pattern: 'fur' },
    white:        { base: [241, 237, 228], noise: 3,  edge: -4, pattern: 'fur' },
    white_hi:     { base: [252, 250, 245], noise: 2,  edge: -3, pattern: 'fur' },
    crystal:      { base: [128, 74, 190],  noise: 6,  edge: -5, pattern: 'crystal' },
    crystal_hi:   { base: [190, 139, 235], noise: 5,  edge: -4, pattern: 'crystal' },
    crystal_lo:   { base: [83, 45, 133],   noise: 5,  edge: -5, pattern: 'crystal' },
    eye:          { base: [18, 17, 21],    noise: 1,  edge: 0,  pattern: 'eye' },
    eye_light:    { base: [248, 246, 255], noise: 1,  edge: 0,  pattern: 'eye' }
  };

  function fnv(str) {
    let h = 2166136261;
    for (let i = 0; i < str.length; i++) {
      h ^= str.charCodeAt(i);
      h = Math.imul(h, 16777619);
    }
    return (h >>> 0) / 4294967295;
  }
  function clamp8(v) { return Math.max(0, Math.min(255, Math.round(v))); }
  function lerp(a, b, t) { return a + (b - a) * t; }
  function dist(a, b) { return Math.hypot(a[0] - b[0], a[1] - b[1], a[2] - b[2]); }

  class Atlas {
    constructor(size) {
      this.size = size;
      this.data = new Uint8ClampedArray(size * size * 4);
      for (let i = 3; i < this.data.length; i += 4) this.data[i] = 255;
    }
    set(x, y, r, g, b) {
      if (x < 0 || y < 0 || x >= this.size || y >= this.size) return;
      const i = (y * this.size + x) * 4;
      this.data[i] = clamp8(r);
      this.data[i + 1] = clamp8(g);
      this.data[i + 2] = clamp8(b);
      this.data[i + 3] = 255;
    }
    toDataURL() {
      const canvas = document.createElement('canvas');
      canvas.width = this.size;
      canvas.height = this.size;
      const ctx = canvas.getContext('2d');
      ctx.putImageData(new ImageData(this.data, this.size, this.size), 0, 0);
      return canvas.toDataURL('image/png');
    }
  }

  function faceRects(u, v, w, h, d) {
    return {
      up:    [u + d,         v,         w, d],
      down:  [u + d + w,     v,         w, d],
      east:  [u,             v + d,     d, h],
      north: [u + d,         v + d,     w, h],
      west:  [u + d + w,     v + d,     d, h],
      south: [u + d + w + d, v + d,     w, h]
    };
  }

  // Map a texel back to authoring-space coordinates so noise/veins/fur patches
  // continue across neighbouring cubes instead of restarting on every face.
  function worldTexel(face, lx, ly, from, to) {
    switch (face) {
      case 'up':    return [from[0] + lx + 0.5, to[1], from[2] + ly + 0.5];
      case 'down':  return [from[0] + lx + 0.5, from[1], from[2] + ly + 0.5];
      case 'north': return [from[0] + lx + 0.5, to[1] - ly - 0.5, from[2]];
      case 'south': return [from[0] + lx + 0.5, to[1] - ly - 0.5, to[2]];
      case 'east':  return [to[0], to[1] - ly - 0.5, from[2] + lx + 0.5];
      case 'west':  return [from[0], to[1] - ly - 0.5, from[2] + lx + 0.5];
      default:      return [from[0], from[1], from[2]];
    }
  }

  function paintFace(atlas, rect, matKey, seed, face, cube, extra) {
    const mat = MATERIALS[matKey] || MATERIALS.ash;
    const [x0, y0, w, h] = rect;
    for (let y = y0; y < y0 + h; y++) {
      for (let x = x0; x < x0 + w; x++) {
        const lx = x - x0, ly = y - y0;
        const p = worldTexel(face, lx, ly, cube.from, cube.to);
        const gx = Math.floor(p[0] / 2), gy = Math.floor(p[1] / 2), gz = Math.floor(p[2] / 2);
        const n = fnv(matKey + ':' + gx + ',' + gy + ',' + gz);
        let r = mat.base[0] + (n - 0.5) * 2 * mat.noise;
        let g = mat.base[1] + (n - 0.5) * 2 * mat.noise;
        let b = mat.base[2] + (n - 0.5) * 2 * mat.noise;
        const border = (lx === 0 || ly === 0 || lx === w - 1 || ly === h - 1);
        if (border && w > 2 && h > 2) { r += mat.edge; g += mat.edge; b += mat.edge; }
        switch (mat.pattern) {
          case 'ceramic':
            if ((Math.floor(p[0] / 8) + Math.floor(p[1] / 8) + Math.floor(p[2] / 8)) % 9 === 0) {
              r -= 7; g -= 7; b -= 6;
            }
            break;
          case 'metal':
            if ((Math.floor(p[0] / 2) + Math.floor(p[1] / 2) + Math.floor(p[2] / 2)) % 5 === 0) {
              r += 22; g += 18; b += 8;
            }
            break;
          case 'feather': {
            const stripe = Math.floor((p[2] + p[0] * 0.35) / 4) % 4;
            if (stripe === 0) { r -= 12; g -= 12; b -= 10; }
            if (stripe === 1) { r += 8; g += 8; b += 7; }
            break;
          }
          case 'basalt': {
            const block = fnv(matKey + ':block:' + Math.floor(p[0] / 4) + ',' + Math.floor(p[1] / 4) + ',' + Math.floor(p[2] / 4));
            const delta = (block - 0.5) * 22;
            r += delta; g += delta; b += delta;
            if (block > 0.86) { r += 12; g += 12; b += 13; }
            break;
          }
          case 'lava':
            if (n > 0.7) { r = 255; g = 195 + n * 45; b = 78; }
            else if (n < 0.2) { r -= 55; g -= 42; b -= 16; }
            break;
          case 'fuzz':
            if (n > 0.62) { r += 13; g += 13; b += 11; }
            else if (n < 0.3) { r -= 11; g -= 11; b -= 10; }
            break;
          case 'wing': {
            const vein = Math.floor((p[2] + Math.abs(p[0]) * 0.22) / 5) % 5;
            if (vein === 0) { r -= 20; g -= 20; b -= 17; }
            if (vein === 1) { r += 9; g += 9; b += 8; }
            break;
          }
          case 'eyespot': {
            if (extra && extra.eyespot_plane === 'xy' && face !== 'north' && face !== 'south') {
              r = 150; g = 147; b = 141;
              break;
            }
            // Rings are painted in the wing's local XZ plane, not per cube.
            const c = extra && extra.eyespot_center ? extra.eyespot_center : [0, 0, 0];
            const vertical = extra && extra.eyespot_plane === 'xy';
            const dx = p[0] - c[0], dz = (vertical ? p[1] : p[2]) - c[vertical ? 1 : 2];
            const d = Math.hypot(dx, dz);
            const radius = extra && extra.eyespot_radius ? extra.eyespot_radius : 10;
            if (d < radius * 0.34) { r = 34; g = 33; b = 32; }
            else if (d < radius * 0.52) { r = 211; g = 207; b = 198; }
            else if (d < radius * 0.78) { r = 57; g = 55; b = 53; }
            else if (d < radius) { r = 122; g = 119; b = 114; }
            break;
          }
          case 'fur': {
            const patch = fnv(matKey + ':patch:' + Math.floor(p[0] / 4) + ',' + Math.floor(p[1] / 4) + ',' + Math.floor(p[2] / 4));
            if (patch > 0.72) { r += 20; g += 16; b += 24; }
            else if (patch < 0.24) { r -= 18; g -= 14; b -= 22; }
            break;
          }
          case 'crystal':
            if ((Math.floor(p[0] / 2) + Math.floor(p[1] / 2)) % 4 === 0) { r += 34; g += 26; b += 44; }
            if ((Math.floor(p[2] / 2) + Math.floor(p[1] / 2)) % 5 === 0) { r -= 24; g -= 18; b -= 30; }
            break;
          case 'eye':
            if (w > 2 && h > 2 && lx === 1 && ly === 1) { r = 250; g = 250; b = 255; }
            break;
        }
        atlas.set(x, y, r, g, b);
      }
    }
  }

  class Builder {
    constructor(spec) {
      this.spec = spec;
      this.id = spec.id;
      this.size = spec.atlas || ATLAS;
      this.bones = new Map();
      this.boneOrder = [];
      this.cubes = [];
      this.anims = Object.create(null);

      newProject(Formats.bedrock_old);
      Project.name = this.id;
      Project.geometry_name = this.id;
      Project.texture_width = this.size;
      Project.texture_height = this.size;
      Project.box_uv = true;
    }

    bone(name, parent, pivot, rotation) {
      if (this.bones.has(name)) throw new Error('duplicate bone ' + name);
      const parentGroup = parent ? this.bones.get(parent) : null;
      if (parent && !parentGroup) throw new Error('unknown parent ' + parent + ' for ' + name);
      const g = new Group({
        name,
        origin: pivot.slice(),
        rotation: rotation ? rotation.slice() : [0, 0, 0],
        export: true,
        visibility: true,
        isOpen: true
      });
      if (parentGroup) g.addTo(parentGroup);
      g.init();
      this.bones.set(name, g);
      this.boneOrder.push(name);
      return g;
    }

    cube(bone, name, from, to, mat, extra) {
      const g = this.bones.get(bone);
      if (!g) throw new Error('unknown bone ' + bone);
      const f = from.map(v => Math.round(v));
      const t = to.map(v => Math.round(v));
      for (let i = 0; i < 3; i++) {
        if (!Number.isInteger(f[i]) || !Number.isInteger(t[i]) || t[i] <= f[i]) {
          throw new Error('cube ' + name + ' needs integer from<to, got ' + JSON.stringify({ from: f, to: t }));
        }
      }
      const c = new Cube({
        name,
        from: f,
        to: t,
        box_uv: true,
        uv_offset: [0, 0],
        mirror_uv: false,
        inflate: 0,
        export: true,
        visibility: true
      }).addTo(g).init();
      this.cubes.push({ cube: c, mat, extra: extra || null, seed: this.id + '/' + bone + '/' + name + '/' + this.cubes.length });
      return c;
    }

    box(bone, name, center, size, mat, extra) {
      const half = size.map(v => v / 2);
      return this.cube(bone, name,
        [center[0] - half[0], center[1] - half[1], center[2] - half[2]],
        [center[0] + half[0], center[1] + half[1], center[2] + half[2]], mat, extra);
    }

    animate(bone, axis, amp, speed, phase) {
      if (!this.bones.has(bone)) throw new Error('unknown anim bone ' + bone);
      this.anims[bone] = { axis, amp, speed, phase: phase || 0 };
    }

    packUV() {
      const items = this.cubes.map(item => {
        const [w, h, d] = item.cube.size();
        return { item, w, h, d, uw: 2 * w + 2 * d, uh: h + d };
      });
      items.sort((a, b) => (b.uh - a.uh) || (b.uw - a.uw) || a.item.seed.localeCompare(b.item.seed));
      let x = 0, y = 0, rowH = 0;
      for (const it of items) {
        if (x + it.uw > this.size) { x = 0; y += rowH + 1; rowH = 0; }
        if (y + it.uh > this.size) throw new Error('atlas overflow for ' + this.id + ' at ' + it.item.seed);
        it.item.cube.uv_offset = [x, y];
        it.uv = [x, y];
        x += it.uw + 1;
        rowH = Math.max(rowH, it.uh);
      }
      this.uvItems = items;
    }

    paintAtlas() {
      const atlas = new Atlas(this.size);
      for (const it of this.uvItems) {
        const rects = faceRects(it.uv[0], it.uv[1], it.w, it.h, it.d);
        for (const face of Object.keys(rects)) {
          paintFace(atlas, rects[face], it.item.mat, it.item.seed, face, it.item.cube, it.item.extra);
        }
      }
      return atlas.toDataURL();
    }

    async addTexture(dataURL) {
      const tex = new Texture({
        name: this.id + '.png',
        width: this.size,
        height: this.size,
        uv_width: this.size,
        uv_height: this.size,
        keep_size: true
      }).fromDataURL(dataURL).add();
      for (let i = 0; i < 150; i++) {
        if (tex.width === this.size && tex.height === this.size && !tex.error) break;
        await new Promise(r => setTimeout(r, 20));
      }
      if (tex.width !== this.size || tex.height !== this.size) {
        throw new Error('texture failed to load for ' + this.id + ': ' + tex.width + 'x' + tex.height + ' error=' + tex.error);
      }
      if (tex.setAsDefaultTexture) tex.setAsDefaultTexture();
      return tex;
    }

    addAnimations() {
      const anim = new Animation({
        name: 'animation.' + this.id + '.idle',
        loop: 'loop',
        length: LOOP,
        snapping: 24
      }).add();
      const times = [0, 0.5, 1.0, 1.5, 2.0];
      for (const boneName of Object.keys(this.anims)) {
        const a = this.anims[boneName];
        if (!a.amp) continue;
        const ba = anim.getBoneAnimator(this.bones.get(boneName));
        for (const t of times) {
          const value = a.amp * Math.sin((2 * Math.PI * t) / LOOP + a.phase);
          const dp = { x: 0, y: 0, z: 0 };
          dp[a.axis] = Math.round(value * 1000) / 1000;
          ba.addKeyframe({ time: t, channel: 'rotation', data_points: [dp], interpolation: 'linear' });
        }
      }
      return anim;
    }

    toRiftworldGeo(bedrock) {
      const byName = new Map(bedrock.bones.map(b => [b.name, b]));
      const bones = [];
      for (const name of this.boneOrder) {
        const b = byName.get(name);
        if (!b) throw new Error('bedrock export missing bone ' + name);
        const out = { name };
        if (b.parent) out.parent = b.parent;
        out.pivot = [b.pivot[0], -b.pivot[1], b.pivot[2]];
        // BB -> runtime reflects both X and Y: D = diag(-1,-1,1).
        // A rotation axis transforms as D for this proper 180-degree rotation.
        // Bedrock's exported [-rx,-ry,rz] already has exactly those signs.
        if (b.rotation) out.rotation = b.rotation.slice();
        const a = this.anims[name];
        if (a && a.amp) out.anim = { axis: a.axis, amp: a.amp, speed: a.speed, phase: a.phase };
        if (b.cubes) {
          out.cubes = b.cubes.map(c => {
            const cube = {
              origin: [c.origin[0], -(c.origin[1] + c.size[1]), c.origin[2]],
              size: c.size.slice(),
              uv: c.uv.slice()
            };
            if (c.mirror) cube.mirror = true;
            if (c.inflate) cube.inflate = c.inflate;
            return cube;
          });
        }
        bones.push(out);
      }
      return {
        texture_width: this.size,
        texture_height: this.size,
        render_scale: RENDER_SCALE,
        translucent: false,
        bones
      };
    }

    async finish() {
      this.packUV();
      const dataURL = this.paintAtlas();
      await this.addTexture(dataURL);
      this.addAnimations();

      const bbmodelRaw = Codecs.project.compile();
      const bbmodel = JSON.stringify(JSON.parse(bbmodelRaw), null, 2);
      const bedrock = Codecs.bedrock_old.compile({ raw: true });
      const geo = this.toRiftworldGeo(bedrock);

      let minX = Infinity, minY = Infinity, minZ = Infinity;
      let maxX = -Infinity, maxY = -Infinity, maxZ = -Infinity;
      for (const item of this.cubes) {
        const f = item.cube.from, t = item.cube.to;
        minX = Math.min(minX, f[0]); maxX = Math.max(maxX, t[0]);
        minY = Math.min(minY, f[1]); maxY = Math.max(maxY, t[1]);
        minZ = Math.min(minZ, f[2]); maxZ = Math.max(maxZ, t[2]);
      }
      const stats = {
        id: this.id,
        title_ru: this.spec.title_ru,
        biome: this.spec.biome,
        concept: this.spec.concept,
        revision: ['porcelain_crane', 'basalt_beetle'].includes(this.id) ? 3 : 2,
        generator: 'Blockbench 5.2.2 API (Group/Cube/Texture/Animation/Codecs)',
        bones: this.bones.size,
        cubes: this.cubes.length,
        atlas: [this.size, this.size],
        units_per_block: UNITS_PER_BLOCK,
        render_scale: RENDER_SCALE,
        bounds_blockbench: { min: [minX, minY, minZ], max: [maxX, maxY, maxZ] },
        size_blocks: [
          (maxX - minX) * RENDER_SCALE / 16,
          (maxY - minY) * RENDER_SCALE / 16,
          (maxZ - minZ) * RENDER_SCALE / 16
        ],
        animations: Object.keys(this.anims).length,
        bbmodel_bytes: bbmodel.length,
        geo_bones: geo.bones.length,
        geo_cubes: geo.bones.reduce((n, b) => n + (b.cubes ? b.cubes.length : 0), 0)
      };
      return { id: this.id, title_ru: this.spec.title_ru, biome: this.spec.biome, concept: this.spec.concept, bbmodel, geo, texture: dataURL, bedrock, stats };
    }
  }

  // ---------------------------------------------------------------- helpers

  function ellipsoidSolid(b, bone, prefix, center, radii, step, mat) {
    const [cx, cy, cz] = center, [rx, ry, rz] = radii;
    let count = 0;
    for (let y = Math.ceil((cy - ry) / step) * step; y < cy + ry; y += step) {
      const yy = Math.min(1, Math.max(-1, (y + step / 2 - cy) / ry));
      const sy = Math.sqrt(Math.max(0, 1 - yy * yy));
      for (let x = Math.ceil((cx - rx * sy) / step) * step; x < cx + rx * sy; x += step) {
        const xx = Math.min(1, Math.max(-1, (x + step / 2 - cx) / (rx * sy || 1)));
        const sz = Math.sqrt(Math.max(0, 1 - xx * xx));
        const z0 = Math.round(cz - rz * sy * sz), z1 = Math.round(cz + rz * sy * sz);
        if (z1 <= z0) continue;
        const x1 = Math.min(Math.round(cx + rx * sy), x + step);
        if (x1 <= x) continue;
        b.cube(bone, prefix + '_' + count++, [x, y, z0], [x1, y + step, z1], typeof mat === 'function' ? mat(x, y, z0) : mat);
      }
    }
  }

  function ellipsoidShell(b, bone, prefix, center, radii, step, matFn, opts) {
    const options = opts || {};
    const [cx, cy, cz] = center, [rx, ry, rz] = radii;
    const i0 = Math.floor((cx - rx) / step), i1 = Math.ceil((cx + rx) / step);
    const j0 = Math.floor((cy - ry) / step), j1 = Math.ceil((cy + ry) / step);
    const k0 = Math.floor((cz - rz) / step), k1 = Math.ceil((cz + rz) / step);
    const inside = (i, j, k) => {
      const x = (i + 0.5) * step - cx, y = (j + 0.5) * step - cy, z = (k + 0.5) * step - cz;
      if (options.minY != null && (j + 0.5) * step < options.minY) return false;
      if (options.maxY != null && (j + 0.5) * step > options.maxY) return false;
      return (x * x) / (rx * rx) + (y * y) / (ry * ry) + (z * z) / (rz * rz) <= 1;
    };
    const surface = new Map();
    for (let j = j0; j < j1; j++) for (let k = k0; k < k1; k++) for (let i = i0; i < i1; i++) {
      if (!inside(i, j, k)) continue;
      const exposed = !inside(i + 1, j, k) || !inside(i - 1, j, k) || !inside(i, j + 1, k) ||
        !inside(i, j - 1, k) || !inside(i, j, k + 1) || !inside(i, j, k - 1);
      if (!exposed) continue;
      const p = [(i + 0.5) * step, (j + 0.5) * step, (k + 0.5) * step];
      const mat = typeof matFn === 'function' ? matFn(p, i, j, k) : matFn;
      if (mat) surface.set(i + ',' + j + ',' + k, { i, j, k, mat });
    }
    let count = 0;
    for (let j = j0; j < j1; j++) for (let k = k0; k < k1; k++) {
      let i = i0;
      while (i < i1) {
        const cell = surface.get(i + ',' + j + ',' + k);
        if (!cell) { i++; continue; }
        let end = i + 1;
        while (end < i1) {
          const next = surface.get(end + ',' + j + ',' + k);
          if (!next || next.mat !== cell.mat) break;
          end++;
        }
        b.cube(bone, prefix + '_' + count++, [i * step, j * step, k * step], [end * step, (j + 1) * step, (k + 1) * step], cell.mat, cell.extra);
        i = end;
      }
    }
  }

  function tubePath(b, bone, prefix, points, width, mat, step) {
    const s = step || 4;
    let count = 0;
    for (let seg = 0; seg < points.length - 1; seg++) {
      const a = points[seg], c = points[seg + 1];
      const n = Math.max(1, Math.ceil(dist(a, c) / s));
      for (let i = 0; i < n; i++) {
        const t = i / n;
        const p = [lerp(a[0], c[0], t), lerp(a[1], c[1], t), lerp(a[2], c[2], t)];
        const w = typeof width === 'function' ? width(seg + t) : width;
        b.box(bone, prefix + '_' + count++, p, [w, w, w], typeof mat === 'function' ? mat(seg + t, p) : mat);
      }
    }
    const last = points[points.length - 1];
    const w = typeof width === 'function' ? width(points.length - 1) : width;
    b.box(bone, prefix + '_end', last, [w, w, w], typeof mat === 'function' ? mat(points.length - 1, last) : mat);
  }

  function feather(b, bone, prefix, root, tip, width, thickness, mat, veinMat) {
    const length = dist(root, tip);
    const n = Math.max(4, Math.ceil(length / 2));
    for (let i = 0; i < n; i++) {
      const t = (i + 0.5) / n;
      const p = [lerp(root[0], tip[0], t), lerp(root[1], tip[1], t), lerp(root[2], tip[2], t)];
      const w = Math.max(2, Math.round(width * (1 - t * 0.78)));
      const dx = Math.abs(tip[0] - root[0]), dy = Math.abs(tip[1] - root[1]), dz = Math.abs(tip[2] - root[2]);
      let size;
      if (dz >= dx && dz >= dy) size = [w, thickness, 4];
      else if (dx >= dy) size = [4, thickness, w];
      else size = [w, 4, thickness];
      b.box(bone, prefix + '_vane_' + i, p, size, mat);
      if (veinMat && i % 2 === 0) {
        const veinSize = dz >= dx && dz >= dy ? [1, thickness + 1, 3] : dx >= dy ? [3, thickness + 1, 1] : [1, 3, thickness + 1];
        b.box(bone, prefix + '_vein_' + i, [p[0], p[1] + 0.5, p[2]], veinSize, veinMat);
      }
    }
  }

  function crystal(b, bone, prefix, base, height, width, lean) {
    const tiers = 6;
    const dx = lean ? lean[0] : 0, dz = lean ? lean[1] : 0;
    for (let i = 0; i < tiers; i++) {
      const t = i / (tiers - 1);
      const w = Math.max(1, Math.round(width * (i === tiers - 1 ? 0.22 : i === tiers - 2 ? 0.48 : 1 - t * 0.45)));
      const h = Math.max(1, Math.round(height / tiers));
      const center = [base[0] + dx * t * height, base[1] + h * (i + 0.5), base[2] + dz * t * height];
      const mat = i % 3 === 0 ? 'crystal_hi' : i % 3 === 1 ? 'crystal' : 'crystal_lo';
      b.box(bone, prefix + '_' + i, center, [w, h, w], mat);
    }
  }

  // ---------------------------------------------------------------- crane

  function buildPorcelainCrane(b) {
    b.bone('body', null, [0, 88, 2]);
    ellipsoidShell(b, 'body', 'porcelain_hull', [0, 90, 4], [19, 23, 34], 4, (p) => {
      if (p[1] > 104 && Math.abs(p[0]) < 12) return 'porcelain_hi';
      if (p[1] < 78) return 'porcelain_lo';
      return 'porcelain';
    });
    // Kintsugi seams: thin raised gold geometry, not random yellow pixels.
    tubePath(b, 'body', 'gold_seam_back', [[-8, 108, -12], [-2, 112, 0], [6, 108, 14], [10, 100, 26]], 1, 'gold', 3);
    tubePath(b, 'body', 'gold_seam_breast', [[0, 96, -28], [4, 86, -30], [2, 76, -24]], 1, 'gold', 3);
    tubePath(b, 'body', 'gold_seam_side', [[18, 94, -8], [19, 86, 4], [16, 78, 16]], 1, 'gold', 3);

    b.bone('neck_lower', 'body', [0, 102, -22]);
    const neck = [[0, 100, -20], [0, 114, -30], [0, 128, -27], [0, 142, -35], [0, 150, -40]];
    tubePath(b, 'neck_lower', 'neck_porcelain', neck, t => t < 1 ? 10 : t < 3 ? 8 : 7, 'porcelain', 3);
    tubePath(b, 'neck_lower', 'neck_gold', [[4, 104, -24], [4, 116, -31], [3, 128, -28], [3, 142, -36]], 1, 'gold', 2);
    b.bone('head', 'neck_lower', [0, 148, -40]);
    b.box('head', 'skull', [0, 152, -42], [12, 10, 16], 'porcelain_hi');
    b.box('head', 'crown', [0, 158, -42], [8, 2, 10], 'porcelain');
    b.box('head', 'beak_base', [0, 151, -53], [6, 4, 8], 'gold');
    b.box('head', 'beak_mid', [0, 150, -60], [4, 3, 7], 'gold_hi');
    b.box('head', 'beak_tip', [0, 149, -66], [2, 2, 6], 'gold');
    b.box('head', 'eye_left', [-6, 154, -45], [1, 3, 3], 'eye');
    b.box('head', 'eye_right', [6, 154, -45], [1, 3, 3], 'eye');

    // Side-view feather vanes are broad in Y and thin in X. Each is a long
    // cuboid on a rotated group, rather than disconnected voxel dots.
    for (const side of [-1, 1]) {
      const wing = side < 0 ? 'wing_left' : 'wing_right';
      b.bone(wing, 'body', [side * 17, 100, 2]);
      b.box(wing, 'shoulder', [side * 19, 99, -2], [5, 19, 23], 'porcelain_hi');
      for (let i = 0; i < 5; i++) {
        const pivot = [side * (21 + i % 2), 104 - i * 4, -10 + i * 4];
        const vane = wing + '_vane_' + i;
        const length = 35 + i * 2;
        b.bone(vane, wing, pivot, [15 + i * 3, 0, 0]);
        // Layered feathers overlap slightly; all stay outside the torso.
        b.box(vane, 'feather_plate', [pivot[0], pivot[1] - 4, pivot[2] + length / 2],
          [3, 11, length], i % 2 ? 'celadon' : 'celadon_hi');
        b.box(vane, 'gold_shaft', [pivot[0] + side * 2, pivot[1] - 3, pivot[2] + length / 2],
          [1, 1, length - 3], 'gold');
        b.box(vane, 'tip', [pivot[0], pivot[1] - 5, pivot[2] + length + 2],
          [2, 7, 5], i % 2 ? 'celadon_lo' : 'celadon');
      }
      // A short porcelain cover over the bases makes a coherent shoulder.
      b.box(wing, 'covert_upper', [side * 24, 103, 0], [3, 8, 22], 'porcelain_hi');
      b.box(wing, 'covert_lower', [side * 24, 96, 4], [3, 6, 26], 'celadon');
      b.animate(wing, 'z', 1.5, 0.8, side < 0 ? 0 : Math.PI);
    }

    b.bone('tail', 'body', [0, 88, 30]);
    // Short, broad overlapping tail fan, not a bundle of long loose strands.
    for (let i = 0; i < 5; i++) {
      const x = (i - 2) * 5;
      const pivot = [x, 87 - Math.abs(i - 2), 29];
      const vane = 'tail_vane_' + i;
      b.bone(vane, 'tail', pivot, [24 + Math.abs(i - 2) * 4, 0, 0]);
      const length = 25 - Math.abs(i - 2) * 2;
      b.box(vane, 'tail_plate', [x, pivot[1] - 3, pivot[2] + length / 2],
        [8, 5, length], i % 2 ? 'porcelain_hi' : 'celadon_hi');
      b.box(vane, 'tail_tip', [x, pivot[1] - 3, pivot[2] + length + 2],
        [6, 4, 5], i % 2 ? 'celadon' : 'celadon_lo');
      b.box(vane, 'tail_gold', [x, pivot[1] + 1, pivot[2] + length / 2], [1, 1, length - 3], 'gold');
    }

    for (const side of [-1, 1]) {
      const upper = side < 0 ? 'leg_left_upper' : 'leg_right_upper';
      const lower = side < 0 ? 'leg_left_lower' : 'leg_right_lower';
      const foot = side < 0 ? 'foot_left' : 'foot_right';
      const x = side * 8;
      b.bone(upper, 'body', [x, 72, 2]);
      b.box(upper, 'thigh', [x, 66, 2], [6, 14, 6], 'porcelain_lo');
      b.bone(lower, upper, [x, 58, 2]);
      b.box(lower, 'knee', [x, 58, 2], [5, 5, 5], 'ivory');
      b.box(lower, 'shin', [x, 32, 2], [3, 48, 3], 'ivory');
      b.bone(foot, lower, [x, 8, 2]);
      b.box(foot, 'ankle', [x, 8, 2], [4, 4, 4], 'ivory');
      b.box(foot, 'toe_front', [x, 5, -6], [2, 2, 14], 'ivory');
      b.box(foot, 'toe_left', [x - 3, 5, -3], [2, 2, 10], 'ivory');
      b.box(foot, 'toe_right', [x + 3, 5, -3], [2, 2, 10], 'ivory');
      b.box(foot, 'toe_back', [x, 5, 8], [2, 2, 6], 'ivory');
      b.animate(upper, 'x', 2, 1.2, side < 0 ? 0 : Math.PI);
    }
    b.animate('neck_lower', 'x', 2, 0.6, 0);
    b.animate('head', 'x', 2.5, 0.6, 0.5);
    b.animate('tail', 'y', 3, 0.7, 0);
  }

  // ---------------------------------------------------------------- beetle

  function nCrack(p) { return fnv('crack:' + Math.floor(p[0] / 4) + ',' + Math.floor(p[1] / 4) + ',' + Math.floor(p[2] / 4)) > 0.72; }

  function buildBasaltBeetle(b) {
    b.bone('body', null, [0, 34, 4]);
    // Faceted carapace built from large, overlapping armor plates. Taper the
    // fore/aft sections and cover the glowing core, leaving only narrow seams.
    b.box('body', 'shell_underside', [0, 42, 8], [68, 24, 76], 'basalt_lo');
    b.box('body', 'core_glow', [0, 52, 8], [64, 7, 74], 'lava');
    const rows = [
      { z: -20, width: 28, length: 24 },
      { z: 6, width: 36, length: 28 },
      { z: 34, width: 28, length: 22 }
    ];
    for (let row = 0; row < rows.length; row++) {
      const {z, width, length} = rows[row];
      b.box('body', 'central_plate_' + row, [0, 66, z], [width, 10, length], row === 1 ? 'basalt_hi' : 'basalt');
      b.box('body', 'crown_plate_' + row, [0, 72, z], [width - 7, 3, length - 4], row === 1 ? 'basalt' : 'basalt_hi');
      for (const side of [-1, 1]) {
        const pivot = [side * (width / 2 - 2), 65, z];
        const panel = 'shell_' + (side < 0 ? 'left_' : 'right_') + row;
        b.bone(panel, 'body', pivot, [0, 0, side * -27]);
        b.box(panel, 'sloped_plate', [side * (width / 2 + 8), 65, z],
          [22, 9, length], row === 1 ? 'basalt_hi' : 'basalt');
        b.box(panel, 'raised_facet', [side * (width / 2 + 8), 70, z],
          [18, 2, length - 4], row === 1 ? 'basalt' : 'basalt_hi');
        b.box('body', 'side_plate_' + side + '_' + row, [side * (width / 2 + 18), 48, z],
          [7, 15, length], row === 1 ? 'basalt' : 'basalt_lo');
      }
    }
    // Raised, thin segmented glowing cracks (not a bright orange belt).
    for (const z of [-7, 21]) {
      b.box('body', 'top_crack_' + z, [0, 74, z], [27, 1, 2], 'lava');
      for (const side of [-1, 1]) {
        const panel = 'shell_' + (side < 0 ? 'left_' : 'right_') + (z < 0 ? 0 : 1);
        const x = side * (z < 0 ? 21 : 25);
        b.box(panel, 'crack_line', [x, 71, z], [16, 1, 2], 'lava');
      }
    }
    // Thorax collar.
    ellipsoidShell(b, 'body', 'thorax', [0, 38, -38], [30, 18, 18], 4, p => p[1] > 44 ? 'basalt_hi' : 'basalt');

    b.bone('head', 'body', [0, 34, -50]);
    b.box('head', 'head_block', [0, 32, -56], [34, 20, 20], 'basalt');
    b.box('head', 'face_plate', [0, 38, -62], [26, 8, 8], 'basalt_hi');
    b.box('head', 'eye_left', [-17, 36, -60], [2, 6, 6], 'lava_hi');
    b.box('head', 'eye_right', [17, 36, -60], [2, 6, 6], 'lava_hi');
    b.box('head', 'mandible_left', [-10, 24, -66], [10, 6, 12], 'basalt_lo');
    b.box('head', 'mandible_right', [10, 24, -66], [10, 6, 12], 'basalt_lo');
    b.box('head', 'mandible_tip_left', [-13, 22, -73], [5, 4, 6], 'basalt');
    b.box('head', 'mandible_tip_right', [13, 22, -73], [5, 4, 6], 'basalt');
    // Horns occupy the forward plane; antennae start behind them. The
    // separation is kept even when the antennae sway about their pivots.
    for (const side of [-1, 1]) {
      const horn = side < 0 ? 'horn_left' : 'horn_right';
      const pivot = [side * 13, 39, -69];
      b.bone(horn, 'head', pivot, [-18, 0, 0]);
      const hornTilt = horn + '_tilt';
      b.bone(hornTilt, horn, pivot, [0, 0, side * -12]);
      b.box(hornTilt, 'horn_base', [pivot[0], 47, pivot[2]], [9, 18, 9], 'ember');
      b.box(hornTilt, 'horn_tip', [pivot[0], 58, pivot[2]], [6, 9, 6], 'lava_hi');
    }
    for (const side of [-1, 1]) {
      const ant = side < 0 ? 'antenna_left' : 'antenna_right';
      const pivot = [side * 20, 45, -42];
      b.bone(ant, 'head', pivot);
      const stem = ant + '_stem';
      b.bone(stem, ant, pivot, [0, 0, side * -30]);
      b.box(stem, 'antenna_stem', [pivot[0], pivot[1] + 13, pivot[2]], [4, 26, 4], 'basalt_lo');
      b.box(stem, 'antenna_club', [pivot[0], pivot[1] + 28, pivot[2]], [7, 7, 7], 'basalt');
      b.animate(ant, 'y', 4, 1.1, side < 0 ? 0 : Math.PI);
    }

    const legs = [
      ['front_left', -24, -34, -1], ['front_right', 24, -34, 1],
      ['middle_left', -30, 2, -1], ['middle_right', 30, 2, 1],
      ['back_left', -24, 36, -1], ['back_right', 24, 36, 1]
    ];
    for (const [name, x, z, side] of legs) {
      const hip = 'leg_' + name, knee = hip + '_lower', foot = hip + '_foot';
      b.bone(hip, 'body', [x, 30, z]);
      b.box(hip, 'coxa', [x + side * 5, 28, z], [12, 10, 12], 'basalt');
      b.bone(knee, hip, [x + side * 10, 26, z], [0, 0, side * 28]);
      b.box(knee, 'femur', [x + side * 10, 19, z], [10, 17, 11], 'basalt_hi');
      b.bone(foot, knee, [x + side * 10, 11, z], [0, 0, side * -28]);
      b.box(foot, 'tibia', [x + side * 10, 6, z], [7, 11, 8], 'basalt');
      b.box(foot, 'claw', [x + side * 10, 1, z - 3], [12, 4, 14], 'basalt_lo');
      b.animate(hip, 'x', 4, 1.4, (legs.findIndex(l => l[0] === name) % 3) * Math.PI / 3);
    }
    b.animate('head', 'x', 1.5, 0.7, 0);
  }

  // ---------------------------------------------------------------- moth

  function buildAshMoth(b) {
    b.bone('body', null, [0, 28, 0]);
    ellipsoidShell(b, 'body', 'thorax_fuzz', [0, 30, -8], [9, 9, 12], 2, p => p[1] > 32 ? 'ash_hi' : 'ash');
    ellipsoidShell(b, 'body', 'abdomen_fuzz', [0, 28, 12], [7, 7, 18], 2, p => Math.floor(p[2] / 4) % 2 ? 'ash' : 'ash_dark');
    b.box('body', 'ember_band', [0, 28, 4], [12, 2, 4], 'ember');

    b.bone('head', 'body', [0, 32, -20]);
    b.box('head', 'head', [0, 33, -22], [12, 10, 10], 'ash_hi');
    b.box('head', 'face', [0, 31, -28], [8, 6, 3], 'ash');
    b.box('head', 'eye_left', [-6, 34, -25], [2, 5, 5], 'eye');
    b.box('head', 'eye_right', [6, 34, -25], [2, 5, 5], 'eye');
    for (const side of [-1, 1]) {
      const ant = side < 0 ? 'antenna_left' : 'antenna_right';
      b.bone(ant, 'head', [side * 4, 38, -24]);
      tubePath(b, ant, 'stalk', [[side * 4, 38, -24], [side * 8, 46, -28], [side * 12, 54, -30]], 2, 'ash_dark', 3);
      for (let i = 0; i < 6; i++) {
        const t = i / 5;
        const p = [side * (5 + t * 8), 40 + t * 14, -25 - t * 5];
        b.box(ant, 'barb_outer_' + i, [p[0] + side * 3, p[1], p[2]], [5, 1, 2], 'ash_dark');
        b.box(ant, 'barb_inner_' + i, [p[0] - side * 2, p[1] + 1, p[2]], [4, 1, 2], 'ash');
      }
      b.animate(ant, 'y', 5, 1.3, side < 0 ? 0 : Math.PI);
    }

    for (const side of [-1, 1]) {
      const fore = side < 0 ? 'wing_fore_left' : 'wing_fore_right';
      const hind = side < 0 ? 'wing_hind_left' : 'wing_hind_right';
      b.bone(fore, 'body', [side * 6, 32, -8]);
      // Tapered, stepped forewing with dihedral and orange tip.
      for (let x = 6; x < 74; x += 2) {
        const t = (x - 6) / 68;
        const chord = Math.round(34 * (1 - t * 0.60));
        const zLead = Math.round(-22 + t * 12);
        const zTrail = zLead + chord;
        const y = 32 + Math.floor(t * 28 / 2) * 2;
        const mat = x > 56 ? (x > 68 ? 'lava_hi' : 'ember') : (Math.floor(x / 6) % 2 ? 'wing' : 'wing_hi');
        b.cube(fore, 'membrane_' + x, [side < 0 ? side * (x + 2) : side * x, y, Math.min(zLead, zTrail)], [side < 0 ? side * x : side * (x + 2), y + 2, Math.max(zLead, zTrail)], mat);
        if (x % 14 === 6 && x < 56) {
          b.box(fore, 'vein_' + x, [side * (x + 1), y + 2, (zLead + zTrail) / 2], [1, 1, Math.abs(zTrail - zLead) - 2], 'wing_lo');
        }
      }
      b.bone(hind, 'body', [side * 6, 28, 6]);
      const eyeCenter = [side * 27, 18, 9];
      for (let x = 6; x < 50; x += 2) {
        const t = (x - 6) / 44;
        const half = Math.round(15 * Math.sqrt(Math.max(0, 1 - Math.pow((t - 0.42) / 0.66, 2))));
        if (half < 2) continue;
        const yCenter = 20 - Math.floor(t * 6 / 2) * 2;
        const z = 8 + Math.floor(t * 4 / 2) * 2;
        b.cube(hind, 'membrane_' + x, [side < 0 ? side * (x + 2) : side * x, yCenter - half, z], [side < 0 ? side * x : side * (x + 2), yCenter + half, z + 2], 'eyespot',
          { eyespot_center: eyeCenter, eyespot_radius: 12, eyespot_plane: 'xy' });
        if (x % 14 === 6 && Math.abs(x - 27) > 13) b.box(hind, 'vein_' + x, [side * (x + 1), yCenter, z - 1], [1, half * 2 - 2, 1], 'wing_lo');
      }
      b.animate(fore, 'z', 10, 2.2, side < 0 ? 0 : Math.PI);
      b.animate(hind, 'z', 7, 2.2, side < 0 ? 0.3 : Math.PI + 0.3);
    }

    b.bone('legs', 'body', [0, 22, -6]);
    for (const side of [-1, 1]) for (let i = 0; i < 3; i++) {
      const z = -12 + i * 8;
      b.box('legs', 'leg_' + side + '_' + i, [side * 5, 18, z], [2, 8, 2], 'ash_dark');
      b.box('legs', 'foot_' + side + '_' + i, [side * 6, 14, z - 2], [3, 2, 4], 'ash_dark');
    }
    b.bone('ash_trail', 'body', [0, 20, 20]);
    const motes = [[-8, 12, 24], [6, 10, 28], [-3, 6, 32], [10, 4, 36], [-10, 2, 40], [2, -2, 44]];
    motes.forEach((p, i) => b.box('ash_trail', 'mote_' + i, p, [2 + i % 2, 2, 2], i % 2 ? 'ash' : 'ash_dark'));
    b.animate('body', 'x', 2, 1.1, 0);
    b.animate('head', 'x', 1.5, 1.1, 0.3);
    b.animate('legs', 'x', 3, 1.1, 0);
    b.animate('ash_trail', 'y', 4, 0.9, 0);
  }

  // ---------------------------------------------------------------- fox

  function buildAmethystFox(b) {
    b.bone('body', null, [0, 44, 0]);
    ellipsoidShell(b, 'body', 'torso_fur', [0, 46, 2], [16, 16, 32], 4, p => {
      if (p[1] > 54) return 'violet_hi';
      if (p[1] < 38) return 'violet_lo';
      return fnv('torso:' + Math.floor(p[0] / 4) + ',' + Math.floor(p[2] / 4)) > 0.55 ? 'lavender' : 'violet';
    });
    // White chest bib.
    ellipsoidShell(b, 'body', 'chest_bib', [0, 40, -25], [12, 12, 8], 4, p => p[1] > 42 ? 'white_hi' : 'white');
    // Shoulder and haunch muscles.
    for (const side of [-1, 1]) {
      b.box('body', 'shoulder_' + side, [side * 14, 44, -16], [8, 18, 16], 'violet_hi');
      b.box('body', 'haunch_' + side, [side * 14, 46, 20], [10, 20, 18], 'lavender');
    }

    b.bone('head', 'body', [0, 58, -28]);
    b.box('head', 'skull', [0, 62, -34], [22, 18, 20], 'violet_hi');
    b.box('head', 'brow', [0, 68, -38], [24, 6, 14], 'lavender');
    b.box('head', 'cheek_left', [-12, 58, -36], [6, 10, 12], 'white');
    b.box('head', 'cheek_right', [12, 58, -36], [6, 10, 12], 'white');
    b.box('head', 'muzzle', [0, 57, -48], [14, 8, 12], 'white_hi');
    b.box('head', 'lower_jaw', [0, 53, -46], [12, 3, 10], 'white');
    b.box('head', 'nose', [0, 60, -55], [6, 4, 3], 'violet_lo');
    b.box('head', 'eye_left', [-11, 64, -42], [2, 4, 5], 'eye_light');
    b.box('head', 'eye_right', [11, 64, -42], [2, 4, 5], 'eye_light');
    b.box('head', 'eye_socket_left', [-12, 64, -42], [1, 6, 7], 'violet_lo');
    b.box('head', 'eye_socket_right', [12, 64, -42], [1, 6, 7], 'violet_lo');
    for (const side of [-1, 1]) {
      const ear = side < 0 ? 'ear_left' : 'ear_right';
      b.bone(ear, 'head', [side * 8, 70, -32]);
      // Tall tapered ear with pale inner face and crystal tip.
      b.box(ear, 'ear_base', [side * 8, 74, -32], [11, 7, 8], 'violet');
      b.box(ear, 'ear_lower', [side * 8, 80, -32], [9, 6, 7], 'violet_hi');
      b.box(ear, 'ear_mid', [side * 10, 86, -32], [7, 6, 6], 'lavender');
      b.box(ear, 'ear_inner', [side * 10, 81, -35], [6, 12, 1], 'white');
      b.box(ear, 'ear_upper', [side * 11, 92, -32], [5, 6, 4], 'violet_hi');
      b.box(ear, 'ear_tip', [side * 12, 97, -32], [3, 5, 3], 'crystal_hi');
      b.box(ear, 'ear_point', [side * 12, 101, -32], [1, 4, 1], 'crystal');
      b.animate(ear, 'y', 2, 1.0, side < 0 ? 0 : Math.PI);
    }

    for (const side of [-1, 1]) {
      const front = side < 0 ? 'leg_front_left' : 'leg_front_right';
      const back = side < 0 ? 'leg_back_left' : 'leg_back_right';
      b.bone(front, 'body', [side * 10, 38, -18]);
      b.box(front, 'upper', [side * 10, 32, -18], [8, 14, 8], 'violet');
      b.box(front, 'white_sock', [side * 10, 16, -18], [6, 20, 6], 'white');
      b.box(front, 'paw', [side * 10, 3, -20], [8, 4, 10], 'white_hi');
      b.box(front, 'toe_band', [side * 10, 1, -22], [8, 2, 4], 'lavender');
      b.bone(back, 'body', [side * 10, 40, 20]);
      b.box(back, 'thigh', [side * 11, 34, 20], [10, 16, 12], 'lavender');
      b.box(back, 'hock', [side * 10, 24, 22], [6, 8, 6], 'violet');
      b.box(back, 'white_sock', [side * 10, 14, 22], [6, 16, 6], 'white');
      b.box(back, 'paw', [side * 10, 3, 20], [8, 4, 10], 'white_hi');
      b.box(back, 'toe_band', [side * 10, 1, 18], [8, 2, 4], 'lavender');
      b.animate(front, 'x', 5, 1.2, side < 0 ? 0 : Math.PI);
      b.animate(back, 'x', 5, 1.2, side < 0 ? Math.PI : 0);
    }

    // Bushy tail stepped downward toward the ground, ending in a crystal cluster.
    b.bone('tail', 'body', [0, 48, 30]);
    const tailPts = [[0, 48, 30], [0, 44, 42], [0, 38, 54], [0, 30, 66], [0, 22, 76]];
    for (let i = 0; i < tailPts.length - 1; i++) {
      const a = tailPts[i], c = tailPts[i + 1];
      const center = [(a[0] + c[0]) / 2, (a[1] + c[1]) / 2, (a[2] + c[2]) / 2];
      const w = 26 - i * 2;
      b.box('tail', 'fur_' + i, center, [w, w, 18], i % 2 ? 'lavender' : 'violet_hi');
      b.box('tail', 'fur_patch_' + i, [center[0] + w / 2, center[1] + 2, center[2]], [2, w - 4, 12], 'violet');
    }
    crystal(b, 'tail', 'tail_cluster_main', [0, 24, 78], 24, 12, [0, 0.22]);
    crystal(b, 'tail', 'tail_cluster_left', [-9, 21, 76], 18, 8, [-0.22, 0.14]);
    crystal(b, 'tail', 'tail_cluster_right', [9, 21, 76], 18, 8, [0.22, 0.14]);
    crystal(b, 'tail', 'tail_cluster_back', [0, 18, 86], 16, 7, [0, 0.28]);
    crystal(b, 'tail', 'tail_cluster_low_left', [-6, 14, 80], 12, 6, [-0.18, 0.2]);
    crystal(b, 'tail', 'tail_cluster_low_right', [6, 14, 80], 12, 6, [0.18, 0.2]);

    // Dorsal crystal ridge, faceted stacks instead of square posts.
    const ridge = [[-2, 60, -12, 12, 6], [2, 62, -2, 18, 8], [-1, 61, 10, 14, 7], [3, 58, 22, 11, 5]];
    ridge.forEach((r, i) => crystal(b, 'body', 'spine_' + i, [r[0], r[1], r[2]], r[3], r[4], [i % 2 ? 0.1 : -0.08, 0.05]));
    crystal(b, 'body', 'shoulder_crystal', [14, 54, -14], 10, 5, [0.14, 0]);

    b.animate('head', 'x', 2, 0.8, 0);
    b.animate('tail', 'y', 6, 1.3, 0);
  }

  const SPECS = [
    { id: 'porcelain_crane', title_ru: 'Фарфоровый журавль', biome: 'elisar_city', concept: 'reports/biome_mob_concepts/01_elisar_city_porcelain_crane.png', atlas: ATLAS, build: buildPorcelainCrane },
    { id: 'basalt_beetle', title_ru: 'Базальтовый жук-углеед', biome: 'ashen_lands', concept: 'reports/biome_mob_concepts/02_ashen_lands_basalt_beetle.png', atlas: ATLAS, build: buildBasaltBeetle },
    { id: 'ash_moth', title_ru: 'Пепельная моль', biome: 'ashen_lands', concept: 'reports/biome_mob_concepts/03_ashen_lands_ash_moth.png', atlas: ATLAS, build: buildAshMoth },
    { id: 'amethyst_fox', title_ru: 'Аметистовый лис', biome: 'violet_hills', concept: 'reports/biome_mob_concepts/04_violet_hills_amethyst_fox.png', atlas: ATLAS, build: buildAmethystFox }
  ];

  async function generateOne(spec) {
    const builder = new Builder(spec);
    spec.build(builder);
    return await builder.finish();
  }
  async function generateAll(options) {
    const opts = options || {};
    const ids = opts.ids || SPECS.map(s => s.id);
    const out = [];
    for (const id of ids) {
      const spec = SPECS.find(s => s.id === id);
      if (!spec) throw new Error('unknown model id ' + id);
      out.push(await generateOne(spec));
    }
    return out;
  }

  global.RiftworldFauna = {
    version: '0.3.0',
    blockbench: '5.2.2',
    revision: 3,
    specs: SPECS.map(s => ({ id: s.id, title_ru: s.title_ru, biome: s.biome, concept: s.concept })),
    generateAll,
    generateOne: id => {
      const spec = SPECS.find(s => s.id === id);
      if (!spec) throw new Error('unknown model id ' + id);
      return generateOne(spec);
    }
  };

  if (global.BBPlugin && global.BBPlugin.register) {
    global.BBPlugin.register('riftworld_fauna', {
      title: 'Riftworld Fauna',
      author: 'Riftworld',
      icon: 'fa-cube',
      version: '0.3.0',
      min_version: '5.2.0',
      variant: 'both',
      description: 'Reference-detail procedural Blockbench-API source for the first four Riftworld biome mobs.',
      onload() {},
      onunload() {}
    });
  }
})(typeof window !== 'undefined' ? window : globalThis);
