/**
 * @author VISTALL
 * @since 2020-10-24
 */
import org.jspecify.annotations.NullMarked;

@NullMarked
module consulo.ui.api {
    requires consulo.util.lang;
    requires consulo.util.collection;
    requires consulo.util.concurrent;
    requires consulo.util.concurrent.coroutine;
    requires consulo.util.dataholder;
    requires consulo.localize.api;
    requires consulo.disposer.api;

    requires consulo.container.api;

    exports consulo.ui;
    exports consulo.ui.annotation;
    exports consulo.ui.clipboard;
    exports consulo.ui.color;
    exports consulo.ui.cursor;
    exports consulo.ui.event;
    exports consulo.ui.event.details;
    exports consulo.ui.font;
    exports consulo.ui.image;
    exports consulo.ui.image.canvas;
    exports consulo.ui.layout;
    exports consulo.ui.layout.event;
    exports consulo.ui.model;
    exports consulo.ui.style;
    exports consulo.ui.util;

    exports consulo.ui.image.internal;

    exports consulo.ui.internal to
        consulo.ui.ex.api, consulo.ui.impl,
        consulo.desktop.awt.ide.impl, consulo.desktop.awt.editor.impl, consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl,
        consulo.color.scheme.ui.api,
        consulo.ide.impl,
        consulo.test.impl,
        consulo.it,
        consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;

    uses consulo.ui.image.IconLibraryDescriptor;
    uses consulo.ui.internal.UIInternal;
}