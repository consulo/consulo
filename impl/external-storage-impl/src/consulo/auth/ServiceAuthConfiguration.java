/*
 * Copyright 2013-2017 consulo.io
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
package consulo.auth;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ImageLoader;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import com.intellij.util.ui.JBImageIcon;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.Base64;

/**
 * @author VISTALL
 * @since 06-Mar-17
 */
@State(name = "ServiceAuthConfiguration", storages = @Storage(value = "auth.xml", roamingType = RoamingType.DISABLED))
public class ServiceAuthConfiguration implements PersistentStateComponent<ServiceAuthConfiguration.State> {
  private static final Logger LOGGER = Logger.getInstance(ServiceAuthConfiguration.class);

  protected static class State {
    public String email;
    public String iconBytes;
  }

  @NotNull
  public static ServiceAuthConfiguration getInstance() {
    return ServiceManager.getService(ServiceAuthConfiguration.class);
  }

  private final State myState = new State();
  private Icon myUserIcon;

  public void updateIcon() {
    myUserIcon = null;

    String email = myState.email;
    if (email == null) {
      myState.iconBytes = null;
      return;
    }

    // get node size
    int size = AllIcons.Actions.Find.getIconHeight();
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      String emailHash = DigestUtils.md5Hex(email.toLowerCase().trim());

      try {
        byte[] bytes = HttpRequests.request("https://www.gravatar.com/avatar/" + emailHash + ".png?s=" + size + "&d=identicon").readBytes(null);

        myState.iconBytes = Base64.getEncoder().encodeToString(bytes);
      }
      catch (IOException e) {
        LOGGER.error(e);
      }
    });
  }

  @Nullable
  public Icon getUserIcon() {
    if(myUserIcon != null) {
      return myUserIcon;
    }

    String iconBytes = myState.iconBytes;
    if(iconBytes != null) {
      byte[] bytes = Base64.getDecoder().decode(iconBytes);
      myUserIcon = new JBImageIcon(ImageLoader.loadFromStream(new UnsyncByteArrayInputStream(bytes)));
    }
    return myUserIcon;
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    XmlSerializerUtil.copyBean(state, myState);
  }

  @Nullable
  public String getEmail() {
    return myState.email;
  }

  public void setEmail(@Nullable String email) {
    myState.email = email;
  }
}
