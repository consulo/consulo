/*
 * Copyright 2013-2025 consulo.io
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

import consulo.annotation.component.ServiceImpl;
import consulo.diff.DiffDialogHints;
import consulo.versionControlSystem.impl.internal.action.ShowDiffAction;
import consulo.versionControlSystem.internal.ShowDiffContext;
import consulo.localHistory.internal.LocalHistoryHelperInternal;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-08-01
 */
@Singleton
@ServiceImpl
public class LocalHistoryHelperInternalImpl implements LocalHistoryHelperInternal {
    @Override
    @RequiredUIAccess
    public void showDiff(Project project, List changes, int index) {
        ShowDiffAction.showDiffForChange(project, changes, index, new ShowDiffContext(DiffDialogHints.FRAME));
    }
}
