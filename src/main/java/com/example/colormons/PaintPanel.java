package com.example.colormons;

import net.minecraft.client.gui.GuiGraphics;

/** The paint grid. Click maths and drawing share one layout function. */
public class PaintPanel {
    public static final int CELL = 3;

    public static int[] layout(String species, int box) {
        ScreenData.Size s = ScreenData.size(species);
        if (s == null) return new int[]{1, 1};
        int cols = Math.max(1, Math.min(s.width(), box / CELL));
        int rows = Math.max(1, Math.min(s.height(), box / CELL));
        return new int[]{cols, rows};
    }

    public static void draw(GuiGraphics g, String species, MonColors mc, int x, int y, int box) {
        int[] pixels = ScreenData.pixels(species);
        int[] map = ScreenData.groups(species);
        ScreenData.Size s = ScreenData.size(species);
        if (pixels == null || map == null || s == null) return;

        int[] l = layout(species, box);
        int cols = l[0], rows = l[1];
        int tw = s.width(), th = s.height();

        int[] effective = map.clone();
        for (int i = 0; i < mc.editPixels.size() && i < mc.editGroups.size(); i++) {
            int idx = mc.editPixels.get(i);
            if (idx >= 0 && idx < effective.length) effective[idx] = mc.editGroups.get(i);
        }

        for (int cy = 0; cy < rows; cy++) for (int cx = 0; cx < cols; cx++) {
            int px = cx * tw / cols;
            int py = cy * th / rows;
            int idx = py * tw + px;
            int argb = pixels[idx];
            if (((argb >>> 24) & 0xFF) == 0) continue;
            int rgb = argb & 0xFFFFFF;
            int grp = effective[idx];
            if (grp >= 0 && grp < Palette.HIGHLIGHT.length) rgb = Palette.HIGHLIGHT[grp];
            g.fill(x + cx * CELL, y + cy * CELL, x + cx * CELL + CELL, y + cy * CELL + CELL,
                    0xFF000000 | rgb);
        }
    }

    public static int[] pixelAt(String species, double mx, double my, int x, int y, int box) {
        ScreenData.Size s = ScreenData.size(species);
        if (s == null) return null;
        int[] l = layout(species, box);
        int cols = l[0], rows = l[1];
        int cx = (int) Math.floor((mx - x) / CELL);
        int cy = (int) Math.floor((my - y) / CELL);
        if (cx < 0 || cy < 0 || cx >= cols || cy >= rows) return null;
        return new int[]{cx * s.width() / cols, cy * s.height() / rows};
    }
}
