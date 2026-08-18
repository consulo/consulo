/**
 * The awt ui layer - the swing component delegates, the facade, the look and feel and the images behind them.
 * Sits below the editor so both it and the ide layer can build on the same widgets.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
open module consulo.desktop.awt.ui.impl {
    requires consulo.application.api;
    requires consulo.application.ui.api;
    requires consulo.code.editor.api;
    requires consulo.color.scheme.api;
    requires consulo.component.api;
    requires consulo.component.store.impl;
    requires consulo.datacontext.api;
    requires consulo.disposer.api;
    requires consulo.language.api;
    requires consulo.localize.api;
    requires consulo.logging.api;
    requires consulo.platform.api;
    requires consulo.project.api;
    requires consulo.project.ui.api;
    requires consulo.proxy;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.util.collection;
    requires consulo.util.collection.primitive;
    requires consulo.util.concurrent;
    requires consulo.util.dataholder;
    requires consulo.util.io;
    requires consulo.util.lang;
    requires consulo.virtual.file.system.api;
    requires consulo.ide.api;
    requires consulo.language.editor.api;
    requires consulo.language.editor.ui.api;
    requires consulo.virtual.file.status.api;
    requires imgscalr.lib;
    requires com.sun.jna;
    requires consulo.base.icon.library;
    requires consulo.base.localize.library;
    requires kava.beans;
    requires consulo.annotation;
    requires java.desktop;
    requires miglayout;
    requires com.google.common;
    requires com.github.weisj.jsvg;

    requires jdk.xml.dom;
    requires cobra.core;

    requires consulo.container.api;

    requires consulo.ui.ex.awt.api;
    requires consulo.ui.ex.impl;
    requires consulo.ui.impl;

    requires consulo.application.impl;

    requires consulo.external.service.api;
    requires consulo.web.browser.api;

    requires com.formdev.flatlaf;
    requires swingx.all;

    exports consulo.desktop.awt.ui.impl.action;
    exports consulo.desktop.awt.ui.impl.action.menu;
    exports consulo.desktop.awt.ui.impl.action.toolbar;
    exports consulo.desktop.awt.ui.impl.facade;
    exports consulo.desktop.awt.ui.impl.animation;
    exports consulo.desktop.awt.ui.impl.clipboard;
    exports consulo.desktop.awt.ui.impl;
    exports consulo.desktop.awt.ui.impl.alert;
    exports consulo.desktop.awt.ui.impl.base;
    exports consulo.desktop.awt.ui.impl.components;
    exports consulo.desktop.awt.ui.impl.components.fields;
    exports consulo.desktop.awt.ui.impl.event;
    exports consulo.desktop.awt.ui.impl.htmlView;
    exports consulo.desktop.awt.ui.impl.image;
    exports consulo.desktop.awt.ui.impl.image.canvas;
    exports consulo.desktop.awt.ui.impl.image.reference;
    exports consulo.desktop.awt.ui.impl.layout;
    exports consulo.desktop.awt.ui.impl.progressBar;
    exports consulo.desktop.awt.ui.impl.style;
    exports consulo.desktop.awt.ui.impl.taskBar;
    exports consulo.desktop.awt.ui.impl.textBox;
    exports consulo.desktop.awt.ui.impl.util;
    exports consulo.desktop.awt.ui.impl.tabs;
    exports consulo.desktop.awt.ui.impl.tabs.laf;
    exports consulo.desktop.awt.ui.impl.tabs.singleRow;
    exports consulo.desktop.awt.ui.impl.tabs.table;
    exports consulo.desktop.awt.ui.impl.validableComponent;
    exports consulo.desktop.awt.ui.impl.window;
    exports consulo.desktop.awt.ui.impl.plaf;
    exports consulo.desktop.awt.ui.impl.plaf.extend.textBox;
    exports consulo.desktop.awt.ui.impl.plaf2;
    exports consulo.desktop.awt.ui.impl.plaf2.flat;
    exports consulo.desktop.awt.ui.impl.plaf2.flat.kde;

    exports com.mxgraph.analysis;
    exports com.mxgraph.canvas;
    exports com.mxgraph.costfunction;
    exports com.mxgraph.generatorfunction;
    exports com.mxgraph.io;
    exports com.mxgraph.io.graphml;
    exports com.mxgraph.layout;
    exports com.mxgraph.layout.hierarchical;
    exports com.mxgraph.layout.hierarchical.model;
    exports com.mxgraph.layout.hierarchical.stage;
    exports com.mxgraph.layout.orthogonal;
    exports com.mxgraph.layout.orthogonal.model;
    exports com.mxgraph.model;
    exports com.mxgraph.reader;
    exports com.mxgraph.shape;
    exports com.mxgraph.sharing;
    exports com.mxgraph.swing;
    exports com.mxgraph.swing.handler;
    exports com.mxgraph.swing.util;
    exports com.mxgraph.swing.view;
    exports com.mxgraph.util;
    exports com.mxgraph.util.png;
    exports com.mxgraph.util.svg;
    exports com.mxgraph.view;

    provides consulo.ui.internal.UIInternal with consulo.desktop.awt.ui.impl.DesktopUIInternalImpl;
    provides consulo.ui.ex.awtUnsafe.internal.TargetAWTFacade with consulo.desktop.awt.ui.impl.facade.DesktopAWTTargetAWTImpl;
    provides com.formdev.flatlaf.FlatDefaultsAddon with consulo.desktop.awt.ui.impl.plaf2.flat.ConsuloFlatDefaultsAddon;
}
