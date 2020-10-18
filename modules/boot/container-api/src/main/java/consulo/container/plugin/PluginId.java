/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.container.plugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author max
 */
public class PluginId implements Comparable<PluginId> {
  public static final PluginId[] EMPTY_ARRAY = new PluginId[0];

  private static final Map<String, PluginId> ourRegisteredIds = new HashMap<String, PluginId>();

  private final String myIdString;

  private PluginId(@Nonnull String idString) {
    myIdString = idString;
  }

  @Override
  public int compareTo(@Nonnull PluginId o) {
    return myIdString.compareTo(o.myIdString);
  }

  @Nonnull
  public static synchronized PluginId getId(@Nonnull String idString) {
    PluginId pluginId = ourRegisteredIds.get(idString);
    if (pluginId == null) {
      pluginId = new PluginId(idString);
      ourRegisteredIds.put(idString, pluginId);
    }
    return pluginId;
  }

  @Nullable
  public static synchronized PluginId findId(@Nonnull String... idStrings) {
    for (String idString : idStrings) {
      PluginId pluginId = ourRegisteredIds.get(idString);
      if (pluginId != null) {
        return pluginId;
      }
    }
    return null;
  }

  @Nonnull
  public String getIdString() {
    return myIdString;
  }

  @Override
  public String toString() {
    return getIdString();
  }

  @Nonnull
  public static synchronized Map<String, PluginId> getRegisteredIds() {
    return new HashMap<String, PluginId>(ourRegisteredIds);
  }
}
