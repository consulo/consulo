/**
 * @author VISTALL
 * @since 2024-11-15
 */
module consulo.http.adapter.httpclient4 {
    requires transitive consulo.http.api;

    requires transitive org.apache.httpcomponents.httpcore;
    requires transitive org.apache.httpcomponents.httpmime;
    requires transitive org.apache.httpcomponents.httpclient;

    exports consulo.http.adapter.httpclient4;
}