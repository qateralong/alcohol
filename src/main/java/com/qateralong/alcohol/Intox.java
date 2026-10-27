package com.qateralong.alcohol;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class Intox {
    private static final Map<UUID, State> STATES = new HashMap<>();

    private Intox() {
    }

    private static final class State {
        double level;
        int tier;
        boolean heavy;
        long hangoverUntil;
    }

    public static double level(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state == null ? 0.0 : state.level;
    }

    public static void clear(ServerPlayer player) {
        State state = STATES.remove(player.getUUID());
        if (state != null) {
            wipe(player);
        }
    }

    public static void reset() {
        STATES.clear();
    }

    public static void drink(ServerPlayer player, Config.Drink drink) {
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        state.level = Math.min(Config.get().maxLevel, state.level + drink.strength);
        float scale = (float) Math.min(1.0, 0.5 + state.level * 0.1);
        for (Config.Fx fx : drink.screenfx) {
            Fx.play(player, fx.effect, fx.seconds, fx.intensity * scale, fx.color, "alc_drink_" + fx.effect);
        }
        state.tier = -1;
    }

    public static void tick(MinecraftServer server) {
        if (STATES.isEmpty()) {
            return;
        }
        double decay = Config.get().decayPerMinute / 60.0;
        long now = server.getTickCount();
        STATES.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                return true;
            }
            State state = entry.getValue();
            state.level = Math.max(0.0, state.level - decay);
            int tier = state.level >= 7 ? 4 : state.level >= 5 ? 3 : state.level >= 3 ? 2 : state.level >= 1 ? 1 : 0;
            if (tier >= 2) {
                state.heavy = true;
            }
            boolean changed = tier != state.tier;
            if (tier >= 2) {
                apply(player, tier, changed, now);
            }
            if (tier < 2 && state.tier >= 2) {
                wipe(player);
            }
            state.tier = tier;
            if (state.level <= 0.01) {
                if (state.heavy) {
                    hangover(player);
                }
                return true;
            }
            return false;
        });
    }

    private static void apply(ServerPlayer player, int tier, boolean changed, long now) {
        if (changed || now % 160 == 0) {
            Fx.play(player, "drunk", 10, 0.35F + 0.12F * tier, null, "alc_drunk");
            Fx.play(player, "doublevision", 10, 0.25F + 0.1F * tier, null, "alc_double");
            if (tier >= 3) {
                Fx.play(player, "wave", 10, 0.4F, null, "alc_wave");
                Fx.play(player, "vignette", 10, 0.5F, null, "alc_vignette");
            }
        }
        effect(player, MobEffects.CONFUSION, 120, 0);
        if (tier >= 3) {
            effect(player, MobEffects.MOVEMENT_SLOWDOWN, 80, tier - 3);
        }
        if (tier >= 4 && now % 400 == 0) {
            Fx.play(player, "blackout", 3, 1.0F, null, "alc_blackout");
            effect(player, MobEffects.BLINDNESS, 60, 0);
            player.displayClientMessage(Component.literal("Мир плывёт…").withStyle(ChatFormatting.DARK_GRAY), true);
        }
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int ticks, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, ticks, amplifier, true, false));
    }

    private static void wipe(ServerPlayer player) {
        for (String key : new String[]{"alc_drunk", "alc_double", "alc_wave", "alc_vignette", "alc_blackout"}) {
            Fx.clear(player, key);
        }
    }

    private static void hangover(ServerPlayer player) {
        player.displayClientMessage(Component.literal("Похмелье…").withStyle(ChatFormatting.GRAY), true);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 90, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20 * 90, 0, true, false));
        Fx.play(player, "grayscale", 60, 0.5F, null, "alc_hangover");
        Fx.play(player, "darken", 60, 0.25F, null, "alc_hangover_dark");
    }
}
