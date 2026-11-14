package dev.lumen.client.theme;

public final class ColorUtils {
    private ColorUtils() {}

    public static int argb(int alpha, int rgb) {
        return (clamp(alpha) << 24) | (rgb & 0xFFFFFF);
    }

    public static int argb(int alpha, int r, int g, int b) {
        return (clamp(alpha) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int withAlpha(int argb, int alpha) {
        return (clamp(alpha) << 24) | (argb & 0xFFFFFF);
    }

    public static int alphaOf(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    public static int rgbOf(int argb) {
        return argb & 0xFFFFFF;
    }

    public static int scaleAlpha(int argb, float factor) {
        return withAlpha(argb, (int) (alphaOf(argb) * factor));
    }

    public static int lerp(int from, int to, float t) {
        if (t <= 0.0f) return from;
        if (t >= 1.0f) return to;
        int a = (int) (alphaOf(from) + (alphaOf(to) - alphaOf(from)) * t);
        int r = (int) ((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
        int g = (int) ((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return argb(a, r, g, b);
    }

    public static int hsbToRgb(float hue, float saturation, float brightness, int alpha) {
        int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
        return argb(alpha, rgb);
    }

    public static float[] rgbToHsb(int argb) {
        return java.awt.Color.RGBtoHSB(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, null);
    }

    public static int parseHex(String hex) {
        hex = hex.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() == 6) {
            return argb(255, (int) Long.parseLong(hex, 16));
        }
        if (hex.length() == 8) {
            return (int) Long.parseLong(hex, 16);
        }
        return -1;
    }

    public static String toHex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    public static int brighten(int argb, float amount) {
        float[] hsb = rgbToHsb(argb);
        return hsbToRgb(hsb[0], hsb[1], Math.min(1.0f, hsb[2] + amount), alphaOf(argb));
    }

    public static int darken(int argb, float amount) {
        float[] hsb = rgbToHsb(argb);
        return hsbToRgb(hsb[0], hsb[1], Math.max(0.0f, hsb[2] - amount), alphaOf(argb));
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
