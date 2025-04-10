/**
 * @author VISTALL
 * @since 2025-04-10
 */
module consulo.external.system.impl {
    // TODO remove in future
    requires java.desktop;

    requires consulo.external.system.api;
    requires consulo.version.control.system.api;
    requires consulo.compiler.api;

    requires java.rmi;
    requires consulo.util.rmi;

    requires consulo.external.system.rt;

    requires it.unimi.dsi.fastutil;

    exports consulo.externalSystem.impl.internal.service.action to consulo.component.impl;
}