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
package consulo.ide.impl.idea.openapi.vcs.ex;

import consulo.ide.impl.idea.diff.util.DiffUtil;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.versionControlSystem.VcsBundle;
import consulo.ide.impl.idea.openapi.vcs.impl.LineStatusTrackerManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.BitSet;
import java.util.List;

public class RollbackLineStatusAction extends DumbAwareAction {
  @Override
  public void update(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (project == null || editor == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    if (DiffUtil.isDiffEditor(editor)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    LineStatusTracker tracker = LineStatusTrackerManager.getInstance(project).getLineStatusTracker(editor.getDocument());
    if (tracker == null || !tracker.isValid() || tracker.isSilentMode()) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    if (!isSomeChangeSelected(editor, tracker)) {
      e.getPresentation().setVisible(true);
      e.getPresentation().setEnabled(false);
      return;
    }
    e.getPresentation().setEnabledAndVisible(true);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
    LineStatusTracker tracker = LineStatusTrackerManager.getInstance(project).getLineStatusTracker(editor.getDocument());
    assert tracker != null;

    rollback(tracker, editor, null);
  }

  protected static boolean isSomeChangeSelected(@Nonnull Editor editor, @Nonnull LineStatusTracker tracker) {
    List<Caret> carets = editor.getCaretModel().getAllCarets();
    if (carets.size() != 1) return true;
    Caret caret = carets.get(0);
    if (caret.hasSelection()) return true;
    if (caret.getOffset() == editor.getDocument().getTextLength() &&
        tracker.getRangeForLine(editor.getDocument().getLineCount()) != null) {
      return true;
    }
    return tracker.getRangeForLine(caret.getLogicalPosition().line) != null;
  }

  protected static void rollback(@Nonnull LineStatusTracker tracker, @Nullable Editor editor, @Nullable Range range) {
    assert editor != null || range != null;

    if (range != null) {
      doRollback(tracker, range);
      return;
    }

    doRollback(tracker, DiffUtil.getSelectedLines(editor));
  }

  private static void doRollback(@Nonnull final LineStatusTracker tracker, @Nonnull final Range range) {
    execute(tracker, () -> tracker.rollbackChanges(range));
  }

  private static void doRollback(@Nonnull final LineStatusTracker tracker, @Nonnull final BitSet lines) {
    execute(tracker, () -> tracker.rollbackChanges(lines));
  }

  private static void execute(@Nonnull final LineStatusTracker tracker, @Nonnull final Runnable task) {
    DiffUtil.executeWriteCommand(tracker.getDocument(), tracker.getProject(), VcsBundle.message("command.name.rollback.change"), task);
  }
}
