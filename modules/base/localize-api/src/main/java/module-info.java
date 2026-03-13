/**
 * @author VISTALL
 * @since 2020-10-19
 */
import org.jspecify.annotations.NullMarked;

@NullMarked
module consulo.localize.api {
    requires transitive consulo.annotation;
    requires transitive consulo.disposer.api;

    exports consulo.localize;

    uses consulo.localize.LocalizeManager;

    exports consulo.localize.internal to
        consulo.application.impl,
        consulo.localize.impl;
}