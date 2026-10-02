package consulo.endpoint.internal;

public final class EndpointStringUtil {
    private EndpointStringUtil() {
    }

    public static boolean isBlank(CharSequence s) {
        return s.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }
}
