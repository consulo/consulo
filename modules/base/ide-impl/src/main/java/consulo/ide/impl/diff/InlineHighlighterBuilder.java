/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.diff;

import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.TextAttributes;
import consulo.diff.util.TextDiffType;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

public class InlineHighlighterBuilder {
  @Nonnull
  private final Editor editor;
  @Nonnull
  private final TextDiffType type;
  private final int start;
  private final int end;

  InlineHighlighterBuilder(@Nonnull Editor editor, int start, int end, @Nonnull TextDiffType type) {
    this.editor = editor;
    this.type = type;
    this.start = start;
    this.end = end;
  }

  @Nonnull
  public List<RangeHighlighter> done() {
    TextAttributes attributes = DiffDrawUtil.getTextAttributes(type, editor, false);

    RangeHighlighter highlighter =
      editor.getMarkupModel().addRangeHighlighter(start, end, DiffDrawUtil.INLINE_LAYER, attributes, HighlighterTargetArea.EXACT_RANGE);

    if (start == end) installEmptyRangeRenderer(highlighter, type);

    return Collections.singletonList(highlighter);
  }

  private static void installEmptyRangeRenderer(@Nonnull RangeHighlighter highlighter, @Nonnull TextDiffType type) {
    highlighter.setCustomRenderer(new DiffEmptyHighlighterRenderer(type));
  }
}
