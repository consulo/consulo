import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-08-04
 */
@NullMarked
module consulo.ai.api {
    requires transitive consulo.project.api;
    requires transitive consulo.credential.storage.api;
    requires transitive consulo.ui.api;

    exports consulo.ai;
}
