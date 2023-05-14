/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.options.colors;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.ide.ServiceManager;
import consulo.colorScheme.TextAttributesKey;
import consulo.util.lang.Pair;

import jakarta.annotation.Nullable;

/**
 * Registry for custom pages shown in the "Colors and Fonts" settings dialog.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class ColorSettingsPages {
  /**
   * Gets the global instance of the registry.
   *
   * @return the registry instance.
   */
  public static ColorSettingsPages getInstance() {
    return ServiceManager.getService(ColorSettingsPages.class);
  }

  /**
   * Returns the list of all registered pages in the "Colors and Fonts" dialog.
   *
   * @return the list of registered pages.
   */
  public abstract ColorSettingsPage[] getRegisteredPages();

  @Nullable
  public abstract Pair<ColorSettingsPage,AttributesDescriptor> getAttributeDescriptor(TextAttributesKey key);
}
