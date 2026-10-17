package com.example.colormons;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ColorWheelScreen extends Screen {
    private static final int WHEEL = 150;
    private static final int LEFT_X = 16, LEFT_W = 150;
    private static final int RIGHT_W = 196;
    private static final int TOP = 48, ROW_H = 24;

    private static ResourceLocation wheelTex;

    private final UUID monId;
    private final String species;
    private final MonColors mc;
    private final List<int[]> history = new ArrayList<>();

    private int selected = 0;
    private float brightness = 1f;
    private boolean editing = false;
    private int hovered = -1;

    private int wheelX, wheelY, barVX, barSX;
    private int groupX, groupY, swatchX;
    private int mapX, mapY, mapSize;
    private int paintX, paintY;
    private int actX, actY, finX, finY;
    private int brushX, brushY;

    public ColorWheelScreen(PokemonEntity mon) {
        super(Component.literal(mon.getPokemon().getSpecies().getName() + " Colours"));
        this.monId = mon.getPokemon().getUuid();
        this.species = mon.getPokemon().getSpecies().getResourceIdentifier().getPath();
        this.mc = ColorStore.of(monId);
        for (int i = 0; i < MonColors.GROUPS; i++) if (mc.colors[i] >= 0) { selected = i; break; }
        ScreenData.groups(species);
    }

    private void snapshot() {
        history.add(mc.colors.clone());
        if (history.size() > 20) history.remove(0);
    }

    @Override
    protected void init() {
        buildWheelTexture();

        int rightX = width - RIGHT_W - 12;
        groupX = rightX;
        groupY = TOP;
        swatchX = rightX + 150;

        wheelX = LEFT_X + LEFT_W + 46;
        wheelY = TOP + 30;
        barVX = wheelX + WHEEL + 12;
        barSX = wheelX + WHEEL + 36;

        mapX = rightX + 6;
        mapY = TOP + MonColors.GROUPS * ROW_H + 96;
        mapSize = 104;

        paintX = LEFT_X;
        paintY = TOP + LEFT_W + 30;

        brushX = rightX;
        brushY = TOP + MonColors.GROUPS * ROW_H + 32;

        actX = LEFT_X;
        actY = height - 56;
        finX = LEFT_X;
        finY = height - 30;

        for (int i = 0; i < MonColors.GROUPS; i++) {
            final int g = i;
            addRenderableWidget(Button.builder(Component.literal(Palette.GROUP_NAMES[i]), b -> selected = g)
                    .bounds(groupX, groupY + i * ROW_H, 146, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal(editing ? "Edit: ON" : "Edit: OFF"), b -> {
            editing = !editing;
            b.setMessage(Component.literal(editing ? "Edit: ON" : "Edit: OFF"));
        }).bounds(brushX, brushY, 92, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Brush " + (EditBrush.size + 1)), b -> {
            EditBrush.size = (EditBrush.size + 1) % 4;
            b.setMessage(Component.literal("Brush " + (EditBrush.size + 1)));
        }).bounds(brushX + 96, brushY, 92, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Undo brush"), b -> {
            EditBrush.undo(mc, (EditBrush.size + 1) * (EditBrush.size + 1));
            ColorStore.touched(monId);
        }).bounds(brushX, brushY + 24, 188, 20).build());

        for (int i = 0; i < 4; i++) {
            final int f = i;
            addRenderableWidget(Button.builder(Component.literal(Palette.FINISH_NAMES[i]), b -> {
                snapshot();
                mc.finish = f;
                ColorStore.touched(monId);
            }).bounds(finX + i * 78, finY, 74, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> snapshot())
                .bounds(actX, actY, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Undo"), b -> {
            if (!history.isEmpty()) {
                System.arraycopy(history.remove(history.size() - 1), 0, mc.colors, 0, MonColors.GROUPS);
                ColorStore.touched(monId);
            }
        }).bounds(actX + 78, actY, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            snapshot();
            mc.reset();
            ColorStore.clear(monId);
        }).bounds(actX + 156, actY, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(actX + 234, actY, 74, 20).build());

        for (int i = 0; i < 4; i++) {
            final int s = i;
            addRenderableWidget(Button.builder(Component.literal("S" + (i + 1)), b -> {
                if (isSaved(s)) {
                    snapshot();
                    System.arraycopy(ColorStore.SAVED[s], 0, mc.colors, 0, MonColors.GROUPS);
                    ColorStore.touched(monId);
                } else {
                    System.arraycopy(mc.colors, 0, ColorStore.SAVED[s], 0, MonColors.GROUPS);
                    ColorStore.save();
                }
            }).bounds(width - 32, 16 + i * 22, 22, 18).build());
        }
    }

    private boolean isSaved(int slot) {
        for (int v : ColorStore.SAVED[slot]) if (v >= 0) return true;
        return false;
    }

    private void buildWheelTexture() {
        if (wheelTex != null) return;
        NativeImage img = new NativeImage(WHEEL, WHEEL, true);
        float r = WHEEL / 2f;
        for (int y = 0; y < WHEEL; y++) for (int x = 0; x < WHEEL; x++) {
            float dx = x - r, dy = y - r;
            float d = (float) Math.sqrt(dx * dx + dy * dy) / r;
            if (d > 1f) { img.setPixelRGBA(x, y, 0); continue; }
            float hue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1);
            int rgb = Color.HSBtoRGB(hue, d, 1f);
            int rr = (rgb >> 16) & 0xFF, gg = (rgb >> 8) & 0xFF, bb = rgb & 0xFF;
            img.setPixelRGBA(x, y, 0xFF000000 | (bb << 16) | (gg << 8) | rr);
        }
        wheelTex = minecraft.getTextureManager().register("colormons/wheel", new DynamicTexture(img));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        hovered = groupUnderMouse(mouseX, mouseY);
        super.render(g, mouseX, mouseY, delta);

        g.fill(LEFT_X - 2, TOP - 16, LEFT_X + LEFT_W + 2, TOP + LEFT_W + 2, 0xCC000000);
        g.drawString(font, species, LEFT_X + 2, TOP - 14, 0xAAAAAA);
        GroupOverlay.drawColoured(g, species, LEFT_X, TOP, LEFT_W, mc);

        int gridTop = paintY - 14;
        g.fill(paintX - 2, gridTop, paintX + LEFT_W + 2, paintY + LEFT_W + 2, 0xCC000000);
        g.drawString(font, editing ? "1. pick a group   2. click pixels" : "Edit is OFF - press Edit to paint",
                paintX, gridTop + 3, editing ? 0x66FF66 : 0x999999);
        PaintPanel.draw(g, species, mc, paintX, paintY, LEFT_W);

        if (wheelTex != null) g.blit(wheelTex, wheelX, wheelY, 0, 0, WHEEL, WHEEL, WHEEL, WHEEL);
        drawBar(g, barVX, brightness, 0xFFFF4444);
        drawBar(g, barSX, saturation(), 0xFF44FF44);
        g.drawString(font, "brightness", barVX - 16, wheelY - 12, 0xFFFF8888);
        g.drawString(font, "saturation", barSX - 10, wheelY + WHEEL + 4, 0xFF88FF88);

        for (int i = 0; i < MonColors.GROUPS; i++) {
            int y = groupY + i * ROW_H;
            int n = countPixels(i);
            if (i == selected) g.fill(groupX - 3, y - 2, groupX + 172, y + 22, 0x55FFFFFF);
            String cnt = n + " px";
            g.drawString(font, cnt, swatchX - font.width(cnt) - 6, y + 6, 0xBBBBBB);
            g.fill(swatchX, y, swatchX + 20, y + 20, 0xFF000000 | Palette.shown(mc, i));
            if (mc.colors[i] < 0) g.drawString(font, "-", swatchX + 7, y + 6, 0xFFFFFF);
        }

        String label = hovered >= 0 ? Palette.GROUP_NAMES[hovered] : "hover a group";
        g.drawString(font, label, mapX, mapY - 12, hovered >= 0 ? 0xFFFF66 : 0x888888);
        g.fill(mapX - 2, mapY - 2, mapX + mapSize + 2, mapY + mapSize + 2, 0x66000000);
        GroupOverlay.draw(g, species, mapX, mapY, mapSize, hovered);

        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        if (editing) g.drawCenteredString(font,
                "painting into " + Palette.GROUP_NAMES[selected] + " - brush size " + (EditBrush.size + 1),
                width / 2, 26, 0x66FF66);
    }

    private void drawBar(GuiGraphics g, int x, float value, int knob) {
        for (int i = 0; i < WHEEL; i++) {
            int v = 255 - i * 255 / WHEEL;
            g.fill(x, wheelY + i, x + 10, wheelY + i + 1, 0xFF000000 | (v << 16) | (v << 8) | v);
        }
        int y = wheelY + (int) ((1f - value) * (WHEEL - 2));
        g.fill(x - 3, y - 1, x + 13, y + 1, knob);
    }

    private float saturation() {
        int c = Palette.shown(mc, selected);
        float[] hsb = Color.RGBtoHSB((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, null);
        return hsb[1];
    }

    private int countPixels(int group) {
        int n = ScreenData.count(species, group);
        int[] map = ScreenData.groups(species);
        if (map == null) return n;
        for (int i = 0; i < mc.editPixels.size(); i++) {
            int idx = mc.editPixels.get(i);
            if (idx < 0 || idx >= map.length) continue;
            if (mc.editGroups.get(i) == group) n++;
            if (map[idx] == group) n--;
        }
        return Math.max(0, n);
    }

    private int groupUnderMouse(int mx, int my) {
        for (int i = 0; i < MonColors.GROUPS; i++) {
            int y = groupY + i * ROW_H;
            if (my >= y - 2 && my <= y + 22 && mx >= groupX - 4 && mx <= swatchX + 22) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (editing && !isOverButton(mx, my)) {
            int[] px = PaintPanel.pixelAt(species, mx, my, paintX, paintY, LEFT_W);
            if (px != null) {
                ScreenData.Size s = ScreenData.size(species);
                EditBrush.paint(mc, px[0], px[1], s.width(), s.height(), selected);
                ColorStore.touched(monId);
                return true;
            }
        }
        return pick(mx, my) || super.mouseClicked(mx, my, b);
    }

    private boolean isOverButton(double mx, double my) {
        for (var child : children()) {
            if (child instanceof Button btn && btn.isMouseOver(mx, my)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
        if (editing && !isOverButton(mx, my)) {
            int[] px = PaintPanel.pixelAt(species, mx, my, paintX, paintY, LEFT_W);
            if (px != null) {
                ScreenData.Size s = ScreenData.size(species);
                EditBrush.paint(mc, px[0], px[1], s.width(), s.height(), selected);
                ColorStore.touched(monId);
                return true;
            }
        }
        return pick(mx, my) || super.mouseDragged(mx, my, b, dx, dy);
    }

    private boolean pick(double mx, double my) {
        if (overBar(mx, my, barVX)) {
            brightness = clamp01(1f - (float) ((my - wheelY) / WHEEL));
            setFromWheel();
            return true;
        }
        if (overBar(mx, my, barSX)) {
            applySaturation(clamp01(1f - (float) ((my - wheelY) / WHEEL)));
            return true;
        }
        float r = WHEEL / 2f;
        double dx = mx - (wheelX + r), dy = my - (wheelY + r);
        double d = Math.sqrt(dx * dx + dy * dy) / r;
        if (d <= 1.0) {
            float hue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1);
            mc.colors[selected] = Color.HSBtoRGB(hue, (float) d, brightness) & 0xFFFFFF;
            ColorStore.touched(monId);
            return true;
        }
        return false;
    }

    private boolean overBar(double mx, double my, int x) {
        return mx >= x - 4 && mx <= x + 14 && my >= wheelY - 2 && my <= wheelY + WHEEL + 2;
    }

    private void setFromWheel() {
        int c = Palette.shown(mc, selected);
        float[] hsb = Color.RGBtoHSB((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, null);
        mc.colors[selected] = Color.HSBtoRGB(hsb[0], hsb[1], brightness) & 0xFFFFFF;
        ColorStore.touched(monId);
    }

    private void applySaturation(float sat) {
        int c = Palette.shown(mc, selected);
        float[] hsb = Color.RGBtoHSB((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, null);
        mc.colors[selected] = Color.HSBtoRGB(hsb[0], sat, brightness) & 0xFFFFFF;
        ColorStore.touched(monId);
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    @Override
    public boolean mouseReleased(double mx, double my, int b) {
        ColorStore.save();
        return true;
    }

    @Override
    public void onClose() {
        ColorStore.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
