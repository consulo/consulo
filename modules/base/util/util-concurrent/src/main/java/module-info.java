/**
 * @author VISTALL
 * @since 2020-10-23
 */
module consulo.util.concurrent {
  requires org.slf4j;
  requires consulo.annotation;
  requires consulo.util.lang;
  requires consulo.util.collection;

  exports consulo.util.concurrent;

  exports consulo.util.concurrent.internal to consulo.ui.api;
}