package com.qateralong.alcohol;

import com.qateralong.screenfx.api.ScreenFxApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

final class Fx {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("screenfx");

    private Fx() {
    }

    static void play(ServerPlayer player, String effect, float seconds, float intensity, String color, String key) {
        if (!LOADED) {
            return;
        }
        try {
            if (!ScreenFxApi.hasClient(player)) {
                return;
            }
            ScreenFxApi.Effect fx = ScreenFxApi.effect(effect).seconds(seconds).intensity(Math.min(1.0F, intensity)).fade(1.0F).key(key);
            if (color != null && color.startsWith("#")) {
                fx.color(Integer.parseInt(color.substring(1), 16));
            }
            fx.send(player);
        } catch (IllegalArgumentException e) {
            Main.LOGGER.warn("Screen effect '{}' failed: {}", effect, e.getMessage());
        }
    }

    static void clear(ServerPlayer player, String key) {
        if (LOADED) {
            ScreenFxApi.clear(player, key);
        }
    }
}
