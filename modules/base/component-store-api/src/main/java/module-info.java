/**
 * @author VISTALL
 * @since 2025-01-31
 */
@SuppressWarnings("module")
module consulo.component.store.api {
    requires consulo.annotation;
    requires consulo.ui.api;
    requires consulo.component.api;
    requires consulo.application.api;

    exports consulo.component.store.internal to
        consulo.application.impl,
        consulo.component.store.impl,
        consulo.external.service.impl,
        consulo.ide.impl,
        consulo.it,
        consulo.language.index.impl,
        consulo.module.impl,
        consulo.project.impl;
}