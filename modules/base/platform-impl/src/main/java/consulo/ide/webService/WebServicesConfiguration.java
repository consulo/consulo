/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.webService;

import com.intellij.openapi.components.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 19-Nov-16.
 */
@Singleton
@State(name = "WebServicesConfiguration", storages = @Storage(value = "webServices.xml", roamingType = RoamingType.DISABLED))
public class WebServicesConfiguration implements PersistentStateComponent<WebServicesConfiguration.State> {
  @Nonnull
  public static WebServicesConfiguration getInstance() {
    return ServiceManager.getService(WebServicesConfiguration.class);
  }

  protected static final class State {
    public Map<WebServiceApi, String> oauthKeys = new HashMap<>();
  }

  private State myState = new State();

  @Nullable
  public String getOAuthKey(WebServiceApi api) {
    return myState.oauthKeys.get(api);
  }

  public void setOAuthKey(WebServiceApi api, String key) {
    key = StringUtil.nullize(key, true);
    if (key == null) {
      myState.oauthKeys.remove(api);
    }
    else {
      myState.oauthKeys.put(api, key);
    }
  }

  @Nullable
  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    XmlSerializerUtil.copyBean(state, myState);
  }
}
