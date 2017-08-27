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
package consulo.module.extension.impl;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.NotNullLazyValue;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionProviderEP;
import consulo.module.extension.MutableModuleExtension;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 25-Nov-16.
 */
public class ModuleExtensionProviders {
  private static final ExtensionPointName<ModuleExtensionProviderEP> EP_NAME = ExtensionPointName.create("com.intellij.moduleExtensionProvider");

  private static final NotNullLazyValue<ModuleExtensionProviderEP[]> ourExtensions = new NotNullLazyValue<ModuleExtensionProviderEP[]>() {
    @NotNull
    @Override
    protected ModuleExtensionProviderEP[] compute() {
      ModuleExtensionProviderEP[] extensions = EP_NAME.getExtensions();
      for (ModuleExtensionProviderEP extension : extensions) {
        Class<ModuleExtension> immutableClass = extension.getImmutableClass();
        if (immutableClass != null) {
          ModuleExtensionIndexCache.put(immutableClass, extension.getInternalIndex());
        }

        Class<MutableModuleExtension> mutableClass = extension.getMutableClass();
        if (mutableClass != null) {
          ModuleExtensionIndexCache.put(mutableClass, extension.getInternalIndex());
        }
      }
      return extensions;
    }
  };

  private static NotNullLazyValue<Map<String, ModuleExtensionProviderEP>> ourAllExtensionsValue = new NotNullLazyValue<Map<String, ModuleExtensionProviderEP>>() {
    @NotNull
    @Override
    protected Map<String, ModuleExtensionProviderEP> compute() {
      Map<String, ModuleExtensionProviderEP> map = new THashMap<>();
      for (ModuleExtensionProviderEP ep : getProviders()) {
        map.put(ep.key, ep);
      }
      return map;
    }
  };

  @NotNull
  public static String getEpName() {
    return EP_NAME.getName();
  }

  @NotNull
  public static ModuleExtensionProviderEP[] getProviders() {
    return ourExtensions.getValue();
  }

  @Nullable
  public static ModuleExtensionProviderEP findProvider(@NotNull String id) {
    return ourAllExtensionsValue.getValue().get(id);
  }
}
