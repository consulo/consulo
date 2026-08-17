import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-01-19
 */
@NullMarked
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
        consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl,
        consulo.ui.ex.impl,
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl, consulo.desktop.awt.ui.impl,
        consulo.sand.language.plugin,
        consulo.application.impl,
        consulo.component.impl,
        consulo.module.impl,
        consulo.version.control.system.api,
        consulo.version.control.system.impl,
        consulo.compiler.artifact.impl,
        consulo.project.impl,
        consulo.project.ui.impl,
        consulo.execution.impl,
        consulo.language.impl,
        consulo.language.index.impl,
        consulo.virtual.file.system.impl,
        consulo.external.system.impl,
        consulo.external.service.impl,
        consulo.module.content.impl,
        consulo.file.editor.impl,
        consulo.file.chooser.impl,
        consulo.it,
        consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;
}