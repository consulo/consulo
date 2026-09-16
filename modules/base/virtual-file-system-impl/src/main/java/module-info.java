/**
 * @author VISTALL
 * @since 2022-02-27
 */
@SuppressWarnings("module")
module consulo.virtual.file.system.impl {
    requires consulo.virtual.file.system.api;
    requires consulo.util.collection;
    requires consulo.util.xml.serializer;

    requires consulo.process.api;
    requires consulo.application.api;
    requires consulo.project.api;
    requires consulo.module.content.api;
    requires consulo.index.io;

    requires static com.sun.jna;
    requires static consulo.util.jna;

    requires gnu.trove;

    // TODO maybe rework - used by readonly status service
    requires consulo.ui.ex.awt.api;
    requires forms.rt;

    exports consulo.virtualFileSystem.impl.internal to consulo.language.index.impl;
    exports consulo.virtualFileSystem.impl.internal.fileType to
        consulo.ide.impl,
        consulo.it;
    exports consulo.virtualFileSystem.impl.internal.encoding to
        consulo.ide.impl,
        consulo.it;

    // FIXME used by AtomicFieldUpdater - maybe replace it?
    opens consulo.virtualFileSystem.impl.internal.entry to consulo.util.concurrent;

    opens consulo.virtualFileSystem.impl.internal.readOnlyStatus to consulo.util.xml.serializer;

    opens consulo.virtualFileSystem.impl.internal.encoding to consulo.util.xml.serializer;
}