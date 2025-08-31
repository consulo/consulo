/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.configuration;

import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jdom.Element;

import java.io.File;

/**
 * @author dyoma
 */
public class ExternalizablePath implements JDOMExternalizable {
  private static final String VALUE_ATTRIBUTE = "value";

  private String myUrl;

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    String value = element.getAttributeValue(VALUE_ATTRIBUTE);
    myUrl = value != null ? value : "";
    String protocol = VirtualFileManager.extractProtocol(myUrl);
    if (protocol == null) myUrl = urlValue(myUrl);
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    element.setAttribute(VALUE_ATTRIBUTE, myUrl);
  }

  public String getLocalPath() {
    return localPathValue(myUrl);
  }

  public static String urlValue(String localPath) {
    if (localPath == null) return "";
    localPath = localPath.trim();
    if (localPath.length() == 0) return "";
    return VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, localPath.replace(File.separatorChar, '/'));
  }

  public static String localPathValue(String url) {
    if (url == null) return "";
    url = url.trim();
    if (url.length() == 0) return "";
    return VirtualFileManager.extractPath(url).replace('/', File.separatorChar);
  }
}
