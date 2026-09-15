/**
 * @author VISTALL
 * @since 2022-04-04
 */
@SuppressWarnings("module")
module consulo.local.history.api {
    requires transitive consulo.project.api;

    exports consulo.localHistory;
    exports consulo.localHistory.localize;

    exports consulo.localHistory.internal to
        consulo.local.history.impl,
        consulo.version.control.system.impl;
}