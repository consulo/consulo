import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@NullMarked
module consulo.mcp.server.api {
    requires transitive consulo.mcp.api;
    requires transitive consulo.project.api;
    requires consulo.ui.api;

    exports consulo.mcpServer;
}
