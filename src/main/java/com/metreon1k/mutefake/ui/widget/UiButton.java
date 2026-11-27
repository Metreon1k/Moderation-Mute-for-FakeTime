package com.metreon1k.mutefake.ui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import com.metreon1k.mutefake.ui.UiDraw;


public class UiButton extends ButtonWidget {

    private final String label;
    private final int accent;

    public UiButton(int x, int y, int width, int height, String label, Runnable onClick) {
        this(x, y, width, height, label, onClick, UiDraw.ACCENT);
    }

    public UiButton(int x, int y, int width, int height, String label, Runnable onClick, int accent) {
        super(x, y, width, height, Text.literal(label), b -> onClick.run(), DEFAULT_NARRATION_SUPPLIER);
        this.label = label;
        this.accent = accent;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        UiDraw.rounded(ctx, x, y, w, h, hovered ? UiDraw.SURFACE_HOVER : UiDraw.SURFACE);
        UiDraw.border(ctx, x, y, w, h, hovered ? accent : 0xFF2B3140);
        UiDraw.centered(ctx, MinecraftClient.getInstance().textRenderer, label,
                x, y, w, h, hovered ? UiDraw.TEXT : UiDraw.TEXT_DIM);
    }
}
