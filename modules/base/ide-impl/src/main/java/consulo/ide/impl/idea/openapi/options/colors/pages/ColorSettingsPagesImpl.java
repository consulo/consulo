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
package consulo.ide.impl.idea.openapi.options.colors.pages;

import consulo.annotation.component.ServiceImpl;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.ide.impl.idea.openapi.options.colors.ColorSettingsPages;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.util.lang.Pair;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Singleton
@ServiceImpl
public class ColorSettingsPagesImpl extends ColorSettingsPages {
  private Map<TextAttributesKey, Pair<ColorSettingsPage, AttributesDescriptor>> myKeyToDescriptorMap = new HashMap<>();

  @Override
  public ColorSettingsPage[] getRegisteredPages() {
    return ColorSettingsPage.EP_NAME.getExtensions();
  }

  @Override
  @Nullable
  public Pair<ColorSettingsPage,AttributesDescriptor> getAttributeDescriptor(TextAttributesKey key) {
    if (myKeyToDescriptorMap.containsKey(key)) {
      return myKeyToDescriptorMap.get(key);
    }
    else {
      for (ColorSettingsPage page : getRegisteredPages()) {
        for (AttributesDescriptor descriptor : page.getAttributeDescriptors()) {
          if (descriptor.getKey() == key) {
            Pair<ColorSettingsPage,AttributesDescriptor> result = new Pair<>(page, descriptor);
            myKeyToDescriptorMap.put(key, result);
            return result;
          }
        }
      }
      myKeyToDescriptorMap.put(key, null);
    }
    return null;
  }
}
