import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-11-08
 */
@NullMarked
module consulo.module.creation.impl {
    requires consulo.module.creation.api;
    requires consulo.project.api;
    requires consulo.base.localize.library;
    requires consulo.external.service.api;

    exports consulo.module.creation.impl.internal to
        consulo.ide.impl,
        consulo.welcome.screen.impl;

    exports consulo.module.creation.impl.localize to
        consulo.welcome.screen.impl;
}
