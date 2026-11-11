import org.jspecify.annotations.NullMarked;

/**
 * The Agent Client Protocol for AI providers: the client of an agent, and a provider type to build one agent on.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@NullMarked
module consulo.ai.acp.api {
    requires transitive consulo.ai.api;
    requires transitive consulo.process.api;
    requires transitive com.google.gson;

    exports consulo.ai.acp;
    exports consulo.ai.acp.localize;
}
