package com.metreon1k.mutefake.mute;

import com.metreon1k.mutefake.MuteFakeClient;
import com.metreon1k.mutefake.config.MuteFakeConfig;
import com.metreon1k.mutefake.screenshot.ScreenshotUploader;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MuteService {
    private static final Pattern NAME = Pattern.compile("^[.A-Za-z0-9_]{1,32}$");
    private static final Pattern WHISPER = Pattern.compile("^/(?:msg|tell|w|m|whisper)\\s+(\\S+)");
    private MuteService() {}

    public static String extractPlayerName(Style style) {
        if (style == null) return null;
        String insertion = style.getInsertion();
        if (valid(insertion)) return insertion;
        HoverEvent hover = style.getHoverEvent();
        if (hover != null && hover.getAction() == HoverEvent.Action.SHOW_ENTITY) {
            HoverEvent.EntityContent content = hover.getValue(HoverEvent.Action.SHOW_ENTITY);
            if (content != null && content.name.isPresent()) {
                String n = content.name.get().getString(); if (valid(n)) return n;
            }
        }
        ClickEvent click = style.getClickEvent();
        if (click != null && click.getAction() == ClickEvent.Action.SUGGEST_COMMAND) {
            Matcher m = WHISPER.matcher(click.getValue()); if (m.find() && valid(m.group(1))) return m.group(1);
        }
        return null;
    }
    private static boolean valid(String s) { return s != null && NAME.matcher(s).matches(); }

    public static void mute(String player) { mute(player, "30m", "Оскорбления в чате", null); }
    public static void mute(String player, String time, String reason) { mute(player, time, reason, null); }
    public static void mute(String player, String time, String reason, Path screenshot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;
        String apiKey = MuteFakeConfig.get().apiKey;
        if (apiKey != null && !apiKey.isBlank() && screenshot != null) {
            ScreenshotUploader.upload(screenshot, apiKey).whenComplete((url, err) -> mc.execute(() -> {
                if (err != null) {
                    MuteFakeClient.LOGGER.error("Screenshot upload failed", err);
                    send(player, time, reason.replace("{screenshot}", ""));
                } else {
                    String finalReason = reason.contains("{screenshot}")
                            ? reason.replace("{screenshot}", url)
                            : (reason + " " + url).trim();
                    send(player, time, finalReason);
                }
            }));
        } else {
            send(player, time, reason.replace("{screenshot}", ""));
        }
    }
    private static void send(String player, String time, String reason) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;
        String cmd = ("mute " + player + " " + time + " " + reason).replaceAll("\\s+", " ").trim();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        mc.getNetworkHandler().sendChatCommand(cmd);
    }
}
