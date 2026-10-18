
import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@NullMarked
module consulo.version.control.system.review.impl {
    requires consulo.version.control.system.review.api;
    requires consulo.review.impl;
    requires consulo.ui.ex.api;
    requires consulo.version.control.system.impl;
    requires consulo.diff.api;
    requires consulo.language.editor.api;
}
