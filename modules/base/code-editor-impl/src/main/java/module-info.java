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
        consulo.desktop.ide.impl,
        consulo.util.xml.serializer,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.swt.ide.impl,
        consulo.language.editor.impl,
        consulo.execution.debug.impl;

    exports consulo.codeEditor.impl.util to
        consulo.ide.impl,
        consulo.language.inject.impl,
        consulo.desktop.ide.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.swt.ide.impl;

    exports consulo.codeEditor.impl.softwrap.mapping to consulo.ide.impl, consulo.desktop.swt.ide.impl, consulo.desktop.awt.ide.impl;
    exports consulo.codeEditor.impl.softwrap to consulo.ide.impl, consulo.desktop.swt.ide.impl, consulo.desktop.awt.ide.impl;
}