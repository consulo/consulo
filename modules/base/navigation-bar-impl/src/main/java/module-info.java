/**
 * @author VISTALL
 * @since 2026-07-04
 */
@SuppressWarnings("module")
module consulo.navigation.bar.impl {
    requires consulo.navigation.bar.api;
    requires consulo.module.content.api;
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.language.ui.api;

    exports consulo.navigationBar.impl.internal to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;
}