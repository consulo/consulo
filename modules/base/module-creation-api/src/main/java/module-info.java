/**
 * @author VISTALL
 * @since 2026-05-01
 */
module consulo.module.creation.api {
    requires transitive consulo.module.api;
    requires transitive consulo.module.content.api;
    requires transitive consulo.ui.ex.api;
    requires consulo.application.ui.api;
    requires consulo.module.ui.api;

    exports consulo.module.creation;
    exports consulo.module.creation.importing;
    exports consulo.module.creation.scratch;
    exports consulo.module.creation.ui;
}