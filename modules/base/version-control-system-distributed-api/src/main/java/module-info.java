/**
 * @author VISTALL
 * @since 2022-07-31
 */
module consulo.version.control.system.distributed.api {
    requires transitive consulo.version.control.system.api;
    requires transitive consulo.version.control.system.log.api;
    requires transitive consulo.file.editor.api;
    requires transitive consulo.module.content.api;
    requires consulo.language.editor.ui.api;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;
    requires forms.rt;

    exports consulo.versionControlSystem.distributed;
    exports consulo.versionControlSystem.distributed.action;
    exports consulo.versionControlSystem.distributed.branch;
    exports consulo.versionControlSystem.distributed.icon;
    exports consulo.versionControlSystem.distributed.localize;
    exports consulo.versionControlSystem.distributed.push;
    exports consulo.versionControlSystem.distributed.repository;
    exports consulo.versionControlSystem.distributed.ui;
    exports consulo.versionControlSystem.distributed.ui.awt;

    opens consulo.versionControlSystem.distributed.branch to consulo.util.xml.serializer;

    exports consulo.versionControlSystem.distributed.internal to consulo.desktop.awt.ide.impl;
}