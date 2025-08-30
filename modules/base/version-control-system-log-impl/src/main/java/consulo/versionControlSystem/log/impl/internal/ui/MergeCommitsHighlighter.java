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
package consulo.versionControlSystem.log.impl.internal.ui;

import consulo.versionControlSystem.log.internal.VcsLogColors;
import consulo.versionControlSystem.log.*;
import jakarta.annotation.Nonnull;

public class MergeCommitsHighlighter implements VcsLogHighlighter {
  @Nonnull
  private final VcsLogUi myLogUi;

  public MergeCommitsHighlighter(@Nonnull VcsLogUi logUi) {
    myLogUi = logUi;
  }

  @Nonnull
  @Override
  public VcsCommitStyle getStyle(@Nonnull VcsShortCommitDetails details, boolean isSelected) {
    if (isSelected || !myLogUi.isHighlighterEnabled(MergeCommitsHighlighterFactory.ID)) return VcsCommitStyle.DEFAULT;
    if (details.getParents().size() >= 2) return VcsCommitStyleFactory.foreground(VcsLogColors.MERGE_COMMIT_FOREGROUND);
    return VcsCommitStyle.DEFAULT;
  }

  @Override
  public void update(@Nonnull VcsLogDataPack dataPack, boolean refreshHappened) {
  }
}
