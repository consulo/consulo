/**
 * @author VISTALL
 * @since 20-Feb-22
 */
module consulo.component.impl {
  requires transitive consulo.component.api;
  requires transitive consulo.container.api;
  requires transitive consulo.injecting.api;
  requires transitive consulo.proxy;

  requires consulo.container.impl;
  requires consulo.util.nodep;

  exports consulo.component.impl to consulo.ide.impl, consulo.test.impl;
  exports consulo.component.impl.extension to consulo.ide.impl, consulo.test.impl;
  exports consulo.component.impl.messagebus to consulo.ide.impl, consulo.test.impl;
}