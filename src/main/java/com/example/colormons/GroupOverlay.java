package com.example.colormons;

import net.minecraft.client.gui.GuiGraphics;

/** Draws the group map and the coloured preview from cached arrays. */
public class GroupOverlay {

    public static void draw(GuiGraphics g, String species, int x, int y, int box, int highlight) {
        int[] map = ScreenData.groups(species);
        ScreenData.Size s = ScreenData.size(species);
        if (map == null || s == null) return;

        int w = s.width(), h = s.height();
        int cell = Math.max(1, box / Math.max(w, h));
        for (int cy = 0; cy < h; cy++) for (int cx = 0; cx < w; cx++) {
            int grp = map[cy * w + cx];
            if (grp < 0 || grp >= Palette.HIGHLIGHT.length) continue;
            int col = (highlight >= 0 && grp != highlight)
                    ? 0x33000000
                    : (0xFF000000 | Palette.HIGHLIGHT[grp]);
            g.fill(x + cx * cell, y + cy * cell, x + cx * cell + cell, y + cy * cell + cell, col);
        }
    }

    public static void drawColoured(GuiGraphics g, String species, int x, int y, int box, MonColors mc) {
        int[] map = ScreenData.groups(species);
        int[] pixels = ScreenData.pixels(species);
        ScreenData.Size s = ScreenData.size(species);
        if (map == null || pixels == null || s == null) return;

        int[] effective = map.clone();
        for (int i = 0; i < mc.editPixels.size() && i < mc.editGroups.size(); i++) {
            int idx = mc.editPixels.get(i);
            if (idx >= 0 && idx < effective.length) effective[idx] = mc.editGroups.get(i);
        }

        int w = s.width(), h = s.height();
        int cell = Math.max(1, box / Math.max(w, h));
        for (int cy = 0; cy < h; cy++) for (int cx = 0; cx < w; cx++) {
            int i = cy * w + cx;
            int argb = pixels[i];
            if (((argb >>> 24) & 0xFF) == 0) continue;
            int r = (argb >> 16) & 0xFF, gg = (argb >> 8) & 0xFF, b = argb & 0xFF;
            float l = (r * 0.299f + gg * 0.587f + b * 0.114f) / 255f;

            int grp = effective[i];
            int out;
            if (grp >= 0 && grp < mc.colors.length && mc.colors[grp] >= 0) {
                int t = mc.colors[grp];
                float shade = 0.45f + 0.75f * l;
                out = 0xFF000000 | (clamp(((t >> 16) & 0xFF) * shade) << 16)
                        | (clamp(((t >> 8) & 0xFF) * shade) << 8)
                        | clamp((t & 0xFF) * shade);
            } else {
                out = 0xFF000000 | (r << 16) | (gg << 8) | b;
            }
            g.fill(x + cx * cell, y + cy * cell, x + cx * cell + cell, y + cy * cell + cell, out);
        }
    }

    private static int clamp(float v) { return Math.max(0, Math.min(255, (int) v)); }
}
