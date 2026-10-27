package com.qateralong.alcohol;

import java.util.ArrayList;
import java.util.List;

final class Defaults {
    private Defaults() {
    }

    static List<Config.Drink> drinks() {
        List<Config.Drink> list = new ArrayList<>();
        list.add(drink("tincture", "Настойка", "#C9B26B", 1.0, 5, 1, List.of("Мутная жидкость с резким запахом."),
                List.of(potion("minecraft:water", 1, "Бутылка воды")), List.of(eff("minecraft:nausea", 6, 0)), List.of(fx("chromatic", 20, 0.5F))));
        list.add(drink("beer", "Пиво", "#E0A12D", 1.0, 8, 3, List.of("Пенное, холодное, жизнеутверждающее."),
                List.of(item("minecraft:wheat", 6, "Пшеница"), potion("minecraft:water", 3, "Бутылка воды")),
                List.of(eff("minecraft:saturation", 1, 0)), List.of(fx("sway", 25, 0.45F))));
        list.add(drink("mead", "Медовуха", "#F2C14E", 1.5, 10, 3, List.of("Тягучая и сладкая, бьёт в голову не сразу."),
                List.of(item("minecraft:honey_bottle", 3, "Бутылка мёда"), potion("minecraft:water", 3, "Бутылка воды")),
                List.of(eff("minecraft:regeneration", 5, 0)), List.of(fx("dream", 30, 0.5F))));
        list.add(drink("cider", "Сидр", "#BFD36B", 1.0, 8, 3, List.of("Яблочный, лёгкий, с кислинкой."),
                List.of(item("minecraft:apple", 6, "Яблоко"), potion("minecraft:water", 3, "Бутылка воды")),
                List.of(eff("minecraft:speed", 10, 0)), List.of(fx("wave", 25, 0.4F))));
        list.add(drink("wine", "Вино", "#7B1E3A", 1.5, 12, 2, List.of("Выдержанное, тёмное, с тяжёлым послевкусием."),
                List.of(item("minecraft:sweet_berries", 8, "Сладкие ягоды"), item("minecraft:sugar", 2, "Сахар"), potion("minecraft:water", 2, "Бутылка воды")),
                List.of(eff("minecraft:resistance", 15, 0)), List.of(fx("vignette", 30, 0.5F, "#5A0A1E"), fx("tint", 30, 0.2F, "#B01040"))));
        list.add(drink("rum", "Ром", "#8B4A1F", 2.5, 15, 2, List.of("Сахарный тростник и морская соль в каждой капле."),
                List.of(item("minecraft:sugar_cane", 8, "Сахарный тростник"), drinkIng("tincture", 2, "Настойка")),
                List.of(eff("minecraft:strength", 20, 0)), List.of(fx("tilt", 30, 0.5F), fx("swirl", 15, 0.3F))));
        list.add(drink("vodka", "Водка", "#DDEFFF", 3.0, 15, 3, List.of("Прозрачная, как слеза, и такая же горькая."),
                List.of(item("minecraft:potato", 6, "Картофель"), drinkIng("tincture", 3, "Настойка")),
                List.of(eff("minecraft:resistance", 20, 1)), List.of(fx("blur", 20, 0.5F), fx("radialblur", 20, 0.4F))));
        list.add(drink("whiskey", "Виски", "#9A5B12", 3.0, 20, 3, List.of("Долгая выдержка, дубовый характер."),
                List.of(drinkIng("beer", 3, "Пиво"), item("minecraft:wheat", 3, "Пшеница")),
                List.of(eff("minecraft:strength", 30, 0), eff("minecraft:slowness", 10, 0)), List.of(fx("hue", 30, 0.5F), fx("vertigo", 20, 0.4F))));
        list.add(drink("healing_liqueur", "Целебный ликёр", "#E0457B", 1.0, 10, 2, List.of("Лечебная настойка на зелье."),
                List.of(potion("minecraft:healing", 1, "Зелье исцеления"), drinkIng("tincture", 2, "Настойка")),
                List.of(eff("minecraft:regeneration", 12, 1)), List.of(fx("vivid", 25, 0.5F))));
        list.add(drink("night_liqueur", "Ночной ликёр", "#2B2F8F", 1.5, 12, 2, List.of("Показывает мир таким, каким он бывает ночью."),
                List.of(potion("minecraft:night_vision", 1, "Зелье ночного зрения"), drinkIng("mead", 2, "Медовуха")),
                List.of(eff("minecraft:night_vision", 90, 0)), List.of(fx("nightvision", 40, 0.5F))));
        list.add(drink("energy_shot", "Энергетический шот", "#33E0C0", 2.0, 8, 2, List.of("Заряд на пять минут и расплата после."),
                List.of(potion("minecraft:swiftness", 1, "Зелье скорости"), drinkIng("vodka", 1, "Водка")),
                List.of(eff("minecraft:speed", 40, 1)), List.of(fx("zoom", 20, 0.5F))));
        Config.Drink sake = drink("sake", "Саке", "#F2EFE0", 1.5, 12, 2, List.of("Рисовое вино, пьётся мягко."),
                List.of(item("farmersdelight:rice", 8, "Рис"), potion("minecraft:water", 2, "Бутылка воды")),
                List.of(eff("minecraft:luck", 30, 0)), List.of(fx("pixelate", 25, 0.35F)));
        sake.requires = List.of("farmersdelight");
        list.add(sake);
        Config.Drink mary = drink("bloody_mary", "Кровавая Мэри", "#B01818", 2.0, 8, 1, List.of("Томат, водка и немного драмы."),
                List.of(drinkIng("vodka", 1, "Водка"), item("farmersdelight:tomato", 3, "Помидор")),
                List.of(eff("minecraft:health_boost", 30, 0)), List.of(fx("pulse", 20, 0.4F, "#C01010")));
        mary.requires = List.of("farmersdelight");
        list.add(mary);
        return list;
    }

