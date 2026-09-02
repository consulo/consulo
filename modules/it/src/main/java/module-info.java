/**
 * @author VISTALL
 * @since 2026-07-19
 */
module consulo.it {
    requires consulo.annotation;
    requires jakarta.inject;
    requires consulo.container.api;
    requires consulo.application.api;
    requires consulo.application.impl;
    requires consulo.application.ui.api;
    requires consulo.datacontext.api;
    requires consulo.component.api;
    requires consulo.component.impl;
    requires consulo.component.store.api;
    requires consulo.ai.api;
    requires consulo.credential.storage.impl;
    requires consulo.ai.impl;
    requires consulo.mcp.server.api;
    requires consulo.mcp.server.impl;
    requires consulo.project.api;
    requires consulo.project.impl;
    requires consulo.project.ui.api;
    requires consulo.project.ui.impl;
    requires consulo.module.api;
    requires consulo.module.content.api;
    requires consulo.document.api;
    requires consulo.code.editor.api;
    requires consulo.code.editor.impl;
    requires consulo.language.api;
    requires consulo.language.impl;
    requires consulo.language.editor.api;
    requires consulo.platform.api;
    requires consulo.platform.impl;
    requires consulo.localization.api;
    requires consulo.localize.api;
    requires consulo.ui.api;
    requires consulo.ui.impl;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.impl;
    requires consulo.util.collection;
    requires consulo.util.concurrent;
    requires consulo.util.jdom;
    requires consulo.util.lang;
    requires org.jdom;
    requires consulo.logging.api;
    requires consulo.disposer.api;

    requires transitive org.junit.jupiter.api;

    exports consulo.it;

    provides consulo.container.internal.PluginManagerInternal with consulo.it.internal.HeadlessPluginManager;
    provides consulo.logging.attachment.AttachmentFactory with consulo.it.internal.HeadlessAttachmentFactory;
    provides consulo.platform.internal.PlatformInternal with consulo.it.internal.HeadlessPlatformInternal;
    provides consulo.ui.internal.UIInternal with consulo.it.internal.HeadlessUIInternal;
}























































