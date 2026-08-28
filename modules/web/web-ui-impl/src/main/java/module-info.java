/**
 * The web ui layer - the vaadin component delegates, images and the platform ui services behind them.
 * Sits below the editor so both it and the ide layer can build on the same widgets.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
module consulo.web.ui.impl {
    requires vaadin.text.field.flow;
    requires vaadin.virtual.list.flow;
    requires consulo.file.editor.api;
    requires consulo.proxy;
    requires colorpicker;
    requires html.table;
    requires java.desktop;
    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.application.impl;
    requires consulo.application.ui.api;
    requires consulo.application.ui.impl;
    requires consulo.base.localize.library;
    requires consulo.color.scheme.api;
    requires consulo.component.api;
    requires consulo.component.store.impl;
    requires consulo.datacontext.api;
    requires consulo.disposer.api;
    requires consulo.localize.api;
    requires consulo.logging.api;
    requires consulo.project.api;
    requires consulo.project.ui.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.ui.ex.impl;
    requires consulo.ui.impl;
    requires consulo.util.collection;
    requires consulo.util.dataholder;
    requires consulo.util.io;
    requires consulo.util.lang;
    requires flow.data;
    requires flow.html.components;
    requires flow.server;
    requires vaadin.button.flow;
    requires vaadin.checkbox.flow;
    requires vaadin.context.menu.flow;
    requires vaadin.details.flow;
    requires vaadin.dialog.flow;
    requires togglebutton;
    requires vaadin.flow.components.base;
    requires vaadin.grid.flow;
    requires vaadin.icons.flow;
    requires vaadin.list.box.flow;
    requires vaadin.menu.bar.flow;
    requires vaadin.ordered.layout.flow;
    requires vaadin.popover.flow;
    requires vaadin.progress.bar.flow;
    requires vaadin.renderer.flow;
    requires vaadin.select.flow;
    requires vaadin.slider.flow;
    requires vaadin.date.picker.flow;
    requires vaadin.split.layout.flow;
    requires vaadin.tabs.flow;
    requires jakarta.inject;
    requires com.github.weisj.jsvg;
    requires pngj;
    requires tools.jackson.databind;

    exports consulo.web.ui.impl.internal to
        consulo.web.ide, consulo.web.editor.impl;
    exports consulo.web.ui.impl.internal.action to
        consulo.web.ide, consulo.web.editor.impl;
    exports consulo.web.ui.impl.internal.base to
        consulo.web.ide, consulo.web.editor.impl;
    exports consulo.web.ui.impl.internal.clipboard to
        consulo.web.ide, consulo.web.editor.impl;
    exports consulo.web.ui.impl.internal.htmlView to
        consulo.web.ide, consulo.web.editor.impl;
    exports consulo.web.ui.impl.internal.image to
        consulo.web.ide, consulo.web.editor.impl;
    exports consulo.web.ui.impl.internal.vaadin to
        consulo.web.ide, consulo.web.editor.impl;
    exports consulo.web.ui.impl.internal.vaadin.carousel to
        consulo.web.ide, consulo.web.editor.impl;

    provides consulo.ui.internal.UIInternal with consulo.web.ui.impl.internal.WebUIInternalImpl;
    provides consulo.ui.ex.awtUnsafe.internal.TargetAWTFacade with consulo.web.ui.impl.internal.TargetAWTFacadeStub;
}
