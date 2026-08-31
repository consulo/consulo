import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@NullMarked
module consulo.mcp.api {
    requires transitive consulo.localize.api;
    requires transitive com.google.gson;

    exports consulo.mcp.protocol;
    exports consulo.mcp.protocol.model;
    exports consulo.mcp.tool;

    opens consulo.mcp.protocol to com.google.gson;
    opens consulo.mcp.protocol.model to com.google.gson;
}
