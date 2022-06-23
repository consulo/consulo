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
import consulo.ide.impl.idea.ide.highlighter.custom.CustomFileHighlighter;
import consulo.ide.impl.idea.ide.highlighter.custom.CustomHighlighterColors;
import consulo.ide.impl.idea.ide.highlighter.custom.SyntaxTable;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.configurable.OptionsBundle;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.Map;

@ExtensionImpl(id = "custom")
public class CustomColorsPage implements ColorSettingsPage {
  private static final AttributesDescriptor[] ATTRS = {
    new AttributesDescriptor(OptionsBundle.message("options.custom.attribute.descriptor.keyword1"), CustomHighlighterColors.CUSTOM_KEYWORD1_ATTRIBUTES),
    new AttributesDescriptor(OptionsBundle.message("options.custom.attribute.descriptor.keyword2"), CustomHighlighterColors.CUSTOM_KEYWORD2_ATTRIBUTES),
    new AttributesDescriptor(OptionsBundle.message("options.custom.attribute.descriptor.keyword3"), CustomHighlighterColors.CUSTOM_KEYWORD3_ATTRIBUTES),
    new AttributesDescriptor(OptionsBundle.message("options.custom.attribute.descriptor.keyword4"), CustomHighlighterColors.CUSTOM_KEYWORD4_ATTRIBUTES),
    new AttributesDescriptor(OptionsBundle.message("options.custom.attribute.descriptor.number"), CustomHighlighterColors.CUSTOM_NUMBER_ATTRIBUTES),
    new AttributesDescriptor(OptionsBundle.message("options.custom.attribute.descriptor.string"), CustomHighlighterColors.CUSTOM_STRING_ATTRIBUTES),
    new AttributesDescriptor(OptionsBundle.message("options.custom.attribute.descriptor.line.comment"), CustomHighlighterColors.CUSTOM_LINE_COMMENT_ATTRIBUTES),
    new AttributesDescriptor(OptionsBundle.message("options.custom.attribute.descriptor.block.comment"), CustomHighlighterColors.CUSTOM_MULTI_LINE_COMMENT_ATTRIBUTES),
    new AttributesDescriptor(OptionsBundle.message("options.custom.attribute.descriptor.valid.string.escape"), CustomHighlighterColors.CUSTOM_VALID_STRING_ESCAPE),
    new AttributesDescriptor(OptionsBundle.message("options.custom.attribute.descriptor.invalid.string.escape"), CustomHighlighterColors.CUSTOM_INVALID_STRING_ESCAPE),
  };

  @NonNls private static final SyntaxTable SYNTAX_TABLE = new SyntaxTable();
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
    return OptionsBundle.message("options.custom.display.name");
  }

  @Override
  @Nonnull
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATTRS;
  }

  @Override
  @Nonnull
  public ColorDescriptor[] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @Override
  @Nonnull
  public SyntaxHighlighter getHighlighter() {
    return new CustomFileHighlighter(SYNTAX_TABLE);
  }

  @Override
  @Nonnull
  public String getDemoText() {
    return "# Line comment\n"
           + "aKeyword1 variable = 123;\n"
           + "anotherKeyword1 someString = \"SomeString\";\n"
           + "aKeyword2 variable = 123;\n"
           + "anotherKeyword2 someString = \"SomeString\";\n"
           + "aKeyword3 variable = 123;\n"
           + "anotherKeyword3 someString = \"SomeString\";\n"
           + "aKeyword4 variable = 123;\n"
           + "anotherKeyword4 someString = \"SomeString\\n\\x\";\n"
           + "/* \n"
           + " * Block comment\n"
           + " */\n"
           + "\n";
  }

  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
  }
}