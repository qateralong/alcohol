package com.qateralong.alcohol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class Intox {
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final ResourceLocation COMA = ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "coma");
    private static final List<Holder<Attribute>> COMA_ATTRIBUTES = List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH);
    private static final List<Holder<MobEffect>> OWN_EFFECTS = List.of(MobEffects.CONFUSION, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.BLINDNESS,
            MobEffects.WEAKNESS, MobEffects.DIG_SLOWDOWN, MobEffects.HUNGER);
    private static final String[] KEYS = {"alc_drunk", "alc_double", "alc_wave", "alc_vignette", "alc_blackout", "alc_coma", "alc_hangover", "alc_hangover_dark"};
    private static long now;

    private Intox() {
    }

    private static final class State {
        double level;
        boolean heavy;
        long comaUntil;
        long nextBlink;
    }

    public static double level(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state == null ? 0.0 : state.level;
    }

    public static boolean inComa(Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.comaUntil > now;
    }

    public static void set(ServerPlayer player, double level) {
        if (level <= 0.0) {
            clear(player);
            return;
        }
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        state.level = Math.min(Config.get().maxLevel, level);
        refresh(player, state);
    }

    public static void clear(ServerPlayer player) {
        STATES.remove(player.getUUID());
        wake(player);
        for (String key : KEYS) {
            Fx.clear(player, key);
        }
        for (Config.Drink drink : Config.get().drinks) {
            for (Config.Fx fx : drink.screenfx) {
                Fx.clear(player, "alc_drink_" + fx.effect);
            }
        }
        for (Holder<MobEffect> effect : OWN_EFFECTS) {
            MobEffectInstance instance = player.getEffect(effect);
            if (instance != null && instance.isAmbient() && !instance.isVisible()) {
                player.removeEffect(effect);
            }
        }
    }

    public static void reset() {
        STATES.clear();
    }

    public static void drink(ServerPlayer player, Config.Drink drink) {
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        state.level = Math.min(Config.get().maxLevel, state.level + drink.strength);
        float scale = (float) Math.min(1.0, 0.5 + state.level * 0.1);
        float length = (float) (Config.get().fxDurationScale * Math.min(1.0, 0.6 + state.level * 0.08));
        for (Config.Fx fx : drink.screenfx) {
            Fx.play(player, fx.effect, Math.max(3.0F, fx.seconds * length), fx.intensity * scale, fx.color, "alc_drink_" + fx.effect);
        }
        if (state.comaUntil <= now && state.level >= Config.get().comaLevel && !player.isCreative() && !player.isSpectator()) {
            coma(player, state);
        } else {
            refresh(player, state);
        }
    }

    public static void tick(MinecraftServer server) {
        now = server.getTickCount();
        if (STATES.isEmpty()) {
            return;
        }
        double decay = Config.get().decayPerMinute / 60.0;
        STATES.entrySet().removeIf(entry -> {
            State state = entry.getValue();
            boolean coma = state.comaUntil > now;
            if (!coma) {
                state.level = Math.max(0.0, state.level - decay);
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                return state.level <= 0.01;
            }
            if (state.comaUntil != 0 && !coma) {
                state.comaUntil = 0;
                state.level = Math.min(state.level, Config.get().comaLevel - 3.0);
                wake(player);
                Fx.clear(player, "alc_coma");
                player.displayClientMessage(Component.literal("Вы пришли в себя").withStyle(ChatFormatting.GRAY), true);
            }
            if (coma) {
                holdComa(player);
            } else if (now % 80 == 0) {
                refresh(player, state);
            }
            if (state.level >= 3.0) {
                state.heavy = true;
            }
            if (!coma) {
                effects(player, state);
            }
            if (state.level <= 0.01) {
                for (String key : new String[]{"alc_drunk", "alc_double", "alc_wave", "alc_vignette"}) {
                    Fx.clear(player, key);
                }
                if (state.heavy) {
                    hangover(player);
                }
                return true;
            }
            return false;
        });
    }

    private static void refresh(ServerPlayer player, State state) {
        double level = state.level;
        float seconds = 6.0F;
        Fx.play(player, "doublevision", seconds, (float) Math.min(1.0, 0.12 + 0.09 * level), null, "alc_double");
        if (level >= 2.0) {
            Fx.play(player, "drunk", seconds, (float) Math.min(1.0, 0.1 + 0.08 * level), null, "alc_drunk");
        }
        if (level >= 5.0) {
            Fx.play(player, "wave", seconds, (float) Math.min(0.8, 0.05 * level), null, "alc_wave");
            Fx.play(player, "vignette", seconds, (float) Math.min(0.8, 0.07 * level), null, "alc_vignette");
        }
    }

    private static void effects(ServerPlayer player, State state) {
        double level = state.level;
        if (level >= 4.0) {
            effect(player, MobEffects.CONFUSION, 100, 0);
        }
        if (level >= 5.0) {
            effect(player, MobEffects.MOVEMENT_SLOWDOWN, 60, level >= 7.0 ? 1 : 0);
        }
        if (level >= 7.0 && now >= state.nextBlink) {
            state.nextBlink = now + 300 + player.getRandom().nextInt(300);
            Fx.play(player, "blackout", 2, 1.0F, null, "alc_blackout");
            effect(player, MobEffects.BLINDNESS, 40, 0);
            player.displayClientMessage(Component.literal("Мир плывёт…").withStyle(ChatFormatting.DARK_GRAY), true);
        }
    }

    private static void coma(ServerPlayer player, State state) {
        int seconds = Math.max(1, Config.get().comaSeconds);
        state.comaUntil = now + seconds * 20L;
        state.heavy = true;
        player.stopUsingItem();
        float damage = Config.get().comaDamage;
        if (damage > 0.0F && player.getHealth() > 1.0F) {
            player.setHealth(Math.max(1.0F, player.getHealth() - damage));
        }
        Fx.play(player, "blackout", seconds, 1.0F, null, "alc_coma");
        effect(player, MobEffects.BLINDNESS, seconds * 20 + 40, 0);
        effect(player, MobEffects.HUNGER, seconds * 20, 1);
        holdComa(player);
        player.displayClientMessage(Component.literal("Алкогольная кома").withStyle(ChatFormatting.DARK_RED), true);
        player.sendSystemMessage(Component.literal("Вы перебрали и потеряли сознание на " + seconds + " с").withStyle(ChatFormatting.DARK_RED));
    }

    private static void holdComa(ServerPlayer player) {
        for (Holder<Attribute> attribute : COMA_ATTRIBUTES) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null && !instance.hasModifier(COMA)) {
                instance.addTransientModifier(new AttributeModifier(COMA, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        effect(player, MobEffects.BLINDNESS, 60, 0);
        if (player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    private static void wake(ServerPlayer player) {
        for (Holder<Attribute> attribute : COMA_ATTRIBUTES) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(COMA);
            }
        }
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int ticks, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, ticks, amplifier, true, false));
    }

    private static void hangover(ServerPlayer player) {
        int seconds = Config.get().hangoverSeconds;
        if (seconds <= 0) {
            return;
        }
        player.displayClientMessage(Component.literal("Похмелье…").withStyle(ChatFormatting.GRAY), true);
        effect(player, MobEffects.WEAKNESS, 20 * seconds, 0);
        effect(player, MobEffects.DIG_SLOWDOWN, 20 * seconds, 0);
        Fx.play(player, "grayscale", seconds, 0.4F, null, "alc_hangover");
        Fx.play(player, "darken", seconds, 0.2F, null, "alc_hangover_dark");
    }
}
