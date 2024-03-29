/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.ui.console;

import consulo.colorScheme.TextAttributes;
import consulo.document.Document;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public interface FilterMixin {
  boolean shouldRunHeavy();

  void applyHeavyFilter(@Nonnull Document copiedFragment, int startOffset, int startLineNumber, @Nonnull Consumer<? super AdditionalHighlight> consumer);

  @Nonnull
  String getUpdateMessage();

  class AdditionalHighlight extends Filter.Result {
    public AdditionalHighlight(int start, int end) {
      super(start, end, null);
    }

    public AdditionalHighlight(@Nonnull List<? extends Filter.ResultItem> resultItems) {
      super(resultItems);
    }

    @Deprecated
    public int getStart() {
      return getHighlightStartOffset();
    }

    @Deprecated
    public int getEnd() {
      return getHighlightEndOffset();
    }

    @Nullable
    public TextAttributes getTextAttributes(@Nullable final TextAttributes source) {
      return null;
    }
  }
}
