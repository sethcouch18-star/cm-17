package com.example.colormons;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Flat arrays the screen draws from. No textures, nothing that can fail mid-frame. */
public class ScreenData {
    public record Size(int width, int height) {}

    private static final Map<String, int[]> GROUPS = new HashMap<>();
    private static final Map<String, int[]> PIXELS = new HashMap<>();
    private static final Map<String, Size> SIZES = new HashMap<>();

    public static void reset() { GROUPS.clear(); PIXELS.clear(); SIZES.clear(); }

    public static ResourceLocation textureOf(String species) {
        return ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/pokemon/" + species + ".png");
    }

    private static void ensure(String species) {
        if (GROUPS.containsKey(species)) return;
        NativeImage img = read(textureOf(species));
        if (img == null) return;

        NativeImage mask = read(TintCache.maskLocation(species));
        int[] group = TextureAnalyzer.analyse(img, mask);
        if (mask != null) mask.close();

        int w = img.getWidth(), h = img.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int abgr = img.getPixelRGBA(x, y);
            int a = (abgr >>> 24) & 0xFF;
            int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
            pixels[y * w + x] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        img.close();

        SIZES.put(species, new Size(w, h));
        PIXELS.put(species, pixels);
        GROUPS.put(species, group);
    }

    public static int[] groups(String species) { ensure(species); return GROUPS.get(species); }
    public static int[] pixels(String species) { ensure(species); return PIXELS.get(species); }
    public static Size size(String species) { ensure(species); return SIZES.get(species); }

    public static int count(String species, int wanted) {
        int[] g = groups(species);
        if (g == null) return 0;
        int n = 0;
        for (int v : g) if (v == wanted) n++;
        return n;
    }

    private static NativeImage read(ResourceLocation loc) {
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
        if (res.isEmpty()) return null;
        try (InputStream in = res.get().open()) { return NativeImage.read(in); }
        catch (Exception e) { return null; }
    }
}
