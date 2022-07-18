import consulo.component.impl.internal.inject.DefaultRootInjectingContainerFactory;

/**
 * @author VISTALL
 * @since 20-Feb-22
 */
module consulo.component.impl {
  requires transitive consulo.component.api;
  requires transitive consulo.container.api;
  requires transitive consulo.proxy;
  requires transitive consulo.virtual.file.system.api;
  requires transitive jakarta.inject;

  requires consulo.container.impl;
  requires consulo.util.nodep;

  exports consulo.component.impl.internal to consulo.ide.impl, consulo.component.store.impl, consulo.application.impl, consulo.test.impl, consulo.module.impl;
  exports consulo.component.impl.internal.extension to consulo.ide.impl, consulo.test.impl, consulo.util.xml.serializer, consulo.application.impl, consulo.module.impl;
  exports consulo.component.impl.internal.messagebus to consulo.ide.impl, consulo.test.impl;
  exports consulo.component.impl.internal.macro to consulo.component.store.impl, consulo.application.impl, consulo.ide.impl, consulo.module.impl;

  exports consulo.component.impl.util;

  provides consulo.component.internal.inject.RootInjectingContainerFactory with DefaultRootInjectingContainerFactory;
}