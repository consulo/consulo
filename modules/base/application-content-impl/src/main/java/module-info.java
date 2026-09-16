/**
 * @author VISTALL
 * @since 2022-04-09
 */
@SuppressWarnings("module")
module consulo.application.content.impl {
    requires transitive consulo.application.content.api;
    requires transitive consulo.virtual.file.system.api;
    requires static consulo.component.impl;

    exports consulo.application.content.impl.internal to
        consulo.ide.impl,
        consulo.module.impl;
    exports consulo.application.content.impl.internal.bundle to consulo.ide.impl;
    exports consulo.application.content.impl.internal.library to
        consulo.ide.impl,
        consulo.module.impl,
        consulo.project.content.impl;
}