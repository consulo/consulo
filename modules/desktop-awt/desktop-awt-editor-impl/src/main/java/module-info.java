/**
 * The awt editor surface - the swing peers, the view layer and the models behind them.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
module consulo.desktop.awt.editor.impl {
    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.application.ui.api;
    requires consulo.code.editor.api;
    requires consulo.color.scheme.api;
    requires consulo.component.api;
    requires consulo.datacontext.api;
    requires consulo.diff.api;
    requires consulo.disposer.api;
    requires consulo.document.api;
    requires consulo.document.impl;
    requires consulo.file.editor.api;
    requires consulo.language.api;
    requires consulo.language.code.style.api;
    requires consulo.language.editor.api;
    requires consulo.language.editor.ui.api;
    requires consulo.localize.api;
    requires consulo.logging.api;
    requires consulo.platform.api;
    requires consulo.project.api;
    requires consulo.project.ui.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.undo.redo.api;
    requires consulo.util.collection;
    requires consulo.util.collection.primitive;
    requires consulo.util.concurrent;
    requires consulo.util.dataholder;
    requires consulo.util.lang;
    requires consulo.virtual.file.system.api;
    requires consulo.base.icon.library;
    requires consulo.base.localize.library;
    requires kava.beans;

    requires consulo.application.impl;

    requires consulo.code.editor.impl;

    requires consulo.desktop.awt.hacking;
    requires consulo.desktop.awt.ui.impl;

    requires consulo.execution.coverage.api;
    requires consulo.execution.debug.api;

    requires consulo.ide.impl;
    requires consulo.language.editor.impl;

    requires consulo.ui.ex.awt.api;
    requires consulo.ui.ex.impl;

    requires consulo.version.control.system.api;

    requires gnu.trove;

    requires it.unimi.dsi.fastutil;
    requires java.desktop;

    requires jetbrains.runtime.api;

    exports consulo.desktop.awt.editor.impl.internal to
        consulo.desktop.awt.ide.impl;
    exports consulo.desktop.awt.editor.impl.internal.gutter to
        consulo.desktop.awt.ide.impl;
    exports consulo.desktop.awt.editor.impl.internal.stickyLine to
        consulo.desktop.awt.ide.impl;
    exports consulo.desktop.awt.editor.impl.internal.view to
        consulo.desktop.awt.ide.impl;
}
