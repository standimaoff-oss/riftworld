package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Cold-cache races across independent worldgen workers must not deadlock or return duplicate islands. */
public final class WorldgenConcurrencyProbe {
    private static final int WORKERS = 6;
    private record Result(WildIslands.Cell cell, CrystalRifts.Region region, BlacktideCitadel.Site site) {}

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        ExecutorService pool = Executors.newFixedThreadPool(WORKERS, r -> {
            Thread t = new Thread(r, "worldgen-race-probe");
            t.setDaemon(true); // report a deadlock as a failed task, not a stuck CI job
            return t;
        });
        try {
            int populated = 0;
            for (int sample = 0; sample < 8; ++sample) {
                // Different positive/negative keys keep these races cold; the
                // same keys inside a round force duplicate concurrent requests.
                int x = 11 + sample * 7, z = -23 - sample * 5;
                CyclicBarrier start = new CyclicBarrier(WORKERS);
                List<Future<Result>> results = new ArrayList<>();
                for (int worker = 0; worker < WORKERS; ++worker) {
                    results.add(pool.submit(() -> {
                        start.await(30, TimeUnit.SECONDS);
                        return new Result(WildIslands.cell(x, z), CrystalRifts.region(x, z),
                                BlacktideCitadel.siteInCell(x, z));
                    }));
                }
                Result expected = results.get(0).get(90, TimeUnit.SECONDS);
                if (!expected.cell().islands.isEmpty()) ++populated;
                for (Future<Result> f : results) {
                    Result actual = f.get(90, TimeUnit.SECONDS);
                    if (actual.cell() != expected.cell() || actual.region() != expected.region()
                            || actual.site() != expected.site()) {
                        throw new AssertionError("non-canonical cache result at " + x + "," + z);
                    }
                }
                if (WildIslands.cell(x, z) != expected.cell() || CrystalRifts.region(x, z) != expected.region())
                    throw new AssertionError("cache identity changed after the race");
            }
            if (populated == 0) throw new AssertionError("probe exercised no wild islands");
            System.out.println("OK: eight cold, signed-coordinate worldgen cache races; " + populated + " populated cells");
        } finally {
            pool.shutdownNow();
        }
    }
}
