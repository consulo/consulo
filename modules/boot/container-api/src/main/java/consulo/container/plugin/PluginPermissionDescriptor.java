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
package consulo.container.plugin;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * @author VISTALL
 * @since 24/10/2021
 */
public final class PluginPermissionDescriptor {
  private final PluginPermissionType myType;
  private final Set<String> myOptions;

  public PluginPermissionDescriptor(@Nonnull PluginPermissionType type, @Nonnull Set<String> options) {
    myType = type;
    myOptions = Collections.unmodifiableSet(options);
  }

  @Nonnull
  public PluginPermissionType getType() {
    return myType;
  }

  @Nonnull
  public Set<String> getOptions() {
    return myOptions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PluginPermissionDescriptor that = (PluginPermissionDescriptor)o;

    if (myType != that.myType) return false;
    if (!myOptions.equals(that.myOptions)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myType.hashCode();
    result = 31 * result + myOptions.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "PluginPermissionDescriptor{" + "myType=" + myType + ", myOptions=" + myOptions + '}';
  }
}
