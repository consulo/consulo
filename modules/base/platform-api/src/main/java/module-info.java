import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-01-13
 */
@NullMarked
@SuppressWarnings("module")
module consulo.platform.api {
    requires consulo.ui.api;
    requires consulo.annotation;
    requires consulo.container.api;
    requires consulo.util.lang;
    requires consulo.util.dataholder;

    uses consulo.platform.internal.PlatformInternal;

    exports consulo.platform;
    exports consulo.platform.os;
    exports consulo.platform.internal to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.it,
        consulo.test.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;
}