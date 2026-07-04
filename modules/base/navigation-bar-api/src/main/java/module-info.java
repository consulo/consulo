/**
 * @author VISTALL
 * @since 2026-07-04
 */
module consulo.navigation.bar.api {
    requires transitive consulo.project.api;
    requires transitive consulo.ui.ex.api;

    exports consulo.navigationBar;
    exports consulo.navigationBar.model;

    exports consulo.navigationBar.internal to
        consulo.desktop.awt.ide.impl,
        consulo.ide.impl,
        consulo.navigation.bar.impl;
}