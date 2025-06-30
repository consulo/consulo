/**
 * @author VISTALL
 * @since 2022-04-02
 */
module consulo.execution.coverage.api {
    // TODO remove in future
    requires java.desktop;

    requires transitive consulo.project.api;
    requires transitive consulo.project.ui.view.api;
    requires transitive consulo.ui.ex.awt.api;
    requires transitive consulo.execution.api;
    requires transitive consulo.execution.test.api;
    requires transitive consulo.xcoverage.rt;

    exports consulo.execution.coverage;
    exports consulo.execution.coverage.action;
    exports consulo.execution.coverage.localize;
    exports consulo.execution.coverage.icon;
    exports consulo.execution.coverage.view;

    exports consulo.execution.coverage.internal to
        consulo.ide.impl,
        consulo.execution.coverage.impl;
}