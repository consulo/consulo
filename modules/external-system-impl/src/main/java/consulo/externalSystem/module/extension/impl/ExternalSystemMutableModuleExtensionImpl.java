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
package consulo.externalSystem.module.extension.impl;

import consulo.externalSystem.module.extension.ExternalSystemMutableModuleExtension;
import consulo.roots.ModuleRootLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 03-Jun-17
 */
public class ExternalSystemMutableModuleExtensionImpl extends ExternalSystemModuleExtensionImpl
        implements ExternalSystemMutableModuleExtension<ExternalSystemModuleExtensionImpl> {
  public ExternalSystemMutableModuleExtensionImpl(@NotNull String id, @NotNull ModuleRootLayer moduleRootLayer) {
    super(id, moduleRootLayer);
  }

  @Override
  public void setOption(@NotNull String key, @Nullable String value) {
    if (value == null) {
      myOptions.remove(key);
    }
    else {
      myOptions.put(key, value);
    }
  }

  @Override
  public void removeOption(@NotNull String key) {
    myOptions.remove(key);
  }

  @Override
  public void removeAllOptions() {
    myOptions.clear();
  }

  @Override
  public void setEnabled(boolean val) {
    myIsEnabled = val;
  }

  @Override
  public boolean isModified(@NotNull ExternalSystemModuleExtensionImpl originalExtension) {
    return myIsEnabled != originalExtension.isEnabled() || !myOptions.equals(originalExtension.myOptions);
  }
}
