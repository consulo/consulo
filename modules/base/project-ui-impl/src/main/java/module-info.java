/**
 * @author VISTALL
 * @since 2024-07-24
 */
module consulo.project.ui.impl {
    requires transitive consulo.project.ui.api;
    requires consulo.undo.redo.api;

    exports consulo.project.ui.impl.internal to
        consulo.version.control.system.impl,
        consulo.ide.impl;

    exports consulo.project.ui.impl.internal.wm.statusBar to
        consulo.ide.impl;

    opens consulo.project.ui.impl.internal.wm.statusBar to
        consulo.util.xml.serializer,
        consulo.component.impl;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ide.api;
    requires consulo.ui.ex.awt.api;
}