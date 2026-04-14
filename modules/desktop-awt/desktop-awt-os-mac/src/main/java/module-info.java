/**
 * @author VISTALL
 * @since 2025-06-07
 */
module consulo.desktop.awt.os.mac {
    requires consulo.ide.api;
    requires consulo.ide.impl;
    requires consulo.desktop.awt.ide.impl;

    requires consulo.desktop.awt.hacking;
    requires consulo.application.api;
    requires consulo.util.jna;
    requires consulo.file.chooser.api;
    requires consulo.application.impl;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.undo.redo.api;
    requires consulo.code.editor.api;
    requires consulo.execution.api;

    opens consulo.desktop.awt.os.mac.internal.touchBar to com.sun.jna;
}