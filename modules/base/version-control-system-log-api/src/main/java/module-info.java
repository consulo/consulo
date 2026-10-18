/**
 * @author VISTALL
 * @since 2022-07-31
 */
@SuppressWarnings("module")
module consulo.version.control.system.log.api {
    requires transitive consulo.ui.ex.api;
    requires consulo.index.io;

    requires transitive consulo.version.control.system.api;

    requires consulo.external.service.api;

    exports consulo.versionControlSystem.log;
    exports consulo.versionControlSystem.log.base;
    exports consulo.versionControlSystem.log.event;
    exports consulo.versionControlSystem.log.graph;
    exports consulo.versionControlSystem.log.graph.action;
    exports consulo.versionControlSystem.log.graph.paint;
    exports consulo.versionControlSystem.log.paint;
    exports consulo.versionControlSystem.log.localize;
    exports consulo.versionControlSystem.log.ui;
    exports consulo.versionControlSystem.log.util;

    exports consulo.versionControlSystem.log.internal to
        consulo.version.control.system.log.impl;
}