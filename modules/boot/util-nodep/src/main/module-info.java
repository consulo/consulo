@org.jspecify.annotations.NullMarked
module consulo.util.nodep {
  requires static org.jspecify;

  requires java.xml;

  exports consulo.util.nodep;
  exports consulo.util.nodep.annotation;
  exports consulo.util.nodep.classloader;
  exports consulo.util.nodep.io;
  exports consulo.util.nodep.reference;
  exports consulo.util.nodep.text;
  exports consulo.util.nodep.xml;
  exports consulo.util.nodep.xml.node;
}