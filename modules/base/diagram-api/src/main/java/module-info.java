import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2025-09-02
 */
@NullMarked
module consulo.diagram.api {
    requires transitive consulo.application.api;

    requires transitive consulo.datacontext.api;

    exports consulo.diagram;

    exports consulo.diagram.internal to consulo.desktop.awt.ide.impl;
}