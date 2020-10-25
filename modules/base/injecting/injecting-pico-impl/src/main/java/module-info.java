import consulo.injecting.RootInjectingContainerFactory;
import consulo.injecting.pico.PicoRootInjectingContainerFactory;

/**
 * @author VISTALL
 * @since 2020-10-25
 */
module consulo.injecting.pico.impl {
  requires consulo.injecting.api;
  requires consulo.logging.api;
  requires consulo.annotation;

  requires jakarta.inject;

  requires consulo.util.collection;
  requires consulo.util.lang;
  requires gnu.trove;

  provides RootInjectingContainerFactory with PicoRootInjectingContainerFactory;
}