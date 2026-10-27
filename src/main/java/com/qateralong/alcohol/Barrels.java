package com.qateralong.alcohol;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class Barrels {
    private static final Set<BarrelBlockEntity> TRACKED = ConcurrentHashMap.newKeySet();

    private Barrels() {
    }

    public static void track(BarrelBlockEntity barrel) {
        TRACKED.add(barrel);
    }

    public static void untrack(BarrelBlockEntity barrel) {
        TRACKED.remove(barrel);
    }

    public static String debug() {
        StringBuilder text = new StringBuilder("tracked=" + TRACKED.size());
        for (BarrelBlockEntity barrel : TRACKED) {
            if (barrel.getLevel() instanceof ServerLevel level) {
                List<ItemStack> contents = new ArrayList<>();
                for (int i = 0; i < barrel.getContainerSize(); i++) {
                    contents.add(barrel.getItem(i));
                }
                Drinks.Recipe recipe = Drinks.match(contents);
                text.append("\n").append(barrel.getBlockPos().toShortString()).append(" buried=").append(buried(level, barrel.getBlockPos()))
                        .append(" recipe=").append(recipe == null ? "none" : recipe.drink.id).append(" state=").append(((BrewHolder) barrel).alcohol$recipe());
            }
        }
        return text.toString();
    }

    public static void clear() {
        TRACKED.clear();
    }

    public static boolean buried(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (!earth(level.getBlockState(pos.relative(direction)))) {
                return false;
            }
        }
        return true;
    }

    private static boolean earth(BlockState state) {
        for (String raw : Config.get().buriedBlocks) {
            boolean tag = raw.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? raw.substring(1) : raw);
            if (id == null) {
                continue;
            }
            if (tag ? state.is(TagKey.create(Registries.BLOCK, id)) : BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static void scan(long unused) {
        for (BarrelBlockEntity barrel : new ArrayList<>(TRACKED)) {
            if (barrel.isRemoved() || !(barrel.getLevel() instanceof ServerLevel level) || !level.isLoaded(barrel.getBlockPos())) {
                continue;
            }
            BrewHolder holder = (BrewHolder) barrel;
            List<ItemStack> contents = new ArrayList<>();
            for (int i = 0; i < barrel.getContainerSize(); i++) {
                contents.add(barrel.getItem(i));
            }
            Drinks.Recipe recipe = Drinks.match(contents);
            long now = level.getGameTime();
            if (recipe == null || !buried(level, barrel.getBlockPos())) {
                if (!holder.alcohol$recipe().isEmpty()) {
                    holder.alcohol$set("", 0L);
                    barrel.setChanged();
                }
                continue;
            }
            if (!recipe.drink.id.equals(holder.alcohol$recipe())) {
                holder.alcohol$set(recipe.drink.id, now);
                barrel.setChanged();
                continue;
            }
            if (now - holder.alcohol$start() >= recipe.ticks) {
                finish(barrel, holder, recipe);
            }
        }
    }

    private static void finish(BarrelBlockEntity barrel, BrewHolder holder, Drinks.Recipe recipe) {
        barrel.clearContent();
        for (int i = 0; i < Math.min(recipe.drink.output, barrel.getContainerSize()); i++) {
            barrel.setItem(i, Drinks.create(recipe.drink));
        }
        holder.alcohol$set("", 0L);
        barrel.setChanged();
        Main.LOGGER.info("Barrel at {} finished {}", barrel.getBlockPos().toShortString(), recipe.drink.id);
    }
}
