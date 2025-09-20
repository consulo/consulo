/**
 * @author VISTALL
 * @since 04-Aug-22
 */
module consulo.file.editor.impl {
    requires consulo.file.editor.api;
    requires consulo.project.ui.view.api;
    requires consulo.find.api;

    requires static consulo.task.api;

    requires com.google.common;

    exports consulo.fileEditor.impl.internal to consulo.desktop.awt.ide.impl, consulo.ide.impl;

    opens consulo.fileEditor.impl.internal to consulo.util.xml.serializer, consulo.proxy;

    exports consulo.fileEditor.impl.internal.search to consulo.ide.impl, consulo.desktop.awt.ide.impl;

    // TODO remove in future
    requires java.desktop;
}