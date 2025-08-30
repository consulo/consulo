/**
 * @author VISTALL
 * @since 2022-01-29
 */
module consulo.project.ui.api {
    // todo obsolete dep
    requires java.desktop;

    requires transitive consulo.application.ui.api;
    requires transitive consulo.project.api;
    requires transitive consulo.ui.ex.api;
    requires transitive kava.beans;

    exports consulo.project.ui;
    exports consulo.project.ui.action;
    exports consulo.project.ui.localize;
    exports consulo.project.ui.notification;
    exports consulo.project.ui.notification.event;
    exports consulo.project.ui.util;
    exports consulo.project.ui.wm;
    exports consulo.project.ui.wm.event;
    exports consulo.project.ui.wm.dock;
    exports consulo.project.ui.wm.action;

    exports consulo.project.ui.internal to
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.os.mac,
        consulo.desktop.swt.ide.impl,
        consulo.external.system.impl,
        consulo.desktop.ide.impl,
        consulo.ui.ex.awt.api,
        consulo.execution.test.sm.api,
        consulo.execution.coverage.impl,
        consulo.version.control.system.log.impl,
        consulo.file.editor.api,
        consulo.file.editor.impl,
        consulo.execution.impl,
        consulo.project.impl,
        consulo.project.ui.impl;
}