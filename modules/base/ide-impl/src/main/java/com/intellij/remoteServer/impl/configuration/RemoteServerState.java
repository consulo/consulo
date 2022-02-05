package com.intellij.remoteServer.impl.configuration;

import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import org.jdom.Element;

/**
* @author nik
*/
@Tag("remote-server")
public class RemoteServerState {
  @Attribute("name")
  public String myName;
  @Attribute("type")
  public String myTypeId;
  @Tag("configuration")
  public Element myConfiguration;
}
