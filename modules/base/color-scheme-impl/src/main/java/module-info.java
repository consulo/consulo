/**
 * @author VISTALL
 * @since 2022-03-19
 */
@SuppressWarnings("module")
module consulo.color.scheme.impl {
    requires transitive consulo.color.scheme.api;
    requires transitive consulo.application.ui.api;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;

    exports consulo.colorScheme.impl.internal to
        consulo.code.editor.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl;

    opens consulo.colorScheme.impl.internal to consulo.util.xml.serializer;
}