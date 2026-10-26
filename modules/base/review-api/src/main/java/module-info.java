import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@NullMarked
module consulo.review.api {
    requires transitive consulo.project.api;
    requires transitive consulo.project.ui.api;
    requires transitive consulo.ui.api;
    requires transitive consulo.code.editor.api;
    requires consulo.module.content.api;

    exports consulo.review;
    exports consulo.review.async;
    exports consulo.review.comment;
    exports consulo.review.create;
    exports consulo.review.details;
    exports consulo.review.details.data;
    exports consulo.review.details.model;
    exports consulo.review.diff;
    exports consulo.review.diff.action;
    exports consulo.review.diff.model;
    exports consulo.review.editor;
    exports consulo.review.icon;
    exports consulo.review.list;
    exports consulo.review.list.error;
    exports consulo.review.list.search;
    exports consulo.review.localize;
    exports consulo.review.notification;
    exports consulo.review.reactions;
    exports consulo.review.snippet;
    exports consulo.review.submit;
    exports consulo.review.timeline;
    exports consulo.review.timeline.thread;
    exports consulo.review.toolwindow;
    exports consulo.review.user;
    exports consulo.review.util;
    exports consulo.review.util.popup;
}
