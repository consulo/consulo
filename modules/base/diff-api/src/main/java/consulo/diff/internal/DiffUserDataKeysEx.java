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
package consulo.diff.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.LogicalPosition;
import consulo.diff.DiffNavigationContext;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.fragment.LineFragment;
import consulo.diff.merge.MergeResult;
import consulo.diff.merge.MergeTool;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public interface DiffUserDataKeysEx extends DiffUserDataKeys {
  Key<String> FILE_NAME = Key.create("Diff.FileName");

  //
  // DiffRequest
  //
  enum ScrollToPolicy {
    FIRST_CHANGE, LAST_CHANGE;

    @Nullable
    public <T> T select(@Nonnull List<T> changes) {
      if (this == FIRST_CHANGE) return ContainerUtil.getFirstItem(changes);
      if (this == LAST_CHANGE) return ContainerUtil.getLastItem(changes);
      throw new IllegalStateException();
    }
  }

  Key<ScrollToPolicy> SCROLL_TO_CHANGE = Key.create("Diff.ScrollToChange");
  Key<LogicalPosition[]> EDITORS_CARET_POSITION = Key.create("Diff.EditorsCaretPosition");

  Key<DiffNavigationContext> NAVIGATION_CONTEXT = Key.create("Diff.NavigationContext");

  interface DiffComputer {
    @Nonnull
    List<LineFragment> compute(@Nonnull CharSequence text1,
                               @Nonnull CharSequence text2,
                               @Nonnull ComparisonPolicy policy,
                               boolean innerChanges,
                               @Nonnull ProgressIndicator indicator);
  }
  Key<DiffComputer> CUSTOM_DIFF_COMPUTER = Key.create("Diff.CustomDiffComputer");

  //
  // DiffContext
  //

  Key<JComponent> BOTTOM_PANEL = Key.create("Diff.BottomPanel"); // Could implement Disposable

  Key<Boolean> SHOW_READ_ONLY_LOCK = Key.create("Diff.ShowReadOnlyLock");

  //
  // MergeContext / MergeRequest
  //

  // return false if merge window should be prevented from closing and canceling resolve.
  Key<Predicate<MergeTool.MergeViewer>> MERGE_CANCEL_HANDLER = Key.create("Diff.MergeCancelHandler");
  // (title, message)
  Key<Couple<String>> MERGE_CANCEL_MESSAGE = Key.create("Diff.MergeCancelMessage");
  // null -> default
  Key<Function<MergeResult, String>> MERGE_ACTION_CAPTIONS = Key.create("Diff.MergeActionCaptions");


  Key<String> VCS_DIFF_LEFT_CONTENT_TITLE = Key.create("Diff.Left.Panel.Title");
  Key<String> VCS_DIFF_RIGHT_CONTENT_TITLE = Key.create("Diff.Right.Panel.Title");
}
