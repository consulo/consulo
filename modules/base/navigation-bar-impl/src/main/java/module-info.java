/**
 * @author VISTALL
 * @since 2026-07-04
 */
module consulo.navigation.bar.impl {
    requires consulo.navigation.bar.api;
    requires consulo.module.content.api;
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.language.ui.api;

    exports consulo.navigationBar.impl.internal to consulo.desktop.awt.ide.impl;
}