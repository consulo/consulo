/**
 * @author VISTALL
 * @since 2022-01-29
 */
@SuppressWarnings("module")
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
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.awt.os.mac,
        consulo.desktop.ide.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.execution.test.sm.api,
        consulo.execution.coverage.impl,
        consulo.external.system.impl,
        consulo.file.editor.api,
        consulo.file.editor.impl,
        consulo.ide.impl,
        consulo.it,
        consulo.execution.impl,
        consulo.project.impl,
        consulo.project.ui.impl,
        consulo.ui.ex.awt.api,
        consulo.version.control.system.log.impl,
        consulo.version.control.system.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;
}