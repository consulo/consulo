/**
 * @author VISTALL
 * @since 13/01/2022
 */
module consulo.proxy {
  requires consulo.disposer.api;
  requires consulo.logging.api;
  requires consulo.container.api;
  requires consulo.util.nodep;

  requires consulo.util.collection;

  requires static net.bytebuddy;

  exports consulo.proxy;
  exports consulo.proxy.advanced;
}