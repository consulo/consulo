/**
 * @author VISTALL
 * @since 2022-03-19
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
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.ide.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.execution.debug.impl,
        consulo.file.editor.impl,
        consulo.ide.impl,
        consulo.language.editor.impl,
        consulo.language.inject.impl,
        consulo.util.xml.serializer,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl,
        consulo.it;

    exports consulo.codeEditor.impl.internal to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl;

    exports consulo.codeEditor.impl.internal.action to
        consulo.language.editor.impl,
        consulo.ide.impl;

    exports consulo.codeEditor.impl.softwrap to
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ui.impl,
        consulo.ide.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;

    exports consulo.codeEditor.impl.softwrap.mapping to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.ide.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;

    exports consulo.codeEditor.impl.util to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.ide.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.ide.impl,
        consulo.language.inject.impl;
}