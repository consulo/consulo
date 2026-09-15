/**
 * @author VISTALL
 * @since 2022-02-05
 */
@SuppressWarnings("module")
module consulo.process.api {
    requires transitive consulo.application.api;
    requires transitive consulo.virtual.file.system.api;
    requires transitive consulo.util.dataholder;

    requires consulo.util.jna;

    exports consulo.process;
    exports consulo.process.cmd;
    exports consulo.process.io;
    exports consulo.process.event;
    exports consulo.process.local;
    exports consulo.process.localize;
    exports consulo.process.util;

    exports consulo.process.internal to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.execution.debug.api,
        consulo.execution.impl,
        consulo.execution.test.api,
        consulo.execution.test.sm.api,
        consulo.ide.impl,
        consulo.process.impl,
        consulo.remote.server.impl,
        consulo.virtual.file.system.impl;
}