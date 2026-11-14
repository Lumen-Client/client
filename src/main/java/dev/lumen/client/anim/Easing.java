package dev.lumen.client.anim;

public final class Easing {
    private Easing() {}

    public static float linear(float t) {
        return clamp01(t);
    }

    public static float easeOutCubic(float t) {
        t = clamp01(t);
        float f = t - 1.0f;
        return 1.0f + f * f * f;
    }

    public static float easeInCubic(float t) {
        t = clamp01(t);
        return t * t * t;
    }

    public static float easeInOutQuart(float t) {
        t = clamp01(t);
        if (t < 0.5f) {
            return 8.0f * t * t * t * t;
        }
        float f = t - 1.0f;
        return 1.0f - 8.0f * f * f * f * f;
    }

    public static float easeOutBack(float t) {
        t = clamp01(t);
        final float c1 = 1.20158f;
        final float c3 = c1 + 1.0f;
        float f = t - 1.0f;
        return 1.0f + c3 * f * f * f + c1 * f * f;
    }

    public static float easeOutExpo(float t) {
        t = clamp01(t);
        if (t >= 1.0f) return 1.0f;
        return 1.0f - (float) Math.pow(2.0, -9.0 * t);
    }

    public static float spring(float t) {
        t = clamp01(t);
        if (t == 0.0f || t == 1.0f) return t;
        return (float) (1.0 - Math.pow(2.718281828459045, -6.0 * t)
                * Math.cos(9.0 * t));
    }

    public static float easeOutQuad(float t) {
        t = clamp01(t);
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    private static float clamp01(float t) {
        return t < 0.0f ? 0.0f : (t > 1.0f ? 1.0f : t);
    }
}
