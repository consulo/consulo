/**
 * @author VISTALL
 * @since 2025-01-16
 */
module consulo.module.content.impl {
    requires transitive consulo.module.content.api;

    exports consulo.module.content.impl.internal.scope to consulo.ide.impl;
}