import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-02-03
 */
@NullMarked
module consulo.navigation.api {
    requires transitive consulo.application.api;
    requires transitive consulo.virtual.file.system.api;

    exports consulo.navigation;

    exports consulo.navigation.internal to
        consulo.ui.ex.api,
        consulo.language.api;
}