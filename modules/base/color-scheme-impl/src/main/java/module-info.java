/**
 * @author VISTALL
 * @since 19-Mar-22
 */
module consulo.color.scheme.impl {
    requires transitive consulo.color.scheme.api;
    requires transitive consulo.application.ui.api;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;

    exports consulo.colorScheme.impl.internal to consulo.code.editor.impl,
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl;

    opens consulo.colorScheme.impl.internal to consulo.util.xml.serializer;
}