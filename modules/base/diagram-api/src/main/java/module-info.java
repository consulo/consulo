import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2025-09-02
 */
@NullMarked
@SuppressWarnings("module")
module consulo.diagram.api {
    requires transitive consulo.application.api;
    requires transitive consulo.datacontext.api;
    requires transitive consulo.language.api;
    requires transitive consulo.ui.ex.api;

    exports consulo.diagram;
    exports consulo.diagram.presentation;
    exports consulo.diagram.providers;
    exports consulo.diagram.settings;
}
