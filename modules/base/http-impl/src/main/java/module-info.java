/**
 * @author VISTALL
 * @since 02-Aug-22
 */
module consulo.http.impl {
  requires transitive consulo.http.api;

  requires consulo.project.api;

  requires consulo.credential.storage.api;

  // TODO remove in future
  requires java.desktop;
  requires consulo.ui.ex.awt.api;
  
  requires org.apache.commons.codec;
  requires proxy.vole;
  requires org.apache.httpcomponents.httpcore;
  requires org.apache.httpcomponents.httpclient;
  requires org.apache.httpcomponents.httpmime;

  exports consulo.http.impl.internal.proxy to consulo.ide.impl;
  exports consulo.http.impl.internal.ssl to consulo.ide.impl;

  opens consulo.http.impl.internal.proxy to consulo.util.xml.serializer;
  opens consulo.http.impl.internal.ssl to consulo.util.xml.serializer;
}