    private static Config.Drink drink(String id, String name, String color, double strength, double minutes, int output, List<String> lore,
                                      List<Config.Ingredient> ingredients, List<Config.Eff> effects, List<Config.Fx> fx) {
        Config.Drink drink = new Config.Drink();
        drink.id = id;
        drink.name = name;
        drink.color = color;
        drink.strength = strength;
        drink.agingMinutes = minutes;
        drink.output = output;
        drink.lore = new ArrayList<>(lore);
        drink.ingredients = new ArrayList<>(ingredients);
        drink.effects = new ArrayList<>(effects);
        drink.screenfx = new ArrayList<>(fx);
        return drink;
    }

    private static Config.Ingredient item(String id, int count, String label) {
        Config.Ingredient ingredient = new Config.Ingredient();
        ingredient.item = id;
        ingredient.count = count;
        ingredient.label = label;
        return ingredient;
    }

    private static Config.Ingredient potion(String id, int count, String label) {
        Config.Ingredient ingredient = new Config.Ingredient();
        ingredient.potion = id;
        ingredient.count = count;
        ingredient.label = label;
        return ingredient;
    }

    private static Config.Ingredient drinkIng(String id, int count, String label) {
        Config.Ingredient ingredient = new Config.Ingredient();
        ingredient.drink = id;
        ingredient.count = count;
        ingredient.label = label;
        return ingredient;
    }

    private static Config.Eff eff(String id, int seconds, int amplifier) {
        Config.Eff effect = new Config.Eff();
        effect.effect = id;
        effect.seconds = seconds;
        effect.amplifier = amplifier;
        return effect;
    }

    private static Config.Fx fx(String id, float seconds, float intensity) {
        return fx(id, seconds, intensity, null);
    }

    private static Config.Fx fx(String id, float seconds, float intensity, String color) {
        Config.Fx fx = new Config.Fx();
        fx.effect = id;
        fx.seconds = seconds;
        fx.intensity = intensity;
        fx.color = color;
        return fx;
    }
}
