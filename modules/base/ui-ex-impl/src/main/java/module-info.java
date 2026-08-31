/**
 * @author VISTALL
 * @since 2026-07-27
 */
module consulo.ui.ex.impl {
    requires transitive consulo.ui.ex.api;
    requires transitive consulo.ui.ex.awt.api;
    requires transitive consulo.application.impl;
    requires transitive consulo.project.ui.api;
    requires transitive consulo.language.api;
    requires transitive consulo.code.editor.api;
    requires consulo.ui.impl;
    requires consulo.util.nodep;
    requires gnu.trove;

    exports consulo.ui.ex.impl.internal to consulo.desktop.awt.ide.impl, consulo.desktop.awt.editor.impl, consulo.desktop.awt.ui.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl, consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;

    exports consulo.ui.ex.impl.internal.clipboard to consulo.desktop.ide.impl, consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;
    exports consulo.ui.ex.impl.internal.util to consulo.ide.impl;

    exports consulo.ui.ex.impl.internal.action to
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl, consulo.desktop.awt.editor.impl, consulo.desktop.awt.ui.impl,
        consulo.desktop.awt.os.mac,
        consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl,
        consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;

    exports consulo.ui.ex.impl.internal.popup.action to
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl, consulo.desktop.awt.editor.impl, consulo.desktop.awt.ui.impl;

    exports consulo.ui.ex.impl.internal.keymap to
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl, consulo.desktop.awt.editor.impl, consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl,
        consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;

    opens consulo.ui.ex.impl.internal.keymap to consulo.util.xml.serializer;
}
