/**
 * @author VISTALL
 * @since 2022-08-02
 */
module consulo.http.impl {
    requires transitive consulo.http.api;

    requires java.net.http;

    requires com.google.common;

    requires consulo.project.api;
    requires consulo.credential.storage.api;
    requires consulo.util.io;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;
    requires forms.rt;

    requires org.apache.commons.codec;
    requires proxy.vole;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpmime;

    exports consulo.http.impl.internal.proxy to consulo.ide.impl;
    exports consulo.http.impl.internal.ssl to consulo.ide.impl, consulo.proxy;

    opens consulo.http.impl.internal.proxy to consulo.util.xml.serializer;
    opens consulo.http.impl.internal.ssl to consulo.util.xml.serializer;
}