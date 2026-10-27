package com.qateralong.alcohol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

public final class Drinks {
    public static final String TAG = "alcohol";
    private static final Map<String, Recipe> RECIPES = new LinkedHashMap<>();

    private Drinks() {
    }

    public static final class Ing {
        final Config.Ingredient source;
        final Item item;
        final TagKey<Item> tag;
        final ResourceLocation potion;
        final String drink;
        final int count;

        Ing(Config.Ingredient source, Item item, TagKey<Item> tag, ResourceLocation potion, String drink) {
            this.source = source;
            this.item = item;
            this.tag = tag;
            this.potion = potion;
            this.drink = drink;
            this.count = Math.max(1, source.count);
        }

        boolean matches(ItemStack stack) {
            String id = drinkId(stack);
            if (this.drink != null) {
                return this.drink.equals(id);
            }
            if (id != null) {
                return false;
            }
            if (this.potion != null) {
                if (!stack.is(Items.POTION)) {
                    return false;
                }
                PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
                Optional<Holder<net.minecraft.world.item.alchemy.Potion>> potion = contents == null ? Optional.empty() : contents.potion();
                return potion.isPresent() && BuiltInRegistries.POTION.getKey(potion.get().value()).equals(this.potion);
            }
            if (this.tag != null) {
                return stack.is(this.tag);
            }
            return stack.is(this.item) && !stack.is(Items.POTION);
        }

        public Component name() {
            if (!this.source.label.isEmpty()) {
                return Component.literal(this.source.label);
            }
            if (this.item != null) {
                return Component.translatable(this.item.getDescriptionId());
            }
            return Component.literal(String.valueOf(this.potion != null ? this.potion : this.tag != null ? this.tag.location() : this.drink));
        }

        public int count() {
            return this.count;
        }
    }

    public static final class Recipe {
        public final Config.Drink drink;
        public final List<Ing> ingredients;
        public final long ticks;

        Recipe(Config.Drink drink, List<Ing> ingredients) {
            this.drink = drink;
            this.ingredients = ingredients;
            this.ticks = Math.max(1L, Math.round(drink.agingMinutes * 60.0 * 20.0 / Math.max(0.0001, Config.get().speed)));
        }
    }

    public static void reload() {
        RECIPES.clear();
        Config config = Config.get();
        for (Config.Drink drink : config.drinks) {
            if (drink.id == null || drink.name == null) {
                continue;
            }
            boolean missingMod = drink.requires.stream().anyMatch(mod -> !FabricLoader.getInstance().isModLoaded(mod));
            if (missingMod) {
                continue;
            }
            List<Ing> ingredients = new ArrayList<>();
            boolean ok = !drink.ingredients.isEmpty();
            for (Config.Ingredient source : drink.ingredients) {
                Ing ing = resolve(source);
                if (ing == null) {
                    Main.LOGGER.warn("Drink '{}' disabled: unknown ingredient {}", drink.id, describe(source));
                    ok = false;
                    break;
                }
                ingredients.add(ing);
            }
            if (ok) {
                RECIPES.put(drink.id, new Recipe(drink, ingredients));
            }
        }
        Main.LOGGER.info("Loaded {} drinks", RECIPES.size());
    }

    private static String describe(Config.Ingredient source) {
        return String.valueOf(source.item != null ? source.item : source.tag != null ? "#" + source.tag : source.potion != null ? source.potion : source.drink);
    }

    private static Ing resolve(Config.Ingredient source) {
        if (source.drink != null) {
            return new Ing(source, null, null, null, source.drink);
        }
        if (source.potion != null) {
            ResourceLocation id = ResourceLocation.tryParse(source.potion);
            return id != null && BuiltInRegistries.POTION.containsKey(id) ? new Ing(source, null, null, id, null) : null;
        }
        if (source.tag != null) {
            ResourceLocation id = ResourceLocation.tryParse(source.tag);
            return id == null ? null : new Ing(source, null, TagKey.create(Registries.ITEM, id), null, null);
        }
        if (source.item != null) {
            ResourceLocation id = ResourceLocation.tryParse(source.item);
            Optional<Item> item = id == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(id);
            return item.map(value -> new Ing(source, value, null, null, null)).orElse(null);
        }
        return null;
    }

    public static Map<String, Recipe> recipes() {
        return RECIPES;
    }

    public static String drinkId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.getUnsafe();
        return tag.contains(TAG) ? tag.getString(TAG) : null;
    }

    public static Recipe match(List<ItemStack> contents) {
        for (Recipe recipe : RECIPES.values()) {
            int[] got = new int[recipe.ingredients.size()];
            boolean ok = true;
            for (ItemStack stack : contents) {
                if (stack.isEmpty()) {
                    continue;
                }
                int slot = -1;
                for (int i = 0; i < recipe.ingredients.size(); i++) {
                    if (recipe.ingredients.get(i).matches(stack)) {
                        slot = i;
                        break;
                    }
                }
                if (slot < 0) {
                    ok = false;
                    break;
                }
                got[slot] += stack.getCount();
            }
            if (!ok) {
                continue;
            }
            for (int i = 0; i < got.length && ok; i++) {
                ok = got[i] == recipe.ingredients.get(i).count;
            }
            if (ok) {
                return recipe;
            }
        }
        return null;
    }

    public static ItemStack create(Config.Drink drink) {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(drink.name).withStyle(Style.EMPTY.withItalic(false).withColor(color(drink.color))));
        List<Component> lore = new ArrayList<>();
        for (String line : drink.lore) {
            lore.add(Component.literal(line).withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY)));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        List<MobEffectInstance> effects = new ArrayList<>();
        for (Config.Eff eff : drink.effects) {
            ResourceLocation id = ResourceLocation.tryParse(eff.effect);
            Optional<Holder.Reference<MobEffect>> holder = id == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(id);
            holder.ifPresent(h -> effects.add(new MobEffectInstance(h, eff.seconds * 20, eff.amplifier)));
        }
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(color(drink.color)), effects));
        stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG, drink.id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    static int color(String hex) {
        try {
            return Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xC8A060;
        }
    }
}
