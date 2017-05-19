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
package consulo;

import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;

import static consulo.application.ApplicationProperties.CONSULO_AS_WEB_APP;

/**
 * @author VISTALL
 * @since 16-May-17
 */
public enum Platform {
  DESKTOP("consulo.platform.desktop"),
  WEB("consulo.platform.web");

  private static final NotNullLazyValue<Platform> ourPlatformValue = NotNullLazyValue.createValue(() -> Boolean.getBoolean(CONSULO_AS_WEB_APP) ? WEB : DESKTOP);

  @NotNull
  public static Platform get() {
    return ourPlatformValue.getValue();
  }

  private final PluginId myPluginId;

  Platform(String pluginId) {
    myPluginId = PluginId.getId(pluginId);
  }

  @NotNull
  public PluginId getPluginId() {
    return myPluginId;
  }
}
