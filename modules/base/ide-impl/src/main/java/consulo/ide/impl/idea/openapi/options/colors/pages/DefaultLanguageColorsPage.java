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
import consulo.language.editor.highlight.DefaultSyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.configurable.OptionsBundle;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.configurable.internal.ConfigurableWeight;

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
      OptionsBundle.message("options.java.attribute.descriptor.bad.character"), HighlighterColors.BAD_CHARACTER),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.keyword"), DefaultLanguageHighlighterColors.KEYWORD),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.macro.keyword"), DefaultLanguageHighlighterColors.MACRO_KEYWORD),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.identifier"), DefaultLanguageHighlighterColors.IDENTIFIER),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.string"), DefaultLanguageHighlighterColors.STRING),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.valid.esc.seq"), DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.invalid.esc.seq"), DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.number"), DefaultLanguageHighlighterColors.NUMBER),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.operation"), DefaultLanguageHighlighterColors.OPERATION_SIGN),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.braces"), DefaultLanguageHighlighterColors.BRACES),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.parentheses"), DefaultLanguageHighlighterColors.PARENTHESES),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.brackets"), DefaultLanguageHighlighterColors.BRACKETS),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.dot"), DefaultLanguageHighlighterColors.DOT),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.comma"), DefaultLanguageHighlighterColors.COMMA),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.semicolon"), DefaultLanguageHighlighterColors.SEMICOLON),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.line.comment"), DefaultLanguageHighlighterColors.LINE_COMMENT),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.block.comment"), DefaultLanguageHighlighterColors.BLOCK_COMMENT),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.doc.comment"), DefaultLanguageHighlighterColors.DOC_COMMENT),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.doc.markup"), DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.doc.tag"), DefaultLanguageHighlighterColors.DOC_COMMENT_TAG),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.label"), DefaultLanguageHighlighterColors.LABEL),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.constant"), DefaultLanguageHighlighterColors.CONSTANT),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.predefined"), DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.local.variable"), DefaultLanguageHighlighterColors.LOCAL_VARIABLE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.global.variable"), DefaultLanguageHighlighterColors.GLOBAL_VARIABLE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.function.declaration"), DefaultLanguageHighlighterColors.FUNCTION_DECLARATION),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.function.call"), DefaultLanguageHighlighterColors.FUNCTION_CALL),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.parameter"), DefaultLanguageHighlighterColors.PARAMETER),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.interface.name"), DefaultLanguageHighlighterColors.INTERFACE_NAME),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.type.alias.name"), DefaultLanguageHighlighterColors.TYPE_ALIAS_NAME),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.metadata"), DefaultLanguageHighlighterColors.METADATA),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.class.name"), DefaultLanguageHighlighterColors.CLASS_NAME),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.class.reference"), DefaultLanguageHighlighterColors.CLASS_REFERENCE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.instance.method"), DefaultLanguageHighlighterColors.INSTANCE_METHOD),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.instance.field"), DefaultLanguageHighlighterColors.INSTANCE_FIELD),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.static.method"), DefaultLanguageHighlighterColors.STATIC_METHOD),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.static.field"), DefaultLanguageHighlighterColors.STATIC_FIELD),

    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.markup.tag"), DefaultLanguageHighlighterColors.MARKUP_TAG),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.markup.attribute"), DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.markup.entity"), DefaultLanguageHighlighterColors.MARKUP_ENTITY),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.template.language"), DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR),
    
  };

  private static final AttributesDescriptor INLINE_PARAMETER_HINT_DESCRIPTOR = new AttributesDescriptor(
          OptionsBundle.message("options.java.attribute.descriptor.inline.parameter.hint"),
          DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT);

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
    return OptionsBundle.message("options.language.defaults.display.name");
  }

  @Override
  public int getConfigurableWeight() {
    return Integer.MAX_VALUE - 1;
  }
}
