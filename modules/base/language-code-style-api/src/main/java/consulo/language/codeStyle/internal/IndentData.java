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

package consulo.language.codeStyle.internal;

import consulo.language.codeStyle.IndentInfo;
import consulo.util.xml.serializer.InvalidDataException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class IndentData {
  private final int myIndentSpaces;
  private final int mySpaces;

  public IndentData(int indentSpaces, int spaces) {
    assert indentSpaces >= 0 : "Indent spaces can't be negative";
    myIndentSpaces = indentSpaces;
    mySpaces = spaces;
  }

  public IndentData(int indentSpaces) {
    this(indentSpaces, 0);
  }

  public int getTotalSpaces() {
    return mySpaces + myIndentSpaces;
  }

  public int getIndentSpaces() {
    return myIndentSpaces;
  }

  public int getSpaces() {
    return mySpaces;
  }

  public IndentData add(IndentData childOffset) {
    return new IndentData(myIndentSpaces + childOffset.getIndentSpaces(), mySpaces + childOffset.getSpaces());
  }

  public IndentData add(WhiteSpace whiteSpace) {
    return new IndentData(myIndentSpaces + whiteSpace.getIndentOffset(), mySpaces + whiteSpace.getSpaces());
  }

  public boolean isEmpty() {
    return myIndentSpaces == 0 && mySpaces == 0;
  }

  public IndentInfo createIndentInfo() {
    return new IndentInfo(0, myIndentSpaces, mySpaces);
  }

  @Override
  public String toString() {
    return "spaces=" + mySpaces + ", indent spaces=" + myIndentSpaces;
  }

  public static IndentData createFrom(@Nonnull CharSequence chars, int startOffset, int endOffset, int tabSize) {
    assert tabSize > 0 : "Invalid tab size: " + tabSize;
    int indent = 0;
    int alignment = 0;
    boolean hasTabs = false;
    boolean isInAlignmentArea = false;
    for (int i = startOffset; i < Math.min(chars.length(), endOffset); i++) {
      char c = chars.charAt(i);
      switch (c) {
        case ' ':
          if (hasTabs) {
            isInAlignmentArea = true;
            alignment++;
          }
          else {
            indent++;
          }
          break;
        case '\t':
          if (isInAlignmentArea) {
            alignment = (alignment / tabSize + 1) * tabSize;
          }
          else {
            hasTabs = true;
            indent += tabSize;
          }
          break;
        default:
          throw new InvalidDataException("Unexpected indent character: '" + c + "'");
      }
    }
    return new IndentData(indent, alignment);
  }

  @Nullable
  public static IndentData min(@Nullable IndentData first, @Nullable IndentData second) {
    if (first == null) return second;
    if (second == null) return first;
    return first.getTotalSpaces() < second.getTotalSpaces() ? first : second;
  }
}
