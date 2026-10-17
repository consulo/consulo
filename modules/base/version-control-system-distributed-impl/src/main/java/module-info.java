/**
 * @author VISTALL
 * @since 2025-08-14
 */
module consulo.version.control.system.distributed.impl {
    requires consulo.mcp.server.api;
    requires transitive consulo.version.control.system.distributed.api;

    requires consulo.language.editor.ui.api;
    requires consulo.ui.ex.impl;

    opens consulo.versionControlSystem.distributed.impl.internal.push to consulo.util.xml.serializer;
}