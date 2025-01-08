/**
 * @author VISTALL
 * @since 2025-01-08
 */
module consulo.language.editor.refactoring.impl {
    requires consulo.language.editor.refactoring.api;

    opens consulo.language.editor.refactoring.impl.internal.action to consulo.component.impl;
}