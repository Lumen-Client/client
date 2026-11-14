package dev.lumen.client.anim;

/**
 * Static helpers for delta-time interpolation. All methods are allocation free.
 */
public final class AnimationUtils {
    private static final long SECOND_NANOS = 1_000_000_000L;

    private AnimationUtils() {}

    public static float deltaSeconds(long lastNanos) {
        float delta = (System.nanoTime() - lastNanos) / (float) SECOND_NANOS;
        return delta > 0.25f ? 0.25f : delta;
    }

    public static float approach(float current, float target, float speed, float deltaSeconds) {
        float diff = target - current;
        if (Math.abs(diff) < 0.0001f) return target;
        return current + diff * Math.min(1.0f, speed * deltaSeconds);
    }

    public static float springApproach(float current, float target, VelocityState state, float deltaSeconds) {
        final float stiffness = 170.0f;
        final float damping = 22.0f;
        float force = -stiffness * (current - target);
        state.velocity += (force - damping * state.velocity) * deltaSeconds;
        current += state.velocity * deltaSeconds;
        if (Math.abs(current - target) < 0.0005f && Math.abs(state.velocity) < 0.0005f) {
            current = target;
            state.velocity = 0.0f;
        }
        return current;
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    public static final class VelocityState {
        public float velocity;
    }
}
