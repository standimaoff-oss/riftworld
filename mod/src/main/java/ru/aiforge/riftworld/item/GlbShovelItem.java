package ru.aiforge.riftworld.item;

import java.util.function.Consumer;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Shovel Элисара, который рисуется настоящей объёмной моделью GLB из
 * {@code assets/riftworld/models/glb/}, а не плоским спрайтом 16x16 —
 * так же сделаны morgrath_scythe, guard_sickle и crystal_katar.
 */
public class GlbShovelItem extends ShovelItem implements GlbItem {
    private final String model;
    private final float gripToTip;

    public GlbShovelItem(Tier tier, float attackDamageBonus, float attackSpeedBonus, String model, float gripToTip, Properties props) {
        super(tier, attackDamageBonus, attackSpeedBonus, props);
        this.model = model;
        this.gripToTip = gripToTip;
    }

    @Override
    public String glb() { return model; }

    @Override
    public float length() { return gripToTip; }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ru.aiforge.riftworld.client.ClientHooks.geoItemExt());
    }
}
