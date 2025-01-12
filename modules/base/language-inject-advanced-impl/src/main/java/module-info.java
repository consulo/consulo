/**
 * @author VISTALL
 * @since 2025-01-12
 */
module consulo.language.inject.advanced.impl {
    requires consulo.language.inject.advanced.api;
    requires consulo.language.inject.impl;

    // TODO remove in future
    requires consulo.ui.ex.awt.api;
    requires java.desktop;
}