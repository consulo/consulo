import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@NullMarked
module consulo.review.impl {
    requires transitive consulo.review.api;
    requires consulo.project.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.code.editor.api;
    requires consulo.file.editor.api;
    requires consulo.language.editor.ui.api;
    requires consulo.diff.api;

    exports consulo.review.impl.internal to consulo.version.control.system.review.impl;
    exports consulo.review.impl.internal.editor to consulo.version.control.system.review.impl;
}
