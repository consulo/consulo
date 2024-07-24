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
package consulo.codeEditor;

import consulo.colorScheme.TextAttributesKey;

/**
 * Base highlighter colors for multiple languages.
 *
 * @author Rustam Vishnyakov
 */
public interface DefaultLanguageHighlighterColors {

  TextAttributesKey TEMPLATE_LANGUAGE_COLOR = TextAttributesKey.of("DEFAULT_TEMPLATE_LANGUAGE_COLOR", HighlighterColors.TEXT);
  TextAttributesKey IDENTIFIER = TextAttributesKey.of("DEFAULT_IDENTIFIER", HighlighterColors.TEXT);
  TextAttributesKey NUMBER = TextAttributesKey.of("DEFAULT_NUMBER");
  TextAttributesKey KEYWORD = TextAttributesKey.of("DEFAULT_KEYWORD");
  TextAttributesKey MACRO_KEYWORD = TextAttributesKey.of("DEFAULT_MACRO_KEYWORD");
  TextAttributesKey STRING = TextAttributesKey.of("DEFAULT_STRING");
  TextAttributesKey BLOCK_COMMENT = TextAttributesKey.of("DEFAULT_BLOCK_COMMENT");
  TextAttributesKey LINE_COMMENT = TextAttributesKey.of("DEFAULT_LINE_COMMENT");
  TextAttributesKey DOC_COMMENT = TextAttributesKey.of("DEFAULT_DOC_COMMENT");
  TextAttributesKey OPERATION_SIGN = TextAttributesKey.of("DEFAULT_OPERATION_SIGN");
  TextAttributesKey BRACES = TextAttributesKey.of("DEFAULT_BRACES");
  TextAttributesKey DOT = TextAttributesKey.of("DEFAULT_DOT");
  TextAttributesKey SEMICOLON = TextAttributesKey.of("DEFAULT_SEMICOLON");
  TextAttributesKey COMMA = TextAttributesKey.of("DEFAULT_COMMA");
  TextAttributesKey PARENTHESES = TextAttributesKey.of("DEFAULT_PARENTHS");
  TextAttributesKey BRACKETS = TextAttributesKey.of("DEFAULT_BRACKETS");

  TextAttributesKey LABEL = TextAttributesKey.of("DEFAULT_LABEL", IDENTIFIER);
  TextAttributesKey CONSTANT = TextAttributesKey.of("DEFAULT_CONSTANT", IDENTIFIER);
  TextAttributesKey LOCAL_VARIABLE = TextAttributesKey.of("DEFAULT_LOCAL_VARIABLE", IDENTIFIER);
  TextAttributesKey GLOBAL_VARIABLE = TextAttributesKey.of("DEFAULT_GLOBAL_VARIABLE", IDENTIFIER);

  TextAttributesKey FUNCTION_DECLARATION = TextAttributesKey.of("DEFAULT_FUNCTION_DECLARATION", IDENTIFIER);
  TextAttributesKey FUNCTION_CALL = TextAttributesKey.of("DEFAULT_FUNCTION_CALL", IDENTIFIER);
  TextAttributesKey PARAMETER = TextAttributesKey.of("DEFAULT_PARAMETER", IDENTIFIER);
  TextAttributesKey TYPE_ALIAS_NAME = TextAttributesKey.of("DEFAULT_TYPE_ALIAS_NAME", IDENTIFIER);
  TextAttributesKey CLASS_NAME = TextAttributesKey.of("DEFAULT_CLASS_NAME", IDENTIFIER);
  TextAttributesKey INTERFACE_NAME = TextAttributesKey.of("DEFAULT_INTERFACE_NAME", IDENTIFIER);
  TextAttributesKey CLASS_REFERENCE = TextAttributesKey.of("DEFAULT_CLASS_REFERENCE", IDENTIFIER);
  TextAttributesKey INSTANCE_METHOD = TextAttributesKey.of("DEFAULT_INSTANCE_METHOD", FUNCTION_DECLARATION);
  TextAttributesKey INSTANCE_FIELD = TextAttributesKey.of("DEFAULT_INSTANCE_FIELD", IDENTIFIER);
  TextAttributesKey STATIC_METHOD = TextAttributesKey.of("DEFAULT_STATIC_METHOD", FUNCTION_DECLARATION);
  TextAttributesKey STATIC_FIELD = TextAttributesKey.of("DEFAULT_STATIC_FIELD", IDENTIFIER);

  TextAttributesKey DOC_COMMENT_MARKUP = TextAttributesKey.of("DEFAULT_DOC_MARKUP");
  TextAttributesKey DOC_COMMENT_TAG = TextAttributesKey.of("DEFAULT_DOC_COMMENT_TAG");
  TextAttributesKey DOC_COMMENT_TAG_VALUE = TextAttributesKey.of("DEFAULT_DOC_COMMENT_TAG_VALUE");
  TextAttributesKey VALID_STRING_ESCAPE = TextAttributesKey.of("DEFAULT_VALID_STRING_ESCAPE");
  TextAttributesKey INVALID_STRING_ESCAPE = TextAttributesKey.of("DEFAULT_INVALID_STRING_ESCAPE");

  TextAttributesKey PREDEFINED_SYMBOL = TextAttributesKey.of("DEFAULT_PREDEFINED_SYMBOL", IDENTIFIER);

  TextAttributesKey METADATA = TextAttributesKey.of("DEFAULT_METADATA", HighlighterColors.TEXT);

  TextAttributesKey MARKUP_TAG = TextAttributesKey.of("DEFAULT_TAG", HighlighterColors.TEXT);
  TextAttributesKey MARKUP_ATTRIBUTE = TextAttributesKey.of("DEFAULT_ATTRIBUTE", IDENTIFIER);
  TextAttributesKey MARKUP_ENTITY = TextAttributesKey.of("DEFAULT_ENTITY", IDENTIFIER);
  TextAttributesKey INLINE_PARAMETER_HINT = TextAttributesKey.of("INLINE_PARAMETER_HINT");
}
