/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.formatting;

import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 01-May-17
 * <p>
 * from kotlin platform\lang-impl\src\com\intellij\formatting\FormatTextRanges.kt
 */
public class FormatRangesStorage {
  private List<FormatTextRange> rangesByStartOffset = new ArrayList<>();

  public void add(TextRange range, boolean processHeadingWhitespace) {
    FormatTextRange formatRange = new FormatTextRange(range, processHeadingWhitespace);
    rangesByStartOffset.add(formatRange);
  }

  public boolean isWhiteSpaceReadOnly(TextRange range) {
    return ContainerUtil.find(rangesByStartOffset, it -> !it.isWhitespaceReadOnly(range)) == null;
  }

  public boolean isReadOnly(TextRange range) {
    return ContainerUtil.find(rangesByStartOffset, it -> !it.isReadOnly(range)) == null;
  }

  public List<FormatTextRange> getRanges() {
    return rangesByStartOffset;
  }

  public boolean isEmpty() {
    return rangesByStartOffset.isEmpty();
  }
}
