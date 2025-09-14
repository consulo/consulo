/**
 * @author VISTALL
 * @since 2025-09-14
 */
module consulo.color.scheme.ui.api {
    requires transitive consulo.color.scheme.api;

    exports consulo.colorScheme.ui;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;
}