import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2025-04-14
 */
@NullMarked
module consulo.graphql.api {
    requires transitive consulo.util.lang;

    exports consulo.graphql;
    exports consulo.graphql.dto;
}
