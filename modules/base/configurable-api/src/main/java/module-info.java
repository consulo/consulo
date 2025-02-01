/**
 * @author VISTALL
 * @since 20/01/2022
 */
module consulo.configurable.api {
    // TODO [VISTALL] obsolete dep
    requires java.desktop;

    requires transitive consulo.project.api;
    requires transitive consulo.ui.api;
    requires transitive consulo.base.localize.library;

    requires static consulo.ui.ex.api;

    exports consulo.configurable;
    exports consulo.configurable.localize;
    exports consulo.configurable.internal to consulo.ide.impl,
        consulo.language.editor.api,
        consulo.language.code.style.ui.api,
        consulo.ide.api,
        consulo.task.impl,
        consulo.desktop.awt.ide.impl,
        consulo.ui.ex.awt.api,
        consulo.execution.debug.impl,
        consulo.remote.server.impl,
        consulo.execution.impl,
        consulo.web.browser.impl,
        consulo.configuration.editor.api,
        consulo.language.inject.advanced.impl,
        consulo.external.service.impl;
}