package dev.lumen.client.core;

public enum Category {
    HUD("HUD"),
    VISUALS("Visuals"),
    QOL("QoL"),
    PERFORMANCE("Performance");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
