/**
 * The web editor surface - the vaadin component, the view layer and the models behind them.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
module consulo.web.editor.impl {
    requires consulo.language.editor.ui.api;
    requires java.desktop;
    requires consulo.annotation;
    requires consulo.application.api;

    requires consulo.base.icon.library;
    requires consulo.base.localize.library;
    requires consulo.code.editor.api;
    requires consulo.code.editor.impl;
    requires consulo.color.scheme.api;
    requires consulo.component.api;

    requires consulo.datacontext.api;
    requires consulo.diff.api;
    requires consulo.disposer.api;
    requires consulo.document.api;
    requires consulo.execution.coverage.api;
    requires consulo.ide.impl;
    requires consulo.web.ui.impl;
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.language.editor.impl;
    requires consulo.language.editor.refactoring.api;
    requires consulo.localize.api;
    requires consulo.navigation.api;

    requires consulo.project.api;

    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.ui.ex.impl;
    requires consulo.undo.redo.api;
    requires consulo.util.collection;
    requires consulo.util.dataholder;
    requires consulo.util.lang;
    requires consulo.version.control.system.api;
    requires consulo.virtual.file.system.api;

    requires flow.server;

    requires jakarta.inject;

    exports consulo.web.editor.impl.internal to
        consulo.web.ide;
    exports consulo.web.editor.impl.internal.gutter to
        consulo.web.ide;
}
