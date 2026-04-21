import org.jspecify.annotations.NullMarked;

@NullMarked
module consulo.util.lang {
  requires jdk.unsupported;
  requires static consulo.annotation;
  requires static com.uber.nullaway.annotations;
  requires org.slf4j;
  
  exports consulo.util.lang;
  exports consulo.util.lang.xml;
  exports consulo.util.lang.function;
  exports consulo.util.lang.lazy;
  exports consulo.util.lang.ref;
  exports consulo.util.lang.text;
  exports consulo.util.lang.reflect;
  exports consulo.util.lang.reflect.unsafe;
}