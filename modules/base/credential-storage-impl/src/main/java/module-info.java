/**
 * @author VISTALL
 * @since 23/01/2023
 */
module consulo.credential.storage.impl {
  requires consulo.credential.storage.api;

  requires com.sun.jna;
  requires com.sun.jna.platform;
  requires org.apache.commons.codec;

  // TODO remove this dependencies in future
  requires java.desktop;
  requires consulo.ui.ex.awt.api;
  requires forms.rt;

  exports consulo.credentialStorage.impl.internal to consulo.ide.impl;
  exports consulo.credentialStorage.impl.internal.provider.masterKey to consulo.ide.impl;
  exports consulo.credentialStorage.impl.internal.ui to consulo.ide.impl;

  opens consulo.credentialStorage.impl.internal to consulo.util.xml.serializer;
  opens consulo.credentialStorage.impl.internal.provider.masterKey to consulo.util.xml.serializer;
}