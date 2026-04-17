import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2025-04-14
 */
@NullMarked
module consulo.collaboration.api {
    requires transitive consulo.application.api;
    requires transitive consulo.credential.storage.api;
    requires transitive consulo.http.api;

    requires transitive java.desktop;

    exports consulo.collaboration.util;
    exports consulo.collaboration.api;
    exports consulo.collaboration.api.json;
    exports consulo.collaboration.auth;
    requires transitive consulo.project.api;

    exports consulo.collaboration.ui.codereview.list;
    exports consulo.collaboration.ui.codereview.details;
    exports consulo.collaboration.ui.codereview.details.data;
    exports consulo.collaboration.ui.codereview.comment;
    exports consulo.collaboration.ui.codereview.timeline;
    exports consulo.collaboration.ui.codereview.timeline.thread;
    exports consulo.collaboration.ui.codereview.diff;
    exports consulo.collaboration.ui.codereview.changes;
}
