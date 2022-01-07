/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.updateSettings.impl;

import consulo.container.plugin.PluginId;
import consulo.localize.LocalizeValue;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23/11/2021
 */
public class PluginDownloadFailedException extends Exception {
  @Nonnull
  private final PluginId myPluginId;
  @Nonnull
  private final String myPluginName;
  @Nonnull
  private final LocalizeValue myMessage;

  public PluginDownloadFailedException(@Nonnull PluginId pluginId, @Nonnull String pluginName, @Nonnull LocalizeValue message) {
    super(message.get());
    myPluginId = pluginId;
    myPluginName = pluginName;
    myMessage = message;
  }

  @Nonnull
  public LocalizeValue getLocalizeMessage() {
    return myMessage;
  }

  @Nonnull
  public PluginId getPluginId() {
    return myPluginId;
  }

  @Nonnull
  public String getPluginName() {
    return myPluginName;
  }

  @Override
  public String getLocalizedMessage() {
    return myMessage.get();
  }
}
