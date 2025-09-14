/**
 * @author VISTALL
 * @since 2025-09-14
 */
module consulo.language.editor.todo.impl {
    requires consulo.language.editor.refactoring.api;
    requires consulo.version.control.system.api;
    requires consulo.color.scheme.ui.api;
    requires consulo.project.ui.view.api;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;

    // TODO remove after migrate to binding actions, instead xml
    opens consulo.language.editor.todo.impl.internal.action to consulo.component.impl;
    opens consulo.language.editor.todo.impl.internal to consulo.component.impl, consulo.util.xml.serializer;
}