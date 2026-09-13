/**
 * @author VISTALL
 * @since 2022-12-30
 */
@SuppressWarnings("module")
module consulo.application.ui.impl {
    requires transitive consulo.application.ui.api;
    requires consulo.project.ui.api;

    exports consulo.application.ui.impl.internal to
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

    opens consulo.application.ui.impl.internal to consulo.util.xml.serializer;
}
