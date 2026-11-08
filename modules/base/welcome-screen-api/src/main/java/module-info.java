import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-11-08
 */
@NullMarked
module consulo.welcome.screen.api {
    requires transitive consulo.ui.ex.api;

    exports consulo.welcomeScreen;
}
