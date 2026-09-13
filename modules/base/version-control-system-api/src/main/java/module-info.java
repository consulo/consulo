/**
 * @author VISTALL
 * @since 2022-07-10
 */
@SuppressWarnings("module")
module consulo.version.control.system.api {
    // TODO remove this in future
    requires java.desktop;
    requires forms.rt;

    requires transitive consulo.application.api;
    requires transitive consulo.project.api;
    requires transitive consulo.virtual.file.status.api;
    requires transitive consulo.local.history.api;

    requires consulo.application.ui.api;
    requires consulo.code.editor.api;
    requires consulo.execution.api;
    requires consulo.language.api;
    requires transitive consulo.diff.api;

    requires consulo.ui.ex.awt.api;

    exports consulo.versionControlSystem;
    exports consulo.versionControlSystem.codeVision;
    exports consulo.versionControlSystem.checkin;
    exports consulo.versionControlSystem.checkout;
    exports consulo.versionControlSystem.localize;
    exports consulo.versionControlSystem.contentAnnotation;
    exports consulo.versionControlSystem.change;
    exports consulo.versionControlSystem.change.action;
    exports consulo.versionControlSystem.change.diff;
    exports consulo.versionControlSystem.change.patch;
    exports consulo.versionControlSystem.change.shelf;
    exports consulo.versionControlSystem.base;
    exports consulo.versionControlSystem.history;
    exports consulo.versionControlSystem.ui;
    exports consulo.versionControlSystem.ui.awt;
    exports consulo.versionControlSystem.icon;
    exports consulo.versionControlSystem.action;
    exports consulo.versionControlSystem.diff;
    exports consulo.versionControlSystem.rollback;
    exports consulo.versionControlSystem.versionBrowser;
    exports consulo.versionControlSystem.annotate;
    exports consulo.versionControlSystem.change.commited;
    exports consulo.versionControlSystem.merge;
    exports consulo.versionControlSystem.update;
    exports consulo.versionControlSystem.root;
    exports consulo.versionControlSystem.util;
    exports consulo.versionControlSystem.versionBrowser.ui.awt;
    exports consulo.versionControlSystem.virtualFileSystem;

    exports consulo.versionControlSystem.internal to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl,
        consulo.local.history.impl,
        consulo.test.impl,
        consulo.version.control.system.distributed.api,
        consulo.version.control.system.distributed.impl,
        consulo.version.control.system.impl,
        consulo.version.control.system.log.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;

    requires com.google.common;
    requires it.unimi.dsi.fastutil;

    opens consulo.versionControlSystem to consulo.util.xml.serializer;
}