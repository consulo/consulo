/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.shelf;

import consulo.annotation.component.ActionImpl;
import consulo.dataContext.DataContext;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unshelves the selected change lists, or the selected files of one, into the change lists they were shelved from.
 */
@ActionImpl(id = "ChangesView.UnshelveSilently")
public class UnshelveSilentlyAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public UnshelveSilentlyAction() {
        super(VcsLocalize.actionUnshelveSilentlyText(), VcsLocalize.actionUnshelveSilentlyDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        FileDocumentManager.getInstance().saveAllDocuments();
        DataContext dataContext = e.getDataContext();
        ShelveChangesManagerImpl.getInstance(project).unshelveSilentlyAsynchronously(
            project,
            getShelvedLists(dataContext),
            orEmpty(dataContext.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGE_KEY)),
            orEmpty(dataContext.getData(ShelvedChangesViewManagerImpl.SHELVED_BINARY_FILE_KEY)),
            null
        );
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY) && !getShelvedLists(e.getDataContext()).isEmpty());
    }

    private static List<ShelvedChangeListImpl> getShelvedLists(DataContext dataContext) {
        List<ShelvedChangeListImpl> lists = new ArrayList<>();
        ShelvedChangeListImpl[] shelved = dataContext.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGELIST_KEY);
        if (shelved != null) {
            Collections.addAll(lists, shelved);
        }
        ShelvedChangeListImpl[] recycled = dataContext.getData(ShelvedChangesViewManagerImpl.SHELVED_RECYCLED_CHANGELIST_KEY);
        if (recycled != null) {
            Collections.addAll(lists, recycled);
        }
        return lists;
    }

    private static <T> List<T> orEmpty(@Nullable List<T> list) {
        return list == null ? List.of() : list;
    }
}
