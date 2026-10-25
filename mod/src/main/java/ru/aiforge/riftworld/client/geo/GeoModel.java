package ru.aiforge.riftworld.client.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Рев 11 — МОБ НА JSON. Модель существа целиком описана файлом {@code assets/riftworld/geo/<имя>.json}:
 * кости (pivot + rotation + необязательный родитель), коробки (origin/size/uv) и анимация каждой кости.
 *
 * <p>Раньше рыбы рисовались «на лету» дебаг-квадами с белой текстурой — отсюда и «поломанные без текстур».
 * Теперь это обычные ванильные {@link ModelPart}, собранные из JSON: у них есть настоящий атлас,
 * корректные UV, освещение и скелетная анимация (хвост машет, плавники трепещут, щупальца колышутся).
 *
 * <p>Формат: <pre>
 * { "texture_width": 64, "texture_height": 64, "render_scale": 0.75, "translucent": false,
 *   "bones": [ { "name": "tail", "parent": "body", "pivot": [0,0,4], "rotation": [0,0,0],
 *                "anim": {"axis": "y", "amp": 22, "speed": 1.7, "phase": 0.0},
 *                "cubes": [ {"origin": [-1,0,0], "size": [2,8,4], "uv": [0,0]} ] } ] }
 * </pre>
 */
public final class GeoModel {
    /** Кость с собственной анимацией: поворот вокруг своей оси на amp·sin(speed·t + phase) градусов. */
    private record Bone(ModelPart part, float baseX, float baseY, float baseZ,
                        char axis, float amp, float speed, float phase) {
    }

    public final ModelPart root;
    public final int texWidth;
    public final int texHeight;
    public final float renderScale;
    public final boolean translucent;
    private final List<Bone> bones;

    private GeoModel(ModelPart root, int texWidth, int texHeight, float renderScale, boolean translucent, List<Bone> bones) {
        this.root = root;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.renderScale = renderScale;
        this.translucent = translucent;
        this.bones = bones;
    }

    /** Покадровая анимация: сбрасываем позы костей и заново считаем синус. */
    public void animate(float time) {
        for (Bone b : bones) {
            b.part().xRot = b.baseX();
            b.part().yRot = b.baseY();
            b.part().zRot = b.baseZ();
            if (b.amp() == 0.0F) continue;
            float a = (float) Math.toRadians(b.amp() * Math.sin(time * 0.18F * b.speed() + b.phase()));
            switch (b.axis()) {
                case 'x' -> b.part().xRot += a;
                case 'z' -> b.part().zRot += a;
                default -> b.part().yRot += a;
            }
        }
    }

