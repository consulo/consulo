
import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@NullMarked
module consulo.version.control.system.review.api {
    requires transitive consulo.review.api;
    requires transitive consulo.version.control.system.api;
    requires consulo.file.editor.api;
    requires consulo.application.api;

    exports consulo.versionControlSystem.review;
    exports consulo.versionControlSystem.review.action;
    exports consulo.versionControlSystem.review.changes;
    exports consulo.versionControlSystem.review.commit;
    exports consulo.versionControlSystem.review.comment;
    exports consulo.versionControlSystem.review.details;
    exports consulo.versionControlSystem.review.details.model;
    exports consulo.versionControlSystem.review.diff;
    exports consulo.versionControlSystem.review.editor;
    exports consulo.versionControlSystem.review.file;
    exports consulo.versionControlSystem.review.issue;
    exports consulo.versionControlSystem.review.diff.model;
    exports consulo.versionControlSystem.review.timeline;
    exports consulo.versionControlSystem.review.util;
}
