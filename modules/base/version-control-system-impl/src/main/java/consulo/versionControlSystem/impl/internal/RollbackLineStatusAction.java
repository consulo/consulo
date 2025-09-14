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

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.diff.internal.DiffImplUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.undoRedo.CommandProcessor;
import consulo.versionControlSystem.internal.LineStatusTrackerManagerI;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.BitSet;
import java.util.List;

@ActionImpl(id = "Vcs.RollbackChangedLines")
public class RollbackLineStatusAction extends DumbAwareAction {
    public RollbackLineStatusAction() {
        super(
            ActionLocalize.actionVcsRollbackchangedlinesText(),
            ActionLocalize.actionVcsRollbackchangedlinesDescription(),
            PlatformIconGroup.generalReset()
        );
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
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
        LineStatusTracker tracker = (LineStatusTracker) LineStatusTrackerManagerI.getInstance(project).getLineStatusTracker(editor.getDocument());
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
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        Editor editor = e.getRequiredData(Editor.KEY);
        LineStatusTracker tracker = (LineStatusTracker) LineStatusTrackerManagerI.getInstance(project).getLineStatusTracker(editor.getDocument());
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
    protected static void rollback(@Nonnull LineStatusTracker tracker, @Nullable Editor editor, @Nullable VcsRange range) {
        assert editor != null || range != null;

        if (range != null) {
            doRollback(tracker, range);
            return;
        }

        doRollback(tracker, DiffImplUtil.getSelectedLines(editor));
    }

    @RequiredUIAccess
    private static void doRollback(@Nonnull LineStatusTracker tracker, @Nonnull VcsRange range) {
        execute(tracker, () -> tracker.rollbackChanges(range));
    }

    @RequiredUIAccess
    private static void doRollback(@Nonnull LineStatusTracker tracker, @Nonnull BitSet lines) {
        execute(tracker, () -> tracker.rollbackChanges(lines));
    }

    @RequiredUIAccess
    private static void execute(@Nonnull LineStatusTracker tracker, @RequiredUIAccess @Nonnull Runnable task) {
        CommandProcessor.getInstance().newCommand()
            .project(tracker.getProject())
            .document(tracker.getDocument())
            .name(VcsLocalize.commandNameRollbackChange())
            .inWriteAction()
            .run(task);
    }
}
