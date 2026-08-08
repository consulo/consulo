import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@NullMarked
module consulo.mcp.server.impl {
    requires transitive consulo.mcp.server.api;
    requires consulo.builtin.web.server.api;
    requires consulo.ai.api;
    requires consulo.configurable.api;
    requires consulo.component.store.api;
    requires consulo.ui.api;

    exports consulo.mcpServer.impl.internal to consulo.it;
    exports consulo.mcpServer.impl.internal.setting to consulo.it;

    opens consulo.mcpServer.impl.internal.setting to consulo.util.xml.serializer;
}
