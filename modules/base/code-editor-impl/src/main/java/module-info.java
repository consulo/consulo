/**
 * @author VISTALL
 * @since 19-Mar-22
 */
module consulo.code.editor.impl {
    // TODO remove in future this dependency
    requires java.desktop;
    requires consulo.desktop.awt.hacking;

    requires transitive consulo.code.editor.api;
    requires transitive consulo.ui.ex.awt.api;
    requires transitive consulo.document.impl;
    requires transitive consulo.color.scheme.impl;
    requires transitive consulo.language.api;
    requires transitive consulo.language.code.style.api;

    exports consulo.codeEditor.impl to
        consulo.ide.impl,
        consulo.language.inject.impl,
        consulo.file.editor.impl,
        consulo.desktop.ide.impl,
        consulo.util.xml.serializer,
        consulo.desktop.awt.ide.impl, consulo.desktop.awt.ui.impl,
        consulo.desktop.swt.ide.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl,
        consulo.language.editor.impl,
        consulo.execution.debug.impl,
        consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;

    exports consulo.codeEditor.impl.util to
        consulo.ide.impl,
        consulo.language.inject.impl,
        consulo.desktop.ide.impl,
        consulo.desktop.awt.ide.impl, consulo.desktop.awt.ui.impl,
        consulo.desktop.swt.ide.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl;

    exports consulo.codeEditor.impl.internal to consulo.ide.impl, consulo.desktop.awt.ide.impl, consulo.desktop.awt.ui.impl;

    exports consulo.codeEditor.impl.softwrap.mapping to consulo.ide.impl, consulo.desktop.swt.ide.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl, consulo.desktop.awt.ide.impl, consulo.desktop.awt.ui.impl, consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;
    exports consulo.codeEditor.impl.softwrap to consulo.ide.impl, consulo.desktop.swt.ide.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl, consulo.desktop.awt.ide.impl, consulo.desktop.awt.ui.impl, consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;

    exports consulo.codeEditor.impl.internal.action to consulo.ide.impl;
}