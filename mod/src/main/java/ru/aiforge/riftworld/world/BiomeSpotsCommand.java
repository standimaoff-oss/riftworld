package ru.aiforge.riftworld.world;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;

/**
 * {@code /riftbiomes} — координаты ближайших к началу мира пятен биомов (rev 9).
 * Нужен и игроку (куда лететь), и CI: клиентский шаг берёт отсюда точки съёмки,
 * а не захардкоженные числа, — поэтому камеры всегда попадают в биом.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BiomeSpotsCommand {
    private static final int RADIUS_CELLS = 6;

    private BiomeSpotsCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("riftbiomes").requires(s -> s.hasPermission(2)).executes(c -> {
            String line = line();
            c.getSource().sendSuccess(() -> Component.literal(line), false);
            return 1;
        }));
    }

    /** Строка вида: BIOMES violet=1234,5678,101;terraced=2345,6789,150;ocean=45 */
    public static String line() {
        StringBuilder sb = new StringBuilder("BIOMES");
        int ocean = CityPlan.get().ocean;
        sb.append(" ocean=").append(ocean);
        for (int kind = 0; kind < 2; ++kind) {
            String tag = kind == 0 ? "violet" : "terraced";
            int best = Integer.MAX_VALUE;
            String found = null;
            for (int sx = -RADIUS_CELLS; sx <= RADIUS_CELLS; ++sx) {
                for (int sz = -RADIUS_CELLS; sz <= RADIUS_CELLS; ++sz) {
                    int cx, cz, top;
                    if (kind == 0) {
                        VioletHills.Region rg = VioletHills.region(sx, sz);
                        if (rg == null) continue;
                        cx = rg.cx;
                        cz = rg.cz;
                        top = (int) Math.floor(rg.hills(cx + 0.5, cz + 0.5));
                    } else {
                        TerracedMountains.Region rg = TerracedMountains.region(sx, sz);
                        if (rg == null) continue;
                        cx = rg.cx;
                        cz = rg.cz;
                        top = (int) Math.floor(TerracedMountains.top(rg, cx + 0.5, cz + 0.5));
                    }
                    int dist = Math.abs(cx) + Math.abs(cz);
                    if (dist < best) {
                        best = dist;
                        found = tag + "=" + cx + "," + cz + "," + top;
                    }
                }
            }
            if (found != null) sb.append(';').append(found);
        }
        return sb.toString();
    }
}
