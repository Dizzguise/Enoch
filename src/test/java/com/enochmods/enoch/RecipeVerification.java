package com.enochmods.enoch;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/** Decodes the shipped data with vanilla codecs, then checks crafting and recipe-book output. */
public final class RecipeVerification {
    private static int checks;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var lookup = VanillaRegistries.createLookup();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup).forEach(pending -> pending.apply());
        var ops = lookup.createSerializationContext(JsonOps.INSTANCE);
        Recipe<?> decoded = Recipe.CODEC.parse(ops, resource("data/enoch/recipe/sigil.json")).getOrThrow();
        check(decoded instanceof ShapedRecipe, "shipped data uses vanilla shaped recipe");
        ShapedRecipe recipe = (ShapedRecipe) decoded;
        check(recipe.getSerializer() == ShapedRecipe.SERIALIZER, "no custom network serializer needed");
        check(recipe.getWidth() == 3 && recipe.getHeight() == 3, "recipe requires a 3x3 crafting table");
        List<ItemStack> grid = List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.ELYTRA), new ItemStack(Items.DIAMOND),
                new ItemStack(Items.FEATHER), new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.FEATHER),
                new ItemStack(Items.DIAMOND), new ItemStack(Items.ELYTRA), new ItemStack(Items.DIAMOND));
        CraftingInput input = CraftingInput.of(3, 3, grid);
        check(recipe.matches(input, null), "diamond corners, elytra top/bottom, feather sides and pearl center craft");
        var oldGrid = new ArrayList<>(grid);
        oldGrid.set(1, new ItemStack(Items.FEATHER));
        oldGrid.set(7, new ItemStack(Items.FEATHER));
        check(!recipe.matches(CraftingInput.of(3, 3, oldGrid), null), "old four-feather recipe is rejected");
        for (int slot = 0; slot < 9; slot++) {
            var wrong = new ArrayList<>(grid);
            wrong.set(slot, new ItemStack(Items.AMETHYST_SHARD));
            check(!recipe.matches(CraftingInput.of(3, 3, wrong), null), "wrong ingredient rejected at slot " + slot);
            var missing = new ArrayList<>(grid);
            missing.set(slot, ItemStack.EMPTY);
            check(!recipe.matches(CraftingInput.of(3, 3, missing), null), "missing ingredient rejected at slot " + slot);
            check(recipe.getIngredients().get(slot).orElseThrow().test(grid.get(slot)), "ingredient codec preserves slot " + slot);
        }
        check(!recipe.matches(CraftingInput.of(2, 2, grid.subList(0, 4)), null), "player 2x2 grid cannot craft the sigil");
        ItemStack result = recipe.assemble(input);
        check(ItemStack.matches(result, SigilData.createSigil()), "crafted output exactly matches command-created sigil components");
        check(result.getCount() == 1 && result.getMaxStackSize() == 1, "crafting creates one unstackable sigil");
        check(SigilData.isSigil(result), "crafted sigil activates via vanilla custom-data identity");
        check("enoch:sigil".equals(result.get(DataComponents.CUSTOM_MODEL_DATA).getString(0)), "crafted sigil selects original artwork");
        result.setCount(0);
        check(recipe.assemble(input).getCount() == 1, "each craft creates an independent output stack");
        check(!recipe.isSpecial() && !recipe.display().isEmpty(), "recipe appears in the vanilla recipe book");
        var display = (ShapedCraftingRecipeDisplay) recipe.display().getFirst();
        check(display.width() == 3 && display.height() == 3 && display.ingredients().size() == 9, "book displays full crafting grid");
        ItemStack bookResult = ((SlotDisplay.ItemStackSlotDisplay) display.result()).stack().create();
        check(ItemStack.matches(bookResult, SigilData.createSigil()), "vanilla book display preserves all sigil components");
        Advancement advancement = Advancement.CODEC.parse(ops,
                resource("data/enoch/advancement/recipes/misc/sigil.json")).getOrThrow();
        check(advancement.rewards().recipes().size() == 1
                && advancement.rewards().recipes().getFirst().identifier().toString().equals("enoch:sigil"), "advancement unlocks shipped recipe");
        var pearl = (InventoryChangeTrigger.TriggerInstance) advancement.criteria().get("has_ender_pearl").triggerInstance();
        check(pearl.matches(null, new ItemStack(Items.ENDER_PEARL), 1, 0, 0), "acquiring an ender pearl meets unlock criterion");
        check(!pearl.matches(null, new ItemStack(Items.DIAMOND), 1, 0, 0), "unrelated ingredient does not unlock recipe");
        check(advancement.display().isEmpty(), "recipe unlock has no advancement toast or tab");
        System.out.println("Enoch recipe verification: " + checks + " checks passed.");
    }

    private static JsonElement resource(String name) throws Exception {
        try (var stream = RecipeVerification.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) throw new AssertionError("Missing packaged resource: " + name);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
