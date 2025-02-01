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

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.ide.impl.idea.ide.highlighter.custom.CustomFileHighlighter;
import consulo.ide.impl.idea.ide.highlighter.custom.CustomHighlighterColors;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.internal.custom.SyntaxTable;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import java.util.Map;

@ExtensionImpl(id = "custom")
public class CustomColorsPage implements ColorSettingsPage {
  private static final AttributesDescriptor[] ATTRS = {
    new AttributesDescriptor(
      ConfigurableLocalize.optionsCustomAttributeDescriptorKeyword1(),
      CustomHighlighterColors.CUSTOM_KEYWORD1_ATTRIBUTES
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsCustomAttributeDescriptorKeyword2(),
      CustomHighlighterColors.CUSTOM_KEYWORD2_ATTRIBUTES
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsCustomAttributeDescriptorKeyword3(),
      CustomHighlighterColors.CUSTOM_KEYWORD3_ATTRIBUTES
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsCustomAttributeDescriptorKeyword4(),
      CustomHighlighterColors.CUSTOM_KEYWORD4_ATTRIBUTES
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsCustomAttributeDescriptorNumber(),
      CustomHighlighterColors.CUSTOM_NUMBER_ATTRIBUTES
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsCustomAttributeDescriptorString(),
      CustomHighlighterColors.CUSTOM_STRING_ATTRIBUTES
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsCustomAttributeDescriptorLineComment(),
      CustomHighlighterColors.CUSTOM_LINE_COMMENT_ATTRIBUTES
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsCustomAttributeDescriptorBlockComment(),
      CustomHighlighterColors.CUSTOM_MULTI_LINE_COMMENT_ATTRIBUTES
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsCustomAttributeDescriptorValidStringEscape(),
      CustomHighlighterColors.CUSTOM_VALID_STRING_ESCAPE
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsCustomAttributeDescriptorInvalidStringEscape(),
      CustomHighlighterColors.CUSTOM_INVALID_STRING_ESCAPE
    ),
  };

  @NonNls
  private static final SyntaxTable SYNTAX_TABLE = new SyntaxTable();
  static {
    SYNTAX_TABLE.setLineComment("#");
    SYNTAX_TABLE.setStartComment("/*");
    SYNTAX_TABLE.setEndComment("*/");
    SYNTAX_TABLE.setHexPrefix("0x");
    SYNTAX_TABLE.setNumPostfixChars("dDlL");
    SYNTAX_TABLE.setNumPostfixChars("dDlL");
    SYNTAX_TABLE.setHasStringEscapes(true);
    SYNTAX_TABLE.addKeyword1("aKeyword1");
    SYNTAX_TABLE.addKeyword1("anotherKeyword1");
    SYNTAX_TABLE.addKeyword2("aKeyword2");
    SYNTAX_TABLE.addKeyword2("anotherKeyword2");
    SYNTAX_TABLE.addKeyword3("aKeyword3");
    SYNTAX_TABLE.addKeyword3("anotherKeyword3");
    SYNTAX_TABLE.addKeyword4("aKeyword4");
    SYNTAX_TABLE.addKeyword4("anotherKeyword4");
  }

  @Override
  @Nonnull
  public String getDisplayName() {
    return ConfigurableLocalize.optionsCustomDisplayName().get();
  }

  @Override
  @Nonnull
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATTRS;
  }

  @Override
  @Nonnull
  public SyntaxHighlighter getHighlighter() {
    return new CustomFileHighlighter(SYNTAX_TABLE);
  }

  @Override
  @Nonnull
  public String getDemoText() {
    return IdeLocalize.colorCustom().get();
  }

  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
  }
}