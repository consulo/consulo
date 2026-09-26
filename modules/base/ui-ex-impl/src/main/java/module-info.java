/**
 * @author VISTALL
 * @since 2026-07-27
 */
@SuppressWarnings("module")
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
    requires com.sun.jna;

    exports consulo.ui.ex.impl.internal to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;

    exports consulo.ui.ex.impl.internal.clipboard to
        consulo.desktop.ide.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;
    exports consulo.ui.ex.impl.internal.util to consulo.ide.impl;

    exports consulo.ui.ex.impl.internal.action to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.awt.os.mac,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.ide.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;

    exports consulo.ui.ex.impl.internal.popup.action to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl;

    exports consulo.ui.ex.impl.internal.keymap to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.ide.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.it,
        consulo.web.ui.impl;

    exports consulo.ui.ex.impl.internal.animation to
        consulo.code.editor.impl,
        consulo.desktop.awt.ui.impl;

    opens consulo.ui.ex.impl.internal.keymap to consulo.util.xml.serializer;
    opens consulo.ui.ex.impl.internal.animation to com.sun.jna;
}
