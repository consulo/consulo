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
package consulo.ide.impl.idea.ide.highlighter.custom;

import consulo.language.internal.custom.CustomFileTypeLexer;
import consulo.language.lexer.Lexer;
import consulo.language.lexer.LayeredLexer;
import consulo.language.lexer.StringLiteralLexer;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.internal.custom.CustomHighlighterTokenType;
import consulo.language.ast.StringEscapesTokenTypes;
import consulo.language.ast.IElementType;
import consulo.language.internal.custom.SyntaxTable;

import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;

public class CustomFileHighlighter extends SyntaxHighlighterBase {
  private static final Map<IElementType, TextAttributesKey> ourKeys;
  private final SyntaxTable myTable;

  public CustomFileHighlighter(SyntaxTable table) {
    myTable = table;
  }

  static {
    ourKeys = new HashMap<IElementType, TextAttributesKey>();

    ourKeys.put(CustomHighlighterTokenType.KEYWORD_1, CustomHighlighterColors.CUSTOM_KEYWORD1_ATTRIBUTES);
    ourKeys.put(CustomHighlighterTokenType.KEYWORD_2, CustomHighlighterColors.CUSTOM_KEYWORD2_ATTRIBUTES);
    ourKeys.put(CustomHighlighterTokenType.KEYWORD_3, CustomHighlighterColors.CUSTOM_KEYWORD3_ATTRIBUTES);
    ourKeys.put(CustomHighlighterTokenType.KEYWORD_4, CustomHighlighterColors.CUSTOM_KEYWORD4_ATTRIBUTES);
    ourKeys.put(CustomHighlighterTokenType.NUMBER, CustomHighlighterColors.CUSTOM_NUMBER_ATTRIBUTES);
    ourKeys.put(CustomHighlighterTokenType.STRING, CustomHighlighterColors.CUSTOM_STRING_ATTRIBUTES);
    ourKeys.put(CustomHighlighterTokenType.SINGLE_QUOTED_STRING, CustomHighlighterColors.CUSTOM_STRING_ATTRIBUTES);
    ourKeys.put(StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN, CustomHighlighterColors.CUSTOM_VALID_STRING_ESCAPE);
    ourKeys.put(StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN, CustomHighlighterColors.CUSTOM_INVALID_STRING_ESCAPE);
    ourKeys.put(StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN, CustomHighlighterColors.CUSTOM_INVALID_STRING_ESCAPE);
    ourKeys.put(CustomHighlighterTokenType.LINE_COMMENT, CustomHighlighterColors.CUSTOM_LINE_COMMENT_ATTRIBUTES);
    ourKeys.put(CustomHighlighterTokenType.MULTI_LINE_COMMENT,
                CustomHighlighterColors.CUSTOM_MULTI_LINE_COMMENT_ATTRIBUTES);
  }

  @Override
  @Nonnull
  public Lexer getHighlightingLexer() {
    Lexer customFileTypeLexer = new CustomFileTypeLexer(myTable, true);
    if (myTable.isHasStringEscapes()) {
      customFileTypeLexer = new LayeredLexer(customFileTypeLexer);
      ((LayeredLexer)customFileTypeLexer).registerSelfStoppingLayer(new StringLiteralLexer('\"', CustomHighlighterTokenType.STRING,true,"x"),
                                new IElementType[]{CustomHighlighterTokenType.STRING}, IElementType.EMPTY_ARRAY);
      ((LayeredLexer)customFileTypeLexer).registerSelfStoppingLayer(new StringLiteralLexer('\'', CustomHighlighterTokenType.STRING,true,"x"),
                                new IElementType[]{CustomHighlighterTokenType.SINGLE_QUOTED_STRING}, IElementType.EMPTY_ARRAY);
    }
    return customFileTypeLexer;
  }

  @Override
  @Nonnull
  public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    return pack(ourKeys.get(tokenType));
  }

  public static Map<IElementType, TextAttributesKey> getKeys() {
    return ourKeys;
  }
}
