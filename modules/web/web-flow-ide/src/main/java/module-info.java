/**
 * @author VISTALL
 * @since 2026-08-15
 */
module consulo.web.ide {
    // TODO [VISTALL] obsolete requires
    requires java.desktop;

    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.application.impl;
    requires consulo.application.ui.api;
    requires consulo.application.ui.impl;
    requires consulo.base.icon.library;
    requires consulo.base.localize.library;
    requires consulo.code.editor.api;
    requires consulo.code.editor.impl;
    requires consulo.color.scheme.api;
    requires consulo.component.api;
    requires consulo.component.store.impl;
    requires consulo.container.api;
    requires consulo.datacontext.api;
    requires consulo.diff.api;
    requires consulo.disposer.api;
    requires consulo.document.api;
    requires consulo.execution.api;
    requires consulo.execution.coverage.api;
    requires consulo.file.editor.api;
    requires consulo.ide.impl;
    requires consulo.web.editor.impl;
    requires consulo.web.ui.impl;
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.language.editor.impl;
    requires consulo.language.editor.refactoring.api;
    requires consulo.localize.api;
    requires consulo.logging.api;
    requires consulo.navigation.api;
    requires consulo.navigation.bar.api;
    requires consulo.navigation.bar.impl;
    requires consulo.platform.api;
    requires consulo.platform.impl;
    requires consulo.project.api;
    requires consulo.project.ui.api;
    requires consulo.project.ui.impl;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.ui.ex.impl;
    requires consulo.ui.impl;
    requires consulo.undo.redo.api;
    requires consulo.util.collection;
    requires consulo.util.concurrent;
    requires consulo.util.dataholder;
    requires consulo.util.io;
    requires consulo.util.lang;
    requires consulo.version.control.system.api;
    requires consulo.virtual.file.system.api;

    requires flow.data;
    requires flow.html.components;
    requires flow.server;
    requires vaadin.accordion.flow;
    requires vaadin.aura.theme;
    requires vaadin.breadcrumbs.flow;
    requires vaadin.button.flow;
    requires vaadin.checkbox.flow;
    requires vaadin.combo.box.flow;
    requires vaadin.context.menu.flow;
    requires vaadin.dev.server;
    requires vaadin.details.flow;
    requires vaadin.date.picker.flow;
    requires vaadin.slider.flow;
    requires togglebutton;
    requires vaadin.dialog.flow;
    requires vaadin.flow.components.base;
    requires vaadin.grid.flow;
    requires vaadin.icons.flow;
    requires vaadin.list.box.flow;
    requires vaadin.menu.bar.flow;
    requires vaadin.ordered.layout.flow;
    requires vaadin.popover.flow;
    requires vaadin.progress.bar.flow;
    requires vaadin.radio.button.flow;
    requires vaadin.renderer.flow;
    requires vaadin.select.flow;
    requires vaadin.split.layout.flow;
    requires vaadin.tabs.flow;
    requires vaadin.text.field.flow;
    requires vaadin.virtual.list.flow;
    requires html.table;
    requires colorpicker;
    requires vaadin.custom.field.flow;

    requires jakarta.inject;
    requires jakarta.servlet;
    requires org.eclipse.jetty.ee11.servlet;
    requires org.eclipse.jetty.ee11.websocket.jakarta.server;
    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.util;

    requires com.github.weisj.jsvg;
    requires jediterm.core;
    requires org.jdom;
    requires org.slf4j;
    requires pngj;
    requires tools.jackson.databind;

    exports consulo.web.internal.servlet to flow.server;
    exports consulo.web.internal.startup to flow.server;

    opens consulo.web.internal.wm to consulo.util.xml.serializer;
    opens consulo.web.internal.servlet to org.eclipse.jetty.ee11.servlet, flow.server;
    opens consulo.web.internal.startup to org.eclipse.jetty.ee11.servlet, flow.server;

    provides consulo.container.boot.ContainerStartup with consulo.web.internal.startup.WebContainerStartup;
    provides consulo.platform.internal.PlatformInternal with consulo.web.internal.platform.WebPlatformInternalImpl;
}
