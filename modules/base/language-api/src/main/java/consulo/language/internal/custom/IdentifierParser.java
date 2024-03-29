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

/**
 * @author dsl
 */
public class IdentifierParser extends TokenParser {
  public IdentifierParser() {
  }

  public boolean hasToken(int position) {
    if (!Character.isJavaIdentifierStart(myBuffer.charAt(position))) return false;
    final int start = position;
    for (position++; position < myEndOffset; position++) {
      final char c = myBuffer.charAt(position);
      if (!isIdentifierPart(c)) break;
    }
    IElementType tokenType = CustomHighlighterTokenType.IDENTIFIER;
    myTokenInfo.updateData(start, position, tokenType);
    return true;
  }

  protected boolean isIdentifierPart(final char c) {
    return Character.isJavaIdentifierPart(c) || c == '-';
  }
}
