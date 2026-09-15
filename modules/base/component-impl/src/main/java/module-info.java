import consulo.component.impl.internal.inject.DefaultRootInjectingContainerFactory;

/**
 * @author VISTALL
 * @since 2022-02-20
 */
@SuppressWarnings("module")
module consulo.component.impl {
    requires transitive consulo.component.api;
    requires transitive consulo.container.api;
    requires transitive consulo.proxy;
    requires transitive consulo.virtual.file.system.api;
    requires transitive jakarta.inject;

    requires consulo.util.nodep;

    exports consulo.component.impl.internal to
        consulo.application.impl,
        consulo.component.store.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.ide.impl,
        consulo.it,
        consulo.module.impl,
        consulo.project.impl,
        consulo.test.impl;

    exports consulo.component.impl.internal.messagebus to
        consulo.ide.impl,
        consulo.test.impl;
    exports consulo.component.impl.internal.macro to
        consulo.application.impl,
        consulo.component.store.impl,
        consulo.ide.impl,
        consulo.module.impl,
        consulo.project.impl;

    opens consulo.component.impl.internal to consulo.util.xml.serializer;

    provides consulo.component.internal.inject.RootInjectingContainerFactory with DefaultRootInjectingContainerFactory;
}