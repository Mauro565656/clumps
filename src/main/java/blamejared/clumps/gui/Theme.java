package blamejared.clumps.gui;

import java.util.LinkedHashMap;
import java.util.Map;

public class Theme {

    public final String name;
    public final int primary;
    public final int glow;
    public final int white;
    public final int gray;
    public final int muted;
    public final int accent;

    public Theme(String name, int primary, int glow, int white, int gray, int muted, int accent) {
        this.name = name;
        this.primary = primary;
        this.glow = glow;
        this.white = white;
        this.gray = gray;
        this.muted = muted;
        this.accent = accent;
    }

    public static final Theme VAULT = new Theme("Vault",
        0xFF00D5FF, 0x3D00D5FF, 0xFFFFFFFF, 0xFF6F788C, 0xFF5E687A, 0x2200D5FF);
    public static final Theme NOVA = new Theme("Nova",
        0xFFAA66FF, 0x3DAA66FF, 0xFFFFFFFF, 0xFF7A6B8C, 0xFF685E7A, 0x22AA66FF);
    public static final Theme ROSE = new Theme("Rose",
        0xFFFF6B9D, 0x3DFF6B9D, 0xFFFFFFFF, 0xFF8C6B7A, 0xFF7A5E6B, 0x22FF6B9D);
    public static final Theme EMBER = new Theme("Ember",
        0xFFFF8844, 0x3DFF8844, 0xFFFFFFFF, 0xFF8C7A6B, 0xFF7A6B5E, 0x22FF8844);
    public static final Theme FOREST = new Theme("Forest",
        0xFF66FF88, 0x3D66FF88, 0xFFFFFFFF, 0xFF6B8C74, 0xFF5E7A66, 0x2266FF88);
    public static final Theme INDIGO = new Theme("Indigo",
        0xFF6666FF, 0x3D6666FF, 0xFFFFFFFF, 0xFF6B6B8C, 0xFF5E5E7A, 0x226666FF);
    public static final Theme MAGENTA = new Theme("Magenta",
        0xFFFF66FF, 0x3DFF66FF, 0xFFFFFFFF, 0xFF8C6B8C, 0xFF7A5E7A, 0x22FF66FF);
    public static final Theme OCEAN = new Theme("Ocean",
        0xFF00FFCC, 0x3D00FFCC, 0xFFFFFFFF, 0xFF6B8C84, 0xFF5E7A74, 0x2200FFCC);
    public static final Theme CRIMSON = new Theme("Crimson",
        0xFFFF4444, 0x3DFF4444, 0xFFFFFFFF, 0xFF8C6B6B, 0xFF7A5E5E, 0x22FF4444);
    public static final Theme LIME = new Theme("Lime",
        0xFF88FF44, 0x3D88FF44, 0xFFFFFFFF, 0xFF7A8C6B, 0xFF6B7A5E, 0x2288FF44);
    public static final Theme GOLD = new Theme("Gold",
        0xFFFFD700, 0x3DFFD700, 0xFFFFFFFF, 0xFF8C846B, 0xFF7A745E, 0x22FFD700);
    public static final Theme AURORA = new Theme("Aurora",
        0xFF66DDFF, 0x3D66DDFF, 0xFFFFFFFF, 0xFF6B7A8C, 0xFF5E6B7A, 0x2266DDFF);

    private static final Map<String, Theme> THEMES = new LinkedHashMap<>();

    static {
        for (Theme t : new Theme[]{VAULT, NOVA, ROSE, EMBER, FOREST, INDIGO, MAGENTA, OCEAN, CRIMSON, LIME, GOLD, AURORA}) {
            THEMES.put(t.name, t);
        }
    }

    public static Theme get(String name) {
        return THEMES.getOrDefault(name, VAULT);
    }

    public static Map<String, Theme> all() { return THEMES; }

    public static String[] names() {
        return THEMES.keySet().toArray(new String[0]);
    }

    private static String currentThemeName = "Vault";

    public static Theme current() { return get(currentThemeName); }
    public static void setCurrent(String name) { if (THEMES.containsKey(name)) currentThemeName = name; }
    public static String currentName() { return currentThemeName; }
    public static void cycle() {
        String[] names = names();
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(currentThemeName)) {
                setCurrent(names[(i + 1) % names.length]);
                return;
            }
        }
        setCurrent(names[0]);
    }
}
