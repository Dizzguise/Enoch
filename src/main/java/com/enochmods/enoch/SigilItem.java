package com.enochmods.enoch;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

final class SigilItem extends Item {
    SigilItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.enoch.sigil.tooltip"));
        lines.accept(Component.translatable("item.enoch.sigil.tooltip_cooldown"));
    }
}
