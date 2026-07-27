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
    requires consulo.util.nodep;
    requires gnu.trove;

    exports consulo.ui.ex.impl.internal.action to
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.os.mac,
        consulo.desktop.swt.ide.impl;

    exports consulo.ui.ex.impl.internal.keymap to
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.swt.ide.impl;

    opens consulo.ui.ex.impl.internal.keymap to consulo.util.xml.serializer;
}
