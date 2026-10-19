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
package consulo.versionControlSystem.impl.internal.change.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.application.progress.ProgressManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.impl.internal.change.patch.CreatePatchCommitExecutor;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelvedChangeListImpl;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelvedChangesViewManagerImpl;
import consulo.versionControlSystem.impl.internal.change.ui.SessionDialogDescriptor;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
@ActionImpl(id = "ChangesView.CreatePatchFromChanges")
public class CreatePatchFromChangesAction extends LegacyDumbAwareAction {
    public CreatePatchFromChangesAction() {
        super(
            VcsLocalize.actionNameCreatePatchForSelectedRevisions(),
            VcsLocalize.actionDescriptionCreatePatchForSelectedRevisions(),
            PlatformIconGroup.filetypesPatch()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Change[] changes = e.getData(VcsDataKeys.CHANGES);
        if (changes == null || changes.length == 0) {
            return;
        }
        String commitMessage = null;
        ShelvedChangeListImpl[] shelvedChangeLists = e.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGELIST_KEY);
        if (shelvedChangeLists != null && shelvedChangeLists.length > 0) {
            commitMessage = shelvedChangeLists[0].DESCRIPTION;
        }
        else {
            ChangeList[] changeLists = e.getData(VcsDataKeys.CHANGE_LISTS);
            if (changeLists != null && changeLists.length > 0) {
                commitMessage = changeLists[0].getComment();
            }
        }
        if (commitMessage == null) {
            commitMessage = e.getData(VcsDataKeys.PRESET_COMMIT_MESSAGE);
        }
        if (commitMessage == null) {
            commitMessage = "";
        }
        List<Change> changeCollection = new ArrayList<>();
        Collections.addAll(changeCollection, changes);
        createPatch(project, commitMessage, changeCollection);
    }

    @RequiredUIAccess
    public static void createPatch(@Nullable Project project, String commitMessage, List<Change> changeCollection) {
        Project targetProject = project == null ? ProjectManager.getInstance().getDefaultProject() : project;
        CreatePatchCommitExecutor executor = new CreatePatchCommitExecutor(targetProject);
        CommitContext commitContext = new CommitContext();
        CommitSession commitSession = executor.createCommitSession(commitContext);
        if (commitSession instanceof CommitSessionContextAware commitSessionContextAware) {
            commitSessionContextAware.setContext(commitContext);
        }
        UIAccess uiAccess = UIAccess.current();
        SessionDialogDescriptor.configureCommitSession(
                targetProject,
                executor.getActionText(),
                commitSession,
                changeCollection,
                commitMessage
            )
            .whenCompleteAsync((ok, error) -> {
                if (!Boolean.TRUE.equals(ok)) {
                    return;
                }
                targetProject.getApplication().getInstance(ProgressBuilderFactory.class)
                    .newProgressBuilder(targetProject, VcsLocalize.createPatchLoadingContentProgress())
                    .cancelable()
                    .execute(uiAccess, () -> Coroutine.first(CodeExecution.<Object, Object>apply((input, continuation) -> {
                        preloadContent(changeCollection);
                        commitSession.execute(changeCollection, commitMessage);
                        return null;
                    })));
            }, uiAccess);
    }

    private static void preloadContent(List<Change> changes) {
        for (Change change : changes) {
            checkLoadContent(change.getBeforeRevision());
            checkLoadContent(change.getAfterRevision());
        }
    }

    private static void checkLoadContent(@Nullable ContentRevision revision) {
        ProgressManager.checkCanceled();
        if (revision != null && !(revision instanceof BinaryContentRevision)) {
            try {
                revision.getContent();
            }
            catch (VcsException ignored) {
            }
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Boolean haveSelectedChanges = e.getData(VcsDataKeys.HAVE_SELECTED_CHANGES);
        Change[] changes;
        ChangeList[] data1 = e.getData(VcsDataKeys.CHANGE_LISTS);
        ShelvedChangeListImpl[] data2 = e.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGELIST_KEY);
        ShelvedChangeListImpl[] data3 = e.getData(ShelvedChangesViewManagerImpl.SHELVED_RECYCLED_CHANGELIST_KEY);

        int sum = data1 == null ? 0 : data1.length;
        sum += data2 == null ? 0 : data2.length;
        sum += data3 == null ? 0 : data3.length;

        e.getPresentation().setEnabled(
            Boolean.TRUE.equals(haveSelectedChanges) && sum == 1
                && (changes = e.getData(VcsDataKeys.CHANGES)) != null && changes.length > 0
        );
    }
}
