/**
 * @author VISTALL
 * @since 2022-01-14
 */
@SuppressWarnings("module")
module consulo.platform.impl {
    requires consulo.annotation;
    requires consulo.platform.api;
    requires consulo.util.lang;
    requires consulo.util.collection;
    requires consulo.util.dataholder;

    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.slf4j;

    opens consulo.platform.impl to com.sun.jna;

    exports consulo.platform.impl to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.ide.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.it,
        consulo.test.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;
}