/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.remote;

import consulo.util.lang.StringUtil;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author traff
 */
public class WebDeploymentCredentialsHolder {
  public static final String WEB_SERVER_CONFIG_ID = "WEB_SERVER_CONFIG_ID";
  public static final String WEB_SERVER_CONFIG_NAME = "WEB_SERVER_CONFIG_NAME";


  private String myWebServerConfigId;
  private final RemoteCredentialsHolder myRemoteCredentials = new RemoteCredentialsHolder();
  private String myWebServerConfigName;


  public WebDeploymentCredentialsHolder() {
  }

  public WebDeploymentCredentialsHolder(@Nonnull String webServerConfigId, String name, @Nonnull RemoteCredentials remoteCredentials) {
    myWebServerConfigId = webServerConfigId;
    myWebServerConfigName = name;
    myRemoteCredentials.copyFrom(remoteCredentials);
  }

  @Nullable
  public String getWebServerConfigId() {
    return myWebServerConfigId;
  }

  public void setWebServerConfigId(@Nonnull String webServerConfigId) {
    myWebServerConfigId = webServerConfigId;
  }

  public String getWebServerConfigName() {
    return myWebServerConfigName;
  }

  public void setWebServerConfigName(@Nonnull String name) {
    myWebServerConfigName = name;
  }

  public void load(Element element) {
    myRemoteCredentials.load(element);
    setWebServerConfigId(element.getAttributeValue(WEB_SERVER_CONFIG_ID));
    setWebServerConfigName(StringUtil.notNullize(element.getAttributeValue(WEB_SERVER_CONFIG_NAME)));
  }

  public void save(Element element) {
    element.setAttribute(WEB_SERVER_CONFIG_ID, getWebServerConfigId());
    element.setAttribute(WEB_SERVER_CONFIG_NAME, getWebServerConfigName());

    myRemoteCredentials.save(element);
  }

  public RemoteCredentials getSshCredentials() {
    return myRemoteCredentials;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WebDeploymentCredentialsHolder holder = (WebDeploymentCredentialsHolder)o;

    if (!myWebServerConfigId.equals(holder.myWebServerConfigId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myWebServerConfigId.hashCode();
  }
}

