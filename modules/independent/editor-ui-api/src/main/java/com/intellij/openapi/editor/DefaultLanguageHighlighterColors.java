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
package com.intellij.openapi.editor;

import com.intellij.openapi.editor.colors.TextAttributesKey;

/**
 * Base highlighter colors for multiple languages.
 *
 * @author Rustam Vishnyakov
 */
public interface DefaultLanguageHighlighterColors {

  TextAttributesKey TEMPLATE_LANGUAGE_COLOR = TextAttributesKey.createTextAttributesKey("DEFAULT_TEMPLATE_LANGUAGE_COLOR", HighlighterColors.TEXT);
  TextAttributesKey IDENTIFIER = TextAttributesKey.createTextAttributesKey("DEFAULT_IDENTIFIER", HighlighterColors.TEXT);
  TextAttributesKey NUMBER = TextAttributesKey.createTextAttributesKey("DEFAULT_NUMBER");
  TextAttributesKey KEYWORD = TextAttributesKey.createTextAttributesKey("DEFAULT_KEYWORD");
  TextAttributesKey MACRO_KEYWORD = TextAttributesKey.createTextAttributesKey("DEFAULT_MACRO_KEYWORD");
  TextAttributesKey STRING = TextAttributesKey.createTextAttributesKey("DEFAULT_STRING");
  TextAttributesKey BLOCK_COMMENT = TextAttributesKey.createTextAttributesKey("DEFAULT_BLOCK_COMMENT");
  TextAttributesKey LINE_COMMENT = TextAttributesKey.createTextAttributesKey("DEFAULT_LINE_COMMENT");
  TextAttributesKey DOC_COMMENT = TextAttributesKey.createTextAttributesKey("DEFAULT_DOC_COMMENT");
  TextAttributesKey OPERATION_SIGN = TextAttributesKey.createTextAttributesKey("DEFAULT_OPERATION_SIGN");
  TextAttributesKey BRACES = TextAttributesKey.createTextAttributesKey("DEFAULT_BRACES");
  TextAttributesKey DOT = TextAttributesKey.createTextAttributesKey("DEFAULT_DOT");
  TextAttributesKey SEMICOLON = TextAttributesKey.createTextAttributesKey("DEFAULT_SEMICOLON");
  TextAttributesKey COMMA = TextAttributesKey.createTextAttributesKey("DEFAULT_COMMA");
  TextAttributesKey PARENTHESES = TextAttributesKey.createTextAttributesKey("DEFAULT_PARENTHS");
  TextAttributesKey BRACKETS = TextAttributesKey.createTextAttributesKey("DEFAULT_BRACKETS");

  TextAttributesKey LABEL = TextAttributesKey.createTextAttributesKey("DEFAULT_LABEL", IDENTIFIER);
  TextAttributesKey CONSTANT = TextAttributesKey.createTextAttributesKey("DEFAULT_CONSTANT", IDENTIFIER);
  TextAttributesKey LOCAL_VARIABLE = TextAttributesKey.createTextAttributesKey("DEFAULT_LOCAL_VARIABLE", IDENTIFIER);
  TextAttributesKey GLOBAL_VARIABLE = TextAttributesKey.createTextAttributesKey("DEFAULT_GLOBAL_VARIABLE", IDENTIFIER);

  TextAttributesKey FUNCTION_DECLARATION = TextAttributesKey.createTextAttributesKey("DEFAULT_FUNCTION_DECLARATION", IDENTIFIER);
  TextAttributesKey FUNCTION_CALL = TextAttributesKey.createTextAttributesKey("DEFAULT_FUNCTION_CALL", IDENTIFIER);
  TextAttributesKey PARAMETER = TextAttributesKey.createTextAttributesKey("DEFAULT_PARAMETER", IDENTIFIER);
  TextAttributesKey TYPE_ALIAS_NAME = TextAttributesKey.createTextAttributesKey("DEFAULT_TYPE_ALIAS_NAME", IDENTIFIER);
  TextAttributesKey CLASS_NAME = TextAttributesKey.createTextAttributesKey("DEFAULT_CLASS_NAME", IDENTIFIER);
  TextAttributesKey INTERFACE_NAME = TextAttributesKey.createTextAttributesKey("DEFAULT_INTERFACE_NAME", IDENTIFIER);
  TextAttributesKey CLASS_REFERENCE = TextAttributesKey.createTextAttributesKey("DEFAULT_CLASS_REFERENCE", IDENTIFIER);
  TextAttributesKey INSTANCE_METHOD = TextAttributesKey.createTextAttributesKey("DEFAULT_INSTANCE_METHOD", FUNCTION_DECLARATION);
  TextAttributesKey INSTANCE_FIELD = TextAttributesKey.createTextAttributesKey("DEFAULT_INSTANCE_FIELD", IDENTIFIER);
  TextAttributesKey STATIC_METHOD = TextAttributesKey.createTextAttributesKey("DEFAULT_STATIC_METHOD", FUNCTION_DECLARATION);
  TextAttributesKey STATIC_FIELD = TextAttributesKey.createTextAttributesKey("DEFAULT_STATIC_FIELD", IDENTIFIER);

  TextAttributesKey DOC_COMMENT_MARKUP = TextAttributesKey.createTextAttributesKey("DEFAULT_DOC_MARKUP");
  TextAttributesKey DOC_COMMENT_TAG = TextAttributesKey.createTextAttributesKey("DEFAULT_DOC_COMMENT_TAG");
  TextAttributesKey DOC_COMMENT_TAG_VALUE = TextAttributesKey.createTextAttributesKey("DEFAULT_DOC_COMMENT_TAG_VALUE");
  TextAttributesKey VALID_STRING_ESCAPE = TextAttributesKey.createTextAttributesKey("DEFAULT_VALID_STRING_ESCAPE");
  TextAttributesKey INVALID_STRING_ESCAPE = TextAttributesKey.createTextAttributesKey("DEFAULT_INVALID_STRING_ESCAPE");

  TextAttributesKey PREDEFINED_SYMBOL = TextAttributesKey.createTextAttributesKey("DEFAULT_PREDEFINED_SYMBOL", IDENTIFIER);

  TextAttributesKey METADATA = TextAttributesKey.createTextAttributesKey("DEFAULT_METADATA", HighlighterColors.TEXT);

  TextAttributesKey MARKUP_TAG = TextAttributesKey.createTextAttributesKey("DEFAULT_TAG", HighlighterColors.TEXT);
  TextAttributesKey MARKUP_ATTRIBUTE = TextAttributesKey.createTextAttributesKey("DEFAULT_ATTRIBUTE", IDENTIFIER);
  TextAttributesKey MARKUP_ENTITY = TextAttributesKey.createTextAttributesKey("DEFAULT_ENTITY", IDENTIFIER);
  TextAttributesKey INLINE_PARAMETER_HINT = TextAttributesKey.createTextAttributesKey("INLINE_PARAMETER_HINT");
}
