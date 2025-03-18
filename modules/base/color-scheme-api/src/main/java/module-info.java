/**
 * @author VISTALL
 * @since 19-Feb-22
 */
module consulo.color.scheme.api {
    // TODO obsolete dep
    requires java.desktop;

    requires transitive consulo.application.api;

    exports consulo.colorScheme;
    exports consulo.colorScheme.setting;
    exports consulo.colorScheme.event;

    exports consulo.colorScheme.internal to consulo.color.scheme.impl, consulo.ide.impl;
}