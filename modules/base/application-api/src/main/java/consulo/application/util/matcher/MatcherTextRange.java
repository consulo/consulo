/*
 * Copyright 2013-2022 consulo.io
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
package consulo.application.util.matcher;

import jakarta.annotation.Nonnull;
import java.util.Objects;

/**
 * Almost copy&paste from document-api TextRange, we don't need dep to it
 *
 * @author VISTALL
 * @since 04-Apr-22
 */
public class MatcherTextRange {
  @Nonnull
  public static MatcherTextRange from(int offset, int length) {
    return create(offset, offset + length);
  }

  @Nonnull
  public static MatcherTextRange create(int startOffset, int endOffset) {
    return new MatcherTextRange(startOffset, endOffset);
  }

  private final int myStartOffset;
  private final int myEndOffset;

  MatcherTextRange(int startOffset, int endOffset) {
    myStartOffset = startOffset;
    myEndOffset = endOffset;
  }

  public boolean intersectsStrict(int startOffset, int endOffset) {
    return Math.max(myStartOffset, startOffset) < Math.min(myEndOffset, endOffset);
  }

  public int getStartOffset() {
    return myStartOffset;
  }

  public int getEndOffset() {
    return myEndOffset;
  }

  public final int getLength() {
    return myEndOffset - myStartOffset;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatcherTextRange textRange = (MatcherTextRange)o;
    return myStartOffset == textRange.myStartOffset && myEndOffset == textRange.myEndOffset;
  }

  @Override
  public int hashCode() {
    return Objects.hash(myStartOffset, myEndOffset);
  }

  public MatcherTextRange shiftRight(int delta) {
    if (delta == 0) return this;
    return new MatcherTextRange(myStartOffset + delta, myEndOffset + delta);
  }

  @Override
  public String toString() {
    return "TextRange{" + "myStartOffset=" + myStartOffset + ", myEndOffset=" + myEndOffset + '}';
  }
}
