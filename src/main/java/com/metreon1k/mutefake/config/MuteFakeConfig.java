
package com.metreon1k.mutefake.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.metreon1k.mutefake.MuteFakeClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MuteFakeConfig {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static final Path DIRECTORY =
            FabricLoader.getInstance().getConfigDir().resolve("mutefake");

    public static final Path PATH = DIRECTORY.resolve("mutefake.json");

    private static MuteFakeConfig instance = new MuteFakeConfig();

    public String apiKey = "";

    public List<MutePreset> presets = defaults();

    public List<String> durations = new ArrayList<>(List.of(
            "30m", "1h", "2h", "6h", "8h", "12h", "1d"
    ));

    public static class MutePreset {
        public String rule;
        public String reason;
        public String time;

        public MutePreset() {}

        public MutePreset(String rule, String reason, String time) {
            this.rule = rule;
            this.reason = reason;
            this.time = time;
        }
    }


    private static List<MutePreset> defaults() {
        return new ArrayList<>(List.of(
                new MutePreset("3.1", "Реклама серверов и ресурсов", "8h"),
                new MutePreset("3.2", "Вмешательство в чужие наказания", "30m"),
                new MutePreset("3.3", "Флуд и спам", "1h"),
                new MutePreset("3.4", "Оскорбления", "1h"),
                new MutePreset("3.5", "CAPS более 50%", "30m"),
                new MutePreset("3.6", "Организация флуда", "1h"),
                new MutePreset("3.7", "Оскорбление родных", "1h"),
                new MutePreset("3.8", "Политика и дискриминация", "1h"),
                new MutePreset("3.9", "Лексика сексуального характера", "30m"),
                new MutePreset("3.10", "Оскорбление сервера и провокации", "2h")
        ));
    }


    public static MuteFakeConfig get() {
        return instance;
    }

    public static void load() {
        try {
            Files.createDirectories(DIRECTORY.resolve("evidence"));

            if (Files.exists(PATH)) {
                MuteFakeConfig loaded = GSON.fromJson(
                        Files.readString(PATH, StandardCharsets.UTF_8),
                        MuteFakeConfig.class
                );

                if (loaded != null) {
                    instance = loaded;
                }
            }

            if (instance.presets == null || instance.presets.isEmpty()) {
                instance.presets = defaults();
            }

            if (instance.durations == null || instance.durations.isEmpty()) {
                instance.durations = new ArrayList<>(
                        List.of("30m", "1h", "2h", "6h", "8h", "12h", "1d")
                );
            }

            if (instance.apiKey == null) {
                instance.apiKey = "";
            }

            save();

        } catch (Exception e) {
            MuteFakeClient.LOGGER.error(
                    "Не удалось загрузить конфиг MuteFake", e
            );
        }
    }

    public static void save() {
        try {
            Files.createDirectories(DIRECTORY);

            Files.writeString(
                    PATH,
                    GSON.toJson(instance),
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {
            MuteFakeClient.LOGGER.error(
                    "Не удалось сохранить конфиг MuteFake", e
            );
        }
    }
}
