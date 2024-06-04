/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.application.options.colors;

import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorAndFontDescriptors;
import consulo.configurable.OptionsBundle;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeveritiesProvider;
import consulo.localize.LocalizeValue;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * @author lesya
 */
public class ColorSettingsUtil {
  private ColorSettingsUtil() {
  }

  @Nonnull
  public static Map<TextAttributesKey, LocalizeValue> keyToDisplayTextMap(final ColorSettingsPage page) {
    final List<AttributesDescriptor> attributeDescriptors = getAllAttributeDescriptors(page);
    final Map<TextAttributesKey, LocalizeValue> displayText = new HashMap<>();
    for (AttributesDescriptor attributeDescriptor : attributeDescriptors) {
      final TextAttributesKey key = attributeDescriptor.getKey();
      displayText.put(key, attributeDescriptor.getDisplayName());
    }
    return displayText;
  }

  public static List<AttributesDescriptor> getAllAttributeDescriptors(ColorAndFontDescriptors provider) {
    List<AttributesDescriptor> result = new ArrayList<AttributesDescriptor>();
    Collections.addAll(result, provider.getAttributeDescriptors());
    if (isInspectionColorsPage(provider)) {
      addInspectionSeverityAttributes(result);
    }
    return result;
  }

  private static boolean isInspectionColorsPage(ColorAndFontDescriptors provider) {
    // the first registered page implementing InspectionColorSettingsPage
    // gets the inspection attribute descriptors added to its list
    if (!(provider instanceof InspectionColorSettingsPage)) return false;
    for(ColorSettingsPage settingsPage: ColorSettingsPage.EP_NAME.getExtensionList()) {
      if (settingsPage == provider) break;
      if (settingsPage instanceof InspectionColorSettingsPage) return false;
    }
    return true;
  }

  private static void addInspectionSeverityAttributes(List<AttributesDescriptor> descriptors) {
    descriptors.add(new AttributesDescriptor(
      ConfigurableLocalize.optionsJavaAttributeDescriptorUnknownSymbol(),
      CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES
    ));
    descriptors.add(new AttributesDescriptor(
      ConfigurableLocalize.optionsJavaAttributeDescriptorDeprecatedSymbol(),
      CodeInsightColors.DEPRECATED_ATTRIBUTES
    ));
    descriptors.add(new AttributesDescriptor(
      ConfigurableLocalize.optionsJavaAttributeDescriptorUnusedSymbol(),
      CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES
    ));
    descriptors.add(new AttributesDescriptor(
      ConfigurableLocalize.optionsJavaAttributeDescriptorError(),
      CodeInsightColors.ERRORS_ATTRIBUTES
    ));
    descriptors.add(new AttributesDescriptor(
      ConfigurableLocalize.optionsJavaAttributeDescriptorWarning(),
      CodeInsightColors.WARNINGS_ATTRIBUTES
    ));
    descriptors.add(new AttributesDescriptor(
      ConfigurableLocalize.optionsJavaAttributeDescriptorWeakWarning(),
      CodeInsightColors.WEAK_WARNING_ATTRIBUTES
    ));
    descriptors.add(new AttributesDescriptor(
      ConfigurableLocalize.optionsJavaAttributeDescriptorServerProblems(),
      CodeInsightColors.GENERIC_SERVER_ERROR_OR_WARNING
    ));
    descriptors.add(new AttributesDescriptor(
      ConfigurableLocalize.optionsJavaAttributeDescriptorServerDuplicate(),
      CodeInsightColors.DUPLICATE_FROM_SERVER
    ));

    for (SeveritiesProvider provider : SeveritiesProvider.EP_NAME.getExtensionList()) {
      for (HighlightInfoType highlightInfoType : provider.getSeveritiesHighlightInfoTypes()) {
        final TextAttributesKey attributesKey = highlightInfoType.getAttributesKey();
        descriptors.add(new AttributesDescriptor(toDisplayName(attributesKey), attributesKey));
      }
    }
  }

  @Nonnull
  private static String toDisplayName(@Nonnull TextAttributesKey attributesKey) {
    return StringUtil.capitalize(attributesKey.getExternalName().toLowerCase().replaceAll("_", " "));
  }
}
