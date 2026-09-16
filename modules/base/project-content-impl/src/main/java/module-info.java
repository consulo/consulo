import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-09-16
 */
@NullMarked
@SuppressWarnings("module")
module consulo.project.content.impl {
    requires transitive consulo.project.content.api;

    requires consulo.application.content.impl;
    requires consulo.module.content.api;
    requires consulo.project.api;

    requires org.jdom;

    opens consulo.project.content.impl.internal.library to consulo.component.impl;
}
