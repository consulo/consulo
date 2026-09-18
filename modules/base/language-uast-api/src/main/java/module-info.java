import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-09-18
 */
@NullMarked
module consulo.language.uast.api {
    requires transitive consulo.language.api;
    requires consulo.application.api;
    requires consulo.logging.api;

    exports consulo.language.uast;
    exports consulo.language.uast.visitor;
    exports consulo.language.uast.util;
}
