import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-08-10
 */
@NullMarked
module consulo.bookmark.api {
    requires transitive consulo.annotation;
    requires transitive consulo.document.api;
    requires transitive consulo.project.api;
    requires transitive consulo.navigation.api;
    requires transitive consulo.code.editor.api;

    exports consulo.bookmark;
    exports consulo.bookmark.event;
    exports consulo.bookmark.icon;
    exports consulo.bookmark.localize;

    exports consulo.bookmark.internal to
        consulo.bookmark.impl,
        consulo.bookmark.ui.view.impl,
        consulo.ide.impl;
}