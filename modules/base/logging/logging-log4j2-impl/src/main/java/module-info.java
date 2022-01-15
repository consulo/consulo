/**
 * @author VISTALL
 * @since 15/01/2022
 */
module consulo.logging.log4j2.impl {
  requires consulo.ide.impl;
  requires consulo.logging.api;
  requires consulo.container.api;

  requires org.apache.logging.log4j;
  requires org.apache.logging.log4j.core;

  provides consulo.logging.attachment.AttachmentFactory with consulo.logging.impl.log4j2.attachment.AttachmentFactoryImpl;
  provides consulo.logging.internal.LoggerFactory with consulo.logging.impl.log4j2.Log4J2LoggerFactory;
}