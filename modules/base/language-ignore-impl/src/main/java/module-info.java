import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-02-14
 */
@NullMarked
module consulo.language.ignore.impl {
    requires consulo.language.ignore.api;
    requires consulo.language.impl;
    requires consulo.language.editor.api;
    requires consulo.version.control.system.api;
}
