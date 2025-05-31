/**
 * @author VISTALL
 * @since 2025-05-31
 */
module consulo.project.ui.view.impl {
    requires consulo.project.ui.view.api;

    opens consulo.project.ui.view.impl.internal.nesting to consulo.util.xml.serializer;
    
    // TODO
    requires java.desktop;
}