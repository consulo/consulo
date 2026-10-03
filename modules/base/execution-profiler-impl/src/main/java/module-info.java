import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@NullMarked
module consulo.execution.profiler.impl {
    requires transitive consulo.execution.profiler.api;

    opens consulo.execution.profiler.impl.internal.action to consulo.component.impl;
    opens consulo.execution.profiler.impl.internal.setting to consulo.util.xml.serializer;
}
