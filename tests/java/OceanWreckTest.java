import ru.aiforge.riftworld.world.DeepOcean;
import ru.aiforge.riftworld.world.OceanWreck;

public class OceanWreckTest {
    public static void main(String[] args) {
        for (int rx = -3; rx <= 3; rx++) for (int rz = -3; rz <= 3; rz++) {
            var r = DeepOcean.region(rx, rz);
            int count = 0, mast = 0, rails = 0;
            for (int x = r.x() + 570; x <= r.x() + 630; x++)
                for (int z = r.z() - 10; z <= r.z() + 10; z++)
                    for (int y = 56; y <= 85; y++) {
                        int m = OceanWreck.materialAt(x, y, z, 62);
                        if (m != OceanWreck.NONE) {
                            if (DeepOcean.openCoreChunk(x, z) || !DeepOcean.contains(x, z)) throw new AssertionError("wreck outside safe ocean");
                            count++;
                            if (m == OceanWreck.RAIL) rails++;
                            if (x == r.x() + 609 && z == r.z() && y > 65) mast++;
                        }
                    }
            if (count < 500 || mast < 10 || rails < 10) throw new AssertionError("wreck incomplete " + count + ", " + mast + ", rails=" + rails);
        }
        System.out.println("49 deterministic ocean wrecks stay outside the Maja core and contain hull/mast");
    }
}
