package com.metreon1k.mutefake.ui;

import com.metreon1k.mutefake.config.MuteFakeConfig;
import com.metreon1k.mutefake.mute.MuteService;
import com.metreon1k.mutefake.ui.widget.UiButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public final class MuteMenuScreen extends Screen {
    private static final int PANEL_W = 300;

    private final String player;
    private final String message;
    private final Screen parent;
    private final Path screenshotPath;
    private NativeImage screenshotImage;
    private NativeImageBackedTexture screenshotTexture;
    private Identifier screenshotTextureId;
    private int panelX;
    private int panelY;
    private int panelH;

    public MuteMenuScreen(String player, String message, Screen parent) {
        this(player, message, parent, null);
    }

    public MuteMenuScreen(String player, String message, Screen parent, Path screenshotPath) {
        super(Text.literal("Мут игрока"));
        this.player = player == null ? "" : player;
        this.message = message == null ? "" : message;
        this.parent = parent;
        this.screenshotPath = screenshotPath;
    }

    @Override
    protected void init() {
        List<MuteFakeConfig.MutePreset> presets = MuteFakeConfig.get().presets;
        loadScreenshot();
        int cols = 3;
        int rows = Math.max(1, (presets.size() + cols - 1) / cols);
        int reasonTopOffset = 96;

        panelH = reasonTopOffset + rows * 26 + 42;
        panelX = Math.max(4, (width - PANEL_W) / 2);
        panelY = Math.max(4, (height - panelH) / 2);

        int bw = (PANEL_W - 24 - (cols - 1) * 6) / cols;
        int top = panelY + reasonTopOffset;

        for (int i = 0; i < presets.size() && i < 18; i++) {
            MuteFakeConfig.MutePreset preset = presets.get(i);
            int col = i % cols;
            int row = i / cols;
            int bx = panelX + 12 + col * (bw + 6);
            int by = top + row * 26;
            String rule = preset.rule == null ? "" : preset.rule;
            String time = preset.time == null ? "" : preset.time;
            String label = rule + " §8· " + time;
            addDrawableChild(new UiButton(bx, by, bw, 20, label,
                    () -> choose(preset), UiDraw.ORANGE));
        }

        if (presets.isEmpty()) {
            addDrawableChild(new UiButton(panelX + 12, top, PANEL_W - 24, 20,
                    "Нет шаблонов мута", () -> {}, UiDraw.TEXT_DIM));
        }

        int footer = panelY + panelH - 26;
        addDrawableChild(new UiButton(panelX + PANEL_W - 112, footer, 100, 20,
                "Назад", this::close, UiDraw.TEXT_DIM));
    }


    private void choose(MuteFakeConfig.MutePreset preset) {
        String rule = preset.rule == null ? "" : preset.rule.trim();

        String time = preset.time == null || preset.time.isBlank()
                ? "30m" : preset.time.trim();

        MuteService.mute(player, time, rule, screenshotPath);
        close();
    }



    private void loadScreenshot() {
        if (screenshotPath == null || !Files.isRegularFile(screenshotPath)) return;
        try (InputStream in = Files.newInputStream(screenshotPath)) {
            screenshotImage = NativeImage.read(in);
            screenshotTexture = new NativeImageBackedTexture(screenshotImage);
            screenshotTextureId = Identifier.of("mutefake", "evidence_" + Integer.toHexString(System.identityHashCode(this)));
            MinecraftClient.getInstance().getTextureManager().registerTexture(screenshotTextureId, screenshotTexture);
        } catch (Throwable ignored) {
            screenshotImage = null;
            screenshotTexture = null;
            screenshotTextureId = null;
        }
    }

    @Override
    public void removed() {
        if (screenshotTextureId != null && client != null) client.getTextureManager().destroyTexture(screenshotTextureId);
        if (screenshotTexture != null) screenshotTexture.close();
        else if (screenshotImage != null) screenshotImage.close();
        screenshotTexture = null;
        screenshotImage = null;
        super.removed();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        UiDraw.rounded(ctx, panelX, panelY, PANEL_W, panelH, 0xF2101318);
        UiDraw.border(ctx, panelX, panelY, PANEL_W, panelH, 0xFF4A3B2A);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§6Мут: §f" + player),
                panelX + PANEL_W / 2, panelY + 8, 0xFFFFFFFF);

        String preview = message.replaceAll("\\s+", " ").trim();
        if (preview.length() > 66) preview = preview.substring(0, 63) + "...";
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7«" + preview + "»"),
                panelX + PANEL_W / 2, panelY + 30, 0xFFFFFFFF);
        if (screenshotTextureId != null) {
            ctx.fill(panelX + 12, panelY + 50, panelX + PANEL_W - 12, panelY + 90, 0xFF080A0E);
            int iw = screenshotImage.getWidth();
            int ih = screenshotImage.getHeight();
            float scale = Math.min((PANEL_W - 24f) / iw, 40f / ih);
            int dw = Math.max(1, Math.round(iw * scale));
            int dh = Math.max(1, Math.round(ih * scale));
            int dx = panelX + (PANEL_W - dw) / 2;
            int dy = panelY + 50 + (40 - dh) / 2;
            ctx.drawTexture(RenderLayer::getGuiTextured, screenshotTextureId, dx, dy, 0f, 0f, dw, dh, dw, dh);
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8скриншот недоступен"),
                    panelX + PANEL_W / 2, panelY + 68, 0xFFFFFFFF);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
