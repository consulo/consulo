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

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.ide.impl.idea.openapi.vcs.impl.LineStatusTrackerManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.BitSet;
import java.util.List;

public class RollbackLineStatusAction extends DumbAwareAction {
    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Editor editor = e.getData(Editor.KEY);
        if (project == null || editor == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        if (DiffImplUtil.isDiffEditor(editor)) {
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
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Editor editor = e.getRequiredData(Editor.KEY);
        LineStatusTracker tracker = LineStatusTrackerManager.getInstance(project).getLineStatusTracker(editor.getDocument());
        assert tracker != null;

        rollback(tracker, editor, null);
    }

    protected static boolean isSomeChangeSelected(@Nonnull Editor editor, @Nonnull LineStatusTracker tracker) {
        List<Caret> carets = editor.getCaretModel().getAllCarets();
        if (carets.size() != 1) {
            return true;
        }
        Caret caret = carets.get(0);
        if (caret.hasSelection()) {
            return true;
        }
        if (caret.getOffset() == editor.getDocument().getTextLength()
            && tracker.getRangeForLine(editor.getDocument().getLineCount()) != null) {
            return true;
        }
        return tracker.getRangeForLine(caret.getLogicalPosition().line) != null;
    }

    @RequiredUIAccess
    protected static void rollback(@Nonnull LineStatusTracker tracker, @Nullable Editor editor, @Nullable Range range) {
        assert editor != null || range != null;

        if (range != null) {
            doRollback(tracker, range);
            return;
        }

        doRollback(tracker, DiffImplUtil.getSelectedLines(editor));
    }

    @RequiredUIAccess
    private static void doRollback(@Nonnull final LineStatusTracker tracker, @Nonnull final Range range) {
        execute(tracker, () -> tracker.rollbackChanges(range));
    }

    @RequiredUIAccess
    private static void doRollback(@Nonnull final LineStatusTracker tracker, @Nonnull final BitSet lines) {
        execute(tracker, () -> tracker.rollbackChanges(lines));
    }

    @RequiredUIAccess
    private static void execute(@Nonnull final LineStatusTracker tracker, @Nonnull final Runnable task) {
        DiffImplUtil.newWriteCommand(task)
            .withProject(tracker.getProject())
            .withDocument(tracker.getDocument())
            .withName(VcsLocalize.commandNameRollbackChange())
            .execute();
    }
}
