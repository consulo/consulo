import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
@NullMarked
module consulo.endpoint.impl {
    requires transitive consulo.endpoint.api;

    requires consulo.diagram.api;
    requires consulo.find.api;
    requires consulo.usage.api;
    requires consulo.language.editor.refactoring.api;
    requires consulo.language.uast.api;
    requires consulo.project.ui.api;
    requires consulo.web.browser.api;
    requires it.unimi.dsi.fastutil;

    opens consulo.endpoint.impl.internal.view to consulo.util.xml.serializer;
}
