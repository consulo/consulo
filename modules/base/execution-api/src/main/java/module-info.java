/**
 * @author VISTALL
 * @since 05-Feb-22
 */
module consulo.execution.api {
    // TODO obsolete dependency
    requires java.desktop;
    requires forms.rt;

    requires transitive consulo.module.api;
    requires transitive consulo.module.content.api;
    requires transitive consulo.ui.ex.api;
    requires transitive consulo.ui.ex.awt.api;
    requires transitive consulo.process.api;
    requires transitive consulo.project.ui.api;
    requires transitive consulo.project.ui.view.api;
    requires transitive consulo.code.editor.api;
    requires transitive consulo.file.editor.api;
    requires transitive consulo.language.api;
    requires transitive consulo.path.macro.api;
    requires consulo.web.browser.api;

    requires consulo.external.service.api;

    requires static com.google.common;

    exports consulo.execution;
    exports consulo.execution.action;
    exports consulo.execution.configuration;
    exports consulo.execution.configuration.log;
    exports consulo.execution.configuration.log.ui;
    exports consulo.execution.configuration.ui;
    exports consulo.execution.configuration.ui.event;
    exports consulo.execution.dashboard;
    exports consulo.execution.service;
    exports consulo.execution.event;
    exports consulo.execution.executor;
    exports consulo.execution.lineMarker;
    exports consulo.execution.runner;
    exports consulo.execution.localize;
    exports consulo.execution.icon;
    exports consulo.execution.process;
    exports consulo.execution.ui;
    exports consulo.execution.ui.awt;
    exports consulo.execution.ui.console;
    exports consulo.execution.ui.console.language;
    exports consulo.execution.ui.event;
    exports consulo.execution.ui.layout;
    exports consulo.execution.unscramble;
    exports consulo.execution.util;
    exports consulo.execution.terminal;
    exports consulo.execution.ui.terminal;

    exports consulo.execution.internal.action to
        consulo.execution.impl,
        consulo.execution.debug.impl,
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl;

    exports consulo.execution.internal to consulo.ide.impl,
        consulo.execution.impl,
        consulo.execution.debug.impl,
        consulo.execution.test.sm.api,
        consulo.desktop.awt.ide.impl,
        consulo.execution.test.api,
        consulo.execution.debug.api;

    exports consulo.execution.internal.layout to consulo.ide.impl,
        consulo.execution.impl,
        consulo.execution.debug.impl,
        consulo.execution.test.sm.api,
        consulo.desktop.awt.ide.impl,
        consulo.execution.test.api;
}