/**
 * @author VISTALL
 * @since 28-Mar-22
 */
module consulo.virtual.file.status.api {
    requires transitive consulo.project.api;
    requires transitive consulo.color.scheme.api;
    requires transitive consulo.document.api;

    exports consulo.virtualFileSystem.status;
    exports consulo.virtualFileSystem.status.localize;

    exports consulo.virtualFileSystem.status.internal to consulo.virtual.file.status.impl, consulo.ide.impl;
}