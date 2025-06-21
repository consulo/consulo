/**
 * @author VISTALL
 * @since 27-Feb-22
 */
module consulo.virtual.file.system.impl {
    requires consulo.virtual.file.system.api;
    requires consulo.util.collection;

    requires consulo.process.api;
    requires consulo.application.api;
    requires consulo.project.api;
    requires consulo.module.content.api;
    requires consulo.index.io;

    requires static com.sun.jna;
    requires static consulo.util.jna;

    requires gnu.trove;

    // TODO remove in future - need for java plugin
    exports consulo.virtualFileSystem.impl;

    // FIXME used by AtomicFieldUpdater - maybe replace it?
    opens consulo.virtualFileSystem.impl.internal.entry to consulo.util.concurrent;
}