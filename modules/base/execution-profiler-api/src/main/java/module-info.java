import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@NullMarked
module consulo.execution.profiler.api {
    requires transitive consulo.execution.api;
    requires transitive consulo.configurable.api;

    exports consulo.execution.profiler;
    exports consulo.execution.profiler.configuration;
    exports consulo.execution.profiler.live;
    exports consulo.execution.profiler.model;
    exports consulo.execution.profiler.ui;
    exports consulo.execution.profiler.view;
    exports consulo.execution.profiler.icon;

    opens consulo.execution.profiler.configuration to consulo.util.xml.serializer;
}
