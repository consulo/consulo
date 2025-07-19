/**
 * @author VISTALL
 * @since 2025-07-19
 */
module consulo.bookmark.impl {
    requires consulo.bookmark.api;

    requires consulo.language.api;

    requires consulo.file.editor.api;
    
    requires consulo.language.editor.api;

    // TODO remove in future
    requires consulo.ui.ex.awt.api;
}