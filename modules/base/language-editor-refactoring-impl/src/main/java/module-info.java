/**
 * @author VISTALL
 * @since 2025-01-08
 */
@SuppressWarnings("module")
module consulo.language.editor.refactoring.impl {
    requires consulo.mcp.server.api;
    requires consulo.language.editor.refactoring.api;

    opens consulo.language.editor.refactoring.impl.internal.action to consulo.component.impl;
}