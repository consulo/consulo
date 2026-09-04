/**
 * @author VISTALL
 * @since 13/01/2022
 */
module consulo.ui.impl {
    requires consulo.annotation;
    requires consulo.proxy;
    requires consulo.disposer.api;
    requires consulo.logging.api;
    requires consulo.container.api;
    requires consulo.ui.api;
    requires consulo.localize.api;
    requires com.google.protobuf;
    requires tools.jackson.core;
    requires com.dslplatform.json;
    requires consulo.component.api;
    requires consulo.application.api;

    requires consulo.base.localize.library;
    requires consulo.base.icon.library;

    requires consulo.util.collection;
    requires consulo.util.dataholder;
    requires consulo.util.lang;
    requires consulo.util.io;
    requires consulo.util.xml.serializer;

    opens consulo.ui.impl.style to consulo.util.xml.serializer;

    exports consulo.ui.impl to consulo.ide.impl, consulo.ui.ex.impl, consulo.test.impl, consulo.desktop.awt.ide.impl, consulo.desktop.awt.editor.impl, consulo.desktop.awt.ui.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl, consulo.it, consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;
    exports consulo.ui.impl.clipboard to consulo.ide.impl, consulo.desktop.awt.ide.impl, consulo.desktop.awt.editor.impl, consulo.desktop.awt.ui.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl, consulo.it, consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;
    exports consulo.ui.impl.font to consulo.ide.impl, consulo.desktop.awt.ide.impl, consulo.desktop.awt.editor.impl, consulo.desktop.awt.ui.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl, consulo.it, consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;
    exports consulo.ui.impl.image to consulo.application.impl, consulo.desktop.awt.ide.impl, consulo.desktop.awt.editor.impl, consulo.desktop.awt.ui.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl, consulo.it, consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;
    exports consulo.ui.impl.model to consulo.ide.impl, consulo.desktop.awt.ide.impl, consulo.desktop.awt.editor.impl, consulo.desktop.awt.ui.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl, consulo.it, consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;
    exports consulo.ui.impl.style to consulo.ide.impl, consulo.desktop.awt.ide.impl, consulo.desktop.awt.editor.impl, consulo.desktop.awt.ui.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl, consulo.it, consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;
}