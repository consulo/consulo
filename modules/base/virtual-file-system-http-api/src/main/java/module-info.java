/**
 * @author VISTALL
 * @since 2026-02-07
 */
module consulo.virtual.file.system.http.api {
    requires consulo.virtual.file.system.api;
    requires consulo.http.api;

    exports consulo.virtualFileSystem.http;
    exports consulo.virtualFileSystem.http.event;

    opens consulo.virtualFileSystem.http.event to consulo.proxy;
}