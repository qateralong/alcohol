package com.qateralong.alcohol.mixin;

import com.qateralong.alcohol.BrewHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BarrelBlockEntity.class)
public abstract class BarrelBlockEntityMixin implements BrewHolder {
    @Unique
    private String alcohol$recipe = "";
    @Unique
    private long alcohol$start;

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void alcohol$save(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (!this.alcohol$recipe.isEmpty()) {
            tag.putString("AlcoholRecipe", this.alcohol$recipe);
            tag.putLong("AlcoholStart", this.alcohol$start);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void alcohol$load(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.alcohol$recipe = tag.getString("AlcoholRecipe");
        this.alcohol$start = tag.getLong("AlcoholStart");
    }

    @Override
    public String alcohol$recipe() {
        return this.alcohol$recipe;
    }

    @Override
    public long alcohol$start() {
        return this.alcohol$start;
    }

    @Override
    public void alcohol$set(String recipe, long start) {
        this.alcohol$recipe = recipe;
        this.alcohol$start = start;
    }
}
