/**
 * @author VISTALL
 * @since 2025-08-30
 */
module consulo.version.control.system.log.impl {
    requires consulo.version.control.system.log.api;
    requires consulo.external.service.api;
    requires consulo.file.chooser.api;
    requires consulo.index.io;
    requires consulo.language.editor.api;
    requires consulo.language.editor.ui.api;

    requires com.google.common;

    // TODO remove in future
    requires gnu.trove;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;
    requires miglayout;

    opens consulo.versionControlSystem.log.impl.internal.data to consulo.util.xml.serializer;
    opens consulo.versionControlSystem.log.impl.internal.ui.action to consulo.component.impl;
}