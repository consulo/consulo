import org.jspecify.annotations.NullMarked;

/**
 * Data grids over documents: the data source which parses a document into rows and writes the edits of the grid back as document
 * changes, and the file editors which show a file as a grid.
 *
 * @since 2026-10-04
 */
@NullMarked
@SuppressWarnings("module")
module consulo.grid.editor.api {
    requires transitive consulo.file.editor.api;
    requires transitive consulo.ui.ex.api;
    requires transitive consulo.document.api;
    requires transitive consulo.undo.redo.api;
    requires transitive consulo.project.api;

    exports consulo.grid.editor;
}
