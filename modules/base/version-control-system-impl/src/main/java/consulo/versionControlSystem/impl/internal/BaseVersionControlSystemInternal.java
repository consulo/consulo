/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.impl.internal;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.impl.internal.action.ActionImplUtil;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.impl.internal.history.VcsDiffImplUtil;
import consulo.versionControlSystem.internal.LineStatusTrackerI;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.versionControlSystem.internal.VersionControlSystemInternal;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;

/**
 * The frontend-neutral part of {@link VersionControlSystemInternal}.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public abstract class BaseVersionControlSystemInternal implements VersionControlSystemInternal {
    @Override
    public String getHtmlWithFonts(String input) {
        return input;
    }

    @Override
    public void moveToRange(VcsRange range, Editor editor, LineStatusTrackerI tracker) {
        LineStatusTrackerDrawing.moveToRange(range, editor, (LineStatusTracker) tracker);
    }

    @Override
    public void showDiffFor(Project project,
                            Collection<Change> changes,
                            String head,
                            String compare,
                            VirtualFile file) {
        VcsDiffImplUtil.showDiffFor(
            project,
            changes,
            VcsDiffImplUtil.getRevisionTitle(compare, false),
            VcsDiffImplUtil.getRevisionTitle(head, true),
            VcsUtil.getFilePath(file)
        );
    }

    @RequiredUIAccess
    @Override
    public void invokeAction(Project project, AnAction action) {
        DataContext dataContext = DataContext.builder().add(Project.KEY, project).build();
        ActionImplUtil.invokeAction(action, dataContext, ActionPlaces.UNKNOWN, null, null);
    }
}
