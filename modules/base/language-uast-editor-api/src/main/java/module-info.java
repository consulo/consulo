import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-09-18
 */
@NullMarked
module consulo.language.uast.editor.api {
    requires transitive consulo.language.uast.api;
    requires transitive consulo.language.editor.api;

    exports consulo.language.editor.uast;
}
