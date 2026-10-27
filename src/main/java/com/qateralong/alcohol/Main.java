package com.qateralong.alcohol;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {
    public static final String MOD_ID = "alcohol";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Path GUIDE = FabricLoader.getInstance().getConfigDir().resolve("alcohol_guide.txt");

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> reload());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Barrels.clear();
            Intox.reset();
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof BarrelBlockEntity barrel) {
                Barrels.track(barrel);
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof BarrelBlockEntity barrel) {
                Barrels.untrack(barrel);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0) {
                Barrels.scan(server.getTickCount());
                Intox.tick(server);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> register(dispatcher));
    }

    private static void reload() {
        Config.load();
        Drinks.reload();
    }

    public static void drank(ServerPlayer player, ItemStack stack) {
        String id = Drinks.drinkId(stack);
        if (id == null) {
            return;
        }
        Drinks.Recipe recipe = Drinks.recipes().get(id);
        if (recipe != null) {
            Intox.drink(player, recipe.drink);
        }
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var drinkArg = Commands.argument("drink", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(Drinks.recipes().keySet(), builder));
        dispatcher.register(Commands.literal("alcohol")
                .executes(Main::list)
                .then(Commands.literal("give").requires(source -> source.hasPermission(Config.get().permissionLevel))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("drink", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(Drinks.recipes().keySet(), builder))
                                        .executes(context -> give(context, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64)).executes(context -> give(context, IntegerArgumentType.getInteger(context, "count")))))))
                .then(Commands.literal("drink").requires(source -> source.hasPermission(Config.get().permissionLevel))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("drink", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(Drinks.recipes().keySet(), builder))
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                            Drinks.Recipe recipe = Drinks.recipes().get(StringArgumentType.getString(context, "drink").toLowerCase(Locale.ROOT));
                                            if (recipe == null) {
                                                context.getSource().sendFailure(Component.literal("Нет такого напитка"));
                                                return 0;
                                            }
                                            Intox.drink(player, recipe.drink);
                                            return 1;
                                        }))))
                .then(Commands.literal("sober").requires(source -> source.hasPermission(Config.get().permissionLevel))
                        .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            Intox.clear(player);
                            context.getSource().sendSuccess(() -> Component.literal(player.getGameProfile().getName() + " протрезвел"), true);
                            return 1;
                        })))
                .then(Commands.literal("level").requires(source -> source.hasPermission(Config.get().permissionLevel))
                        .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Опьянение %s: %.1f", player.getGameProfile().getName(), Intox.level(player))), false);
                            return 1;
                        })))
                .then(Commands.literal("reload").requires(source -> source.hasPermission(Config.get().permissionLevel)).executes(context -> {
                    reload();
                    context.getSource().sendSuccess(() -> Component.literal("Напитков загружено: " + Drinks.recipes().size()), true);
                    return 1;
                }))
                .then(Commands.literal("debug").requires(source -> source.hasPermission(Config.get().permissionLevel)).executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(Barrels.debug()), false);
                    return 1;
                }))
                .then(Commands.literal("export").requires(source -> source.hasPermission(Config.get().permissionLevel)).executes(Main::export))
                .then(drinkArg.executes(Main::show)));
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Напитки (/alcohol <название> — рецепт). Бочка с ингредиентами должна быть закопана: со всех сторон земля.").withStyle(ChatFormatting.GOLD), false);
        for (Drinks.Recipe recipe : Drinks.recipes().values()) {
            context.getSource().sendSuccess(() -> Component.literal(recipe.drink.id + " — ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(recipe.drink.name).withColor(Drinks.color(recipe.drink.color))), false);
        }
        return Drinks.recipes().size();
    }

    private static int show(CommandContext<CommandSourceStack> context) {
        Drinks.Recipe recipe = Drinks.recipes().get(StringArgumentType.getString(context, "drink").toLowerCase(Locale.ROOT));
        if (recipe == null) {
            context.getSource().sendFailure(Component.literal("Нет такого напитка"));
            return 0;
        }
        MutableComponent text = Component.literal(recipe.drink.name).withColor(Drinks.color(recipe.drink.color)).append(Component.literal("\nВ бочку: ").withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < recipe.ingredients.size(); i++) {
            Drinks.Ing ing = recipe.ingredients.get(i);
            text.append(Component.literal((i == 0 ? "" : " + ") + ing.count() + "× ").withStyle(ChatFormatting.WHITE)).append(ing.name().copy().withStyle(ChatFormatting.WHITE));
        }
        text.append(Component.literal("\nПолучится: " + recipe.drink.output + " шт., настаивание " + minutes(recipe.drink.agingMinutes) + ". Бочку нужно закопать.").withStyle(ChatFormatting.GRAY));
        context.getSource().sendSuccess(() -> text, false);
        return 1;
    }

    private static String minutes(double value) {
        return value == Math.floor(value) ? (int) value + " мин" : String.format(Locale.ROOT, "%.1f мин", value);
    }

    private static int give(CommandContext<CommandSourceStack> context, int count) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Drinks.Recipe recipe = Drinks.recipes().get(StringArgumentType.getString(context, "drink").toLowerCase(Locale.ROOT));
        if (recipe == null) {
            context.getSource().sendFailure(Component.literal("Нет такого напитка"));
            return 0;
        }
        for (int i = 0; i < count; i++) {
            player.getInventory().placeItemBackInInventory(Drinks.create(recipe.drink));
        }
        context.getSource().sendSuccess(() -> Component.literal("Выдано " + count + "× " + recipe.drink.name + " игроку " + player.getGameProfile().getName()), true);
        return count;
    }

    private static int export(CommandContext<CommandSourceStack> context) {
        List<String> lines = new ArrayList<>();
        for (Drinks.Recipe recipe : Drinks.recipes().values()) {
            lines.add(recipe.drink.name);
            List<String> parts = new ArrayList<>();
            for (Drinks.Ing ing : recipe.ingredients) {
                parts.add(ing.count() + "× " + plain(ing.name()));
            }
            lines.add("  Рецепт: " + String.join(" + ", parts));
            lines.add("  Результат: " + recipe.drink.output + " шт.");
            lines.add("  Настаивание: " + minutes(recipe.drink.agingMinutes));
            lines.add("");
        }
        try {
            Files.write(GUIDE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            context.getSource().sendFailure(Component.literal("Не удалось записать " + GUIDE));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Рецепты записаны в config/alcohol_guide.txt"), false);
        return lines.size();
    }

    private static String plain(Component component) {
        return component.getString();
    }
}
