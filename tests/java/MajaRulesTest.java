import ru.aiforge.riftworld.world.DeepOcean;
import ru.aiforge.riftworld.world.MajaSafetyPolicy;

public final class MajaRulesTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for (int cx = -12; cx <= 12; cx++) for (int cz = -12; cz <= 12; cz++) {
            var region = DeepOcean.region(cx, cz);
            check(region.equals(DeepOcean.region(cx, cz)), "deterministic centre");
            check(region.equals(DeepOcean.regionAt(region.x(), region.z())), "signed cell mapping");
            check(DeepOcean.contains(region.x(), region.z()), "centre is ocean");
            check(DeepOcean.weight(region.x(), region.z()) == 1.0, "centre has full depth");
            check(!DeepOcean.contains(region.x() + region.radius() + 2, region.z()), "outside radius");
            check(DeepOcean.weight(region.x() + region.radius() - 90, region.z()) > 0.0, "smooth edge");
            check(DeepOcean.weight(region.x() + region.radius() - 90, region.z()) < 1.0, "smooth edge range");
            check(!region.equals(DeepOcean.region(cx + 1, cz)), "different neighbouring cells");
        }
        for (int count = 0; count < 5; count++) {
            check(MajaSafetyPolicy.mayDevour(true, count, false, true), "0..4 allowed");
            check(!MajaSafetyPolicy.mayDevour(true, count, true, true), "nearby player blocks feeding");
            check(!MajaSafetyPolicy.mayDevour(false, count, false, true), "unknown/unloaded chunks block feeding");
            check(!MajaSafetyPolicy.mayDevour(true, count, false, false), "mobGriefing disabled");
        }
        check(!MajaSafetyPolicy.mayDevour(true, 5, false, true), "five player blocks block feeding");
        check(!MajaSafetyPolicy.mayDevour(true, -1, false, true), "invalid count fails closed");
        System.out.println("OK: ocean regions, negative coordinates, coast blending, and island safety gates");
    }
}
