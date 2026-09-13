/**
 * @author VISTALL
 * @since 2022-03-22
 */
@SuppressWarnings("module")
module consulo.component.store.impl {
    requires transitive consulo.component.impl;
    requires transitive consulo.virtual.file.system.api;
    requires transitive consulo.util.io;
    requires transitive consulo.component.store.api;

    requires org.lz4.java;
    requires com.dslplatform.json;

    exports consulo.component.store.impl.internal to
        consulo.application.impl,
        consulo.component.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl,
        consulo.module.impl,
        consulo.project.impl,
        consulo.proxy,
        consulo.test.impl,
        consulo.web.ide,
        consulo.web.ui.impl,
        consulo.web.editor.impl;
    exports consulo.component.store.impl.internal.storage to
        consulo.application.impl,
        consulo.component.impl,
        consulo.ide.impl,
        consulo.project.impl;
    exports consulo.component.store.impl.internal.scheme to consulo.application.impl;
}