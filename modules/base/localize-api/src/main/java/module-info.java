/**
 * @author VISTALL
 * @since 2020-10-19
 */
module consulo.localize.api {
    requires transitive consulo.annotation;
    requires transitive consulo.disposer.api;

    requires consulo.container.api;

    exports consulo.localization;
    exports consulo.localize;

    uses consulo.localize.LocalizeManager;

    exports consulo.localization.internal to
        consulo.application.impl,
        consulo.localize.impl;
    exports consulo.localize.internal to
        consulo.application.impl,
        consulo.localize.impl;
}