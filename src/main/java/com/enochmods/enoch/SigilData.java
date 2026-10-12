package com.enochmods.enoch;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

/** Vanilla item and components only: no custom registry entries or client assets are required. */
public final class SigilData {
    private static final String FLAG = "enoch_sigil";

    private SigilData() { }

    public static ItemStack createSigil() {
        ItemStack stack = new ItemStack(Items.AMETHYST_SHARD);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(FLAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Enoch Sigil")
                .withStyle(style -> style.withColor(0x9EDFFF).withItalic(false)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Hold in your main hand. Swap hands to anchor, then return.").withStyle(ChatFormatting.GRAY),
                Component.literal("Recovery is configured by the server. Death clears your anchor.").withStyle(ChatFormatting.DARK_AQUA))));
        refreshModel(stack);
        return stack;
    }

    public static void refreshModel(ItemStack stack) {
        if (!isSigil(stack)) return;
        var old = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        if ("enoch:sigil".equals(old.getString(0))) return;
        var strings = new java.util.ArrayList<>(old.strings());
        if (strings.isEmpty()) strings.add("enoch:sigil"); else strings.set(0, "enoch:sigil");
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(old.floats(), old.flags(), strings, old.colors()));
    }

    public static boolean isSigil(ItemStack stack) {
        return stack.is(Items.AMETHYST_SHARD)
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(FLAG, false);
    }
}
