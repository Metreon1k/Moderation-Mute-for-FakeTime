package com.metreon1k.mutefake.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import com.metreon1k.mutefake.mixin.ChatHudAccessor;

import java.util.List;
import java.util.regex.Pattern;

public final class ChatInteraction {

    private static final Pattern VALID_NICK = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private ChatInteraction() {}

    public static int lineIndexAt(double mouseX, double mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null || mc.getWindow() == null) return -1;

        ChatHud hud = mc.inGameHud.getChatHud();
        if (hud == null) return -1;

        try {
            ChatHudAccessor accessor = (ChatHudAccessor) (Object) hud;
            List<ChatHudLine.Visible> visible = accessor.mutefake$visibleMessages();
            if (visible == null || visible.isEmpty()) return -1;

            double scale = hud.getChatScale();
            double lineHeight = 9.0 * (mc.options.getChatLineSpacing().getValue() + 1.0);
            if (scale <= 0 || lineHeight <= 0) return -1;

            double lineX = mouseX / scale - 4.0;
            double lineY = (mc.getWindow().getScaledHeight() - mouseY - 40.0) / (scale * lineHeight);

            if (lineX < -4.0 || lineX > Math.floor(hud.getWidth() / scale)) return -1;

            int visibleCount = Math.min(hud.getVisibleLineCount(), visible.size());
            if (lineY < 0 || lineY >= visibleCount) return -1;

            int index = (int) Math.floor(lineY + accessor.mutefake$scrolledLines());
            return index >= 0 && index < visible.size() ? index : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    public static int[] entryRangeAt(double mouseX, double mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) return null;

        ChatHud hud = mc.inGameHud.getChatHud();
        if (hud == null) return null;

        int index = lineIndexAt(mouseX, mouseY);
        if (index < 0) return null;

        try {
            ChatHudAccessor accessor = (ChatHudAccessor) (Object) hud;
            List<ChatHudLine.Visible> visible = accessor.mutefake$visibleMessages();
            if (visible == null || index >= visible.size()) return null;

            int time = visible.get(index).addedTime();

            int start = index;
            while (start > 0
                    && !visible.get(start).endOfEntry()
                    && visible.get(start - 1).addedTime() == time) {
                start--;
            }

            int end = index;
            while (end < visible.size() - 1
                    && !visible.get(end).endOfEntry()
                    && !visible.get(end + 1).endOfEntry()
                    && visible.get(end + 1).addedTime() == time) {
                end++;
            }

            return new int[]{start, end};
        } catch (Throwable t) {
            return null;
        }
    }

    public static String lineAt(double mouseX, double mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) return null;

        ChatHud hud = mc.inGameHud.getChatHud();
        if (hud == null) return null;

        int[] range = entryRangeAt(mouseX, mouseY);
        if (range == null) return null;

        try {
            ChatHudAccessor accessor = (ChatHudAccessor) (Object) hud;
            List<ChatHudLine.Visible> visible = accessor.mutefake$visibleMessages();
            if (visible == null) return null;

            StringBuilder result = new StringBuilder();
            for (int i = range[1]; i >= range[0]; i--) {
                String part = orderedTextToString(visible.get(i).content());
                if (result.length() > 0) result.append('\n');
                result.append(part);
            }
            return result.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public static String authorAt(double mouseX, double mouseY) {
        String message = lineAt(mouseX, mouseY);
        if (message == null || message.isBlank()) return "";

        String firstLine = message.split("\\R", 2)[0];

        String cleaned = firstLine
                .replaceAll("«[^»]*»", " ")
                .replaceAll("\\[[^\\]]*\\]", " ");

        int cut = Integer.MAX_VALUE;
        int idx = cleaned.indexOf(':');
        if (idx > 0) cut = Math.min(cut, idx);
        idx = cleaned.indexOf('»');
        if (idx > 0) cut = Math.min(cut, idx);
        idx = cleaned.indexOf("->");
        if (idx > 0) cut = Math.min(cut, idx);
        if (cut == Integer.MAX_VALUE) cut = cleaned.length();

        String head = cleaned.substring(0, cut)
                .replaceAll("[^A-Za-z0-9_\\s]", " ");

        for (String token : head.split("\\s+")) {
            if (VALID_NICK.matcher(token).matches()) return token;
        }
        for (String token : cleaned.split("\\s+")) {
            String candidate = token.replaceAll("[^A-Za-z0-9_]", "");
            if (VALID_NICK.matcher(candidate).matches()) return candidate;
        }
        return "";
    }

    public static String messageBody(String raw) {
        if (raw == null || raw.isBlank()) return "";

        String[] lines = raw.split("\\R");
        StringBuilder out = new StringBuilder(stripSenderPrefix(lines.length > 0 ? lines[0] : raw));
        for (int i = 1; i < lines.length; i++) {
            String part = lines[i] == null ? "" : lines[i].trim();
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(part);
        }
        return out.toString().trim();
    }

    private static final Pattern LEADING_TAG =
            Pattern.compile("^\\s*[«‹\\[<][^»›\\]>]{0,32}[»›\\]>]\\s*");
    private static final Pattern PREFIX_LETTER =
            Pattern.compile("^[^\\s\\p{L}]{0,2}\\p{L}\\s*\\|\\s*");
    private static final Pattern LEADING_NICK =
            Pattern.compile("^([A-Za-z0-9_]{3,16})(?![A-Za-z0-9_])\\s*[:»›|,]?\\s*");

    private static String stripSenderPrefix(String line) {
        String value = line == null ? "" : line.trim().replaceAll("\\s{2,}", " ");
        if (value.isEmpty()) return "";

        boolean stripped = false;
        for (int i = 0; i < 4; i++) {
            String before = value;
            String next = LEADING_TAG.matcher(value).replaceFirst("");
            if (!next.equals(value)) {
                value = next;
                stripped = true;
            }
            next = PREFIX_LETTER.matcher(value).replaceFirst("");
            if (!next.equals(value)) {
                value = next;
                stripped = true;
            }
            if (value.equals(before)) break;
        }
        value = value.trim();

        java.util.regex.Matcher nick = LEADING_NICK.matcher(value);
        if (nick.find()) {
            value = value.substring(nick.end());
            stripped = true;
        }
        value = value.replaceFirst("^(->|→|»|›|:)\\s*", "").trim();

        if (stripped) {
            int sep = separatorIndex(value, 48);
            if (sep > 0) {
                String head = value.substring(0, sep).trim();
                if (!head.isEmpty() && head.split("\\s+").length <= 4) {
                    value = value.substring(sep + 1).trim();
                }
            }
        }
        return value.trim();
    }

    private static int separatorIndex(String value, int limit) {
        int max = Math.min(value.length(), limit);
        for (int i = 0; i < max; i++) {
            char c = value.charAt(i);
            if (c == ':' || c == '：' || c == '∶' || c == '﹕' || c == '»' || c == '›') return i;
            if (c == '-' && i + 1 < value.length() && value.charAt(i + 1) == '>') return i;
            if (c == '→') return i;
        }
        return -1;
    }
    private static boolean hasNick(String value) {
        if (value == null) return false;
        for (String token : value.split("\\s+")) {
            String clean = token.replaceAll("[^A-Za-z0-9_]", "");
            if (VALID_NICK.matcher(clean).matches()) return true;
        }
        return false;
    }

    private static String orderedTextToString(OrderedText text) {
        StringBuilder builder = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }
}
