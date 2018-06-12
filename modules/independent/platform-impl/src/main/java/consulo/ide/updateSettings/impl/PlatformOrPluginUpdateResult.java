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
package consulo.ide.updateSettings.impl;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.Couple;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 10-Oct-16
 */
public class PlatformOrPluginUpdateResult {
  public static PlatformOrPluginUpdateResult CANCELED = new PlatformOrPluginUpdateResult(Type.CANCELED, Collections.emptyList(), Collections.emptyList());
  public static PlatformOrPluginUpdateResult NO_UPDATE = new PlatformOrPluginUpdateResult(Type.NO_UPDATE, Collections.emptyList(), Collections.emptyList());
  public static PlatformOrPluginUpdateResult UPDATE_RESTART = new PlatformOrPluginUpdateResult(Type.UPDATE_RESTART, Collections.emptyList(), Collections.emptyList());

  public enum Type {
    PLATFORM_UPDATE,
    PLUGIN_UPDATE,
    UPDATE_RESTART,
    NO_UPDATE,
    CANCELED,
    // special case when user install plugins
    PLUGIN_INSTALL
  }

  private final Type myType;
  private final List<Couple<IdeaPluginDescriptor>> myPlugins;
  private final List<PluginId> myBrokenPluginIds;

  public PlatformOrPluginUpdateResult(@Nonnull Type type, @Nonnull List<Couple<IdeaPluginDescriptor>> plugins, @Nonnull List<PluginId> brokenPluginIds) {
    myType = type;
    myPlugins = plugins;
    myBrokenPluginIds = brokenPluginIds;
  }

  @Nonnull
  public List<PluginId> getBrokenPluginIds() {
    return myBrokenPluginIds;
  }

  @Nonnull
  public Type getType() {
    return myType;
  }

  @Nonnull
  public List<Couple<IdeaPluginDescriptor>> getPlugins() {
    return myPlugins;
  }
}
