import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-01-17
 */
@NullMarked
@SuppressWarnings("module")
module consulo.virtual.file.system.api {
    requires transitive consulo.application.api;
    requires transitive consulo.util.io;
    requires transitive consulo.util.jna;
    requires transitive consulo.proxy;

    requires consulo.base.icon.library;
    requires consulo.base.localize.library;

    requires org.jdom;

    exports consulo.virtualFileSystem;
    exports consulo.virtualFileSystem.event;
    exports consulo.virtualFileSystem.fileType;
    exports consulo.virtualFileSystem.fileType.localize;
    exports consulo.virtualFileSystem.fileType.matcher;
    exports consulo.virtualFileSystem.encoding;
    exports consulo.virtualFileSystem.archive;
    exports consulo.virtualFileSystem.pointer;
    exports consulo.virtualFileSystem.light;
    exports consulo.virtualFileSystem.util;
    exports consulo.virtualFileSystem.localize;

    exports consulo.virtualFileSystem.internal.core.local to consulo.test.impl;
    exports consulo.virtualFileSystem.internal.matcher to
        consulo.extension.preview.recorder.impl,
        consulo.ide.impl,
        consulo.virtual.file.system.impl;

    exports consulo.virtualFileSystem.internal to
        consulo.application.impl,
        consulo.application.content.impl,
        consulo.component.store.impl,
        consulo.compiler.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.ide.impl,
        consulo.diff.impl,
        consulo.document.api,
        consulo.execution.coverage.impl,
        consulo.file.editor.impl,
        consulo.ide.impl,
        consulo.it,
        consulo.language.impl,
        consulo.language.editor.impl,
        consulo.language.index.impl,
        consulo.local.history.impl,
        consulo.test.impl,
        consulo.test.junit.impl,
        consulo.version.control.system.impl,
        consulo.virtual.file.system.impl;
}
