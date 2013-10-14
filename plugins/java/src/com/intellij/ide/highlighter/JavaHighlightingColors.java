/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.highlighter;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

/**
 * Highlighting text attributes for Java language.
 *
 * @author Rustam Vishnyakov
 */
public interface JavaHighlightingColors {
  TextAttributesKey LINE_COMMENT =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.LINE_COMMENT);
  TextAttributesKey JAVA_BLOCK_COMMENT =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.BLOCK_COMMENT);
  TextAttributesKey DOC_COMMENT =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.DOC_COMMENT);
  TextAttributesKey NUMBER = TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.NUMBER);
  TextAttributesKey STRING = TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.STRING);
  TextAttributesKey OPERATION_SIGN =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.OPERATION_SIGN);
  TextAttributesKey PARENTHS =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.PARENTHESES);
  TextAttributesKey BRACKETS = TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.BRACKETS);
  TextAttributesKey BRACES = TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.BRACES);
  TextAttributesKey COMMA = TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.COMMA);
  TextAttributesKey DOT = TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.DOT);
  TextAttributesKey JAVA_SEMICOLON =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.SEMICOLON);
  TextAttributesKey DOC_COMMENT_TAG =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.DOC_COMMENT_TAG);
  TextAttributesKey DOC_COMMENT_MARKUP =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP);
  TextAttributesKey VALID_STRING_ESCAPE =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
  TextAttributesKey INVALID_STRING_ESCAPE =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);
  TextAttributesKey KEYWORD = TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.KEYWORD);

  TextAttributesKey ANNOTATION_NAME =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.METADATA);
  TextAttributesKey ENUM_NAME =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.CLASS_NAME);
  TextAttributesKey CLASS_NAME =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.CLASS_NAME);
  TextAttributesKey INTERFACE_NAME =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.CLASS_NAME);
  TextAttributesKey ANONYMOUS_CLASS_NAME =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.CLASS_NAME);
  TextAttributesKey TYPE_PARAMETER_NAME =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.TYPE_ALIAS_NAME);
  TextAttributesKey ABSTRACT_CLASS_NAME =
    TextAttributesKey.createTextAttributesKey(JavaLanguage.INSTANCE, DefaultLanguageHighlighterColors.CLASS_NAME);
}
