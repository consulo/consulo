/**
 * @author VISTALL
 * @since 2022-01-17
 */
@SuppressWarnings("module")
module consulo.document.api {
    requires transitive consulo.application.api;
    requires transitive consulo.virtual.file.system.api;

    requires transitive kava.beans;

    requires static it.unimi.dsi.fastutil;

    exports consulo.document;
    exports consulo.document.event;
    exports consulo.document.localize;
    exports consulo.document.util;

    exports consulo.document.internal to
        consulo.code.editor.api,
        consulo.code.editor.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.document.impl,
        consulo.execution.debug.impl,
        consulo.file.editor.impl,
        consulo.ide.impl,
        consulo.it,
        consulo.language.code.style.ui.api,
        consulo.language.code.style.impl,
        consulo.language.editor.api,
        consulo.language.editor.impl,
        consulo.language.impl,
        consulo.language.inject.impl,
        consulo.version.control.system.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;
}