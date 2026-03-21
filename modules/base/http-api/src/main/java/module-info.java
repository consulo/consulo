import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-08-02
 */
@NullMarked
module consulo.http.api {
    requires transitive consulo.application.api;

    exports consulo.http;
    exports consulo.http.ssl;
    exports consulo.http.localize;
    exports consulo.http.ws;

    exports consulo.http.internal to consulo.http.adapter.httpclient4;
}