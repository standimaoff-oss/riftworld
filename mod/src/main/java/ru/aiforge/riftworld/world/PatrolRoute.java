package ru.aiforge.riftworld.world;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * v0.49.0 — маршруты патрулирования стражей Элисара.
 *
 * <p>Страж больше не бродит наугад: он ходит по настоящим дорогам и мостам
 * города, которые уже описаны в {@link CityPlan}. Класс строит для каждого
 * дорожного сегмента ломаный путь из опорных точек и умеет найти ближайший
 * к точке сегмент, чтобы страж мог «подхватить» маршрут.</p>
 *
 * <p>Дорога задаётся двумя концами {@code (ax,az)-(bx,bz)} и шириной
 * {@code w}; мост — двумя концами и высотами настила {@code ya/yb}.</p>
 */
public final class PatrolRoute {

    /** Одна опорная точка маршрута: позиция в мире и направление движения. */
    public record Waypoint(double x, double y, double z, double yaw) {
    }

    /** Тип опоры: дорога, мост, площадь. */
    public enum Kind {
        ROAD,
        BRIDGE,
        PLAZA
    }

    private final Kind kind;
    private final List<Waypoint> points;

    private PatrolRoute(Kind kind, List<Waypoint> points) {
        this.kind = kind;
        this.points = List.copyOf(points);
    }

    public Kind kind() {
        return this.kind;
    }

    public List<Waypoint> points() {
        return this.points;
    }

    public boolean isEmpty() {
        return this.points.isEmpty();
    }

    public int size() {
        return this.points.size();
    }

    /** Ближайшая точка маршрута к заданной позиции. */
    public Waypoint nearest(double x, double y, double z) {
        Waypoint best = points.get(0);
        double bestD = Double.MAX_VALUE;
        for (Waypoint p : points) {
            double d = (p.x() - x) * (p.x() - x) + (p.y() - y) * (p.y() - y) + (p.z() - z) * (p.z() - z);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** Индекс ближайшей точки — нужен, чтобы начать патруль с неё. */
    public int nearestIndex(double x, double y, double z) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < points.size(); i++) {
            Waypoint p = points.get(i);
            double d = (p.x() - x) * (p.x() - x) + (p.y() - y) * (p.y() - y) + (p.z() - z) * (p.z() - z);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Построение маршрутов
    // ------------------------------------------------------------------

    /**
     * Разбить отрезок дороги на опорные точки с шагом около {@code step} блоков.
     * Точки лежат точно на оси дороги, а высота берётся из реального блока
     * под точкой, чтобы страж не проваливался и не перепрыгивал мост.
     */
    public static PatrolRoute road(CityPlan.Road road, LevelReader level, double step) {
        List<Waypoint> pts = new ArrayList<>();
        double dx = road.bx() - road.ax();
        double dz = road.bz() - road.az();
        double len = Math.hypot(dx, dz);
        if (len < 1.0E-4) {
            return new PatrolRoute(Kind.ROAD, pts);
        }
        int steps = Math.max(1, (int) Math.ceil(len / Math.max(1.0, step)));
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = road.ax() + dx * t;
            double z = road.az() + dz * t;
            double y = groundY(level, x, z);
            pts.add(new Waypoint(x, y, z, yaw));
        }
        return new PatrolRoute(Kind.ROAD, pts);
    }

    /**
     * Маршрут по мосту: точки следуют настилу и учитывают прогиб
     * {@link CityPlan.Bridge#deckY(double)}.
     */
    public static PatrolRoute bridge(CityPlan.Bridge bridge, double step) {
        List<Waypoint> pts = new ArrayList<>();
        double dx = bridge.bx() - bridge.ax();
        double dz = bridge.bz() - bridge.az();
        double len = bridge.length();
        if (len < 1.0E-4) {
            return new PatrolRoute(Kind.BRIDGE, pts);
        }
        int steps = Math.max(1, (int) Math.ceil(len / Math.max(1.0, step)));
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = bridge.ax() + dx * t;
            double z = bridge.az() + dz * t;
            double y = bridge.deckY(t) + 1.0;
            pts.add(new Waypoint(x, y, z, yaw));
        }
        return new PatrolRoute(Kind.BRIDGE, pts);
    }

