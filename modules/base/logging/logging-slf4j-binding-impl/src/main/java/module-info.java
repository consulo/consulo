/**
 * @author VISTALL
 * @since 15/01/2022
 */
module consulo.logging.slf4j.binding.impl {
  requires consulo.logging.api;
  requires org.slf4j;
  requires consulo.util.lang;

  provides org.slf4j.spi.SLF4JServiceProvider with consulo.logging.sfl4j.spi.ConsuloSLF4JServiceProvider;
}