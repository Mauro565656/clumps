package blamejared.clumps.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class RenderHelper {

    public static void fillRounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        if (radius <= 0) { g.fill(x, y, x + w, y + h, color); return; }
        g.fill(x + radius, y + radius, x + w - radius, y + h - radius, color);
        g.fill(x + radius, y, x + w - radius, y + radius, color);
        g.fill(x + radius, y + h - radius, x + w - radius, y + h, color);
        g.fill(x, y + radius, x + radius, y + h - radius, color);
        g.fill(x + w - radius, y + radius, x + w, y + h - radius, color);
        for (int i = 0; i < radius; i++) {
            int step = (int)(radius - Math.sqrt(2 * radius * i - i * i));
            if (step >= radius) continue;
            int left = x + step;
            int right = x + w - step - 1;
            int top = y + i;
            int bot = y + h - i - 1;
            g.fill(left, top, x + radius, top + 1, color);
            g.fill(x + w - radius, top, right + 1, top + 1, color);
            g.fill(left, bot, x + radius, bot + 1, color);
            g.fill(x + w - radius, bot, right + 1, bot + 1, color);
        }
    }

    public static void drawGlow(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int glowColor) {
        for (int i = 4; i > 0; i--) {
            int alpha = (glowColor >> 24) & 0xFF;
            alpha = alpha * i / 5;
            int layer = (alpha << 24) | (glowColor & 0x00FFFFFF);
            int inflate = (5 - i) * 2;
            fillRounded(g, x - inflate, y - inflate, w + inflate * 2, h + inflate * 2, radius + inflate, layer);
        }
    }

    public static void fillGradientHorizontal(GuiGraphicsExtractor g, int x, int y, int w, int h, int leftColor, int rightColor) {
        for (int i = 0; i < w; i++) {
            float t = (float) i / w;
            g.fill(x + i, y, x + i + 1, y + h, lerpColor(leftColor, rightColor, t));
        }
    }

    public static void fillGradientVertical(GuiGraphicsExtractor g, int x, int y, int w, int h, int topColor, int bottomColor) {
        for (int i = 0; i < h; i++) {
            float t = (float) i / h;
            g.fill(x, y + i, x + w, y + i + 1, lerpColor(topColor, bottomColor, t));
        }
    }

    public static void drawGlassOverlay(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        fillGradientVertical(g, x, y, w, h, 0x15FFFFFF, 0x02FFFFFF);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x08FFFFFF);
    }

    public static void drawBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void drawInnerBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        if (radius > 0) {
            for (int i = 0; i < radius; i++) {
                int step = (int)(radius - Math.sqrt(2 * radius * i - i * i));
                if (step >= radius) continue;
                g.fill(x + step, y + i, x + w - step, y + i + 1, color);
                g.fill(x + step, y + h - i - 1, x + w - step, y + h - i, color);
            }
            for (int i = 0; i < radius; i++) {
                int step = (int)(radius - Math.sqrt(2 * radius * i - i * i));
                if (step >= radius) continue;
                g.fill(x + i, y + step, x + i + 1, y + h - step, color);
                g.fill(x + w - i - 1, y + step, x + w - i, y + h - step, color);
            }
        }
        g.fill(x + radius, y, x + w - radius, y + 1, color);
        g.fill(x + radius, y + h - 1, x + w - radius, y + h, color);
        g.fill(x, y + radius, x + 1, y + h - radius, color);
        g.fill(x + w - 1, y + radius, x + w, y + h - radius, color);
    }

    public static void fillDoubleGradient(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                           int topLeft, int topRight, int botLeft, int botRight) {
        for (int row = 0; row < h; row++) {
            float t = (float) row / h;
            int left = lerpColor(topLeft, botLeft, t);
            int right = lerpColor(topRight, botRight, t);
            fillGradientHorizontal(g, x, y + row, w, 1, left, right);
        }
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int gn = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (gn << 8) | b;
    }
}
