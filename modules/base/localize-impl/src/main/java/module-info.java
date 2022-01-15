/**
 * @author VISTALL
 * @since 13/01/2022
 */
module consulo.localize.impl {
  requires consulo.logging.api;
  requires consulo.localize.api;
  requires consulo.util.lang;
  requires consulo.util.io;
  requires consulo.proxy;

  requires org.yaml.snakeyaml;

  exports consulo.localize.impl;

  provides consulo.localize.LocalizeManager with consulo.localize.impl.LocalizeManagerImpl;
}