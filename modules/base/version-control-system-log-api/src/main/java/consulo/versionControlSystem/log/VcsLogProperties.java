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

import jakarta.annotation.Nonnull;

public class VcsLogProperties {
  public static class VcsLogProperty<T> {
    private final T defaultValue;

    private VcsLogProperty(@Nonnull T defaultValue) {
      this.defaultValue = defaultValue;
    }
  }

  @Nonnull
  public static final VcsLogProperty<Boolean> LIGHTWEIGHT_BRANCHES = new VcsLogProperty<>(false);
  @Nonnull
  public static final VcsLogProperty<Boolean> SUPPORTS_INDEXING = new VcsLogProperty<>(false);
  @Nonnull
  public static final VcsLogProperty<Boolean> CASE_INSENSITIVE_REGEX = new VcsLogProperty<>(true);

  @Nonnull
  public static <T> T get(@Nonnull VcsLogProvider provider, VcsLogProperty<T> property) {
    T value = provider.getPropertyValue(property);
    if (value == null) return property.defaultValue;
    return value;
  }
}
