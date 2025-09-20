/**
 * @author VISTALL
 * @since 17/01/2022
 */
module consulo.document.api {
    requires transitive consulo.application.api;
    requires transitive consulo.virtual.file.system.api;

    requires transitive kava.beans;

    requires static it.unimi.dsi.fastutil;

    exports consulo.document;
    exports consulo.document.event;
    exports consulo.document.util;

    exports consulo.document.internal to
        consulo.document.impl,
        consulo.file.editor.impl,
        consulo.ide.impl,
        consulo.language.editor.api,
        consulo.language.editor.impl,
        consulo.code.editor.api,
        consulo.code.editor.impl,
        consulo.language.inject.impl,
        consulo.language.code.style.ui.api,
        consulo.language.code.style.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.swt.ide.impl,
        consulo.language.impl,
        consulo.execution.debug.impl,
        consulo.version.control.system.impl;
}