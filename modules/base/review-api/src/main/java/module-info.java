import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@NullMarked
module consulo.review.api {
    requires transitive consulo.project.api;
    requires transitive consulo.ui.api;

    exports consulo.review.comment;
    exports consulo.review.create;
    exports consulo.review.details.data;
    exports consulo.review.details.model;
    exports consulo.review.diff;
    exports consulo.review.diff.model;
    exports consulo.review.editor;
    exports consulo.review.icon;
    exports consulo.review.list;
    exports consulo.review.list.search;
    exports consulo.review.localize;
    exports consulo.review.reactions;
    exports consulo.review.submit;
    exports consulo.review.timeline;
    exports consulo.review.timeline.thread;
    exports consulo.review.toolwindow;
    exports consulo.review.user;
    exports consulo.review.util;
}
