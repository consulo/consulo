module consulo.logging.api {
  requires transitive consulo.annotation;
  requires consulo.util.nodep;
  requires consulo.container.api;

  exports consulo.logging;
  exports consulo.logging.attachment;

  exports consulo.logging.internal to
      consulo.logging.logback.impl,
      consulo.ide.impl,
      consulo.application.impl;

  uses consulo.logging.internal.LoggerFactoryProvider;
  uses consulo.logging.attachment.AttachmentFactory;
}