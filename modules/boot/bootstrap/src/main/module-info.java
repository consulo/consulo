module consulo.bootstrap {
  exports consulo.bootstrap.concurrent;
  exports consulo.bootstrap.charset;

  provides java.nio.charset.spi.CharsetProvider with consulo.bootstrap.charset.Native2AsciiCharsetProvider;
}