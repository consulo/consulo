import org.jspecify.annotations.NullMarked;

@NullMarked
open module consulo.util.rmi {
  requires java.rmi;

  exports consulo.util.rmi;
}