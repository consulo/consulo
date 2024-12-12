/**
 * @author VISTALL
 * @since 10-Aug-22
 */
module consulo.bookmark.api {
    requires transitive consulo.document.api;
    requires transitive consulo.project.api;
    requires transitive consulo.navigation.api;
    requires transitive consulo.code.editor.api;

    exports consulo.bookmark;
    exports consulo.bookmark.event;
    exports consulo.bookmark.icon;
}