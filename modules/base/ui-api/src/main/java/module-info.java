/**
 * @author VISTALL
 * @since 2020-10-24
 */
module consulo.ui.api {
    requires consulo.util.lang;
    requires consulo.util.collection;
    requires consulo.util.concurrent;
    requires consulo.util.dataholder;
    requires consulo.localize.api;
    requires consulo.disposer.api;

    requires consulo.container.api;

    exports consulo.ui;
    exports consulo.ui.annotation;
    exports consulo.ui.color;
    exports consulo.ui.cursor;
    exports consulo.ui.border;
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
        consulo.desktop.awt.ide.impl,
        consulo.desktop.swt.ide.impl,
        consulo.color.scheme.ui.api,
        consulo.ide.impl,
        consulo.test.impl;

    uses consulo.ui.image.IconLibraryDescriptor;
    uses consulo.ui.internal.UIInternal;
}