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

    exports consulo.ui.ex.impl.internal to consulo.desktop.awt.ide.impl, consulo.web.ide;

    exports consulo.ui.ex.impl.internal.clipboard to consulo.desktop.ide.impl, consulo.web.ide;
    exports consulo.ui.ex.impl.internal.util to consulo.ide.impl;

    exports consulo.ui.ex.impl.internal.action to
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.os.mac,
        consulo.desktop.swt.ide.impl,
        consulo.web.ide;

    exports consulo.ui.ex.impl.internal.popup.action to
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl;

    exports consulo.ui.ex.impl.internal.keymap to
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.swt.ide.impl,
        consulo.web.ide;

    opens consulo.ui.ex.impl.internal.keymap to consulo.util.xml.serializer;
}
