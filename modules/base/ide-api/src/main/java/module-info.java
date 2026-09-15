/**
 * @author VISTALL
 * @since 2022-02-19
 */
@SuppressWarnings("module")
module consulo.ide.api {
    // TODO obsolete dep
    requires java.desktop;

    requires consulo.code.editor.api;
    requires consulo.compiler.artifact.api;
    requires consulo.external.service.api;
    requires consulo.file.template.api;
    requires consulo.http.api;
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.language.editor.refactoring.api;
    requires consulo.local.history.api;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.virtual.file.system.http.api;

    exports consulo.ide;
    exports consulo.ide.action;
    exports consulo.ide.action.ui;
    exports consulo.ide.localize;
    exports consulo.ide.navigation;
    exports consulo.ide.navigationToolbar;
    exports consulo.ide.runAnything;
    exports consulo.ide.setting;
    exports consulo.ide.setting.bundle;
    exports consulo.ide.setting.module;
    exports consulo.ide.setting.module.event;
    exports consulo.ide.tipOfDay;
    exports consulo.ide.ui;
    exports consulo.ide.ui.popup;
    exports consulo.ide.util;

    exports consulo.ide.internal to
        consulo.ide.impl,
        consulo.sand.language.plugin;
}
