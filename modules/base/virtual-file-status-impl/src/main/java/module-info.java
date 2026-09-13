/**
 * @author VISTALL
 * @since 2022-03-28
 */
@SuppressWarnings("module")
module consulo.virtual.file.status.impl {
    requires transitive consulo.virtual.file.status.api;
    requires transitive consulo.code.editor.api;

    exports consulo.virtualFileSystem.status.impl.internal to consulo.ide.impl;
}