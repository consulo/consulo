/**
 * @author VISTALL
 * @since 2025-01-31
 */
module consulo.component.store.api {
    requires consulo.annotation;
    requires consulo.ui.api;
    requires consulo.component.api;
    requires consulo.application.api;

    exports consulo.component.store.internal to
        consulo.component.store.impl,
        consulo.application.impl,
        consulo.project.impl,
        consulo.module.impl,
        consulo.external.service.impl,
        consulo.ide.impl;
}