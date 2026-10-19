/**
 * @author VISTALL
 * @since 2022-06-04
 */
@SuppressWarnings("module")
module consulo.diff.impl {
    requires transitive consulo.diff.api;
    requires transitive consulo.language.api;
    requires transitive consulo.language.editor.api;
    requires consulo.process.api;
    requires consulo.external.service.api;

    // TODO remove in futures
    requires java.desktop;

    exports consulo.diff.impl.internal to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl,
        consulo.version.control.system.impl;

    exports consulo.diff.impl.internal.action to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl,
        consulo.version.control.system.impl;

    exports consulo.diff.impl.internal.editor to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl;

    exports consulo.diff.impl.internal.fragment to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl;

    exports consulo.diff.impl.internal.dir to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl;

    exports consulo.diff.impl.internal.util to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl,
        consulo.version.control.system.impl;

    exports consulo.diff.impl.internal.viewer to
        consulo.desktop.awt.ide.impl,
        consulo.version.control.system.impl;

    exports consulo.diff.impl.internal.viewer.side to
        consulo.desktop.awt.ide.impl,
        consulo.version.control.system.impl;

    exports consulo.diff.impl.internal.viewer.simple to
        consulo.desktop.awt.ide.impl;

    exports consulo.diff.impl.internal.viewer.binary to
        consulo.desktop.awt.ide.impl;

    exports consulo.diff.impl.internal.merge to
        consulo.desktop.awt.ide.impl,
        consulo.version.control.system.impl;

    exports consulo.diff.impl.internal.external to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl;

    opens consulo.diff.impl.internal.action to consulo.component.impl;
    opens consulo.diff.impl.internal to consulo.util.xml.serializer;
    opens consulo.diff.impl.internal.external to consulo.util.xml.serializer;
}