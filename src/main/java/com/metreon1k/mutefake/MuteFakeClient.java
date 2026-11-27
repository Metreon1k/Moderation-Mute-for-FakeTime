package com.metreon1k.mutefake;

import com.metreon1k.mutefake.config.MuteFakeConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.util.Util;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuteFakeClient implements ClientModInitializer {
    public static final String MOD_ID = "mutefake";
    public static final String MOD_NAME = "MuteFake by Metreon1k";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        MuteFakeConfig.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("mutefake")
                        .then(ClientCommandManager.literal("reload").executes(ctx -> {
                            MuteFakeConfig.load();
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("explorer").executes(ctx -> {
                            try {
                                Files.createDirectories(MuteFakeConfig.DIRECTORY.resolve("evidence"));
                                Util.getOperatingSystem().open(MuteFakeConfig.DIRECTORY.toFile());
                            } catch (Exception e) {
                                LOGGER.error("Не удалось открыть папку MuteFake", e);
                            }
                            return 1;
                        }))));

        LOGGER.info("{} загружен. ПКМ по сообщению в обычном чате = меню мута.", MOD_NAME);
    }
}
