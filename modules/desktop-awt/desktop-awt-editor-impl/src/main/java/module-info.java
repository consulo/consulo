/**
 * The awt editor surface - the swing peers, the view layer and the models behind them.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
module consulo.desktop.awt.editor.impl {
    requires java.desktop;

    requires consulo.ide.impl;
    requires consulo.desktop.ide.impl;
    requires consulo.desktop.awt.ui.impl;

    exports consulo.desktop.awt.editor.impl.internal to
        consulo.desktop.awt.ide.impl;
    exports consulo.desktop.awt.editor.impl.internal.gutter to
        consulo.desktop.awt.ide.impl;
    exports consulo.desktop.awt.editor.impl.internal.stickyLine to
        consulo.desktop.awt.ide.impl;
    exports consulo.desktop.awt.editor.impl.internal.view to
        consulo.desktop.awt.ide.impl;
}
