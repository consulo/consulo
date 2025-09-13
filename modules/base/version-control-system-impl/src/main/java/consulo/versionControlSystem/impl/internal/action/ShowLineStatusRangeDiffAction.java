/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.action;

import consulo.codeEditor.Editor;
import consulo.diff.DiffContentFactory;
import consulo.diff.DiffManager;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.versionControlSystem.internal.LineStatusTrackerI;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ShowLineStatusRangeDiffAction extends BaseLineStatusRangeAction {
    public ShowLineStatusRangeDiffAction(
        @Nonnull LineStatusTrackerI lineStatusTracker,
        @Nonnull VcsRange range,
        @Nullable Editor editor
    ) {
        super(lineStatusTracker, range);
        ActionUtil.copyFrom(this, "ChangesView.Diff");
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        DiffManager.getInstance().showDiff(e.getData(Project.KEY), createDiffData());
    }

    private DiffRequest createDiffData() {
        VcsRange range = expand(myRange, myLineStatusTracker.getDocument(), myLineStatusTracker.getVcsDocument());

        DiffContent vcsContent =
            createDiffContent(myLineStatusTracker.getVcsDocument(), myLineStatusTracker.getVcsTextRange(range), null);
        DiffContent currentContent = createDiffContent(
            myLineStatusTracker.getDocument(),
            myLineStatusTracker.getCurrentTextRange(range),
            myLineStatusTracker.getVirtualFile()
        );

        return new SimpleDiffRequest(
            VcsLocalize.dialogTitleDiffForRange().get(),
            vcsContent,
            currentContent,
            VcsLocalize.diffContentTitleUpToDate().get(),
            VcsLocalize.diffContentTitleCurrentRange().get()
        );
    }

    @Nonnull
    private DiffContent createDiffContent(@Nonnull Document document, @Nonnull TextRange textRange, @Nullable VirtualFile file) {
        Project project = myLineStatusTracker.getProject();
        DocumentContent content = DiffContentFactory.getInstance().create(project, document, file);
        return DiffContentFactory.getInstance().createFragment(project, content, textRange);
    }

    @Nonnull
    private static VcsRange expand(@Nonnull VcsRange range, @Nonnull Document document, @Nonnull Document uDocument) {
        boolean canExpandBefore = range.getLine1() != 0 && range.getVcsLine1() != 0;
        boolean canExpandAfter = range.getLine2() < document.getLineCount() && range.getVcsLine2() < uDocument.getLineCount();
        int offset1 = range.getLine1() - (canExpandBefore ? 1 : 0);
        int uOffset1 = range.getVcsLine1() - (canExpandBefore ? 1 : 0);
        int offset2 = range.getLine2() + (canExpandAfter ? 1 : 0);
        int uOffset2 = range.getVcsLine2() + (canExpandAfter ? 1 : 0);
        return new VcsRange(offset1, offset2, uOffset1, uOffset2);
    }
}