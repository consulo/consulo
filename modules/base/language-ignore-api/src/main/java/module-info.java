import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-02-13
 */
@NullMarked
module consulo.language.ignore.api {
    requires transitive consulo.language.api;

    exports consulo.language.ignore;
    exports consulo.language.ignore.cache;
    exports consulo.language.ignore.localize;
    exports consulo.language.ignore.psi;
    exports consulo.language.ignore.util;
}
