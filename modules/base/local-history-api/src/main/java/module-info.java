/**
 * @author VISTALL
 * @since 04-Apr-22
 */
module consulo.local.history.api {
    requires transitive consulo.project.api;

    exports consulo.localHistory;
    exports consulo.localHistory.localize;

    exports consulo.localHistory.internal to consulo.version.control.system.impl, consulo.local.history.impl;
}