/**
 * @author VISTALL
 * @since 16/01/2022
 */
module consulo.application.api {
    // TODO [VISTALL] remove this dependency when we will ready. obsolete dep
    requires java.desktop;
    requires java.management;

    requires transitive consulo.component.api;
    requires transitive consulo.localize.api;
    requires transitive consulo.base.icon.library;
    requires consulo.proxy;

    requires transitive consulo.util.concurrent;
    requires transitive consulo.util.collection;
    requires transitive consulo.util.interner;

    requires transitive jakarta.inject;

    requires static consulo.util.jna;
    requires static com.sun.jna;
    requires static com.sun.jna.platform;

    exports consulo.application;
    exports consulo.application.concurrent;
    exports consulo.application.dumb;
    exports consulo.application.eap;
    exports consulo.application.event;
    exports consulo.application.constraint;
    exports consulo.application.progress;
    exports consulo.application.macro;
    exports consulo.application.presentation;
    exports consulo.application.json;
    exports consulo.application.localize;
    exports consulo.application.plugin;
    exports consulo.application.io;

    exports consulo.application.internal.start to
        consulo.ide.impl,
        consulo.external.service.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.swt.ide.impl;

    exports consulo.application.internal to
        consulo.ide.impl,
        consulo.desktop.ide.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.swt.ide.impl,
        consulo.document.impl,
        consulo.language.impl,
        consulo.application.impl,
        consulo.test.impl,
        consulo.ui.ex.api,
        consulo.project.impl,
        consulo.language.editor.refactoring.api,
        consulo.http.api,
        consulo.builtin.web.server.impl,
        consulo.version.control.system.distributed.api,
        consulo.compiler.impl,
        consulo.logging.logback.impl,
        consulo.virtual.file.system.impl,
        consulo.version.control.system.impl,
        consulo.execution.impl,
        consulo.diff.impl,
        consulo.external.service.impl;

    exports consulo.application.internal.plugin to
        consulo.application.impl,
        consulo.external.service.impl,
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.swt.ide.impl;

    exports consulo.application.internal.util to
        consulo.language.api,
        consulo.language.impl,
        consulo.language.inject.impl,
        consulo.test.impl,
        consulo.application.impl;

    exports consulo.application.internal.perfomance to
        consulo.project.impl,
        consulo.desktop.awt.ide.impl;

    exports consulo.application.util.mac.foundation;

    exports consulo.application.internal.dateTime to consulo.desktop.awt.ide.impl;

    exports consulo.application.util;
    exports consulo.application.util.function;
    exports consulo.application.util.concurrent;
    exports consulo.application.util.registry;
    exports consulo.application.util.diff;
    exports consulo.application.util.query;
    exports consulo.application.util.matcher;
    exports consulo.application.util.graph;

    opens consulo.application.util.mac.foundation to com.sun.jna;
}