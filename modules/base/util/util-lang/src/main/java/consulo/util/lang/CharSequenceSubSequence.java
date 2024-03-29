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

package consulo.util.lang;

import jakarta.annotation.Nonnull;

public class CharSequenceSubSequence implements CharSequence {
  private final CharSequence myChars;
  private final int myStart;
  private final int myEnd;

  public CharSequenceSubSequence(@Nonnull CharSequence chars) {
    this(chars, 0, chars.length());
  }

  public CharSequenceSubSequence(@Nonnull CharSequence chars, int start, int end) {
    if (start < 0 || end > chars.length() || start > end) {
      throw new IndexOutOfBoundsException("chars sequence.length:" + chars.length() +
                                          ", start:" + start +
                                          ", end:" + end);
    }
    myChars = chars;
    myStart = start;
    myEnd = end;
  }

  @Override
  public final int length() {
    return myEnd - myStart;
  }

  @Override
  public final char charAt(int index) {
    return myChars.charAt(index + myStart);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    if (start == myStart && end == myEnd) return this;
    return new CharSequenceSubSequence(myChars, myStart + start, myStart + end);
  }

  @Nonnull
  public String toString() {
    if (myChars instanceof String) return ((String)myChars).substring(myStart, myEnd);
    return new String(CharArrayUtil.fromSequence(myChars, myStart, myEnd));
  }

  public CharSequence getBaseSequence() {
    return myChars;
  }
}
