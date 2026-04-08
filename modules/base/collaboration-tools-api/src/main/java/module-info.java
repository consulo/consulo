/**
 * @author UNV
 * @since 2026-03-07
 */
module consulo.collaboration.tools.api {
    // todo not required dependency
    requires java.desktop;

    requires transitive consulo.application.api;
    requires transitive consulo.code.editor.api;
    requires transitive consulo.diff.api;
    requires transitive consulo.document.api;
    requires transitive consulo.ide.api;
    requires transitive consulo.language.editor.ui.api;
    requires transitive consulo.project.api;
    requires transitive consulo.ui.ex.api;
    requires transitive consulo.ui.ex.awt.api;
    requires transitive consulo.version.control.system.api;
    requires transitive consulo.virtual.file.system.api;
}