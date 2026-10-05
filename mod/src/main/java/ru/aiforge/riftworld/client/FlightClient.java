package ru.aiforge.riftworld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.net.FlightMsg;

/** Client half of the flight: mirrors the server clock, pins the player in the capsule, shake, white flashes, loading mask. */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class FlightClient {
    public static int phase;
    public static BlockPos core = BlockPos.ZERO;
    /** Ticks elapsed in the current phase. */
    public static int tick;
    /** Last elapsed value the server sent: the client clock never runs far ahead of it (server hiccups). */
    private static int synced;

    private FlightClient() {}

    public static void onMsg(FlightMsg m) {
        if (m.phase() != phase || Math.abs(m.elapsed() - tick) > 2) tick = m.elapsed();
        synced = m.elapsed();
        phase = m.phase();
        core = m.pos();
        Flight.clientLocked = phase != Flight.NONE;
    }

    public static float time(float pt) {
        return tick + pt;
    }

    /** Capsule offset above its landed position for the current phase. */
    public static float offset(float pt) {
        float t = time(pt);
        return phase == Flight.LIFT ? Flight.liftOffset(t) : phase == Flight.ARRIVE ? Flight.arriveOffset(t) : 0;
    }

    private static Vec3 want() {
        return Vec3.atBottomCenterOf(core).add(0, 1 + offset(0), 0);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || phase == Flight.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        int len = phase == Flight.LIFT ? Flight.LIFT_T : phase == Flight.TRANSIT ? Flight.TRANSIT_T : Flight.ARRIVE_T;
        tick = Math.min(Math.min(tick + 1, synced + 8), len);
        if (mc.player == null) return;
        Vec3 w = want();
        mc.player.setPos(w.x, w.y, w.z);
        mc.player.setDeltaMovement(Vec3.ZERO);
        mc.player.fallDistance = 0;
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent e) {
        if (phase == Flight.NONE) return;
        var in = e.getInput();
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = in.jumping = in.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void keys(InputEvent.InteractionKeyMappingTriggered e) {
        if (phase == Flight.NONE) return;
        e.setSwingHand(false);
        e.setCanceled(true);
    }

    /** Lift-off rumble, a steady hum in transit and the bump of the landing. */
    @SubscribeEvent
    public static void shake(ViewportEvent.ComputeCameraAngles e) {
        if (phase == Flight.NONE) return;
        float t = time((float) e.getPartialTick());
        float k = phase == Flight.LIFT ? 0.35F + 0.65F * Mth.clamp(t / 30F, 0, 1)
                : phase == Flight.TRANSIT ? 0.12F + 0.5F * bell(t / Flight.TRANSIT_T, 0.3F, 0.65F)
                : Math.max(0.15F, 1.2F * Mth.clamp((t - 50F) / 6F, 0, 1) * Mth.clamp((62F - t) / 6F, 0, 1));
        e.setPitch(e.getPitch() + k * (Mth.sin(t * 2.9F) * 0.35F + Mth.sin(t * 7.3F) * 0.2F));
        e.setRoll(e.getRoll() + k * Mth.sin(t * 2.1F + 1) * 0.45F);
    }

    static float bell(float u, float a, float b) {
        float up = Mth.clamp((u - (a - 0.12F)) / 0.12F, 0, 1), down = Mth.clamp((b + 0.12F - u) / 0.12F, 0, 1);
        return Math.min(up, down);
    }

    /** White of the jump: 0..1. */
    static float flash(float pt) {
        float t = time(pt);
        return switch (phase) {
            case Flight.LIFT -> Mth.clamp((t - 30F) / 10F, 0, 1);
            case Flight.TRANSIT -> Math.max(Mth.clamp(1 - t / 14F, 0, 1), Mth.clamp((t - 186F) / 14F, 0, 1));
            case Flight.ARRIVE -> Mth.clamp(1 - t / 14F, 0, 1);
            default -> 0;
        };
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Post e) {
        if (phase == Flight.NONE) return;
        float a = flash(e.getPartialTick());
        if (a <= 0.003F) return;
        GuiGraphics g = e.getGuiGraphics();
        int al = Mth.clamp((int) (a * 255), 0, 255);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (al << 24) | 0xFFFFFF);
    }

    /** The "loading terrain" screen of the dimension change is replaced by plain white (the flash keeps going). */
    @SubscribeEvent
    public static void screen(ScreenEvent.Opening e) {
        if (phase == Flight.NONE) return;
        var n = e.getNewScreen();
        if (n instanceof ReceivingLevelScreen && !(n instanceof White)) e.setNewScreen(new White());
        else if (n instanceof net.minecraft.client.gui.screens.ProgressScreen || n instanceof net.minecraft.client.gui.screens.GenericDirtMessageScreen) e.setNewScreen(new WhiteHold());
    }

    @SubscribeEvent
    public static void hand(net.minecraftforge.client.event.RenderHandEvent e) {
        if (phase != Flight.NONE) e.setCanceled(true);
    }

    /** Plain white screen standing in for "joining world" while the dimension switches. */
    public static final class WhiteHold extends net.minecraft.client.gui.screens.Screen {
        public WhiteHold() {
            super(net.minecraft.network.chat.Component.empty());
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            g.fill(0, 0, this.width, this.height, 0xFFFFFFFF);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    public static final class White extends ReceivingLevelScreen {
        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            g.fill(0, 0, this.width, this.height, 0xFFFFFFFF);
        }

        @Override
        public void renderBackground(GuiGraphics g) {
        }
    }
}
