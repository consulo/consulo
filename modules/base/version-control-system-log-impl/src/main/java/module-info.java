/**
 * @author VISTALL
 * @since 2025-08-30
 */
@SuppressWarnings("module")
module consulo.version.control.system.log.impl {
    requires consulo.version.control.system.log.api;
    requires consulo.version.control.system.distributed.api;
    requires consulo.external.service.api;
    requires consulo.file.chooser.api;
    requires consulo.index.io;

    requires com.google.common;
    requires it.unimi.dsi.fastutil;

    // TODO remove in future
    requires gnu.trove;

    opens consulo.versionControlSystem.log.impl.internal.data to consulo.util.xml.serializer;
    opens consulo.versionControlSystem.log.impl.internal.ui.editor to consulo.util.xml.serializer;
    opens consulo.versionControlSystem.log.impl.internal.ui.action to consulo.component.impl;
}