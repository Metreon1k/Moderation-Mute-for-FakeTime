package com.metreon1k.mutefake.screenshot;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.metreon1k.mutefake.mixin.ChatHudAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public final class ScreenshotUploader {
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private ScreenshotUploader() {}

    public static Path captureChatLine(double mouseX, double mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        NativeImage full = null;
        NativeImage crop = null;

        try {
            ChatHud hud = mc.inGameHud == null ? null : mc.inGameHud.getChatHud();
            if (hud == null) return null;

            int[] range = com.metreon1k.mutefake.chat.ChatInteraction.entryRangeAt(mouseX, mouseY);
            if (range == null) return null;

            ChatHudAccessor accessor = (ChatHudAccessor) (Object) hud;
            int scroll = accessor.mutefake$scrolledLines();
            int start = range[0] - scroll;
            int end = range[1] - scroll;

            if (start < 0 || end >= hud.getVisibleLineCount()) return null;

            full = ScreenshotRecorder.takeScreenshot(mc.getFramebuffer());

            double scale = hud.getChatScale();
            double lineHeight = 9.0 * (mc.options.getChatLineSpacing().getValue() + 1.0) * scale;
            int sh = mc.getWindow().getScaledHeight();

            int top = (int) Math.round(sh - 40.0 - (end + 1) * lineHeight) - 2;
            int bottom = (int) Math.round(sh - 40.0 - start * lineHeight) + 2;

            int guiW = hud.getWidth() + 12;

            double rx = full.getWidth() / (double) Math.max(1, mc.getWindow().getScaledWidth());
            double ry = full.getHeight() / (double) Math.max(1, sh);

            int x = clamp(0, 0, full.getWidth() - 1);
            int y = clamp((int) Math.floor(Math.max(0, top) * ry), 0, full.getHeight() - 1);
            int w = clamp((int) Math.ceil(guiW * rx), 1, full.getWidth() - x);
            int h = clamp((int) Math.ceil(Math.max(10, bottom - top) * ry), 1, full.getHeight() - y);

            crop = new NativeImage(w, h, false);

            for (int py = 0; py < h; py++) {
                for (int px = 0; px < w; px++) {
                    crop.setColorArgb(px, py, full.getColorArgb(x + px, y + py));
                }
            }

            Path dir = FabricLoader.getInstance().getConfigDir().resolve("mutefake/evidence");
            Files.createDirectories(dir);

            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
            Path file = dir.resolve("chat-" + stamp + ".png");

            crop.writeTo(file);
            return file;
        } catch (Throwable e) {
            return null;
        } finally {
            if (crop != null) crop.close();
            if (full != null) full.close();
        }
    }

    public static CompletableFuture<String> upload(Path image, String apiKey) {
        if (image == null || !Files.isRegularFile(image)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Скриншот сообщения не создан")
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Не настроен FakeModeration API key")
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String boundary = "----MuteFake" + System.nanoTime();
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                write(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"evidence.png\"\r\nContent-Type: image/png\r\n\r\n");
                out.write(Files.readAllBytes(image));
                write(out, "\r\n--" + boundary + "--\r\n");

                HttpRequest request = HttpRequest.newBuilder(
                                URI.create("https://api.faketime.me/api/v1/images")
                        )
                        .timeout(Duration.ofSeconds(60))
                        .header("Authorization", "Bearer " + apiKey.trim())
                        .header("Accept", "application/json")
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                        .build();

                HttpResponse<String> response = HTTP.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("HTTP " + response.statusCode());
                }

                JsonElement parsed = JsonParser.parseString(response.body());
                JsonElement imageObject = parsed.isJsonObject()
                        && parsed.getAsJsonObject().has("image")
                        ? parsed.getAsJsonObject().get("image")
                        : parsed;

                if (imageObject != null && imageObject.isJsonObject()) {
                    JsonElement url = imageObject.getAsJsonObject().has("url")
                            ? imageObject.getAsJsonObject().get("url")
                            : imageObject.getAsJsonObject().get("fileUrl");

                    if (url != null && url.isJsonPrimitive()) {
                        return url.getAsString();
                    }
                }

                throw new IllegalStateException("API не вернул ссылку на скриншот");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static void write(ByteArrayOutputStream out, String s) throws java.io.IOException {
        out.write(s.getBytes(StandardCharsets.UTF_8));
    }
}
