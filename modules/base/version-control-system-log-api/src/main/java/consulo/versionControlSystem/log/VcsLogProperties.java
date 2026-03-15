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
package consulo.versionControlSystem.log;

import consulo.versionControlSystem.log.VcsLogProvider;


public class VcsLogProperties {
  public static class VcsLogProperty<T> {
    private final T defaultValue;

    private VcsLogProperty(T defaultValue) {
      this.defaultValue = defaultValue;
    }
  }

  
  public static final VcsLogProperty<Boolean> LIGHTWEIGHT_BRANCHES = new VcsLogProperty<>(false);
  
  public static final VcsLogProperty<Boolean> SUPPORTS_INDEXING = new VcsLogProperty<>(false);
  
  public static final VcsLogProperty<Boolean> CASE_INSENSITIVE_REGEX = new VcsLogProperty<>(true);

  
  public static <T> T get(VcsLogProvider provider, VcsLogProperty<T> property) {
    T value = provider.getPropertyValue(property);
    if (value == null) return property.defaultValue;
    return value;
  }
}
