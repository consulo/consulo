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
package consulo.diff.old;

import consulo.document.util.TextRange;
import consulo.annotation.DeprecationInfo;
import consulo.logging.Logger;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;

@Deprecated(forRemoval = true)
@DeprecationInfo("Old diff impl, must be removed")
public class Word {
  private static final Logger LOG = Logger.getInstance(Word.class);
  @Nonnull
  private final DiffString myBaseText;
  @Nonnull
  private final TextRange myRange;
  @Nonnull
  private final DiffString myText;

  @TestOnly
  public Word(@Nonnull String baseText, @Nonnull TextRange range) {
    this(DiffString.create(baseText), range);
  }

  public Word(@Nonnull DiffString baseText, @Nonnull TextRange range) {
    myBaseText = baseText;
    myRange = range;
    myText = myBaseText.substring(myRange.getStartOffset(), myRange.getEndOffset());
    LOG.assertTrue(myRange.getStartOffset() >= 0);
    LOG.assertTrue(myRange.getEndOffset() >= myRange.getStartOffset(), myRange);
  }

  public int hashCode() {
    return myText.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof Word)) return false;
    Word other = (Word)obj;
    return getText().equals(other.getText());
  }

  @Nonnull
  public DiffString getText() {
    return myText;
  }

  @Nonnull
  public DiffString getPrefix(int fromPosition) {
    LOG.assertTrue(fromPosition >= 0, fromPosition);
    int wordStart = myRange.getStartOffset();
    LOG.assertTrue(fromPosition <= wordStart, fromPosition + " " + wordStart);
    return myBaseText.substring(fromPosition, wordStart);
  }

  public int getEnd() {
    return myRange.getEndOffset();
  }

  public int getStart() {
    return myRange.getStartOffset();
  }

  public String toString() {
    return myText.toString();
  }

  public boolean isWhitespace() {
    return false;
  }

  public boolean atEndOfLine() {
    int start = myRange.getStartOffset();
    if (start == 0) return true;
    if (myBaseText.charAt(start - 1) == '\n') return true;
    int end = myRange.getEndOffset();
    if (end == myBaseText.length()) return true;
    if (myBaseText.charAt(end) == '\n') return true;
    return false;
  }
}
