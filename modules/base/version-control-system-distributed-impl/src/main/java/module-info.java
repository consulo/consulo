/**
 * @author VISTALL
 * @since 2025-08-14
 */
module consulo.version.control.system.distributed.impl {
    requires transitive consulo.version.control.system.distributed.api;

    requires consulo.language.editor.ui.api;

    opens consulo.versionControlSystem.distributed.impl.internal.push to consulo.util.xml.serializer;

    // TODO remove
    requires miglayout;
}