import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-04-10
 */
@NullMarked
module consulo.path.macro.api {
    requires transitive consulo.application.api;
    requires transitive consulo.datacontext.api;
    requires transitive consulo.module.api;
    requires transitive consulo.localize.api;
    requires transitive consulo.project.api;
    requires transitive consulo.virtual.file.system.api;

    exports consulo.pathMacro;
    exports consulo.pathMacro.localize;
}