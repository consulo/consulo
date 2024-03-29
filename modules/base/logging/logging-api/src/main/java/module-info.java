module consulo.logging.api {
  requires transitive consulo.annotation;
  requires consulo.util.nodep;
  requires consulo.container.api;

  exports consulo.logging;
  exports consulo.logging.attachment;

  exports consulo.logging.internal to consulo.logging.log4j2.impl, consulo.ide.impl, consulo.application.impl;

  uses consulo.logging.internal.LoggerFactory;
  uses consulo.logging.attachment.AttachmentFactory;
}