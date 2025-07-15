/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.annotate;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.ide.impl.idea.openapi.vcs.changes.BackgroundFromStartOption;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.history.ShortVcsRevisionNumber;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.versionBrowser.ChangeBrowserSettings;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.List;

/**
 * @author irengrig
 * @since 2011-03-16
 */
public class ShowAllAffectedGenericAction extends AnAction {

    private static final String ACTION_ID = "VcsHistory.ShowAllAffected";

    @Inject
    private ShowAllAffectedGenericAction() {
    }

    public static ShowAllAffectedGenericAction getInstance() {
        return (ShowAllAffectedGenericAction) ActionManager.getInstance().getAction(ACTION_ID);
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.actionsDiff();
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        VcsKey vcsKey = e.getRequiredData(VcsDataKeys.VCS);
        final VcsFileRevision revision = e.getData(VcsDataKeys.VCS_FILE_REVISION);
        VirtualFile revisionVirtualFile = e.getData(VcsDataKeys.VCS_VIRTUAL_FILE);
        final Boolean isNonLocal = e.getData(VcsDataKeys.VCS_NON_LOCAL_HISTORY_SESSION);
        if (revision != null && revisionVirtualFile != null) {
            showSubmittedFiles(
                project,
                revision.getRevisionNumber(),
                revisionVirtualFile,
                vcsKey,
                revision.getChangedRepositoryPath(),
                Boolean.TRUE.equals(isNonLocal)
            );
        }
    }

    public static void showSubmittedFiles(final Project project, final VcsRevisionNumber revision, final VirtualFile virtualFile, final VcsKey vcsKey) {
        showSubmittedFiles(project, revision, virtualFile, vcsKey, null, false);
    }

    public static void showSubmittedFiles(final Project project, final VcsRevisionNumber revision, final VirtualFile virtualFile,
                                          final VcsKey vcsKey, final RepositoryLocation location, final boolean isNonLocal) {
        final AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).findVcsByName(vcsKey.getName());
        if (vcs == null) {
            return;
        }
        if (isNonLocal && !canPresentNonLocal(project, vcsKey, virtualFile)) {
            return;
        }

        final LocalizeValue title = VcsLocalize.pathsAffectedInRevision(
            revision instanceof ShortVcsRevisionNumber shortVcsRevisionNumber ? shortVcsRevisionNumber.toShortString() : revision.asString()
        );
        final CommittedChangeList[] list = new CommittedChangeList[1];
        final VcsException[] exc = new VcsException[1];
        Task.Backgroundable task = new Task.Backgroundable(project, title.get(), true, BackgroundFromStartOption.getInstance()) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                try {
                    final CommittedChangesProvider provider = vcs.getCommittedChangesProvider();
                    if (!isNonLocal) {
                        final Pair<CommittedChangeList, FilePath> pair = provider.getOneList(virtualFile, revision);
                        if (pair != null) {
                            list[0] = pair.getFirst();
                        }
                    }
                    else {
                        if (location != null) {
                            final ChangeBrowserSettings settings = provider.createDefaultSettings();
                            settings.USE_CHANGE_BEFORE_FILTER = true;
                            settings.CHANGE_BEFORE = revision.asString();
                            final List<CommittedChangeList> changes = provider.getCommittedChanges(settings, location, 1);
                            if (changes != null && changes.size() == 1) {
                                list[0] = changes.get(0);
                            }
                        }
                        else {
                            list[0] = getRemoteList(vcs, revision, virtualFile);
              /*final RepositoryLocation local = provider.getForNonLocal(virtualFile);
              if (local != null) {
                final String number = revision.asString();
                final ChangeBrowserSettings settings = provider.createDefaultSettings();
                final List<CommittedChangeList> changes = provider.getCommittedChanges(settings, local, provider.getUnlimitedCountValue());
                if (changes != null) {
                  for (CommittedChangeList change : changes) {
                    if (number.equals(String.valueOf(change.getNumber()))) {
                      list[0] = change;
                    }
                  }
                }
              } */
                        }
                    }
                }
                catch (VcsException e) {
                    exc[0] = e;
                }
            }

            @RequiredUIAccess
            @Override
            public void onSuccess() {
                final AbstractVcsHelper instance = AbstractVcsHelper.getInstance(project);
                if (exc[0] != null) {
                    instance.showError(exc[0], failedText(virtualFile, revision));
                }
                else if (list[0] == null) {
                    Messages.showErrorDialog(project, failedText(virtualFile, revision), getTitle());
                }
                else {
                    instance.showChangesListBrowser(list[0], virtualFile, title.get());
                }
            }
        };
        ProgressManager.getInstance().run(task);
    }

    public static CommittedChangeList getRemoteList(final AbstractVcs vcs, final VcsRevisionNumber revision, final VirtualFile nonLocal)
        throws VcsException {
        final CommittedChangesProvider provider = vcs.getCommittedChangesProvider();
        final RepositoryLocation local = provider.getForNonLocal(nonLocal);
        if (local != null) {
            final String number = revision.asString();
            final ChangeBrowserSettings settings = provider.createDefaultSettings();
            final List<CommittedChangeList> changes = provider.getCommittedChanges(settings, local, provider.getUnlimitedCountValue());
            if (changes != null) {
                for (CommittedChangeList change : changes) {
                    if (number.equals(String.valueOf(change.getNumber()))) {
                        return change;
                    }
                }
            }
        }
        return null;
    }

    private static String failedText(VirtualFile virtualFile, VcsRevisionNumber revision) {
        return "Show all affected files for " + virtualFile.getPath() + " at " + revision.asString() + " failed";
    }

    @Override
    public void update(AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        final VcsKey vcsKey = e.getData(VcsDataKeys.VCS);
        if (project == null || vcsKey == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        final Boolean isNonLocal = e.getData(VcsDataKeys.VCS_NON_LOCAL_HISTORY_SESSION);
        final VirtualFile revisionVirtualFile = e.getData(VcsDataKeys.VCS_VIRTUAL_FILE);
        boolean enabled = e.hasData(VcsDataKeys.VCS_FILE_REVISION) && revisionVirtualFile != null;
        enabled = enabled && (!Boolean.TRUE.equals(isNonLocal) || canPresentNonLocal(project, vcsKey, revisionVirtualFile));
        e.getPresentation().setEnabled(enabled);
    }

    private static boolean canPresentNonLocal(Project project, VcsKey key, final VirtualFile file) {
        final AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).findVcsByName(key.getName());
        if (vcs == null) {
            return false;
        }
        final CommittedChangesProvider provider = vcs.getCommittedChangesProvider();
        return provider != null && provider.getForNonLocal(file) != null;
    }
}
