import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-07-21
 */
@NullMarked
module consulo.external.service.api {
    requires transitive consulo.application.api;
    requires transitive consulo.project.api;
    requires consulo.configurable.api;

    exports consulo.externalService;
    exports consulo.externalService.update;
    exports consulo.externalService.statistic;
    exports consulo.externalService.localize;
    exports consulo.externalService.plugin;
    exports consulo.externalService.pluginAdvertiser;

    exports consulo.externalService.internal to
        consulo.external.service.impl,
        consulo.ide.impl;
}