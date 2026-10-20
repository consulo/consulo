import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-08-04
 */
@NullMarked
@SuppressWarnings("module")
module consulo.ai.impl {
    requires transitive consulo.ai.api;
    requires consulo.configurable.api;
    requires consulo.project.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.file.chooser.api;
    requires consulo.component.store.api;
    requires consulo.language.ignore.api;

    opens consulo.ai.impl.internal.setting to consulo.util.xml.serializer;
}
