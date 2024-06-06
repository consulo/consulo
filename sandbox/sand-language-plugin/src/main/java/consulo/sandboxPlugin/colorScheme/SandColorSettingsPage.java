/*
 * Copyright 2013-2024 consulo.io
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
package consulo.sandboxPlugin.colorScheme;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.DefaultSyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 06/06/2024
 */
@ExtensionImpl
public class SandColorSettingsPage implements ColorSettingsPage {
  @Nonnull
  @Override
  public ColorDescriptor[] getColorDescriptors() {
    return new ColorDescriptor[]{
      new ColorDescriptor(LocalizeValue.localizeTODO("Sand Color"), SandEditorColors.SAND_COLOR, ColorDescriptor.Kind.BACKGROUND)
    };
  }

  @Nonnull
  @Override
  public SyntaxHighlighter getHighlighter() {
    return new DefaultSyntaxHighlighter();
  }

  @Nonnull
  @Override
  public String getDemoText() {
    return " ";
  }

  @Nullable
  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return Map.of();
  }

  @Nonnull
  @Override
  public AttributesDescriptor[] getAttributeDescriptors() {
    return new AttributesDescriptor[0];
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "Sand";
  }
}
