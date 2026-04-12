/**
 * @author VISTALL
 * @since 2025-08-03
 */
module consulo.search.everywhere.api {
    requires transitive consulo.project.api;
    requires transitive consulo.ui.ex.api;
    requires transitive consulo.application.content.api;
    requires transitive consulo.ui.ex.awt.api;

    exports consulo.searchEverywhere;
}