    /**
     * Рисуем ВСЁ дерево костей: {@code ModelPart.render} рисует только собственные кубы и не спускается
     * к детям — из-за этого раньше выходил один серый ящик вместо рыбы.
     */
    public void render(PoseStack pose, VertexConsumer vc, int light, int overlay) {
        for (ModelPart part : this.root.getAllParts().toList()) {
            part.render(pose, vc, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    // ------------------------------------------------------------------ загрузка с диска
    private static final Map<String, GeoModel> CACHE = new HashMap<>();

    /** Загруженная (и закешированная) модель по имени вида; null — если файла нет, чтобы не падать в игре. */
    public static GeoModel of(String name) {
        return CACHE.computeIfAbsent(name, GeoModel::loadUncached);
    }

    /** Достаёт вложенную кость по цепочке родителей: root -> body -> head. */
    private static ModelPart resolve(ModelPart root, Map<String, String> parentOf, String bone) {
        java.util.ArrayDeque<String> path = new java.util.ArrayDeque<>();
        String cur = bone;
        while (cur != null) {
            path.addFirst(cur);
            cur = parentOf.get(cur);
        }
        ModelPart part = root;
        for (String seg : path) part = part.getChild(seg);
        return part;
    }

    private static GeoModel loadUncached(String name) {
        ResourceLocation loc = new ResourceLocation("riftworld", "geo/" + name + ".json");
        try (InputStreamReader r = new InputStreamReader(
                Minecraft.getInstance().getResourceManager().getResource(loc).orElseThrow().open(), StandardCharsets.UTF_8)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            int tw = o.has("texture_width") ? o.get("texture_width").getAsInt() : 64;
            int th = o.has("texture_height") ? o.get("texture_height").getAsInt() : 64;
            float scale = o.has("render_scale") ? o.get("render_scale").getAsFloat() : 1.0F;
            boolean translucent = o.has("translucent") && o.get("translucent").getAsBoolean();

            MeshDefinition mesh = new MeshDefinition();
            PartDefinition rootDef = mesh.getRoot();
            Map<String, PartDefinition> defs = new HashMap<>();
            List<JsonObject> jsonBones = new ArrayList<>();
            for (var e : o.getAsJsonArray("bones")) jsonBones.add(e.getAsJsonObject());

            // пивоты всех костей: в JSON они заданы в АБСОЛЮТНЫХ координатах модели
            Map<String, float[]> pivots = new HashMap<>();
            Map<String, String> parents = new HashMap<>();
            for (JsonObject b : jsonBones) {
                JsonArray pv = b.getAsJsonArray("pivot");
                pivots.put(b.get("name").getAsString(),
                        new float[]{pv.get(0).getAsFloat(), pv.get(1).getAsFloat(), pv.get(2).getAsFloat()});
                parents.put(b.get("name").getAsString(),
                        b.has("parent") ? b.get("parent").getAsString() : null);
            }

            // первый проход: все кости как узлы скелета
            for (JsonObject b : jsonBones) {
                String boneName = b.get("name").getAsString();
                String parentName = parents.get(boneName);
                PartDefinition parent = parentName == null ? rootDef : defs.get(parentName);
                if (parent == null) throw new IllegalStateException("кость " + boneName + ": неизвестный родитель");
                float[] pivot = pivots.get(boneName);
                float[] parentPivot = parentName == null ? new float[]{0, 0, 0} : pivots.get(parentName);
                CubeListBuilder cubes = CubeListBuilder.create();
                if (b.has("cubes")) {
                    for (var ce : b.getAsJsonArray("cubes")) {
                        JsonObject c = ce.getAsJsonObject();
                        JsonArray org = c.getAsJsonArray("origin");
                        JsonArray size = c.getAsJsonArray("size");
                        JsonArray uv = c.getAsJsonArray("uv");
                        cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt());
                        if (c.has("mirror") && c.get("mirror").getAsBoolean()) cubes.mirror();
                        // коробка задана в абсолютных координатах модели -> переводим в локальные координаты кости
                        cubes.addBox(org.get(0).getAsFloat() - pivot[0], org.get(1).getAsFloat() - pivot[1],
                                org.get(2).getAsFloat() - pivot[2],
                                size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt());
                    }
                }
                float rx = 0, ry = 0, rz = 0;
                if (b.has("rotation")) {
                    JsonArray rot = b.getAsJsonArray("rotation");
                    rx = (float) Math.toRadians(rot.get(0).getAsFloat());
                    ry = (float) Math.toRadians(rot.get(1).getAsFloat());
                    rz = (float) Math.toRadians(rot.get(2).getAsFloat());
                }
                // смещение кости — от родительского пивота (иначе скелет «схлопывается» в точку)
                defs.put(boneName, parent.addOrReplaceChild(boneName, cubes,
                        PartPose.offsetAndRotation(pivot[0] - parentPivot[0], pivot[1] - parentPivot[1],
                                pivot[2] - parentPivot[2], rx, ry, rz)));
            }

            ModelPart baked = LayerDefinition.create(mesh, tw, th).bakeRoot();
            // модель вложенная: кость лежит у своего родителя, поэтому путь собираем по цепочке родителей
            List<Bone> bones = new ArrayList<>();
            for (JsonObject b : jsonBones) {
                ModelPart part = resolve(baked, parents, b.get("name").getAsString());
                JsonObject anim = b.has("anim") ? b.getAsJsonObject("anim") : null;
                char axis = anim != null && anim.has("axis") ? anim.get("axis").getAsString().charAt(0) : 'y';
                float amp = anim != null && anim.has("amp") ? anim.get("amp").getAsFloat() : 0.0F;
                float speed = anim != null && anim.has("speed") ? anim.get("speed").getAsFloat() : 1.0F;
                float phase = anim != null && anim.has("phase") ? anim.get("phase").getAsFloat() : 0.0F;
                bones.add(new Bone(part, part.xRot, part.yRot, part.zRot, axis, amp, speed, phase));
            }
            return new GeoModel(baked, tw, th, scale, translucent, bones);
        } catch (Exception ex) {
            System.err.println("[riftworld] не удалось загрузить geo-модель " + name + ": " + ex);
            return null;
        }
    }
}
