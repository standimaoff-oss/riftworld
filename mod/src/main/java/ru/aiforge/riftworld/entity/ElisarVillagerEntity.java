package ru.aiforge.riftworld.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.ModEquipment;
import ru.aiforge.riftworld.registry.ModItems;

/** Ordinary purple-robed citizen with a small, persistent ducat-based trading stock. */
public final class ElisarVillagerEntity extends ElisarCitizenEntity {
    public ElisarVillagerEntity(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 0.8D, 0.9D));
    }

    private void ensureOffers() {
        if (this.offers != null && !this.offers.isEmpty()) return;
        MerchantOffers stock = new MerchantOffers();
        stock.add(new MerchantOffer(new ItemStack(ModItems.ELISAR_DUCAT.get(), 2), new ItemStack(Items.BREAD, 2), 16, 2, 0.05F));
        stock.add(new MerchantOffer(new ItemStack(ModItems.ELISAR_DUCAT.get(), 3), new ItemStack(Items.ARROW, 12), 12, 3, 0.05F));
        stock.add(new MerchantOffer(new ItemStack(ModItems.ELISAR_DUCAT.get(), 4), new ItemStack(ElisarBlocks.PALE_WHEAT.get(), 3), 12, 3, 0.05F));
        stock.add(new MerchantOffer(new ItemStack(ModItems.ELISAR_DUCAT.get(), 12), new ItemStack(ModEquipment.AURITE_INGOT.get()), 4, 8, 0.08F));
        this.offers = stock;
    }

    /** Called by the trading interaction; stock never rerolls when the menu is reopened. */
    @Override
    public void updateTrades() {
        ensureOffers();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ensureOffers();
        return super.mobInteract(player, hand);
    }
}
