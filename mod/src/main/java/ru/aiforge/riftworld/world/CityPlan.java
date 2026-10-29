package ru.aiforge.riftworld.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * City «Странники» layout, generated offline by tools/rift/gen_city.py (data/riftworld/city/plan.json).
 * Island math must stay identical to gen_city.py (Island.shapeR / t / raw).
 */
public final class CityPlan {
    public static final int CELL = 32;
    private static CityPlan instance;

    public final int ocean;
    public final List<Island> islands = new ArrayList<>();
    public final List<Road> roads = new ArrayList<>();
    public final List<Plaza> plazas = new ArrayList<>();
    public final List<Plot> plots = new ArrayList<>();
    public final List<Prop> props = new ArrayList<>();
    public final List<Bridge> bridges = new ArrayList<>();
    public final List<Channel> channels = new ArrayList<>();
    public final List<Tree> trees = new ArrayList<>();
    public final Map<String, int[]> templates = new HashMap<>();
    public final int spawnX, spawnZ;
    private final Map<Long, Bucket> grid = new HashMap<>();

    public static synchronized CityPlan get() {
        if (instance == null) {
            try (InputStream in = CityPlan.class.getResourceAsStream("/data/riftworld/city/plan.json")) {
                if (in == null) throw new IllegalStateException("plan.json missing");
                instance = new CityPlan(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return instance;
    }

    // ------------------------------------------------------------------ data
    public static final class Island {
        public final double cx, cz, R, base, depth, p1, p2, p3;
        /** 0 = city island, 1 large, 2 medium, 3 small, 4 bowl islet (wild islands of the main biome). */
        public int kind;
        public double hill = 1.0;
        /**
         * Рев 15: вытянутость острова (эллипс). У Фиолетовых холмов луг должен быть ДЛИННЫМ,
         * как на референсе 04: тогда террасы-изолинии идут длинными лентами через склон, а не
         * концентрическими кольцами «свадебного торта». 0 — круглый остров, 0.5 — эллипс 3:2.
         */
        public double elong = 0.0;
        public double elongAng = 0.0;

        public Island(double cx, double cz, double R, double base, double depth, double p1, double p2, double p3, int kind, double hill) {
            this.cx = cx; this.cz = cz; this.R = R; this.base = base; this.depth = depth;
            this.p1 = p1; this.p2 = p2; this.p3 = p3; this.kind = kind; this.hill = hill;
        }

        Island(JsonObject o) {
            cx = o.get("cx").getAsDouble(); cz = o.get("cz").getAsDouble(); R = o.get("R").getAsDouble();
            base = o.get("base").getAsDouble(); depth = o.get("depth").getAsDouble();
            JsonArray p = o.getAsJsonArray("p");
            p1 = p.get(0).getAsDouble(); p2 = p.get(1).getAsDouble(); p3 = p.get(2).getAsDouble();
        }

        double shapeR(double th) {
            return R * (1 + 0.07 * Math.sin(3 * th + p1) + 0.045 * Math.sin(5 * th + p2) + 0.025 * Math.sin(11 * th + p3));
        }

        /**
         * «Глубина» точки внутри острова: 1 в центре, 0 на кромке, &lt;0 снаружи.
         *
         * <p>Рев 15: при {@code elong > 0} остров — ЭЛЛИПС (площадь сохраняется): растяжение
         * считается в нормированных координатах, поэтому поля идут лентами вдоль длинной оси.
         */
        public double t(double x, double z) {
            double dx = x - cx, dz = z - cz;
            if (elong == 0.0) return 1 - Math.hypot(dx, dz) / shapeR(Math.atan2(dz, dx));
            double ca = Math.cos(elongAng), sa = Math.sin(elongAng);
            double u = (dx * ca + dz * sa) / (1 + elong);
            double v = (-dx * sa + dz * ca) * (1 + elong);
            return 1 - Math.hypot(u, v) / shapeR(Math.atan2(v, u));
        }

        public double raw(double x, double z) {
            double h = base + hill * (1.6 * Math.sin(x * 0.043 + p1) * Math.cos(z * 0.037 + p2) + 0.9 * Math.sin((x - z) * 0.071 + p3));
            if (kind == 1 || kind == 2) h += hill * 2.2 * Math.max(0, Math.sin(x * 0.09 + p2 * 2) * Math.sin(z * 0.08 + p3 * 2));  // knolls
            double t = t(x, z);
            if (t < 0.12) {
                double k = 1 - Math.max(t, 0) / 0.12;
                h -= k * k * 4;
            }
            return h;
        }
    }

    public record Road(double ax, double az, double bx, double bz, double w, int kind) {
    }

    public record Plaza(double x, double z, double r, int kind) {
    }

    public record Plot(String template, int x0, int z0, int w, int d, String facing, int h, int yOff, int margin) {
    }

    public record Prop(String kind, int x, int z, String facing, boolean lit, String sign) {
    }

    public record Bridge(double ax, double az, double bx, double bz, int ya, int yb, double sag, double hw) {
        public double length() {
            return Math.hypot(bx - ax, bz - az);
        }

        public int deckY(double u) {
            return (int) Math.round(ya + (yb - ya) * u - sag * 4 * u * (1 - u));
        }
    }

    /** Water channel segment: river pieces, the pond (a == b, round) and rim springs. */
    public record Channel(double ax, double az, double bx, double bz, double hw, int level, int depth) {
    }

    public static final class Tree {
        public final int x, z, kind, h, seed;
        public volatile int baseY = Integer.MIN_VALUE;

        public Tree(int x, int z, int kind, int h, int seed) {
            this.x = x; this.z = z; this.kind = kind; this.h = h; this.seed = seed;
        }

        Tree(JsonArray a) {
            x = a.get(0).getAsInt(); z = a.get(1).getAsInt(); kind = a.get(2).getAsInt(); h = a.get(3).getAsInt(); seed = a.get(4).getAsInt();
        }
    }

    public static final class Bucket {
        public final List<Road> roads = new ArrayList<>(4);
        public final List<Plaza> plazas = new ArrayList<>(1);
        public final List<Plot> plots = new ArrayList<>(2);
        public final List<Prop> props = new ArrayList<>(4);
        public final List<Bridge> bridges = new ArrayList<>(1);
        public final List<Channel> channels = new ArrayList<>(2);
        public final List<Tree> trees = new ArrayList<>(8);
    }

    private static final Bucket EMPTY = new Bucket();

    // ------------------------------------------------------------------ load
    private CityPlan(JsonObject o) {
        ocean = o.get("ocean").getAsInt();
        for (JsonElement e : o.getAsJsonArray("islands")) islands.add(new Island(e.getAsJsonObject()));
        for (JsonElement e : o.getAsJsonArray("roads")) {
            JsonObject r = e.getAsJsonObject();
            JsonArray a = r.getAsJsonArray("a"), b = r.getAsJsonArray("b");
            Road road = new Road(a.get(0).getAsDouble(), a.get(1).getAsDouble(), b.get(0).getAsDouble(), b.get(1).getAsDouble(), r.get("w").getAsDouble(), r.get("k").getAsInt());
            roads.add(road);
            double m = road.w / 2 + 1;
            index(Math.min(road.ax, road.bx) - m, Math.min(road.az, road.bz) - m, Math.max(road.ax, road.bx) + m, Math.max(road.az, road.bz) + m, bk -> bk.roads.add(road));
        }
        for (JsonElement e : o.getAsJsonArray("plazas")) {
            JsonObject r = e.getAsJsonObject();
            Plaza p = new Plaza(r.get("x").getAsDouble(), r.get("z").getAsDouble(), r.get("r").getAsDouble(), r.get("k").getAsInt());
            plazas.add(p);
            index(p.x - p.r - 1, p.z - p.r - 1, p.x + p.r + 1, p.z + p.r + 1, bk -> bk.plazas.add(p));
        }
        for (JsonElement e : o.getAsJsonArray("plots")) {
            JsonObject r = e.getAsJsonObject();
            Plot p = new Plot(r.get("t").getAsString(), r.get("x0").getAsInt(), r.get("z0").getAsInt(), r.get("w").getAsInt(), r.get("d").getAsInt(),
                    r.get("f").getAsString(), r.get("h").getAsInt(), r.get("yo").getAsInt(), r.get("m").getAsInt());
            plots.add(p);
            index(p.x0 - p.margin - 1, p.z0 - p.margin - 1, p.x0 + p.w + p.margin + 1, p.z0 + p.d + p.margin + 1, bk -> bk.plots.add(p));
        }
        for (JsonElement e : o.getAsJsonArray("props")) {
            JsonObject r = e.getAsJsonObject();
            Prop p = new Prop(r.get("k").getAsString(), r.get("x").getAsInt(), r.get("z").getAsInt(), r.get("f").getAsString(), r.get("lit").getAsBoolean(),
                    r.has("sign") ? r.get("sign").getAsString() : null);
            props.add(p);
            int ext = p.kind.equals("wheat_field") ? 14 : 4;
            index(p.x - 4, p.z - 4, p.x + ext, p.z + ext, bk -> bk.props.add(p));
        }
        for (JsonElement e : o.getAsJsonArray("bridges")) {
            JsonObject r = e.getAsJsonObject();
            JsonArray a = r.getAsJsonArray("a"), b = r.getAsJsonArray("b");
            Bridge br = new Bridge(a.get(0).getAsDouble(), a.get(1).getAsDouble(), b.get(0).getAsDouble(), b.get(1).getAsDouble(),
                    r.get("ya").getAsInt(), r.get("yb").getAsInt(), r.get("sag").getAsDouble(), r.get("hw").getAsDouble());
            bridges.add(br);
            index(Math.min(br.ax, br.bx) - 4, Math.min(br.az, br.bz) - 4, Math.max(br.ax, br.bx) + 4, Math.max(br.az, br.bz) + 4, bk -> bk.bridges.add(br));
        }
        JsonObject rv = o.getAsJsonObject("river");
        if (rv != null && rv.has("pts")) {
            JsonArray pts = rv.getAsJsonArray("pts");
            double hw = rv.get("hw").getAsDouble();
            int level = rv.get("level").getAsInt();
            for (int i = 0; i + 1 < pts.size(); ++i) {
                JsonArray a = pts.get(i).getAsJsonArray(), b = pts.get(i + 1).getAsJsonArray();
                addChannel(new Channel(a.get(0).getAsDouble(), a.get(1).getAsDouble(), b.get(0).getAsDouble(), b.get(1).getAsDouble(), hw, level, 2));
            }
            JsonArray pd = rv.getAsJsonArray("pond");
            addChannel(new Channel(pd.get(0).getAsDouble(), pd.get(1).getAsDouble(), pd.get(0).getAsDouble(), pd.get(1).getAsDouble(), rv.get("pr").getAsDouble(), level, 3));
        }
        for (JsonElement e : o.getAsJsonArray("springs")) {
            JsonObject r = e.getAsJsonObject();
            JsonArray s = r.getAsJsonArray("s"), d = r.getAsJsonArray("o");
            addChannel(new Channel(s.get(0).getAsInt() + 0.5, s.get(1).getAsInt() + 0.5, d.get(0).getAsInt() + 0.5, d.get(1).getAsInt() + 0.5, 0.75, r.get("y").getAsInt(), 1));
        }
        for (JsonElement e : o.getAsJsonArray("trees")) {
            Tree t = new Tree(e.getAsJsonArray());
            trees.add(t);
            index(t.x - 4, t.z - 4, t.x + 4, t.z + 4, bk -> bk.trees.add(t));
        }
        JsonObject tp = o.getAsJsonObject("templates");
        for (String k : tp.keySet()) {
            JsonArray a = tp.getAsJsonArray(k);
            templates.put(k, new int[]{a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()});
        }
        JsonArray sp = o.getAsJsonArray("spawn");
        spawnX = sp.get(0).getAsInt();
        spawnZ = sp.get(1).getAsInt();
    }

    private void addChannel(Channel c) {
        channels.add(c);
        double m = c.hw + 4;
        index(Math.min(c.ax, c.bx) - m, Math.min(c.az, c.bz) - m, Math.max(c.ax, c.bx) + m, Math.max(c.az, c.bz) + m, bk -> bk.channels.add(c));
    }

    private void index(double x0, double z0, double x1, double z1, java.util.function.Consumer<Bucket> add) {
        int cx0 = Math.floorDiv((int) Math.floor(x0), CELL), cx1 = Math.floorDiv((int) Math.floor(x1), CELL);
        int cz0 = Math.floorDiv((int) Math.floor(z0), CELL), cz1 = Math.floorDiv((int) Math.floor(z1), CELL);
        for (int cx = cx0; cx <= cx1; ++cx)
            for (int cz = cz0; cz <= cz1; ++cz)
                add.accept(grid.computeIfAbsent(key(cx, cz), k -> new Bucket()));
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    public Bucket at(int x, int z) {
        Bucket b = grid.get(key(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL)));
        return b == null ? EMPTY : b;
    }

    /** Island whose rim is closest (largest t, may be negative outside every island). */
    public Island nearest(double x, double z) {
        Island best = null;
        double bt = -1e9;
        for (Island i : islands) {
            double t = i.t(x, z);
            if (t > bt) { bt = t; best = i; }
        }
        for (Island i : WildIslands.near(x, z)) {
            double t = i.t(x, z);
            if (t > bt) { bt = t; best = i; }
        }
        return best;
    }

    public Island islandAt(double x, double z) {
        for (Island i : islands) {
            if (i.t(x, z) > 0) return i;
        }
        for (Island i : WildIslands.near(x, z)) {
            if (i.t(x, z) > 0) return i;
        }
        return null;
    }

    public List<Prop> propsNear(int x, int z) {
        return Collections.unmodifiableList(at(x, z).props);
    }

    /** Distance from a point to segment ab, and the segment parameter in [0,1]. */
    public static double segDist(double px, double pz, double ax, double az, double bx, double bz, double[] tOut) {
        double vx = bx - ax, vz = bz - az, L2 = vx * vx + vz * vz;
        double t = L2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * vx + (pz - az) * vz) / L2));
        if (tOut != null) tOut[0] = t;
        return Math.hypot(px - ax - vx * t, pz - az - vz * t);
    }
}