    /** Круговой маршрут по площади. */
    public static PatrolRoute plaza(CityPlan.Plaza plaza, LevelReader level, int steps) {
        List<Waypoint> pts = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            double a = (Math.PI * 2.0 * i) / steps;
            double x = plaza.x() + Math.cos(a) * plaza.r();
            double z = plaza.z() + Math.sin(a) * plaza.r();
            double y = groundY(level, x, z);
            pts.add(new Waypoint(x, y, z, Math.toDegrees(a) + 90.0));
        }
        return new PatrolRoute(Kind.PLAZA, pts);
    }

    /** Высота поверхности под точкой (верх твёрдого блока). */
    private static double groundY(LevelReader level, double x, double z) {
        int bx = net.minecraft.util.Mth.floor(x);
        int bz = net.minecraft.util.Mth.floor(z);
        int top = net.minecraft.util.Mth.floor(
                level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz));
        return top + 1.0;
    }

    /**
     * Найти ближайший маршрут к точке среди дорог, мостов и площадей.
     *
     * @param maxDist максимальное горизонтальное расстояние до сегмента
     * @return маршрут или {@code null}, если рядом нет ни дороги, ни моста
     */
    @Nullable
    public static PatrolRoute nearestTo(CityPlan plan, LevelReader level,
                                        double x, double y, double z, double maxDist) {
        CityPlan.Bucket bucket = plan.at(net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(z));
        PatrolRoute best = null;
        double bestD = maxDist * maxDist;

        for (CityPlan.Road road : bucket.roads) {
            double d = distanceToSegment(x, z, road.ax(), road.az(), road.bx(), road.bz());
            if (d < bestD) {
                bestD = d;
                best = road(road, level, 6.0);
            }
        }
        for (CityPlan.Bridge bridge : bucket.bridges) {
            double d = distanceToSegment(x, z, bridge.ax(), bridge.az(), bridge.bx(), bridge.bz());
            if (d < bestD) {
                bestD = d;
                best = bridge(bridge, 6.0);
            }
        }
        for (CityPlan.Plaza plaza : bucket.plazas) {
            double d = Math.hypot(plaza.x() - x, plaza.z() - z) - plaza.r();
            if (d < bestD) {
                bestD = Math.max(0.0, d);
                best = plaza(plaza, level, 8);
            }
        }
        return best;
    }

    /** Кратчайшее расстояние от точки до отрезка на плоскости XZ. */
    public static double distanceToSegment(double px, double pz,
                                           double ax, double az, double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        double lenSq = dx * dx + dz * dz;
        if (lenSq < 1.0E-8) {
            return Math.hypot(px - ax, pz - az);
        }
        double t = ((px - ax) * dx + (pz - az) * dz) / lenSq;
        t = Math.max(0.0, Math.min(1.0, t));
        double cx = ax + dx * t;
        double cz = az + dz * t;
        return Math.hypot(px - cx, pz - cz);
    }

    /** Все маршруты в окрестности точки — для распределения стражей по городу. */
    public static List<PatrolRoute> routesNear(CityPlan plan, LevelReader level,
                                                double x, double z, double radius, int limit) {
        List<PatrolRoute> out = new ArrayList<>();
        CityPlan.Bucket bucket = plan.at(net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(z));
        for (CityPlan.Road road : bucket.roads) {
            if (out.size() >= limit) break;
            out.add(road(road, level, 6.0));
        }
        for (CityPlan.Bridge bridge : bucket.bridges) {
            if (out.size() >= limit) break;
            out.add(bridge(bridge, 6.0));
        }
        for (CityPlan.Plaza plaza : bucket.plazas) {
            if (out.size() >= limit) break;
            out.add(plaza(plaza, level, 8));
        }
        return out;
    }
}
