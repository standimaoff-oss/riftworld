# Blockbench MCP Models: Liopleurodon & Pegasus (`v0.41`)

High-detail 3D remakes of Riftworld's underwater Pliosaur Leviathan (**`riftworld:liopleurodon`**) and celestial winged steed (**`riftworld:pegasus`**) authored via **Blockbench MCP** (`blockbench-mcp-headless` v1.10.1 over `@modelcontextprotocol/sdk` `StdioClientTransport`) and verified in-game in **Minecraft 1.20.1 (Forge 47.4.23)** rendered via **`Xvfb` + `Mesa llvmpipe`** (`1920x1080`).

## Summary Metrics

| Creature | Source `.bbmodel` | Articulated Bones | Sculpted Cuboids | UV Atlas | UV Overlaps | `bbmodel_validate` |
|---|---|---:|---:|---|---:|---|
| **Liopleurodon** (`riftworld:liopleurodon`) | `reports/blockbench_models/liopleurodon.bbmodel` | **24** | **234** *(was 66)* | **`1024x1024`** + **`1024x1024` emissive eyes** *(was 512x512)* | **0** | `0 errors, 0 warnings` (`passed: true`) |
| **Pegasus** (`riftworld:pegasus`) | `reports/blockbench_models/pegasus.bbmodel` | **31** | **177** *(was 54)* | **`512x512`** | **0** | `0 errors, 0 warnings` (`passed: true`) |

## Anatomical & Visual Highlights

### 1. Liopleurodon — Abyssal Pliosaur Leviathan (12.25 blocks long)
- **Authentic Pliosaur Skull & Terminal Rosette**: Recessed temporal fenestra cheeks, armored supraorbital brow crests, retracted dorsal marine-reptile nostrils, pre-rosette maxillary constriction, spoon-shaped terminal premaxillary & mandibular rosettes, glistening crimson palatal rugae, fleshy tongue, and **52 individual heterodont fluted ivory tusks & interlocking fangs**.
- **Emissive Deep-Sea Predator Eyes**: Vertical-slit golden-amber irises (`textures/entity/liopleurodon_eyes.png`) rendered with `RenderType.eyes` + `LightTexture.FULL_BRIGHT`.
- **4 Multi-Jointed Hydrofoil Megapaddles & Crescent Caudal Fluke**: 3-segment cambered pectoral & pelvic flippers (`fin_front_1..3_l/r`, `fin_back_1..3_l/r`) with dorsal slate-navy / ventral seafoam countershading, 3-row dorsal osteoderm scutes, multi-cuboid 3D battle scars, and a 4-stage muscular tail ending in a vertical marine-reptile fluke.

### 2. Pegasus — Celestial Winged Steed of Elisar
- **Sculpted Equine Anatomy**: Proud arched stallion neck (`neck_lower`, `neck_upper`), tucked Arabian head carriage, velvety muzzle, twilight-sapphire eyes, cascading silver-pearl mane & forelock, 3-segment flowing tail plume, and 4 three-jointed equine legs (`upper/thigh -> cannon -> fetlock/pastern/obsidian-gold hoof` resting flush at `Y = 0.0`).
- **Dual-Posture 3-Tiered Feathered Wings (5.3-block span)**: 56 individual wing cuboids arranged in three shingled anatomical tiers (8 graduated slotted primary remiges per wing, long secondaries, greater/median coverts, alula tuft) that fold sleekly along the flanks on land (clear of the rider's seat) and unfurl into a full 5.3-block wingspan in flight.
- **State-Driven Royal Elisar Tack**: `head_tack` (golden jeweled bridle) appears when tamed/saddled; `saddle_tack` (tooled mahogany leather saddle with brass stitching, golden pommel & cantle, quilted cream blanket, stirrups, and celadon-turquoise breastcollar medallion) toggles dynamically via `entity.hasSaddle()`.

## Deliverables in `reports/blockbench_models/`

- `liopleurodon.bbmodel`, `liopleurodon.geo.json`, `liopleurodon.animation.json` — Blockbench 5.0 source project & Bedrock exports.
- `pegasus.bbmodel`, `pegasus.geo.json`, `pegasus.animation.json` — Blockbench 5.0 source project & Bedrock exports.
- `mcp_validation_report.json` — Full `bbmodel_validate` (`self_test: true`) & `bbmodel_validate_animations` gate report.
- `01_liopleurodon_blockbench_sheet.png`, `02_pegasus_blockbench_sheet.png` — Multi-angle Blockbench `.bbmodel` turnaround & close-up sheets.
- `ingame/03_ingame_liopleurodon_hero.png` — Real Minecraft 1.20.1 (`Xvfb` + `Mesa llvmpipe`) 3/4 underwater hero view in `riftworld:elisar`.
- `ingame/04_ingame_liopleurodon_jaws.png` — Real Minecraft 1.20.1 close-up of skull, rosette tusks, crimson palate, and glowing amber eyes.
- `ingame/05_ingame_liopleurodon_dorsal.png` — Real Minecraft 1.20.1 dorsal/flank view of hydrofoil megapaddles, scutes, and tail fluke.
- `ingame/06_ingame_pegasus_wild.png` — Real Minecraft 1.20.1 wild Pegasus standing on Elisar garden grass with folded swan-wings.
- `ingame/07_ingame_pegasus_saddled.png` — Real Minecraft 1.20.1 tamed & saddled Royal Elisar Pegasus.
- `ingame/08_ingame_pegasus_flight.png` — Real Minecraft 1.20.1 airborne Pegasus soaring with full 5.3-block 3-tiered feathered wingspan.
