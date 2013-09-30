/*
 * Copyright 2013 Consulo.org
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
package org.consulo.module.extension;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.KeyedLazyInstanceEP;
import com.intellij.util.containers.HashMap;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11:45/19.05.13
 */
public class ModuleExtensionProviderEP extends KeyedLazyInstanceEP<ModuleExtensionProvider> {
  public static final ExtensionPointName<ModuleExtensionProviderEP> EP_NAME =
    ExtensionPointName.create("com.intellij.moduleExtensionProvider");

  @Attribute("parent-key")
  public String parentKey;

  @Attribute("allow-mixin")
  public boolean allowMixin;

  @Attribute("system-only")
  public boolean systemOnly;

  private static NotNullLazyValue<Map<String, ModuleExtensionProvider>> myLazyMap = new NotNullLazyValue<Map<String, ModuleExtensionProvider>>() {
    @NotNull
    @Override
    protected Map<String, ModuleExtensionProvider> compute() {
      Map<String, ModuleExtensionProvider> map = new HashMap<String, ModuleExtensionProvider>();
      for (ModuleExtensionProviderEP ep : EP_NAME.getExtensions()) {
        ModuleExtensionProvider extensionProvider = ep.getInstance();
        if(extensionProvider == null) {
          continue;
        }
        map.put(ep.getKey(), extensionProvider);
      }
      return map;
    }
  };

  @NotNull
  public static Collection<ModuleExtensionProvider> getProviders() {
    return myLazyMap.getValue().values();
  }

  @Nullable
  public static ModuleExtensionProvider findProvider(@NotNull String id) {
    return myLazyMap.getValue().get(id);
  }
}
