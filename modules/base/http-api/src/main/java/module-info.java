/**
 * @author VISTALL
 * @since 02-Aug-22
 */
module consulo.http.api {
  requires transitive consulo.application.api;

  requires org.apache.httpcomponents.httpcore;
  requires org.apache.httpcomponents.httpclient;
  requires org.apache.httpcomponents.httpmime;

  exports consulo.http;
}