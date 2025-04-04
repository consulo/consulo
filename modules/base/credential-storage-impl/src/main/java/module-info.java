/**
 * @author VISTALL
 * @since 23/01/2023
 */
module consulo.credential.storage.impl {
    requires consulo.credential.storage.api;
    requires consulo.process.api;

    requires consulo.util.jna;

    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.apache.commons.codec;

    // TODO remove this dependencies in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;
    requires forms.rt;
    
    requires org.bouncycastle.provider;
    requires com.google.common;
    requires org.yaml.snakeyaml;
    requires it.unimi.dsi.fastutil;

    requires org.freedesktop.dbus;
    requires jdk.net;

    exports consulo.credentialStorage.impl.internal to consulo.ide.impl;
    exports consulo.credentialStorage.impl.internal.ui to consulo.ide.impl;

    opens consulo.credentialStorage.impl.internal to consulo.util.xml.serializer;

    opens consulo.credentialStorage.impl.internal.linux to com.sun.jna;
    opens consulo.credentialStorage.impl.internal.windows to com.sun.jna;
    opens consulo.credentialStorage.impl.internal.mac to com.sun.jna;
}