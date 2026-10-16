package ru.aiforge.riftworld.client;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;

/**
 * «ФОТО ИЗ ИГРЫ ПРЯМ»: планировщик игровых скриншотов (rev 7b).
 * /riftshot &lt;spot|tour&gt; телепортирует и ставит время на сервере, а клиент через 40 тиков
 * снимает кадр штатным Screenshot.saveScreenshot в &lt;gameDir&gt;/riftshots.
 * tour = все точки подряд (каждые 140 тиков). Ноль пакетов: очередь живёт на клиенте.
 * Headless-CI: -Driftworld.shotauto=1 запускает тур сам сразу после входа на сервер.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShotTour {
    private static final Deque<Task> QUEUE = new ArrayDeque<>();
    public static final String[] TOUR = {"temple", "city", "ocean", "bridge", "gardens"};

    private record Task(int t, String cmd) {
    }

    private ShotTour() {
    }

    public static void scheduleShot(int delayTicks) {
        QUEUE.addLast(new Task(delayTicks, null));
    }

    private static void scheduleCmd(int delayTicks, String cmd) {
        QUEUE.addLast(new Task(delayTicks, cmd));
    }

    public static void startTour() {
        for (int i = 0; i < TOUR.length; ++i) scheduleCmd(i * 140 + 20, "riftshot " + TOUR[i]);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        if (QUEUE.isEmpty()) return;
        Task head = QUEUE.peekFirst();
        if (head.t() > 1) {
            QUEUE.pollFirst();
            QUEUE.addFirst(new Task(head.t() - 1, head.cmd()));
            return;
        }
        QUEUE.pollFirst();
        if (head.cmd() != null) {
            mc.player.connection.sendCommand(head.cmd());
        } else {
            shoot(mc);
        }
    }

    private static void shoot(Minecraft mc) {
        try {
            File dir = new File(mc.gameDirectory, "riftshots");
            Screenshot.saveScreenshot(dir, mc.getWindow().getWidth(), mc.getWindow().getHeight(),
                    mc.getMainRenderTarget(),
                    c -> mc.player.sendSystemMessage(Component.literal("[riftshot] " + c.getString())));
        } catch (Exception ex) {
            mc.player.sendSystemMessage(Component.literal("[riftshot] error: " + ex));
        }
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn e) {
        if ("1".equals(System.getProperty("riftworld.shotauto"))) {
            scheduleCmd(240, "riftshot tour");
        }
    }
}
