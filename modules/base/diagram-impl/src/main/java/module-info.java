/**
 * @author VISTALL
 * @since 2025-09-02
 */
module consulo.diagram.impl {
    requires consulo.diagram.api;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.project.api;
    requires consulo.file.editor.api;

    opens consulo.diagram.impl.internal.action to consulo.component.impl;
}