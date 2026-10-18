/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageButtonRole;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.versionControlSystem.AbstractVcsHelper;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.shelf.ShelvedBinaryFile;
import consulo.versionControlSystem.impl.internal.patch.PatchSyntaxException;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Deletes the selected files from their shelved change list, or else the selected change lists.
 *
 * @author yole
 */
class ShelvedChangeDeleteProvider implements DeleteProvider {
    private final Project myProject;
    private final ShelveChangesManagerImpl myShelveChangesManager;

    ShelvedChangeDeleteProvider(Project project, ShelveChangesManagerImpl shelveChangesManager) {
        myProject = project;
        myShelveChangesManager = shelveChangesManager;
    }

    @Override
    public boolean canDeleteElement(DataContext dataContext) {
        return canDeleteFiles(dataContext) || !getLists(dataContext).isEmpty();
    }

    @Override
    @RequiredUIAccess
    public void deleteElement(DataContext dataContext) {
        if (canDeleteFiles(dataContext)) {
            deleteFiles(dataContext);
        }
        else {
            deleteLists(dataContext);
        }
    }

    private static boolean canDeleteFiles(DataContext dataContext) {
        ShelvedChangeListImpl[] shelved = dataContext.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGELIST_KEY);
        if (shelved == null || shelved.length != 1) {
            return false;
        }
        List<ShelvedChangeImpl> changes = dataContext.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGE_KEY);
        if (changes != null && !changes.isEmpty()) {
            return true;
        }
        List<ShelvedBinaryFileImpl> binaryFiles = dataContext.getData(ShelvedChangesViewManagerImpl.SHELVED_BINARY_FILE_KEY);
        return binaryFiles != null && !binaryFiles.isEmpty();
    }

    @RequiredUIAccess
    private void deleteLists(DataContext dataContext) {
        List<ShelvedChangeListImpl> lists = getLists(dataContext);
        if (lists.isEmpty()) {
            return;
        }
        LocalizeValue message = lists.size() == 1
            ? VcsLocalize.shelveChangesDeleteConfirm(lists.get(0).DESCRIPTION)
            : VcsLocalize.shelveChangesDeleteMultipleConfirm(lists.size());
        confirm(VcsLocalize.shelvedchangesDeleteTitle(), message, CommonLocalize.buttonDelete()).whenCompleteAsync((ok, error) -> {
            if (Boolean.TRUE.equals(ok)) {
                for (ShelvedChangeListImpl list : lists) {
                    myShelveChangesManager.deleteChangeList(list);
                }
            }
        }, UIAccess.current());
    }

    @RequiredUIAccess
    private void deleteFiles(DataContext dataContext) {
        ShelvedChangeListImpl[] shelved = dataContext.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGELIST_KEY);
        if (shelved == null || shelved.length != 1) {
            return;
        }
        List<ShelvedChangeImpl> changes = orEmpty(dataContext.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGE_KEY));
        List<ShelvedBinaryFileImpl> binaryFiles = orEmpty(dataContext.getData(ShelvedChangesViewManagerImpl.SHELVED_BINARY_FILE_KEY));
        ShelvedChangeListImpl list = shelved[0];

        LocalizeValue message = VcsLocalize.shelveChangesDeleteFilesFromList(changes.size() + binaryFiles.size());
        confirm(VcsLocalize.shelveChangesDeleteFilesFromListTitle(), message, CommonLocalize.buttonOk()).whenCompleteAsync((ok, error) -> {
            if (Boolean.TRUE.equals(ok)) {
                deleteFiles(list, changes, binaryFiles);
            }
        }, UIAccess.current());
    }

    private void deleteFiles(ShelvedChangeListImpl list, List<ShelvedChangeImpl> changes, List<ShelvedBinaryFileImpl> binaryFiles) {
        List<ShelvedBinaryFile> remainingBinaries = new ArrayList<>(list.getBinaryFiles());
        List<ShelvedChangeImpl> remainingChanges = new ArrayList<>(list.getChanges(myProject));
        remainingBinaries.removeAll(binaryFiles);
        remainingChanges.removeAll(changes);

        CommitContext commitContext = new CommitContext();
        List<FilePatch> patches = new ArrayList<>();
        List<VcsException> exceptions = new ArrayList<>();
        for (ShelvedChangeImpl change : remainingChanges) {
            try {
                patches.add(change.loadFilePatch(myProject, commitContext));
            }
            catch (IOException | PatchSyntaxException e) {
                exceptions.add(new VcsException(e));
            }
        }

        myShelveChangesManager.saveRemainingPatches(list, patches, remainingBinaries, commitContext);

        if (!exceptions.isEmpty()) {
            String description = list.DESCRIPTION == null ? "" : list.DESCRIPTION;
            String title = description.substring(0, Math.min(10, description.length()));
            AbstractVcsHelper.getInstance(myProject).showErrors(exceptions, "Deleting files from '" + title + "'");
        }
    }

    @RequiredUIAccess
    private CompletableFuture<Boolean> confirm(LocalizeValue title, LocalizeValue message, LocalizeValue okText) {
        return MessageBoxBuilder.<Boolean>create()
            .asWarning()
            .title(title)
            .text(message)
            .button(MessageButtonRole.OK, okText, Boolean.TRUE)
            .asDefaultButton()
            .button(MessageButtonRole.CANCEL, Boolean.FALSE)
            .asExitButton()
            .showAsync(myProject);
    }

    private static List<ShelvedChangeListImpl> getLists(DataContext dataContext) {
        ShelvedChangeListImpl[] shelved = dataContext.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGELIST_KEY);
        ShelvedChangeListImpl[] recycled = dataContext.getData(ShelvedChangesViewManagerImpl.SHELVED_RECYCLED_CHANGELIST_KEY);
        List<ShelvedChangeListImpl> lists = new ArrayList<>();
        if (shelved != null) {
            Collections.addAll(lists, shelved);
        }
        if (recycled != null) {
            Collections.addAll(lists, recycled);
        }
        return lists;
    }

    private static <T> List<T> orEmpty(@Nullable List<T> list) {
        return list == null ? List.of() : list;
    }
}
