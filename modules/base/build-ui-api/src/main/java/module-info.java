import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-08-01
 */
@NullMarked
@SuppressWarnings("module")
module consulo.build.ui.api {
    // TODO remove in future
    requires java.desktop;

    requires transitive consulo.project.api;
    requires transitive consulo.project.ui.api;
    requires transitive consulo.execution.api;
    requires static consulo.find.api;

    exports consulo.build.ui;
    exports consulo.build.ui.event;
    exports consulo.build.ui.output;
    exports consulo.build.ui.issue;
    exports consulo.build.ui.process;
    exports consulo.build.ui.progress;
    exports consulo.build.ui.quickFix;
    exports consulo.build.ui.localize;

    exports consulo.build.ui.internal to
        consulo.external.system.api,
        consulo.ide.impl,
        consulo.language.editor.problem.view.impl;
}