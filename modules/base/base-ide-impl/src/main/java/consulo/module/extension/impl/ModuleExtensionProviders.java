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
import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.openapi.util.NotNullLazyValue;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionProviderEP;
import consulo.module.extension.MutableModuleExtension;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 25-Nov-16.
 */
public class ModuleExtensionProviders {
  private static final ExtensionPointName<ModuleExtensionProviderEP> EP_NAME = ExtensionPointName.create("com.intellij.moduleExtensionProvider");

  private static final Supplier<List<ModuleExtensionProviderEP>> ourExtensions = AtomicNotNullLazyValue.createValue(() -> {
    List<ModuleExtensionProviderEP> extensions = EP_NAME.getExtensionList();
    Map<Class<?>, int[]> map = new HashMap<>();
    for (ModuleExtensionProviderEP extension : extensions) {
      Class<ModuleExtension> immutableClass = extension.getImmutableClass();
      if (immutableClass != null) {
        ModuleExtensionIndexCache.putToMap(map, immutableClass, extension.getInternalIndex());
      }

      Class<MutableModuleExtension> mutableClass = extension.getMutableClass();
      if (mutableClass != null) {
        ModuleExtensionIndexCache.putToMap(map, mutableClass, extension.getInternalIndex());
      }
    }
    ModuleExtensionIndexCache.putMap(map);
    return extensions;
  });

  private static Supplier<Map<String, ModuleExtensionProviderEP>> ourAllExtensionsValue = NotNullLazyValue.createValue(() -> {
    Map<String, ModuleExtensionProviderEP> map = new HashMap<>();
    for (ModuleExtensionProviderEP ep : getProviders()) {
      map.put(ep.key, ep);
    }
    return map;
  });

  @Nonnull
  public static String getEpName() {
    return EP_NAME.getName();
  }

  @Nonnull
  public static List<ModuleExtensionProviderEP> getProviders() {
    return ourExtensions.get();
  }

  @Nullable
  public static ModuleExtensionProviderEP findProvider(@Nonnull String id) {
    return ourAllExtensionsValue.get().get(id);
  }
}
