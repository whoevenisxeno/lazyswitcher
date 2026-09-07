package dev.xeno.accountswitcher.gui;

import dev.xeno.accountswitcher.account.AccountType;

/** LazySwitcher dark/purple palette. All values are ARGB. */
public final class Palette {
    public static final int BG        = 0xE6100C18;
    public static final int TITLE     = 0xFFB98CFF;
    public static final int DIVIDER   = 0xFF5A3E8F;
    public static final int TEXT      = 0xFFECE8F5;
    public static final int TEXT_DIM  = 0xFF8A82A0;
    public static final int OK        = 0xFF8AE0B0;
    public static final int WARN      = 0xFFF3C969;
    public static final int ERROR     = 0xFFF07A7A;

    public static final int ROW       = 0x59171326;
    public static final int ROW_SEL   = 0xFF2E2247;
    public static final int ROW_HOVER = 0x268C6BFF;

    public static final int BADGE_PRISM = 0xFF8C6BFF;
    public static final int BADGE_MSA   = 0xFF54C98A;
    public static final int BADGE_OFF   = 0xFFE8A24C;
    public static final int BADGE_ALT   = 0xFFF06BA6;

    public static int badge(boolean prism, AccountType type) {
        if (prism) return BADGE_PRISM;
        return switch (type) {
            case MICROSOFT   -> BADGE_MSA;
            case OFFLINE     -> BADGE_OFF;
            case ALT_SERVICE -> BADGE_ALT;
        };
    }
}
