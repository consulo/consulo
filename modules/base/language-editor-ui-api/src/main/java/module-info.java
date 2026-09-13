/**
 * @author VISTALL
 * @since 2022-03-15
 */
@SuppressWarnings("module")
module consulo.language.editor.ui.api {
    // TODO remove this dependency in future
    requires java.desktop;
    requires transitive consulo.ui.ex.awt.api;

    requires transitive consulo.code.editor.api;
    requires transitive consulo.language.editor.api;
    requires transitive consulo.language.ui.api;
    requires transitive consulo.language.spellchecker.editor.api;
    requires transitive consulo.usage.api;
    requires consulo.find.api;

    exports consulo.language.editor.ui;
    exports consulo.language.editor.ui.navigation;
    exports consulo.language.editor.ui.navigationBar;
    exports consulo.language.editor.ui.awt;
    exports consulo.language.editor.ui.scope;
    exports consulo.language.editor.ui.awt.scope;
    exports consulo.language.editor.ui.util;

    exports consulo.language.editor.ui.internal to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.editor.impl,
        consulo.execution.debug.impl,
        consulo.file.editor.impl,
        consulo.ide.impl,
        consulo.version.control.system.impl,
        consulo.web.editor.impl;
}