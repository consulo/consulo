/**
 * @author VISTALL
 * @since 2022-08-02
 */
@SuppressWarnings("module")
module consulo.http.impl {
    requires transitive consulo.http.api;

    requires java.net.http;

    requires com.google.common;

    requires consulo.project.api;
    requires consulo.credential.storage.api;
    requires consulo.configurable.api;
    requires consulo.file.chooser.api;
    requires consulo.ui.ex.api;
    requires consulo.base.icon.library;
    requires consulo.util.io;

    requires proxy.vole;

    exports consulo.http.impl.internal.proxy to consulo.ide.impl;
    exports consulo.http.impl.internal.ssl to
        consulo.ide.impl,
        consulo.proxy;

    opens consulo.http.impl.internal.proxy to consulo.util.xml.serializer;
    opens consulo.http.impl.internal.ssl to consulo.util.xml.serializer;
}