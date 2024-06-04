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
package consulo.ide.impl.idea.openapi.options.colors.pages;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.DefaultSyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows to set default colors for multiple languages.
 *
 * @author Rustam Vishnyakov
 */
@ExtensionImpl(id = "defaultLanguage")
public class DefaultLanguageColorsPage implements ColorSettingsPage, ConfigurableWeight {

  private static final Map<String, TextAttributesKey> TAG_HIGHLIGHTING_MAP = new HashMap<>();

  private final static TextAttributesKey FAKE_BAD_CHAR =
    TextAttributesKey.createTextAttributesKey("FAKE_BAD_CHAR", HighlighterColors.BAD_CHARACTER);

  static {
    TAG_HIGHLIGHTING_MAP.put("bad_char", FAKE_BAD_CHAR);
    TAG_HIGHLIGHTING_MAP.put("template_language", DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR);
    TAG_HIGHLIGHTING_MAP.put("identifier", DefaultLanguageHighlighterColors.IDENTIFIER);
    TAG_HIGHLIGHTING_MAP.put("number", DefaultLanguageHighlighterColors.NUMBER);
    TAG_HIGHLIGHTING_MAP.put("keyword", DefaultLanguageHighlighterColors.KEYWORD);
    TAG_HIGHLIGHTING_MAP.put("macro_keyword", DefaultLanguageHighlighterColors.MACRO_KEYWORD);
    TAG_HIGHLIGHTING_MAP.put("string", DefaultLanguageHighlighterColors.STRING);
    TAG_HIGHLIGHTING_MAP.put("line_comment", DefaultLanguageHighlighterColors.LINE_COMMENT);
    TAG_HIGHLIGHTING_MAP.put("block_comment", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    TAG_HIGHLIGHTING_MAP.put("operation_sign", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    TAG_HIGHLIGHTING_MAP.put("braces", DefaultLanguageHighlighterColors.BRACES);
    TAG_HIGHLIGHTING_MAP.put("doc_comment", DefaultLanguageHighlighterColors.DOC_COMMENT);
    TAG_HIGHLIGHTING_MAP.put("dot", DefaultLanguageHighlighterColors.DOT);
    TAG_HIGHLIGHTING_MAP.put("semicolon", DefaultLanguageHighlighterColors.SEMICOLON);
    TAG_HIGHLIGHTING_MAP.put("comma", DefaultLanguageHighlighterColors.COMMA);
    TAG_HIGHLIGHTING_MAP.put("brackets", DefaultLanguageHighlighterColors.BRACKETS);
    TAG_HIGHLIGHTING_MAP.put("parenths", DefaultLanguageHighlighterColors.PARENTHESES);
    TAG_HIGHLIGHTING_MAP.put("func_decl", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);
    TAG_HIGHLIGHTING_MAP.put("func_call", DefaultLanguageHighlighterColors.FUNCTION_CALL);
    TAG_HIGHLIGHTING_MAP.put("param", DefaultLanguageHighlighterColors.PARAMETER);
    TAG_HIGHLIGHTING_MAP.put("class_name", DefaultLanguageHighlighterColors.CLASS_NAME);
    TAG_HIGHLIGHTING_MAP.put("class_ref", DefaultLanguageHighlighterColors.CLASS_REFERENCE);
    TAG_HIGHLIGHTING_MAP.put("inst_method", DefaultLanguageHighlighterColors.INSTANCE_METHOD);
    TAG_HIGHLIGHTING_MAP.put("inst_field", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    TAG_HIGHLIGHTING_MAP.put("static_method", DefaultLanguageHighlighterColors.STATIC_METHOD);
    TAG_HIGHLIGHTING_MAP.put("type_alias_name", DefaultLanguageHighlighterColors.TYPE_ALIAS_NAME);
    TAG_HIGHLIGHTING_MAP.put("static_field", DefaultLanguageHighlighterColors.STATIC_FIELD);
    TAG_HIGHLIGHTING_MAP.put("label", DefaultLanguageHighlighterColors.LABEL);
    TAG_HIGHLIGHTING_MAP.put("local_var", DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
    TAG_HIGHLIGHTING_MAP.put("global_var", DefaultLanguageHighlighterColors.GLOBAL_VARIABLE);
    TAG_HIGHLIGHTING_MAP.put("const", DefaultLanguageHighlighterColors.CONSTANT);
    TAG_HIGHLIGHTING_MAP.put("interface", DefaultLanguageHighlighterColors.INTERFACE_NAME);
    TAG_HIGHLIGHTING_MAP.put("doc_markup", DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP);
    TAG_HIGHLIGHTING_MAP.put("doc_tag", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG);
    TAG_HIGHLIGHTING_MAP.put("valid_esc_seq", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
    TAG_HIGHLIGHTING_MAP.put("invalid_esc_seq", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);
    TAG_HIGHLIGHTING_MAP.put("predefined", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL);
    TAG_HIGHLIGHTING_MAP.put("metadata", DefaultLanguageHighlighterColors.METADATA);
    TAG_HIGHLIGHTING_MAP.put("tag", DefaultLanguageHighlighterColors.MARKUP_TAG);
    TAG_HIGHLIGHTING_MAP.put("attribute", DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE);
    TAG_HIGHLIGHTING_MAP.put("entity", DefaultLanguageHighlighterColors.MARKUP_ENTITY);
  }

  private final static AttributesDescriptor[] ATTRIBUTES_DESCRIPTORS = {
    new AttributesDescriptor(
      ConfigurableLocalize.optionsJavaAttributeDescriptorBadCharacter(),
      HighlighterColors.BAD_CHARACTER
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsKeyword(),
      DefaultLanguageHighlighterColors.KEYWORD
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsMacroKeyword(),
      DefaultLanguageHighlighterColors.MACRO_KEYWORD
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsIdentifier(),
      DefaultLanguageHighlighterColors.IDENTIFIER
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsString(),
      DefaultLanguageHighlighterColors.STRING
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsValidEscSeq(),
      DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsInvalidEscSeq(),
      DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsNumber(),
      DefaultLanguageHighlighterColors.NUMBER
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsOperation(),
      DefaultLanguageHighlighterColors.OPERATION_SIGN
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsBraces(),
      DefaultLanguageHighlighterColors.BRACES
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsParentheses(),
      DefaultLanguageHighlighterColors.PARENTHESES
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsBrackets(),
      DefaultLanguageHighlighterColors.BRACKETS
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsDot(),
      DefaultLanguageHighlighterColors.DOT
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsComma(),
      DefaultLanguageHighlighterColors.COMMA
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsSemicolon(),
      DefaultLanguageHighlighterColors.SEMICOLON
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsLineComment(),
      DefaultLanguageHighlighterColors.LINE_COMMENT
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsBlockComment(),
      DefaultLanguageHighlighterColors.BLOCK_COMMENT
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsDocComment(),
      DefaultLanguageHighlighterColors.DOC_COMMENT
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsDocMarkup(),
      DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsDocTag(),
      DefaultLanguageHighlighterColors.DOC_COMMENT_TAG
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsLabel(),
      DefaultLanguageHighlighterColors.LABEL
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsConstant(),
      DefaultLanguageHighlighterColors.CONSTANT
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsPredefined(),
      DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsLocalVariable(),
      DefaultLanguageHighlighterColors.LOCAL_VARIABLE
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsGlobalVariable(),
      DefaultLanguageHighlighterColors.GLOBAL_VARIABLE
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsFunctionDeclaration(),
      DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsFunctionCall(),
      DefaultLanguageHighlighterColors.FUNCTION_CALL
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsParameter(),
      DefaultLanguageHighlighterColors.PARAMETER
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsInterfaceName(),
      DefaultLanguageHighlighterColors.INTERFACE_NAME
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsTypeAliasName(),
      DefaultLanguageHighlighterColors.TYPE_ALIAS_NAME
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsMetadata(),
      DefaultLanguageHighlighterColors.METADATA
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsClassName(),
      DefaultLanguageHighlighterColors.CLASS_NAME
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsClassReference(),
      DefaultLanguageHighlighterColors.CLASS_REFERENCE
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsInstanceMethod(),
      DefaultLanguageHighlighterColors.INSTANCE_METHOD
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsInstanceField(),
      DefaultLanguageHighlighterColors.INSTANCE_FIELD
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsStaticMethod(),
      DefaultLanguageHighlighterColors.STATIC_METHOD
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsStaticField(),
      DefaultLanguageHighlighterColors.STATIC_FIELD
    ),

    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsMarkupTag(),
      DefaultLanguageHighlighterColors.MARKUP_TAG
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsMarkupAttribute(),
      DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsMarkupEntity(),
      DefaultLanguageHighlighterColors.MARKUP_ENTITY
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsLanguageDefaultsTemplateLanguage(),
      DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR
    )
  };

  private static final AttributesDescriptor INLINE_PARAMETER_HINT_DESCRIPTOR = new AttributesDescriptor(
    ConfigurableLocalize.optionsJavaAttributeDescriptorInlineParameterHint(),
    DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT
  );

  @Nonnull
  @Override
  public SyntaxHighlighter getHighlighter() {
    return new DefaultSyntaxHighlighter();
  }

  @Nonnull
  @Override
  public String getDemoText() {
    return
      "Bad characters: <bad_char>????</bad_char>\n" +
      "<keyword>Keyword</keyword>\n" +
      "<macro_keyword>#keyword</macro_keyword>\n" +
      "<identifier>Identifier</identifier>\n" +
      "<string>'String <valid_esc_seq>\\n</valid_esc_seq><invalid_esc_seq>\\?</invalid_esc_seq>'</string>\n" +
      "<number>12345</number>\n" +
      "<operation_sign>Operator</operation_sign>\n" +
      "Dot: <dot>.</dot> comma: <comma>,</comma> semicolon: <semicolon>;</semicolon>\n" +
      "<braces>{</braces> Braces <braces>}</braces>\n" +
      "<parenths>(</parenths> Parentheses <parenths>)</parenths>\n" +
      "<brackets>[</brackets> Brackets <brackets>]</brackets>\n" +
      "<line_comment>// Line comment</line_comment>\n" +
      "<block_comment>/* Block comment */</block_comment>\n" +
      "<doc_comment>/** \n" +
      " * Doc comment\n" +
      " * <doc_tag>@tag</doc_tag> <doc_markup><code>Markup</code></doc_markup>\n" +
      " */</doc_comment>\n" +
      "<label>:Label</label>\n" +
      "<predefined>predefined_symbol()</predefined>\n" +
      "<const>CONSTANT</const>\n" +
      "Global <global_var>variable</global_var>\n" +
      "Function <func_decl>declaration</func_decl> (<param>parameter</param>)\n" +
      "    Local <local_var>variable</local_var>\n" +
      "Function <func_call>call</func_call>(" +
      "<parameter_hint p>0, <parameter_hint param> 1, <parameter_hint parameterName> 2" +
      ")\n" +
      "Interface <interface>Name</interface>\n" +
      "Type-alias <type_alias_name>Name</type_alias_name>\n" +
      "<metadata>@Metadata</metadata>\n" +
      "Class <class_name>Name</class_name>\n" +
      "    instance <inst_method>method</inst_method>\n" +
      "    instance <inst_field>field</inst_field>\n" +
      "    static <static_method>method</static_method>\n" +
      "    static <static_field>field</static_field>\n" +
      "\n" +
      "<tag><keyword>@TAG</keyword> <attribute>attribute</attribute>=<string>Value</string></tag>\n" +
      "    Entity: <entity>&amp;</entity>\n" +
      "    <template_language>{% Template language %}</template_language>";
  }

  @Nullable
  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return TAG_HIGHLIGHTING_MAP;
  }

  @Nonnull
  @Override
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ArrayUtil.append(ATTRIBUTES_DESCRIPTORS, INLINE_PARAMETER_HINT_DESCRIPTOR);
  }

  @Nonnull
  @Override
  public ColorDescriptor[] getColorDescriptors() {
    return new ColorDescriptor[0];
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return ConfigurableLocalize.optionsLanguageDefaultsDisplayName().get();
  }

  @Override
  public int getConfigurableWeight() {
    return Integer.MAX_VALUE - 1;
  }
}
