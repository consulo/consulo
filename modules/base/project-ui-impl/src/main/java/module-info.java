/**
 * @author VISTALL
 * @since 2024-07-24
 */
module consulo.project.ui.impl {
    requires transitive consulo.project.ui.api;
    
    requires consulo.undo.redo.api;
    requires consulo.file.editor.api;
    requires consulo.module.content.api;
    requires consulo.external.service.api;
    
    exports consulo.project.ui.impl.internal to
        consulo.version.control.system.impl,
        consulo.ide.impl;

    exports consulo.project.ui.impl.internal.wm to
        consulo.desktop.swt.ide.impl,
        consulo.desktop.awt.ide.impl;

    exports consulo.project.ui.impl.internal.wm.statusBar to
        consulo.ide.impl;

    opens consulo.project.ui.impl.internal.wm.statusBar to
        consulo.util.xml.serializer,
        consulo.component.impl;

    exports consulo.project.ui.impl.internal.action to
        consulo.ide.impl;

    exports consulo.project.ui.impl.internal.wm.action to
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;
}