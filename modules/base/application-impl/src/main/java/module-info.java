/**
 * @author VISTALL
 * @since 2022-03-23
 */
@SuppressWarnings("module")
module consulo.application.impl {
    requires transitive consulo.component.impl;
    requires transitive consulo.component.store.impl;
    requires transitive consulo.application.api;
    requires transitive consulo.document.api;
    requires transitive consulo.language.api;
    requires transitive consulo.logging.api;
    requires transitive consulo.localization.api;
    requires transitive consulo.localization.impl;
    requires transitive consulo.process.api;
    requires transitive consulo.project.api;
    requires consulo.util.nodep;
    requires consulo.ui.impl;
    requires consulo.ui.ex.api;
    requires consulo.container.api;

    requires consulo.util.jna;

    // TODO remove this dependency in future
    requires java.desktop;
    requires java.management;

    requires args4j;
    requires com.google.gson;
    requires org.slf4j;

    exports consulo.application.impl.internal to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.awt.os.mac,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.ide.impl,
        consulo.it,
        consulo.language.impl,
        consulo.language.index.impl,
        consulo.language.editor.api,
        consulo.language.editor.impl,
        consulo.logging.logback.impl,
        consulo.module.impl,
        consulo.project.impl,
        consulo.proxy,
        consulo.sand.language.plugin,
        consulo.test.impl,
        consulo.ui.ex.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;

    exports consulo.application.impl.internal.macro to
        consulo.ide.impl,
        consulo.module.impl,
        consulo.project.impl;

    exports consulo.application.impl.internal.progress to
        consulo.compiler.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.awt.os.mac,
        consulo.desktop.ide.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.ide.impl,
        consulo.language.editor.impl,
        consulo.project.impl,
        consulo.test.impl,
        consulo.virtual.file.system.impl,
        consulo.it;

    exports consulo.application.impl.internal.performance to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.awt.os.mac,
        consulo.ide.impl,
        consulo.language.index.impl,
        consulo.project.impl,
        consulo.proxy,
        consulo.ui.ex.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;

    exports consulo.application.impl.internal.plugin to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.ide.impl;

    exports consulo.application.impl.internal.start to
        consulo.builtin.web.server.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.ide.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.ide.impl,
        consulo.language.index.impl,
        consulo.logging.logback.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;

    exports consulo.application.impl.internal.store to
        consulo.ide.impl,
        consulo.it;
    exports consulo.application.impl.internal.util to
        consulo.ide.impl,
        consulo.it,
        consulo.language.impl;

    exports consulo.application.impl.internal.concurent to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl;

    opens consulo.application.impl.internal.start to args4j;

    provides consulo.index.io.internal.LowMemoryWatcherInternal with consulo.application.impl.internal.util.RealLowMemoryWatcherInternal;
    provides consulo.disposer.internal.DiposerRegisterChecker with consulo.application.impl.internal.DisposerPluginChecker;
}