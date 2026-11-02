package ru.aiforge.riftworld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.PaperDragonEntity;
import ru.aiforge.riftworld.net.DragonControlMsg;
import ru.aiforge.riftworld.net.Net;

/**
 * Клиентский контроллер полёта на Бумажном Драконе:
 * <ul>
 *   <li>Мышь задаёт вектор взгляда (как на элитрах);</li>
 *   <li>{@code W}/{@code S} плавно регулируют скорость от полной остановки ({@code 0}) до {@code 2x} элитр с фейерверками;</li>
 *   <li>{@code A}/{@code D} поворачивают курс с точностью ровно по градусам ({@code ±1°} за шаг);</li>
 *   <li>Отрисовывает компактный индикатор скорости, пикирования и запаса тяги подъёма.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class DragonFlightClient {
    private DragonFlightClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        LocalPlayer player = mc.player;
        if (player == null || !(player.getVehicle() instanceof PaperDragonEntity dragon)) return;
        if (!dragon.isOwnedBy(player)) return;

        byte throttle = 0;
        if (player.input.up) throttle += 1;
        if (player.input.down) throttle -= 1;

        byte yawDegStep = 0;
        if (player.input.left) yawDegStep -= 1;
        if (player.input.right) yawDegStep += 1;

        // Точная регулировка курса по градусам клавишами A / D (1° за тик без дёрганья камеры!)
        if (yawDegStep != 0) {
            float exactYaw = Math.round(player.getYRot()) + yawDegStep * 1.0F;
            player.setYRot(exactYaw);
            player.yRotO = player.getYRot();
        }

        Net.CH.sendToServer(new DragonControlMsg(throttle, yawDegStep, player.getYRot(), player.getXRot()));
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        if (!(mc.player.getVehicle() instanceof PaperDragonEntity dragon)) return;

        GuiGraphics gg = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        float speedFrac = Mth.clamp(dragon.syncedSpeedRatio(), 0.0F, 1.0F);
        float thrustFrac = Mth.clamp(dragon.upwardThrust(), 0.0F, 1.0F);
        boolean diving = dragon.isDiving();

        int barW = 142;
        int x0 = (sw - barW) / 2;
        int y0 = sh - 56;

        // Полоска скорости (0 .. 2x Элитр с фейерверками)
        gg.fill(x0 - 1, y0 - 1, x0 + barW + 1, y0 + 5, 0xAA10141C);
        int spW = Math.round(barW * speedFrac);
        int spColor = diving ? 0xFF4CE8FF : 0xFFF5D058;
        if (spW > 0) gg.fill(x0, y0, x0 + spW, y0 + 4, spColor);

        // Полоска тяги подъёма
        int y1 = y0 + 7;
        gg.fill(x0 - 1, y1 - 1, x0 + barW + 1, y1 + 4, 0xAA10141C);
        int thW = Math.round(barW * thrustFrac);
        int thColor = thrustFrac < 0.20F ? 0xFFEE4848 : 0xFF66E29A;
        if (thW > 0) gg.fill(x0, y1, x0 + thW, y1 + 3, thColor);

        String label = diving
                ? String.format("ПИКИРОВАНИЕ  %.0f%%  |  Курс %d°", speedFrac * 200.0F, Math.floorMod(Math.round(Mth.wrapDegrees(dragon.getYRot())), 360))
                : String.format("Скорость %.0f%%  |  Тяга %.0f%%  |  %d°", speedFrac * 200.0F, thrustFrac * 100.0F, Math.floorMod(Math.round(Mth.wrapDegrees(dragon.getYRot())), 360));
        gg.drawCenteredString(mc.font, label, sw / 2, y0 - 10, diving ? 0x80F4FF : 0xF5EFE0);
    }
}
