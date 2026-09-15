import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings("module")
module consulo.util.collection {
    requires transitive consulo.annotation;
    requires transitive consulo.util.lang;

    requires org.slf4j;
    requires it.unimi.dsi.fastutil;

    exports consulo.util.collection;
    exports consulo.util.collection.util;

    exports consulo.util.collection.impl.map to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl,
        consulo.index.io,
        consulo.language.impl,
        consulo.util.collection.primitive,
        consulo.virtual.file.system.impl;

    exports consulo.util.collection.impl.set to consulo.util.collection.primitive;
    exports consulo.util.collection.impl to consulo.util.collection.primitive;
}