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
package com.intellij.openapi.util;

import javax.annotation.Nonnull;

/**
 * Text range which asserts its non-negative startOffset and length
 */
public class ProperTextRange extends TextRange {
  public ProperTextRange(int startOffset, int endOffset) {
    super(startOffset, endOffset);
    assertProperRange(this);
  }

  public ProperTextRange(@Nonnull TextRange range) {
    this(range.getStartOffset(), range.getEndOffset());
  }

  public static void assertProperRange(@Nonnull Segment range) throws AssertionError {
    assertProperRange(range, "");
  }

  public static void assertProperRange(@Nonnull Segment range, Object message) throws AssertionError {
    assertProperRange(range.getStartOffset(), range.getEndOffset(), message);
  }

  public static void assertProperRange(int startOffset, int endOffset, Object message) {
    assert startOffset <= endOffset : "Invalid range specified: (" + startOffset + "," + endOffset + "); " + message;
    assert startOffset >= 0 : "Negative start offset: (" + startOffset + "," + endOffset + "); " + message;
  }

  @Nonnull
  @Override
  public ProperTextRange cutOut(@Nonnull TextRange subRange) {
    assert subRange.getStartOffset() <= getLength() : subRange + "; this="+this;
    assert subRange.getEndOffset() <= getLength() : subRange + "; this="+this;
    return new ProperTextRange(getStartOffset() + subRange.getStartOffset(), Math.min(getEndOffset(), getStartOffset() + subRange.getEndOffset()));
  }

  @Nonnull
  @Override
  public ProperTextRange shiftRight(int delta) {
    if (delta == 0) return this;
    return new ProperTextRange(getStartOffset() + delta, getEndOffset() + delta);
  }

  @Nonnull
  @Override
  public ProperTextRange grown(int lengthDelta) {
    if (lengthDelta == 0) return this;
    return new ProperTextRange(getStartOffset(), getEndOffset() + lengthDelta);
  }

  @Override
  public ProperTextRange intersection(@Nonnull TextRange textRange) {
    assertProperRange(textRange);
    TextRange range = super.intersection(textRange);
    if (range == null) return null;
    return new ProperTextRange(range);
  }

  @Nonnull
  @Override
  public ProperTextRange union(@Nonnull TextRange textRange) {
    assertProperRange(textRange);
    TextRange range = super.union(textRange);
    return new ProperTextRange(range);
  }

  @Nonnull
  public static ProperTextRange create(@Nonnull Segment segment) {
    return new ProperTextRange(segment.getStartOffset(), segment.getEndOffset());
  }

  @Nonnull
  public static ProperTextRange create(int startOffset, int endOffset) {
    return new ProperTextRange(startOffset, endOffset);
  }

  @Nonnull
  public static ProperTextRange from(int offset, int length) {
    return new ProperTextRange(offset, offset + length);
  }
}
