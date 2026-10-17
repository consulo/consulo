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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogService;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.ShowChangesDialogBuilder;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedShowChangesDialogBuilder implements ShowChangesDialogBuilder {
    private final Project myProject;
    private final List<Change> myChanges;

    private LocalizeValue myTitle = VcsLocalize.dialogTitleChangesBrowser();
    private LocalizeValue myDescription = LocalizeValue.empty();

    public UnifiedShowChangesDialogBuilder(Project project, Collection<Change> changes) {
        myProject = project;
        myChanges = List.copyOf(changes);
    }

    @Override
    public ShowChangesDialogBuilder title(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    @Override
    public ShowChangesDialogBuilder description(LocalizeValue description) {
        myDescription = description;
        return this;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<?> showAsync() {
        UnifiedShowChangesDialog descriptor = new UnifiedShowChangesDialog(myTitle, myProject, myChanges, myDescription);
        return myProject.getApplication().getInstance(DialogService.class).build(descriptor).showAsync().handle((value, error) -> null);
    }
}
