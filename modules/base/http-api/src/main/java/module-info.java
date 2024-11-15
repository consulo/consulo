/**
 * @author VISTALL
 * @since 02-Aug-22
 */
module consulo.http.api {
    requires transitive consulo.application.api;

    exports consulo.http;
    exports consulo.http.ssl;

    exports consulo.http.internal to consulo.http.adapter.httpclient4;
}