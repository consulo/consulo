/**
 * @author VISTALL
 * @since 2022-01-20
 */
module consulo.module.api {
    // TODO [VISTALL] remove in future. Used in SwingMutableModuleExtension
    requires static java.desktop;

    requires transitive consulo.project.api;
    requires transitive consulo.application.content.api;
    requires transitive org.jdom;

    exports consulo.module;
    exports consulo.module.macro;
    exports consulo.module.event;
    exports consulo.module.extension;
    exports consulo.module.extension.swing;
    exports consulo.module.extension.condition;
    exports consulo.module.localize;

    exports consulo.module.internal to
        consulo.module.impl,
        consulo.module.content.impl,
        consulo.language.index.impl,
        consulo.ide.impl;
}