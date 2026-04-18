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
package consulo.language.internal.custom;

import consulo.language.ast.IElementType;
import consulo.language.lexer.LexerBase;
import consulo.util.collection.ArrayUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * @author dsl
 */
public class AbstractCustomLexer extends LexerBase {
  protected CharSequence myBuffer = ArrayUtil.EMPTY_CHAR_SEQUENCE;
  protected int myStartOffset = 0;
  protected int myEndOffset = 0;
  private final TokenParser[] myTokenParsers;
  private @Nullable TokenInfo myCurrentToken = null;
  private int myPosition;

  public AbstractCustomLexer(List<TokenParser> tokenParsers) {
    myTokenParsers = tokenParsers.toArray(new TokenParser[tokenParsers.size()]);
  }

  public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
    myBuffer = buffer;
    myStartOffset = startOffset;
    myEndOffset = endOffset;
    myPosition = myStartOffset;
    myCurrentToken = new TokenInfo();
    for (TokenParser tokenParser : myTokenParsers) {
      tokenParser.setBuffer(myBuffer, myStartOffset, myEndOffset);
    }
    advance();
  }

  public int getState() {
    return 0;
  }

  public @Nullable IElementType getTokenType() {
    return Objects.requireNonNull(myCurrentToken).getType();
  }

  public int getTokenStart() {
    return Objects.requireNonNull(myCurrentToken).getStart();
  }

  public int getTokenEnd() {
    return Objects.requireNonNull(myCurrentToken).getEnd();
  }

  public void advance() {
    Objects.requireNonNull(myCurrentToken);
    if (myPosition >= myEndOffset) {
      myCurrentToken.updateData(myPosition, myPosition, null);
      return;
    }
    boolean tokenFound = false;
    for (TokenParser tokenParser : myTokenParsers) {
      if (tokenParser.hasToken(myPosition)) {
        tokenParser.getTokenInfo(myCurrentToken);
        if (myCurrentToken.getEnd() <= myCurrentToken.getStart()) {
          throw new AssertionError(tokenParser);
        }
        tokenFound = true;
        break;
      }
    }

    if (!tokenFound) {
      myCurrentToken.updateData(myPosition, myPosition + 1, CustomHighlighterTokenType.CHARACTER);
    }
    myPosition = myCurrentToken.getEnd();
  }

  public CharSequence getBufferSequence() {
    return myBuffer;
  }

  public int getBufferEnd() {
    return myEndOffset;
  }
}
