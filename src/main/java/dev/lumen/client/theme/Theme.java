package dev.lumen.client.theme;

public class Theme {
    private int primaryColor = 0xFFFF2E93;
    private int secondaryColor = 0xFFFF80BF;
    private int backgroundColor = 0xFF0B0B0E;
    private int cardColor = 0xFF14141A;
    private int textColor = 0xFFF2F2F7;
    private String gradientMode = "ANIMATED";
    private float gradientSpeed = 1.0f;

    public static Theme lumenPinkAndBlack() {
        return new Theme();
    }

    public static Theme midnightRose() {
        Theme t = new Theme();
        t.primaryColor = 0xFFE0245E;
        t.secondaryColor = 0xFFFF7EB6;
        t.backgroundColor = 0xFF0A0810;
        t.cardColor = 0xFF181020;
        return t;
    }

    public static Theme cyberMagenta() {
        Theme t = new Theme();
        t.primaryColor = 0xFFFF00E0;
        t.secondaryColor = 0xFF8A2BE2;
        t.backgroundColor = 0xFF050508;
        t.cardColor = 0xFF101018;
        return t;
    }

    public static Theme obsidianWhite() {
        Theme t = new Theme();
        t.primaryColor = 0xFFF5F5FA;
        t.secondaryColor = 0xFFB8B8C8;
        t.backgroundColor = 0xFF0A0A0C;
        t.cardColor = 0xFF16161C;
        return t;
    }

    public static Theme lavenderDusk() {
        Theme t = new Theme();
        t.primaryColor = 0xFFB57EDC;
        t.secondaryColor = 0xFFE6D6FF;
        t.backgroundColor = 0xFF0D0B14;
        t.cardColor = 0xFF1A1626;
        return t;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(int argb) {
        this.primaryColor = argb;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(int argb) {
        this.secondaryColor = argb;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int argb) {
        this.backgroundColor = argb;
    }

    public int getCardColor() {
        return cardColor;
    }

    public void setCardColor(int argb) {
        this.cardColor = argb;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int argb) {
        this.textColor = argb;
    }

    public String getGradientMode() {
        return gradientMode;
    }

    public void setGradientMode(String gradientMode) {
        this.gradientMode = gradientMode;
    }

    public float getGradientSpeed() {
        return gradientSpeed;
    }

    public void setGradientSpeed(float gradientSpeed) {
        this.gradientSpeed = gradientSpeed;
    }
}
