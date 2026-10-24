import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-08-02
 */
@NullMarked
@SuppressWarnings("module")
module consulo.http.api {
    requires transitive consulo.application.api;
    requires transitive java.net.http;

    requires com.google.common;
    requires org.apache.httpcomponents.httpcore;

    exports consulo.http;
    exports consulo.http.ssl;
    exports consulo.http.localize;
    exports consulo.http.ws;
    exports consulo.http.client;
    exports consulo.http.json;
    exports consulo.http.graphql;
    exports consulo.http.graphql.dto;
    exports consulo.http.page;

    exports consulo.http.internal to
        consulo.http.adapter.httpclient4,
        consulo.http.impl,
        consulo.remote.server.agent.impl;
}