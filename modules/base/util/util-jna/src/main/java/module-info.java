/**
 * @author VISTALL
 * @since 05-Feb-22
 */
module consulo.util.jna {
  requires transitive consulo.annotation;
  requires transitive com.sun.jna;
  requires transitive com.sun.jna.platform;

  requires org.slf4j;
  
  exports consulo.util.jna;
}