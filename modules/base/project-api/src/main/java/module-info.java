import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-01-19
 */
@NullMarked
@SuppressWarnings("module")
module consulo.project.api {
    // TODO [VISTALL] obsolete requires
    requires java.desktop;

    requires transitive consulo.application.api;
    requires transitive consulo.virtual.file.system.api;
    requires transitive consulo.datacontext.api;
    requires consulo.ui.ex.api;

    exports consulo.project;
    exports consulo.project.macro;
    exports consulo.project.event;
    exports consulo.project.startup;
    exports consulo.project.util;
    exports consulo.project.util.query;
    exports consulo.project.localize;

    exports consulo.project.internal to
        consulo.application.impl,
        consulo.compiler.artifact.impl,
        consulo.component.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.execution.impl,
        consulo.external.system.impl,
        consulo.external.service.impl,
        consulo.file.editor.impl,
        consulo.file.chooser.impl,
        consulo.ide.impl,
        consulo.it,
        consulo.language.impl,
        consulo.language.index.impl,
        consulo.module.content.impl,
        consulo.module.impl,
        consulo.project.impl,
        consulo.project.ui.impl,
        consulo.sand.language.plugin,
        consulo.ui.ex.impl,
        consulo.version.control.system.api,
        consulo.version.control.system.impl,
        consulo.virtual.file.system.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;
}