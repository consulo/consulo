/**
 * @author VISTALL
 * @since 2025-08-01
 */
module consulo.local.history.impl {
    requires consulo.local.history.api;
    requires consulo.diff.api;
    requires consulo.undo.redo.api;
    requires consulo.module.content.api;
    requires consulo.version.control.system.api;
    requires consulo.index.io;
    requires consulo.language.api;

    requires consulo.ui.ex.api;

    requires it.unimi.dsi.fastutil;

    // TODO remove after hardcode ref to awt
    requires consulo.version.control.system.impl;
    requires consulo.ui.ex.awt.api;

    opens consulo.localHistory.impl.internal.ui.action to consulo.component.impl;
}