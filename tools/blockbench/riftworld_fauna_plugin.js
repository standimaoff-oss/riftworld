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
    mint:          { base: [139, 183, 172], noise: 4, edge: -4, pattern: 'fur' },
    mint_hi:       { base: [190, 218, 207], noise: 3, edge: -3, pattern: 'fur' },
    mint_lo:       { base: [91, 136, 132], noise: 4, edge: -4, pattern: 'fur' },
    leaf_cream:    { base: [226, 233, 218], noise: 3, edge: -4, pattern: 'feather' },
    leaf_green:    { base: [144, 180, 155], noise: 4, edge: -4, pattern: 'feather' },
    branch:        { base: [99, 83, 69], noise: 4, edge: -4, pattern: 'basalt' },
    flower_blue:   { base: [145, 126, 218], noise: 3, edge: -3, pattern: 'fur' },
    amber:         { base: [206, 137, 45], noise: 5, edge: -4, pattern: 'metal' },
    frog_teal:     { base: [45, 177, 174], noise: 5, edge: -4, pattern: 'frog_spots' },
    frog_hi:       { base: [87, 207, 190], noise: 4, edge: -4, pattern: 'frog_spots' },
    frog_dark:     { base: [27, 129, 147], noise: 5, edge: -4, pattern: 'frog_spots' },
    frog_belly:    { base: [137, 215, 205], noise: 3, edge: -4, pattern: 'ceramic' },
    petal_pink:    { base: [245, 170, 192], noise: 3, edge: -4, pattern: 'fur' },
    petal_light:   { base: [255, 206, 215], noise: 3, edge: -3, pattern: 'fur' },
    frog_toe:      { base: [166, 221, 169], noise: 3, edge: -4, pattern: 'fur' },
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
          case 'frog_spots': {
            const spot = fnv('frog:' + Math.floor(p[0] / 7) + ',' + Math.floor(p[1] / 7) + ',' + Math.floor(p[2] / 7));
            if (spot > 0.82) { r += 43; g += 26; b -= 33; }
            else if (spot < 0.12) { r -= 12; g -= 17; b += 8; }
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
        revision: ['porcelain_crane', 'basalt_beetle', 'ash_moth', 'amethyst_fox'].includes(this.id) ? 3 : 1,
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

  // Moth: broad planar cuboids arranged as asymmetric layered wing plates.
  function buildAshMoth(b) {
    b.bone('body', null, [0, 34, 0]);
    b.box('body', 'thorax', [0, 34, -1], [24, 24, 28], 'ash');
    b.box('body', 'fuzzy_collar', [0, 40, -8], [26, 16, 16], 'ash_hi');
    b.box('body', 'abdomen', [0, 26, 20], [19, 22, 30], 'ash');
    for (let i = 0; i < 3; i++) b.box('body', 'abdomen_band_' + i, [0, 26, 12 + 8*i], [20, 19, 2], 'ash_dark');
    b.bone('head', 'body', [0, 41, -17]);
    b.box('head', 'head', [0, 42, -20], [20, 17, 14], 'ash_hi');
    b.box('head', 'face', [0, 39, -28], [12, 12, 4], 'ash');
    for (const side of [-1, 1]) {
      b.box('head', 'eye_' + side, [side*9, 44, -28], [5, 7, 3], 'eye');
      const ant = side < 0 ? 'antenna_left' : 'antenna_right';
      const pivot = [side * 7, 49, -19];
      b.bone(ant, 'head', pivot, [0, 0, side * -18]);
      b.box(ant, 'stalk', [pivot[0], 62, pivot[2]], [3, 25, 4], 'ash_dark');
      for (let i=0; i<5; i++) {
        b.box(ant, 'barb_' + i, [pivot[0], 53 + i*4, pivot[2]], [11 - i, 2, 4], 'ash_dark');
      }
      b.animate(ant, 'y', 3, 1.2, side < 0 ? 0 : Math.PI);
    }
    for (const side of [-1, 1]) {
      const fore = side < 0 ? 'wing_fore_left' : 'wing_fore_right';
      const hind = side < 0 ? 'wing_hind_left' : 'wing_hind_right';
      const pivot = [side*10, 43, 1];
      b.bone(fore, 'body', pivot, [0, 0, side*10]);
      // Separate wide rectangles overlap: a thick leading edge, tapered inner
      // sections and a glowing tip, rather than dozens of one-voxel strips.
      const plates = [
        [21, 44, 23, 24, 'wing'], [40, 46, 20, 28, 'wing_hi'],
        [58, 49, 18, 25, 'wing'], [72, 52, 14, 20, 'wing_hi'],
        [82, 55, 9, 15, 'ember']
      ];
      plates.forEach(([x,y,w,h,mat],i) => b.box(fore, 'fore_plate_'+i, [side*x,y,0], [w,h,3], mat));
      plates.slice(0,4).forEach(([x,y,w,h], i) =>
        b.box(fore, 'leading_edge_'+i, [side*x, y+h/2-1, -1], [w, 2, 4], 'ash_hi'));
      b.box(fore, 'ember_tip', [side*85, 55, -1], [7, 14, 4], 'lava');
      for (let i=0;i<3;i++) b.box(fore, 'rib_'+i, [side*(28+20*i), 44+i*3, -2], [2, 16, 1], 'wing_lo');
      b.bone(hind, 'body', [side*10, 29, 4], [0, 0, side*-5]);
      const eyeCenter = [side*33, 22, -3];
      b.box(hind, 'lower_wing_base', [side*29, 22, 4], [38, 25, 3], 'eyespot',
        {eyespot_center: eyeCenter, eyespot_radius: 10, eyespot_plane: 'xy'});
      b.box(hind, 'lower_wing_tip', [side*49, 23, 4], [9, 18, 3], 'wing');
      b.box(hind, 'lower_wing_bottom', [side*30, 9, 4], [29, 4, 3], 'wing_lo');
      b.animate(fore, 'z', 7, 2.2, side < 0 ? 0 : Math.PI);
      b.animate(hind, 'z', 5, 2.2, side < 0 ? 0.3 : Math.PI + 0.3);
    }
    b.bone('legs', 'body', [0, 23, -6]);
    for (const side of [-1,1]) for (let i=0;i<3;i++) b.box('legs', 'leg_'+side+'_'+i, [side*8, 17, -9+i*8], [2, 12, 2], 'ash_dark');
    b.bone('ash_trail', 'body', [0, 14, 18]);
    [[-14,6,16],[12,4,22],[-5,0,28],[20,1,30],[-20,3,34],[5,-3,38]].forEach((p,i) => b.box('ash_trail', 'mote_'+i, p, [3,3,3], 'ash_dark'));
    b.animate('head','x',2,0.8,0);
  }

  function buildAmethystFox(b) {
    b.bone('body', null, [0, 44, 0]);
    b.box('body', 'torso', [0, 45, 3], [28, 24, 53], 'violet');
    b.box('body', 'back', [0, 58, 4], [26, 5, 46], 'violet_hi');
    b.box('body', 'belly', [0, 34, 1], [23, 6, 44], 'white');
    b.box('body', 'chest', [0, 41, -25], [23, 23, 9], 'white_hi');
    b.bone('head', 'body', [0, 58, -28]);
    b.box('head', 'skull', [0, 63, -32], [26, 21, 22], 'violet_hi');
    b.box('head', 'brow', [0, 69, -40], [26, 7, 10], 'lavender');
    b.box('head', 'muzzle', [0, 57, -48], [18, 8, 14], 'white');
    b.box('head', 'nose', [0, 60, -56], [7, 4, 3], 'violet_lo');
    for (const side of [-1,1]) {
      b.box('head', 'cheek_'+side, [side*11, 57, -40], [7, 9, 13], 'white_hi');
      b.box('head', 'eye_'+side, [side*8, 65, -44], [5, 5, 2], 'eye_light');
      const ear = side < 0 ? 'ear_left' : 'ear_right';
      const p = [side*8, 72, -32];
      b.bone(ear, 'head', p, [0, 0, side*-16]);
      b.box(ear, 'ear_outer', [p[0], 84, p[2]], [9, 24, 7], 'violet');
      b.box(ear, 'ear_inner', [p[0], 84, p[2]-4], [5, 19, 1], 'white');
      b.box(ear, 'ear_tip', [p[0], 98, p[2]], [5, 6, 5], 'crystal_hi');
      b.animate(ear, 'y', 2, 1.0, side < 0 ? 0 : Math.PI);
    }
    for (const side of [-1,1]) for (const back of [false,true]) {
      const name = 'leg_' + (back ? 'back_' : 'front_') + (side<0?'left':'right');
      const z = back ? 21 : -17;
      b.bone(name, 'body', [side*10, 36, z]);
      b.box(name, 'upper', [side*10, 28, z], [9, 19, 10], back ? 'lavender' : 'violet');
      b.box(name, 'sock', [side*10, 13, z], [7, 17, 8], 'white');
      b.box(name, 'paw', [side*10, 3, z-2], [9, 6, 12], 'white_hi');
      b.box(name, 'toe', [side*10, 1, z-5], [9, 2, 5], 'lavender');
      b.animate(name, 'x', 4, 1.2, (side<0) !== back ? 0 : Math.PI);
    }
    b.bone('tail', 'body', [0, 49, 28], [27, 0, 0]);
    b.box('tail', 'root', [0, 47, 34], [19, 20, 14], 'violet_hi');
    b.box('tail', 'bushy_tail', [0, 46, 52], [30, 30, 31], 'lavender');
    b.box('tail', 'tip_fur', [0, 46, 70], [27, 27, 9], 'violet_hi');
    for (let i=0;i<5;i++) {
      const x=(i-2)*5;
      const p=[x, 47, 74]; const n='tail_crystal_'+i;
      // Fan the crystals outward in two single-axis groups; the bases start
      // beyond the fur tip instead of being buried inside the tail.
      b.bone(n,'tail',p,[0,0,(i-2)*13]);
      const tilt=n+'_tilt'; b.bone(tilt,n,p,[55,0,0]);
      crystal(b,tilt,'crystal',p,21+(2-Math.abs(i-2))*4,7,[0,0]);
    }
    for (let i=0;i<4;i++) {
      const z=-9+i*11, p=[i%2 ? 3 : -3, 58, z], n='back_crystal_'+i;
      b.bone(n,'body',p,[i%2?-22:22,0,0]);
      crystal(b,n,'crystal',p,13+(i%2)*4,7,[0,0]);
    }
    b.animate('head','x',2,0.8,0);
    b.animate('tail','y',5,1.1,0);
  }

  // Living woodland dragon, based on approved Sizo leafwing concept.
  function buildSizoLeafwing(b) {
    b.bone('body', null, [0, 38, 0]);
    b.box('body','torso',[0,39,3],[27,23,47],'mint');
    b.box('body','back',[0,51,3],[24,5,42],'mint_hi');
    b.box('body','belly',[0,30,2],[22,7,37],'leaf_cream');
    b.bone('neck','body',[0,49,-20],[-16,0,0]);
    b.box('neck','neck',[0,57,-23],[19,26,19],'mint');
    b.box('neck','bib',[0,55,-33],[15,22,4],'leaf_cream');
    b.bone('head','neck',[0,70,-27]);
    b.box('head','skull',[0,72,-31],[24,17,20],'mint_hi');
    b.box('head','muzzle',[0,68,-44],[15,8,12],'mint');
    b.box('head','nose',[0,70,-50],[8,3,3],'mint_lo');
    for (const side of [-1,1]) {
      b.box('head','eye_socket_'+side,[side*12,74,-37],[2,8,8],'branch');
      b.box('head','amber_eye_'+side,[side*13,74,-37],[2,6,6],'amber');
      b.box('head','eye_glint_'+side,[side*14,76,-39],[1,2,2],'white_hi');
      b.box('head','ear_'+side,[side*15,76,-25],[9,6,10],'mint_hi');
      b.box('head','cheek_'+side,[side*12,65,-30],[8,6,13],'leaf_cream');
      const ant=side<0?'antler_left':'antler_right';const p=[side*8,79,-27];
      b.bone(ant,'head',p,[0,0,side*-20]);
      b.box(ant,'branch_main',[p[0],90,p[2]],[3,22,4],'branch');
      b.box(ant,'branch_tip',[p[0],102,p[2]],[3,5,3],'branch');
      const fork=ant+'_fork';b.bone(fork,ant,[p[0],91,p[2]],[0,0,side*-38]);
      b.box(fork,'fork',[p[0],98,p[2]],[3,14,3],'branch');
    }
    for (const side of [-1,1]) {
      const wing=side<0?'wing_left':'wing_right';const p=[side*12,49,4];
      b.bone(wing,'body',p,[0,0,side*14]);
      // White and mint leaves arranged as broad rectangles with ribs.
      // Stepped leaf edges sweep upward and narrow at their tips. Pale
      // inset panels keep a green border, with diagonal rather than rail-like veins.
      const widths=[[23,28,31,60],[47,24,35,67],[68,19,28,74],[84,14,16,80]];
      widths.forEach(([x,w,h,y],i)=>{
        b.box(wing,'leaf_'+i,[side*x,y,9],[w,h,3],i%2?'leaf_cream':'leaf_green');
        b.box(wing,'inner_leaf_'+i,[side*x,y+1,7],[Math.max(4,w-6),h-7,2],'leaf_cream');
      });
      b.box(wing,'leading_spar',[side*48,80,7],[76,3,4],'mint_lo');
      for (let i=0;i<3;i++) {
        const v=wing+'_vein_'+i, x=side*(26+i*20), y=58+i*7;
        b.bone(v,wing,[x,y,6],[0,0,side*(20+i*9)]);
        b.box(v,'diagonal_vein',[x,y,6],[2,24-i*4,2],'mint_lo');
      }
      b.animate(wing,'z',7,1.3,side<0?0:Math.PI);
    }
    for (const side of [-1,1]) for (const back of [false,true]) {
      const n='leg_'+(back?'back_':'front_')+(side<0?'left':'right'); const z=back?19:-15;
      b.bone(n,'body',[side*10,31,z]);
      b.box(n,'upper',[side*11,23,z],[10,18,10],'mint');
      b.box(n,'lower',[side*11,10,z-2],[7,13,7],'mint_hi');
      b.box(n,'paw',[side*11,3,z-5],[10,6,12],'mint_lo');
      for(let i=-1;i<=1;i++) b.box(n,'claw_'+i,[side*11+i*3,2,z-11],[2,3,4],'branch');
      b.animate(n,'x',4,1.0,(side<0)!==back?0:Math.PI);
    }
    b.bone('tail','body',[0,39,26]);
    const segments=[[0,37,34,12,13,19],[0,32,48,10,11,17],[0,27,61,8,9,15],[0,25,73,6,7,13],[0,30,82,5,12,6]];
    segments.forEach(([x,y,z,w,h,d],i)=>b.box('tail','segment_'+i,[x,y,z],[w,h,d],i%2?'mint_hi':'mint'));
    b.box('tail','leaf_tail',[0,35,87],[15,13,5],'leaf_green');
    b.box('tail','leaf_vein',[0,35,90],[2,11,1],'mint_lo');
    for(const side of [-1,1]) {
      b.box('neck','flower_'+side,[side*10,58,-29],[5,5,2],'flower_blue');
      b.box('neck','flower_dot_'+side,[side*10,58,-30],[2,2,2],'eye_light');
    }
    b.animate('tail','y',5,1.0,0); b.animate('head','x',2,0.8,0);
  }

  function buildAzureFrog(b) {
    b.bone('body',null,[0,24,2]);
    ellipsoidSolid(b,'body','belly',[0,24,5],[27,21,28],4,'frog_teal');
    b.box('body','belly_front',[0,20,-18],[34,22,6],'frog_belly');
    b.box('body','back',[0,42,7],[37,8,38],'frog_dark');
    b.bone('head','body',[0,40,-12]);
    b.box('head','head',[0,45,-18],[43,22,30],'frog_hi');
    b.box('head','snout',[0,40,-34],[35,13,9],'frog_teal');
    b.box('head','chin',[0,33,-32],[31,5,8],'frog_belly');
    b.box('head','mouth',[0,38,-39],[23,2,1],'frog_dark');
    for(const side of [-1,1]) {
      b.box('head','nostril_'+side,[side*9,46,-37],[3,2,1],'frog_dark');
      b.box('head','eye_tower_'+side,[side*16,56,-26],[14,12,13],'frog_dark');
      b.box('head','gold_eye_'+side,[side*16,57,-34],[12,11,3],'gold_hi');
      b.box('head','pupil_'+side,[side*16,57,-36],[7,8,2],'eye');
      b.box('head','eye_glint_'+side,[side*14,59,-39],[2,2,1],'white_hi');
    }
    // Petal collar: alternating raised, tilted rectangular petals, with buds.
    for(const side of [-1,1]) for(let i=0;i<3;i++) {
      const x=side*(13+i*10), y=31+i*3, n='petal_'+side+'_'+i;
      b.bone(n,'body',[side*12,33,-36],[0,0,side*(18+i*12)]);
      b.box(n,'petal',[x,y,-36],[13,8,3], i%2?'petal_light':'petal_pink');
      b.box(n,'petal_tip',[x+side*5,y,-37],[5,6,3],'petal_light');
    }
    b.box('body','petal_center',[0,27,-39],[13,10,4],'petal_light');
    b.box('body','petal_center_left',[-10,28,-38],[10,9,4],'petal_pink');
    b.box('body','petal_center_right',[10,28,-38],[10,9,4],'petal_pink');
    for(const side of [-1,1]) for(const back of [false,true]) {
      const n='leg_'+(back?'back_':'front_')+(side<0?'left':'right'); const z=back?18:-12;
      b.bone(n,'body',[side*20,21,z]);
      b.box(n,'thigh',[side*25,19,z],[15,18,17],back?'frog_dark':'frog_teal');
      b.box(n,'shin',[side*27,10,z-2],[9,14,10],'frog_hi');
      b.box(n,'palm',[side*27,3,z-6],[14,6,12],'frog_teal');
      for(let i=-1;i<=1;i++) b.box(n,'toe_'+i,[side*27+i*5,3,z-13],[4,4,8],'frog_toe');
      b.animate(n,'x',3,0.9,(side<0)!==back?0:Math.PI);
    }
    b.animate('head','x',2,0.8,0);
  }

  const SPECS = [
    { id: 'porcelain_crane', title_ru: 'Фарфоровый журавль', biome: 'elisar_city', concept: 'reports/biome_mob_concepts/01_elisar_city_porcelain_crane.png', atlas: ATLAS, build: buildPorcelainCrane },
    { id: 'basalt_beetle', title_ru: 'Базальтовый жук-углеед', biome: 'ashen_lands', concept: 'reports/biome_mob_concepts/02_ashen_lands_basalt_beetle.png', atlas: ATLAS, build: buildBasaltBeetle },
    { id: 'ash_moth', title_ru: 'Пепельная моль', biome: 'ashen_lands', concept: 'reports/biome_mob_concepts/03_ashen_lands_ash_moth.png', atlas: ATLAS, build: buildAshMoth },
    { id: 'amethyst_fox', title_ru: 'Аметистовый лис', biome: 'violet_hills', concept: 'reports/biome_mob_concepts/04_violet_hills_amethyst_fox.png', atlas: ATLAS, build: buildAmethystFox },
    { id: 'sizo_leafwing', title_ru: 'Сизый листокрыл', biome: 'sizo_grove', concept: 'reports/biome_mob_concepts/05_sizo_grove_sizo_leafwing.png', atlas: ATLAS, build: buildSizoLeafwing },
    { id: 'azure_frog', title_ru: 'Лазурный древесный лягуш', biome: 'waterfall_gardens', concept: 'reports/biome_mob_concepts/06_waterfall_gardens_azure_frog.png', atlas: ATLAS, build: buildAzureFrog }
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
    version: '0.4.0',
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
      version: '0.4.0',
      min_version: '5.2.0',
      variant: 'both',
      description: 'Reference-detail procedural Blockbench-API source for the first four Riftworld biome mobs.',
      onload() {},
      onunload() {}
    });
  }
})(typeof window !== 'undefined' ? window : globalThis);
