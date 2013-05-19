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
import com.intellij.util.KeyedLazyInstanceEP;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 11:45/19.05.13
 */
public class ModuleExtensionProviderEP extends KeyedLazyInstanceEP<ModuleExtensionProvider> {
  public static final ExtensionPointName<ModuleExtensionProviderEP> EP_NAME =
    ExtensionPointName.create("com.intellij.moduleExtensionProvider");

  @Attribute("parent-key")
  public String parentKey;

  @NotNull
  public static List<ModuleExtensionProvider> getProviders() {
    final ModuleExtensionProviderEP[] extensions = EP_NAME.getExtensions();
    if (extensions.length == 0) {
      return Collections.emptyList();
    }

    List<ModuleExtensionProvider> list = new ArrayList<ModuleExtensionProvider>(extensions.length);
    for (ModuleExtensionProviderEP ep : extensions) {
      list.add(ep.getInstance());
    }
    return list;
  }

  public static ModuleExtensionProvider findProvider(@NotNull String id) {
    final ModuleExtensionProviderEP[] extensions = EP_NAME.getExtensions();
    for (ModuleExtensionProviderEP ep : extensions) {
      if (ep.getKey().equals(id)) {
        return ep.getInstance();
      }
    }
    return null;
  }
}
