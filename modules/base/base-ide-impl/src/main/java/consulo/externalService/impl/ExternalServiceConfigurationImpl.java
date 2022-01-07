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
package consulo.externalService.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.*;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.ui.JBUI;
import com.intellij.util.xmlb.XmlSerializerUtil;
import consulo.externalService.ExternalService;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.externalService.ExternalServiceConfigurationListener;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.util.lang.ThreeState;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.codec.digest.DigestUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 06-Mar-17
 */
@Singleton
@State(name = "ExternalServiceConfiguration", storages = @Storage(value = "externalService.xml", roamingType = RoamingType.DISABLED))
public class ExternalServiceConfigurationImpl implements PersistentStateComponent<ExternalServiceConfigurationImpl.State>, ExternalServiceConfiguration {
  private static final Logger LOG = Logger.getInstance(ExternalServiceConfigurationImpl.class);

  public static class State {
    public String email;
    public String oauthKey;
    public String iconBytes;

    public Map<ExternalService, ThreeState> states = new LinkedHashMap<>();
  }

  private final State myState = new State();
  private Image myUserIcon;

  private final Application myApplication;

  @Inject
  public ExternalServiceConfigurationImpl(Application application) {
    myApplication = application;
  }

  @Override
  public void updateIcon() {
    myUserIcon = null;

    String email = myState.email;
    if (email == null) {
      myState.iconBytes = null;
      return;
    }

    // get node size
    int size = (int)Math.ceil(Image.DEFAULT_ICON_SIZE * JBUI.sysScale());
    Application.get().executeOnPooledThread(() -> {
      String emailHash = DigestUtils.md5Hex(email.toLowerCase().trim());

      try {
        byte[] bytes = HttpRequests.request("https://www.gravatar.com/avatar/" + emailHash + ".png?s=" + size + "&d=identicon").readBytes(null);

        myState.iconBytes = Base64.getEncoder().encodeToString(bytes);
      }
      catch (IOException e) {
        LOG.error(e);
      }
    });
  }

  @Override
  @Nullable
  public Image getUserIcon() {
    if (myUserIcon != null) {
      return myUserIcon;
    }

    String iconBytes = myState.iconBytes;
    if (iconBytes != null) {
      byte[] bytes = Base64.getDecoder().decode(iconBytes);
      try {
        myUserIcon = Image.fromBytes(Image.ImageType.PNG, bytes, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
      }
      catch (IOException ignored) {
      }
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

  @Override
  public void afterLoadState() {
    myApplication.getMessageBus().syncPublisher(ExternalServiceConfigurationListener.TOPIC).configurationChanged(this);
  }

  @Override
  @Nonnull
  public ThreeState getState(@Nonnull ExternalService externalService) {
    ThreeState state = myState.states.getOrDefault(externalService, externalService.getDefaultState());
    if(state == ThreeState.YES && !isAuthorized()) {
      return ThreeState.NO;
    }
    return state;
  }

  @Override
  public void setState(@Nonnull ExternalService externalService, @Nonnull ThreeState state) {
    if(externalService.getDefaultState() == state) {
      myState.states.remove(externalService);
    }
    else {
      myState.states.put(externalService, state);
    }
  }

  @Override
  @Nullable
  public String getEmail() {
    return myState.email;
  }

  @Nullable
  public String getOAuthKey() {
    return myState.oauthKey;
  }

  public void authorize(@Nonnull String email, @Nonnull String token) {
    myState.email = email;
    myState.oauthKey = token;
  }

  @Override
  public boolean isAuthorized() {
    return getEmail() != null;
  }

  public void reset() {
    myState.email = null;
    myState.iconBytes = null;
    myState.oauthKey = null;
  }
}
