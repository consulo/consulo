/*
 * Copyright 2000-2010 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.versionControlSystem.impl.internal;

import consulo.codeEditor.Editor;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.versionControlSystem.internal.VcsRange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RollbackLineStatusRangeAction extends RollbackLineStatusAction {
  @Nonnull
  private final LineStatusTracker myTracker;
  @Nullable private final Editor myEditor;
  @Nonnull
  private final VcsRange myRange;

  public RollbackLineStatusRangeAction(@Nonnull LineStatusTracker tracker, @Nonnull VcsRange range, @Nullable Editor editor) {
    ActionUtil.copyFrom(this, IdeActions.SELECTED_CHANGES_ROLLBACK);

    myTracker = tracker;
    myEditor = editor;
    myRange = range;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(true);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    rollback(myTracker, myEditor, myRange);
  }
}
