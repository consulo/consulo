import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-02-23
 */
@NullMarked
module consulo.language.editor.problem.view.api {
    requires consulo.language.editor.api;

    exports consulo.language.editor.problemView;
}