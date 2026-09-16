import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-01-21
 */
@NullMarked
@SuppressWarnings("module")
module consulo.module.content.api {
    requires transitive consulo.application.content.api;
    requires transitive consulo.project.content.api;
    requires transitive consulo.module.api;

    requires static consulo.index.io;

    requires static consulo.ui.ex.api;

    exports consulo.module.content;
    exports consulo.module.content.scope;
    exports consulo.module.content.util;
    exports consulo.module.content.library;
    exports consulo.module.content.library.util;
    exports consulo.module.content.localize;
    exports consulo.module.content.layer;
    exports consulo.module.content.layer.extension;
    exports consulo.module.content.layer.event;
    exports consulo.module.content.layer.orderEntry;

    exports consulo.module.content.internal to
        consulo.compiler.artifact.impl,
        consulo.compiler.impl,
        consulo.external.system.api,
        consulo.external.system.impl,
        consulo.it,
        consulo.ide.api,
        consulo.ide.impl,
        consulo.language.api,
        consulo.language.editor.impl,
        consulo.language.impl,
        consulo.language.index.impl,
        consulo.module.impl,
        consulo.module.ui.api,
        consulo.module.content.impl,
        consulo.project.content.impl;
}