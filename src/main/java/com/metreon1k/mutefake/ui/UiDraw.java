package com.metreon1k.mutefake.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;


public final class UiDraw {

    public static final int BG = 0xF20A0C11;
    public static final int PANEL = 0xFF101318;
    public static final int PANEL_2 = 0xFF14171E;
    public static final int SURFACE = 0xFF1C2029;
    public static final int SURFACE_HOVER = 0xFF252A36;
    public static final int SURFACE_DARK = 0xFF0D1015;
    public static final int TEXT = 0xFFF1F3F7;
    public static final int TEXT_DIM = 0xFF9AA1AF;
    public static final int TEXT_OFF = 0xFF5F6673;
    public static final int ACCENT = 0xFF8B7CFF;
    public static final int ACCENT_LIGHT = 0xFFAFA5FF;
    public static final int GREEN = 0xFF65D69B;
    public static final int ORANGE = 0xFFE7A65B;
    public static final int RED = 0xFFE86B7A;

    private UiDraw() {}

    public static void rounded(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 2, y, x + w - 2, y + h, color);
        ctx.fill(x, y + 2, x + w, y + h - 2, color);
    }

    public static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void centered(DrawContext ctx, TextRenderer tr, String text,
                                int x, int y, int w, int h, int color) {
        if (text == null) text = "";
        int tx = x + (w - tr.getWidth(text)) / 2;
        int ty = y + (h - 8) / 2;
        ctx.drawTextWithShadow(tr, Text.literal(text), tx, ty, color);
    }

    public static void left(DrawContext ctx, TextRenderer tr, String text, int x, int y, int color) {
        ctx.drawTextWithShadow(tr, Text.literal(text == null ? "" : text), x, y, color);
    }

    public static boolean inside(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static int mix(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = lerp(from >>> 24 & 255, to >>> 24 & 255, t);
        int r = lerp(from >> 16 & 255, to >> 16 & 255, t);
        int g = lerp(from >> 8 & 255, to >> 8 & 255, t);
        int b = lerp(from & 255, to & 255, t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int lerp(int from, int to, float t) {
        return (int) (from + (to - from) * t);
    }

    
    public static boolean button(DrawContext ctx, TextRenderer tr, String label,
                                 int x, int y, int w, int h, boolean selected,
                                 boolean enabled, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        int bg;
        if (!enabled) {
            bg = SURFACE_DARK;
        } else if (selected) {
            bg = mix(SURFACE, ACCENT, 0.42f);
        } else {
            bg = hovered ? SURFACE_HOVER : SURFACE;
        }
        rounded(ctx, x, y, w, h, bg);
        int color = !enabled ? TEXT_OFF : (selected || hovered ? TEXT : TEXT_DIM);
        centered(ctx, tr, label, x, y, w, h, color);
        return hovered && enabled;
    }
}
