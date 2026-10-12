/**
 * @author VISTALL
 * @since 2022-08-04
 */
@SuppressWarnings("module")
module consulo.file.editor.impl {
    requires consulo.file.editor.api;
    requires consulo.project.ui.view.api;
    requires consulo.find.api;

    requires consulo.code.editor.impl;

    requires consulo.language.editor.impl;

    requires consulo.ide.api;
    requires consulo.project.ui.impl;
    requires consulo.ui.ex.impl;

    requires static consulo.task.api;

    requires com.google.common;

    exports consulo.fileEditor.impl.internal to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl;

    opens consulo.fileEditor.impl.internal to
        consulo.proxy,
        consulo.util.xml.serializer;

    exports consulo.fileEditor.impl.internal.search to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl;

    exports consulo.fileEditor.impl.internal.text to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl;

    exports consulo.fileEditor.impl.internal.unified to
        consulo.desktop.awt.ide.impl;

    opens consulo.fileEditor.impl.internal.unified to
        consulo.proxy,
        consulo.util.xml.serializer;

    // TODO remove in future
    requires java.desktop;

    requires forms.rt;
}