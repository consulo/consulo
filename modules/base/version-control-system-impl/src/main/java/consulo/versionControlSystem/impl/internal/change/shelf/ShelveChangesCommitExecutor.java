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

import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.MessageBoxes;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.change.shelf.ShelveChangesManager;
import consulo.versionControlSystem.change.shelf.ShelvedChangeList;
import consulo.versionControlSystem.change.shelf.ShelvedChangesViewManager;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author yole
 * @since 2006-11-23
 */
public class ShelveChangesCommitExecutor extends LocalCommitExecutor {
    private static final Logger LOG = Logger.getInstance(ShelveChangesCommitExecutor.class);

    private final Project myProject;

    public ShelveChangesCommitExecutor(Project project) {
        myProject = project;
    }

    
    @Override
    public LocalizeValue getActionText() {
        return VcsLocalize.shelveChangesAction();
    }

    @Override
    
    public CommitSession createCommitSession() {
        return new ShelveChangesCommitSession();
    }

    @Override
    public String getHelpId() {
        return "reference.dialogs.vcs.shelve";
    }

    private class ShelveChangesCommitSession implements CommitSession, CommitSessionContextAware {

        @Override
        public void setContext(CommitContext context) {
        }

        @Override
        public boolean canExecute(Collection<Change> changes, String commitMessage) {
            return changes.size() > 0;
        }

        @Override
        public void execute(Collection<Change> changes, String commitMessage) {
            if (changes.size() > 0 && !ChangesUtil.hasFileChanges(changes)) {
                showError(VcsLocalize.shelveChangesOnlyDirectories(), VcsLocalize.shelveChangesAction());
                return;
            }
            try {
                ShelvedChangeList list = ShelveChangesManager.getInstance(myProject).shelveChanges(changes, commitMessage, true);
                ShelvedChangesViewManager.getInstance(myProject).activateView(list);

                Change[] changesArray = changes.toArray(new Change[changes.size()]);
                // todo better under lock
                ChangeList changeList = ChangesUtil.getChangeListIfOnlyOne(myProject, changesArray);
                if (changeList instanceof LocalChangeList) {
                    LocalChangeList localChangeList = (LocalChangeList) changeList;
                    if (localChangeList.getChanges().size() == changes.size()
                        && !localChangeList.isReadOnly()
                        && (!localChangeList.isDefault())) {
                        ChangeListManager.getInstance(myProject).removeChangeList(localChangeList.getName());
                    }
                }
            }
            catch (Exception ex) {
                LOG.info(ex);
                showError(VcsLocalize.createPatchErrorTitle(ex.getMessage()), CommonLocalize.titleError());
            }
        }

        private void showError(LocalizeValue message, LocalizeValue title) {
            myProject.getApplication().invokeLater(
                () -> MessageBoxes.okError(message).title(title).showAsync(myProject),
                myProject.getDisposed()
            );
        }

        @Override
        public void executionCanceled() {
        }

        @Override
        public @Nullable String getHelpId() {
            return null;
        }
    }
}
