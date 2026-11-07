/* Riftworld Fauna — first Blockbench-API batch.
 *
 * This file is both a loadable Blockbench plugin and a headless library.
 * It builds editable Bedrock-entity projects with the public Blockbench API
 * (Group / Cube / Texture / Animation / Codecs), then exports:
 *   - .bbmodel via Codecs.project.compile()
 *   - a Riftworld runtime geo JSON converted from Codecs.bedrock_old.compile()
 *   - a pixel-art atlas PNG data URL painted into a Blockbench Texture.
 *
 * Authoring space: Blockbench internal, Y up, ground y=0, nose toward -Z.
 * Runtime space: Riftworld GeoModel, Y down, ground y=0, nose toward -Z.
 */
(function (global) {
  'use strict';

  const ATLAS = 256;
  const LOOP = 2.0;

  const MATERIALS = {
    porcelain:    { base: [244, 239, 230], noise: 4,  edge: -10 },
    celadon:      { base: [126, 197, 187], noise: 6,  edge: -12 },
    gold:         { base: [201, 162, 74],  noise: 8,  edge: -14, pattern: 'metal' },
    basalt:       { base: [43, 43, 48],    noise: 6,  edge: -10 },
    basalt_plate: { base: [58, 58, 66],    noise: 7,  edge: -12, pattern: 'plate' },
    lava:         { base: [255, 122, 24],  noise: 14, edge: -8,  pattern: 'lava' },
    ash:          { base: [154, 154, 152], noise: 8,  edge: -10, pattern: 'fuzz' },
    ash_dark:     { base: [110, 110, 108], noise: 7,  edge: -10 },
    wing:         { base: [176, 176, 172], noise: 6,  edge: -8,  pattern: 'wing' },
    eyespot:      { base: [176, 176, 172], noise: 5,  edge: -8,  pattern: 'eyespot' },
    ember:        { base: [255, 140, 40],  noise: 10, edge: -6,  pattern: 'lava' },
    violet:       { base: [139, 95, 191],  noise: 7,  edge: -12, pattern: 'fur' },
    lavender:     { base: [183, 156, 224], noise: 6,  edge: -10 },
    white:        { base: [242, 236, 250], noise: 4,  edge: -8 },
    crystal:      { base: [106, 63, 160],  noise: 8,  edge: -10, pattern: 'crystal' },
    eye:          { base: [16, 16, 20],    noise: 2,  edge: 0,   pattern: 'eye' },
    eye_light:    { base: [230, 220, 245], noise: 3,  edge: 0,   pattern: 'eye' }
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

  function paintFace(atlas, rect, matKey, seed, face) {
    const mat = MATERIALS[matKey] || MATERIALS.ash;
    const [x0, y0, w, h] = rect;
    const cx = x0 + w / 2;
    const cy = y0 + h / 2;
    for (let y = y0; y < y0 + h; y++) {
      for (let x = x0; x < x0 + w; x++) {
        const n = fnv(seed + ':' + face + ':' + x + ',' + y);
        let r = mat.base[0] + (n - 0.5) * 2 * mat.noise;
        let g = mat.base[1] + (n - 0.5) * 2 * mat.noise;
        let b = mat.base[2] + (n - 0.5) * 2 * mat.noise;
        const border = (x === x0 || y === y0 || x === x0 + w - 1 || y === y0 + h - 1);
        if (border) { r += mat.edge; g += mat.edge; b += mat.edge; }
        const lx = x - x0;
        const ly = y - y0;
        switch (mat.pattern) {
          case 'metal':
            if ((lx + ly) % 7 === 0) { r += 28; g += 24; b += 12; }
            break;
          case 'plate':
            if (lx % 4 === 0 || ly % 4 === 0) { r -= 10; g -= 10; b -= 10; }
            if (n > 0.82) { r += 12; g += 12; b += 14; }
            break;
          case 'lava':
            if (n > 0.72) { r = 255; g = 190 + n * 50; b = 70; }
            else if (n < 0.18) { r -= 70; g -= 45; b -= 20; }
            break;
          case 'fuzz':
            if (n > 0.66) { r += 14; g += 14; b += 12; }
            else if (n < 0.33) { r -= 12; g -= 12; b -= 12; }
            break;
          case 'wing':
            if (lx % 5 === 0) { r -= 16; g -= 16; b -= 14; }
            if (ly === 0 || ly === h - 1) { r -= 10; g -= 10; b -= 8; }
            break;
          case 'eyespot':
            if (face === 'up' || face === 'down') {
              const d = Math.max(Math.abs(x - cx), Math.abs(y - cy));
              if (d < Math.min(w, h) * 0.16) { r = 35; g = 35; b = 38; }
              else if (d < Math.min(w, h) * 0.30) { r = 226; g = 220; b = 205; }
              else if (d < Math.min(w, h) * 0.44) { r = 255; g = 140; b = 45; }
            }
            break;
          case 'fur':
            if ((lx + 2 * ly) % 6 === 0) { r += 12; g += 10; b += 16; }
            break;
          case 'crystal':
            if (lx % 3 === 0) { r += 34; g += 28; b += 44; }
            if ((lx + ly) % 5 === 0) { r -= 22; g -= 18; b -= 26; }
            break;
          case 'eye':
            if (w > 2 && h > 2 && lx === 1 && ly === 1) { r = 245; g = 245; b = 250; }
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

    bone(name, parent, pivot) {
      if (this.bones.has(name)) throw new Error('duplicate bone ' + name);
      const parentGroup = parent ? this.bones.get(parent) : null;
      if (parent && !parentGroup) throw new Error('unknown parent ' + parent + ' for ' + name);
      const g = new Group({
        name,
        origin: pivot.slice(),
        rotation: [0, 0, 0],
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

    cube(bone, name, from, to, mat) {
      const g = this.bones.get(bone);
      if (!g) throw new Error('unknown bone ' + bone);
      for (let i = 0; i < 3; i++) {
        if (!Number.isInteger(from[i]) || !Number.isInteger(to[i]) || to[i] <= from[i]) {
          throw new Error('cube ' + name + ' needs integer from<to, got ' + JSON.stringify({ from, to }));
        }
      }
      const c = new Cube({
        name,
        from: from.slice(),
        to: to.slice(),
        box_uv: true,
        uv_offset: [0, 0],
        mirror_uv: false,
        inflate: 0,
        export: true,
        visibility: true
      }).addTo(g).init();
      this.cubes.push({ cube: c, mat, seed: this.id + '/' + bone + '/' + name });
      return c;
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
          paintFace(atlas, rects[face], it.item.mat, it.item.seed, face);
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
      for (let i = 0; i < 100; i++) {
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
          ba.addKeyframe({
            time: t,
            channel: 'rotation',
            data_points: [dp],
            interpolation: 'linear'
          });
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
        if (b.rotation) out.rotation = [-b.rotation[0], b.rotation[1], -b.rotation[2]];
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
        render_scale: this.spec.render_scale == null ? 1.0 : this.spec.render_scale,
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
        generator: 'Blockbench 5.2.2 API (Group/Cube/Texture/Animation/Codecs)',
        bones: this.bones.size,
        cubes: this.cubes.length,
        atlas: [this.size, this.size],
        bounds_blockbench: { min: [minX, minY, minZ], max: [maxX, maxY, maxZ] },
        size_blocks: [(maxX - minX) / 16, (maxY - minY) / 16, (maxZ - minZ) / 16],
        animations: Object.keys(this.anims).length,
        bbmodel_bytes: bbmodel.length,
        geo_bones: geo.bones.length,
        geo_cubes: geo.bones.reduce((n, b) => n + (b.cubes ? b.cubes.length : 0), 0)
      };
      return {
        id: this.id,
        title_ru: this.spec.title_ru,
        biome: this.spec.biome,
        concept: this.spec.concept,
        bbmodel,
        geo,
        texture: dataURL,
        bedrock,
        stats
      };
    }
  }

  function buildPorcelainCrane(b) {
    b.bone('body', null, [0, 20, 0]);
    b.cube('body', 'porcelain_body', [-4, 17, -7], [4, 24, 7], 'porcelain');
    b.cube('body', 'celadon_back', [-3, 24, -4], [3, 25, 5], 'celadon');
    b.cube('body', 'gold_seam_left', [-4, 20, -2], [-3, 21, 4], 'gold');
    b.cube('body', 'gold_seam_right', [3, 20, -2], [4, 21, 4], 'gold');
    b.cube('body', 'gold_breast_seam', [-1, 18, -7], [1, 22, -6], 'gold');

    b.bone('neck_lower', 'body', [0, 23, -5]);
    b.cube('neck_lower', 'neck_lower', [-2, 23, -8], [2, 29, -4], 'porcelain');
    b.cube('neck_lower', 'neck_gold_ring', [-2, 27, -8], [2, 28, -4], 'gold');
    b.bone('neck_upper', 'neck_lower', [0, 29, -6]);
    b.cube('neck_upper', 'neck_upper', [-1, 29, -9], [1, 34, -5], 'porcelain');
    b.bone('head', 'neck_upper', [0, 34, -8]);
    b.cube('head', 'head', [-2, 34, -11], [2, 38, -7], 'porcelain');
    b.cube('head', 'beak', [-1, 35, -15], [1, 36, -11], 'gold');
    b.cube('head', 'eye_left', [-3, 36, -10], [-2, 37, -9], 'eye');
    b.cube('head', 'eye_right', [2, 36, -10], [3, 37, -9], 'eye');
    b.cube('head', 'crest', [-1, 38, -10], [1, 39, -8], 'celadon');

    b.bone('wing_left', 'body', [-4, 22, 0]);
    b.cube('wing_left', 'wing_upper', [-6, 19, -5], [-4, 24, 6], 'celadon');
    b.cube('wing_left', 'wing_lower', [-5, 17, -3], [-4, 20, 7], 'porcelain');
    b.cube('wing_left', 'wing_tip', [-5, 16, 4], [-4, 18, 8], 'celadon');
    b.cube('wing_left', 'wing_gold_line', [-6, 22, -2], [-5, 23, 5], 'gold');
    b.bone('wing_right', 'body', [4, 22, 0]);
    b.cube('wing_right', 'wing_upper', [4, 19, -5], [6, 24, 6], 'celadon');
    b.cube('wing_right', 'wing_lower', [4, 17, -3], [5, 20, 7], 'porcelain');
    b.cube('wing_right', 'wing_tip', [4, 16, 4], [5, 18, 8], 'celadon');
    b.cube('wing_right', 'wing_gold_line', [5, 22, -2], [6, 23, 5], 'gold');

    b.bone('tail', 'body', [0, 21, 7]);
    b.cube('tail', 'tail_base', [-3, 20, 7], [3, 23, 12], 'celadon');
    b.cube('tail', 'tail_tip', [-2, 19, 12], [2, 21, 15], 'porcelain');

    b.bone('leg_left_upper', 'body', [-2, 17, 0]);
    b.cube('leg_left_upper', 'thigh', [-3, 10, -1], [-1, 17, 1], 'celadon');
    b.bone('leg_left_lower', 'leg_left_upper', [-2, 10, 0]);
    b.cube('leg_left_lower', 'shin', [-3, 3, -1], [-1, 10, 1], 'porcelain');
    b.bone('foot_left', 'leg_left_lower', [-2, 3, 0]);
    b.cube('foot_left', 'ankle', [-3, 1, -1], [-1, 3, 1], 'gold');
    b.cube('foot_left', 'foot', [-4, 0, -4], [0, 1, 2], 'gold');

    b.bone('leg_right_upper', 'body', [2, 17, 0]);
    b.cube('leg_right_upper', 'thigh', [1, 10, -1], [3, 17, 1], 'celadon');
    b.bone('leg_right_lower', 'leg_right_upper', [2, 10, 0]);
    b.cube('leg_right_lower', 'shin', [1, 3, -1], [3, 10, 1], 'porcelain');
    b.bone('foot_right', 'leg_right_lower', [2, 3, 0]);
    b.cube('foot_right', 'ankle', [1, 1, -1], [3, 3, 1], 'gold');
    b.cube('foot_right', 'foot', [0, 0, -4], [4, 1, 2], 'gold');

    b.animate('neck_lower', 'x', 3, 0.6, 0);
    b.animate('head', 'x', 4, 0.6, 0.5);
    b.animate('wing_left', 'z', 2, 0.8, 0);
    b.animate('wing_right', 'z', 2, 0.8, Math.PI);
    b.animate('tail', 'y', 4, 0.7, 0);
    b.animate('leg_left_upper', 'x', 3, 1.2, 0);
    b.animate('leg_right_upper', 'x', 3, 1.2, Math.PI);
  }

  function buildBasaltBeetle(b) {
    b.bone('body', null, [0, 8, 0]);
    b.cube('body', 'abdomen', [-7, 6, 2], [7, 13, 12], 'basalt');
    b.cube('body', 'thorax', [-6, 7, -8], [6, 14, 2], 'basalt_plate');
    b.cube('body', 'lava_core', [-2, 10, -2], [2, 12, 6], 'lava');

    b.bone('head', 'body', [0, 9, -8]);
    b.cube('head', 'head', [-5, 7, -14], [5, 13, -8], 'basalt');
    b.cube('head', 'mandible_left', [-4, 7, -17], [-1, 9, -14], 'basalt_plate');
    b.cube('head', 'mandible_right', [1, 7, -17], [4, 9, -14], 'basalt_plate');
    b.cube('head', 'horn_left', [-3, 13, -13], [-1, 17, -10], 'basalt_plate');
    b.cube('head', 'horn_right', [1, 13, -13], [3, 17, -10], 'basalt_plate');
    b.cube('head', 'eye_left', [-6, 10, -13], [-5, 12, -11], 'lava');
    b.cube('head', 'eye_right', [5, 10, -13], [6, 12, -11], 'lava');
    b.bone('antenna_left', 'head', [-3, 13, -13]);
    b.cube('antenna_left', 'antenna', [-4, 13, -16], [-3, 16, -14], 'basalt');
    b.cube('antenna_left', 'antenna_tip', [-4, 16, -17], [-3, 17, -15], 'ember');
    b.bone('antenna_right', 'head', [3, 13, -13]);
    b.cube('antenna_right', 'antenna', [3, 13, -16], [4, 16, -14], 'basalt');
    b.cube('antenna_right', 'antenna_tip', [3, 16, -17], [4, 17, -15], 'ember');

    b.bone('elytra_left', 'body', [-1, 13, 2]);
    b.cube('elytra_left', 'shell', [-8, 13, 1], [-1, 16, 12], 'basalt_plate');
    b.cube('elytra_left', 'crack_a', [-6, 16, 3], [-5, 17, 9], 'lava');
    b.cube('elytra_left', 'crack_b', [-3, 16, 5], [-2, 17, 10], 'lava');
    b.bone('elytra_right', 'body', [1, 13, 2]);
    b.cube('elytra_right', 'shell', [1, 13, 1], [8, 16, 12], 'basalt_plate');
    b.cube('elytra_right', 'crack_a', [5, 16, 3], [6, 17, 9], 'lava');
    b.cube('elytra_right', 'crack_b', [2, 16, 5], [3, 17, 10], 'lava');

    const legs = [
      ['front_left',  -6, -6, -1], ['front_right',  6, -6, 1],
      ['middle_left', -7,  0, -1], ['middle_right', 7,  0, 1],
      ['back_left',   -6,  7, -1], ['back_right',   6,  7, 1]
    ];
    for (const [name, x, z, side] of legs) {
      const hipX = x;
      const kneeX = x + side * 3;
      const footX = x + side * 6;
      b.bone('leg_' + name, 'body', [hipX, 8, z]);
      b.cube('leg_' + name, 'coxa', [Math.min(hipX, kneeX), 7, z - 1], [Math.max(hipX, kneeX), 10, z + 1], 'basalt');
      b.bone('leg_' + name + '_lower', 'leg_' + name, [kneeX, 8, z]);
      b.cube('leg_' + name + '_lower', 'femur', [Math.min(kneeX, footX), 4, z - 1], [Math.max(kneeX, footX), 8, z + 1], 'basalt_plate');
      b.bone('leg_' + name + '_foot', 'leg_' + name + '_lower', [footX, 4, z]);
      b.cube('leg_' + name + '_foot', 'tibia', [footX - 1, 0, z - 1], [footX + 1, 4, z + 1], 'basalt');
      b.cube('leg_' + name + '_foot', 'ember_claw', [footX - 1, 0, z - 2], [footX + 1, 1, z - 1], 'ember');
    }

    b.animate('head', 'x', 2, 0.7, 0);
    b.animate('antenna_left', 'y', 5, 1.1, 0);
    b.animate('antenna_right', 'y', 5, 1.1, Math.PI);
    b.animate('elytra_left', 'z', 1.5, 0.9, 0);
    b.animate('elytra_right', 'z', 1.5, 0.9, Math.PI);
    legs.forEach(([name], i) => {
      b.animate('leg_' + name, 'x', 6, 1.4, (i % 3) * (Math.PI / 3));
      b.animate('leg_' + name + '_lower', 'x', 4, 1.4, (i % 3) * (Math.PI / 3) + 0.6);
    });
  }

  function buildAshMoth(b) {
    b.bone('body', null, [0, 12, 0]);
    b.cube('body', 'abdomen', [-2, 11, 0], [2, 14, 8], 'ash');
    b.cube('body', 'thorax', [-3, 11, -6], [3, 15, 0], 'ash');
    b.cube('body', 'ember_band', [-2, 12, 2], [2, 13, 4], 'ember');

    b.bone('head', 'body', [0, 13, -6]);
    b.cube('head', 'head', [-2, 12, -9], [2, 16, -6], 'ash');
    b.cube('head', 'eye_left', [-3, 13, -8], [-2, 15, -7], 'eye');
    b.cube('head', 'eye_right', [2, 13, -8], [3, 15, -7], 'eye');
    b.bone('antenna_left', 'head', [-1, 16, -8]);
    b.cube('antenna_left', 'stalk', [-2, 16, -10], [-1, 19, -8], 'ash_dark');
    b.cube('antenna_left', 'feather_a', [-3, 17, -10], [-2, 18, -9], 'ash_dark');
    b.cube('antenna_left', 'feather_b', [-3, 18, -9], [-2, 19, -8], 'ash_dark');
    b.bone('antenna_right', 'head', [1, 16, -8]);
    b.cube('antenna_right', 'stalk', [1, 16, -10], [2, 19, -8], 'ash_dark');
    b.cube('antenna_right', 'feather_a', [2, 17, -10], [3, 18, -9], 'ash_dark');
    b.cube('antenna_right', 'feather_b', [2, 18, -9], [3, 19, -8], 'ash_dark');

    b.bone('wing_fore_left', 'body', [-3, 14, -3]);
    b.cube('wing_fore_left', 'membrane', [-18, 14, -6], [-3, 15, 2], 'eyespot');
    b.cube('wing_fore_left', 'ember_tip', [-18, 14, -6], [-15, 15, -3], 'ember');
    b.bone('wing_hind_left', 'body', [-3, 13, 1]);
    b.cube('wing_hind_left', 'membrane', [-14, 13, 1], [-3, 14, 8], 'wing');
    b.cube('wing_hind_left', 'ember_edge', [-14, 13, 6], [-11, 14, 8], 'ember');

    b.bone('wing_fore_right', 'body', [3, 14, -3]);
    b.cube('wing_fore_right', 'membrane', [3, 14, -6], [18, 15, 2], 'eyespot');
    b.cube('wing_fore_right', 'ember_tip', [15, 14, -6], [18, 15, -3], 'ember');
    b.bone('wing_hind_right', 'body', [3, 13, 1]);
    b.cube('wing_hind_right', 'membrane', [3, 13, 1], [14, 14, 8], 'wing');
    b.cube('wing_hind_right', 'ember_edge', [11, 13, 6], [14, 14, 8], 'ember');

    b.bone('legs', 'body', [0, 10, -2]);
    b.cube('legs', 'leg_left_front', [-2, 8, -4], [-1, 11, -3], 'ash_dark');
    b.cube('legs', 'leg_right_front', [1, 8, -4], [2, 11, -3], 'ash_dark');
    b.cube('legs', 'leg_left_back', [-2, 8, 1], [-1, 11, 2], 'ash_dark');
    b.cube('legs', 'leg_right_back', [1, 8, 1], [2, 11, 2], 'ash_dark');

    b.animate('body', 'x', 3, 1.1, 0);
    b.animate('head', 'x', 2, 1.1, 0.3);
    b.animate('antenna_left', 'y', 6, 1.3, 0);
    b.animate('antenna_right', 'y', 6, 1.3, Math.PI);
    b.animate('wing_fore_left', 'z', 25, 2.2, 0);
    b.animate('wing_fore_right', 'z', 25, 2.2, Math.PI);
    b.animate('wing_hind_left', 'z', 18, 2.2, 0.3);
    b.animate('wing_hind_right', 'z', 18, 2.2, Math.PI + 0.3);
    b.animate('legs', 'x', 4, 1.1, 0);
  }

  function buildAmethystFox(b) {
    b.bone('body', null, [0, 8, 0]);
    b.cube('body', 'body', [-4, 6, -7], [4, 11, 7], 'violet');
    b.cube('body', 'chest', [-3, 6, -7], [3, 9, -4], 'white');
    b.cube('body', 'back_crystal_a', [-2, 11, -2], [0, 14, 0], 'crystal');
    b.cube('body', 'back_crystal_b', [0, 11, 1], [2, 14, 3], 'crystal');
    b.cube('body', 'back_crystal_c', [-1, 11, 4], [1, 13, 6], 'crystal');

    b.bone('head', 'body', [0, 10, -7]);
    b.cube('head', 'head', [-4, 9, -13], [4, 14, -7], 'violet');
    b.cube('head', 'muzzle', [-2, 9, -16], [2, 11, -13], 'white');
    b.cube('head', 'nose', [-1, 10, -17], [1, 11, -16], 'eye');
    b.cube('head', 'eye_left', [-4, 12, -12], [-3, 13, -10], 'eye_light');
    b.cube('head', 'eye_right', [3, 12, -12], [4, 13, -10], 'eye_light');
    b.bone('ear_left', 'head', [-3, 14, -10]);
    b.cube('ear_left', 'ear', [-4, 14, -11], [-1, 18, -9], 'violet');
    b.cube('ear_left', 'inner', [-3, 15, -10], [-2, 17, -9], 'lavender');
    b.bone('ear_right', 'head', [3, 14, -10]);
    b.cube('ear_right', 'ear', [1, 14, -11], [4, 18, -9], 'violet');
    b.cube('ear_right', 'inner', [2, 15, -10], [3, 17, -9], 'lavender');

    b.bone('leg_front_left', 'body', [-3, 6, -5]);
    b.cube('leg_front_left', 'leg', [-4, 0, -6], [-2, 6, -4], 'violet');
    b.cube('leg_front_left', 'paw', [-4, 0, -6], [-2, 1, -4], 'white');
    b.bone('leg_front_right', 'body', [3, 6, -5]);
    b.cube('leg_front_right', 'leg', [2, 0, -6], [4, 6, -4], 'violet');
    b.cube('leg_front_right', 'paw', [2, 0, -6], [4, 1, -4], 'white');
    b.bone('leg_back_left', 'body', [-3, 6, 5]);
    b.cube('leg_back_left', 'haunch', [-5, 5, 3], [-3, 9, 7], 'violet');
    b.cube('leg_back_left', 'leg', [-4, 0, 4], [-2, 6, 6], 'violet');
    b.cube('leg_back_left', 'paw', [-4, 0, 4], [-2, 1, 6], 'white');
    b.bone('leg_back_right', 'body', [3, 6, 5]);
    b.cube('leg_back_right', 'haunch', [3, 5, 3], [5, 9, 7], 'violet');
    b.cube('leg_back_right', 'leg', [2, 0, 4], [4, 6, 6], 'violet');
    b.cube('leg_back_right', 'paw', [2, 0, 4], [4, 1, 6], 'white');

    b.bone('tail', 'body', [0, 9, 7]);
    b.cube('tail', 'tail_base', [-2, 9, 7], [2, 12, 13], 'violet');
    b.cube('tail', 'tail_tip', [-2, 10, 13], [2, 13, 17], 'white');
    b.cube('tail', 'tail_crystal', [-1, 12, 9], [1, 15, 11], 'crystal');

    b.animate('head', 'x', 3, 0.8, 0);
    b.animate('ear_left', 'y', 2, 1.0, 0);
    b.animate('ear_right', 'y', 2, 1.0, Math.PI);
    b.animate('tail', 'y', 12, 1.3, 0);
    b.animate('leg_front_left', 'x', 8, 1.2, 0);
    b.animate('leg_front_right', 'x', 8, 1.2, Math.PI);
    b.animate('leg_back_left', 'x', 8, 1.2, Math.PI);
    b.animate('leg_back_right', 'x', 8, 1.2, 0);
  }

  const SPECS = [
    {
      id: 'porcelain_crane',
      title_ru: 'Фарфоровый журавль',
      biome: 'elisar_city',
      concept: 'reports/biome_mob_concepts/01_elisar_city_porcelain_crane.png',
      atlas: ATLAS,
      render_scale: 1.0,
      build: buildPorcelainCrane
    },
    {
      id: 'basalt_beetle',
      title_ru: 'Базальтовый жук-углеед',
      biome: 'ashen_lands',
      concept: 'reports/biome_mob_concepts/02_ashen_lands_basalt_beetle.png',
      atlas: ATLAS,
      render_scale: 1.0,
      build: buildBasaltBeetle
    },
    {
      id: 'ash_moth',
      title_ru: 'Пепельная моль',
      biome: 'ashen_lands',
      concept: 'reports/biome_mob_concepts/03_ashen_lands_ash_moth.png',
      atlas: ATLAS,
      render_scale: 1.0,
      build: buildAshMoth
    },
    {
      id: 'amethyst_fox',
      title_ru: 'Аметистовый лис',
      biome: 'violet_hills',
      concept: 'reports/biome_mob_concepts/04_violet_hills_amethyst_fox.png',
      atlas: ATLAS,
      render_scale: 1.0,
      build: buildAmethystFox
    }
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
    version: '0.1.0',
    blockbench: '5.2.2',
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
      version: '0.1.0',
      min_version: '5.2.0',
      variant: 'both',
      description: 'Procedural Blockbench-API source for the first four Riftworld biome mobs.',
      onload() {},
      onunload() {}
    });
  }
})(typeof window !== 'undefined' ? window : globalThis);
