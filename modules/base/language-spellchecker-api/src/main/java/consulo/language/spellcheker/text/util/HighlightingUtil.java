package consulo.language.spellcheker.text.util;

public final class HighlightingUtil {
    private HighlightingUtil() {
    }

    public static boolean isSpace(Character symbol) {
        return Character.isWhitespace(symbol) || Character.isSpaceChar(symbol);
    }
}
