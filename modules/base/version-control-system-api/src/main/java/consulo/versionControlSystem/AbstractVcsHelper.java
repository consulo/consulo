/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.errorTreeView.HotfixData;
import consulo.versionControlSystem.annotate.AnnotationProvider;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.CommitResultHandler;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.history.VcsHistoryProvider;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.merge.MergeDialogCustomizer;
import consulo.versionControlSystem.merge.MergeProvider;
import consulo.versionControlSystem.versionBrowser.ChangeBrowserSettings;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 * Component which provides means to invoke different VCS-related services.
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class AbstractVcsHelper {
    protected final Project myProject;

    protected AbstractVcsHelper(@Nonnull Project project) {
        myProject = project;
    }

    @Nonnull
    public static AbstractVcsHelper getInstance(Project project) {
        return project.getInstance(AbstractVcsHelper.class);
    }

    public void showErrors(List<VcsException> abstractVcsExceptions, @Nonnull LocalizeValue tabDisplayName) {
        showErrors(abstractVcsExceptions, tabDisplayName.get());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void showErrors(List<VcsException> abstractVcsExceptions, @Nonnull String tabDisplayName) {
        showErrors(abstractVcsExceptions, LocalizeValue.of(tabDisplayName));
    }

    public void showErrors(Map<HotfixData, List<VcsException>> exceptionGroups, @Nonnull LocalizeValue tabDisplayName) {
        showErrors(exceptionGroups, tabDisplayName.get());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void showErrors(Map<HotfixData, List<VcsException>> exceptionGroups, @Nonnull String tabDisplayName) {
        showErrors(exceptionGroups, LocalizeValue.of(tabDisplayName));
    }

    /**
     * Runs the runnable inside the vcs transaction (if needed), collects all exceptions, commits/rollbacks transaction
     * and returns all exceptions together.
     */
    public abstract List<VcsException> runTransactionRunnable(AbstractVcs vcs, TransactionRunnable runnable, Object vcsParameters);

    public void showError(final VcsException e, final String tabDisplayName) {
        showErrors(Arrays.asList(e), tabDisplayName);
    }

    public abstract void showAnnotation(FileAnnotation annotation, VirtualFile file, AbstractVcs vcs);

    public abstract void showAnnotation(FileAnnotation annotation, VirtualFile file, AbstractVcs vcs, int line);

    public void showChangesListBrowser(CommittedChangeList changelist, @Nonnull LocalizeValue title) {
        showChangesListBrowser(changelist, title.get());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void showChangesListBrowser(CommittedChangeList changelist, @Nls String title) {
        showChangesListBrowser(changelist, LocalizeValue.ofNullable(title));
    }

    public void showChangesListBrowser(CommittedChangeList changelist, @Nullable VirtualFile toSelect, @Nonnull LocalizeValue title) {
        showChangesListBrowser(changelist, title);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void showChangesListBrowser(CommittedChangeList changelist, @Nullable VirtualFile toSelect, @Nls String title) {
        showChangesListBrowser(changelist, title);
    }

    public abstract void showChangesBrowser(List<CommittedChangeList> changelists);

    public void showChangesBrowser(List<CommittedChangeList> changelists, @Nonnull LocalizeValue title) {
        showChangesBrowser(changelists, title.get());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void showChangesBrowser(List<CommittedChangeList> changelists, @Nls String title) {
        showChangesBrowser(changelists, LocalizeValue.ofNullable(title));
    }

    public void showChangesBrowser(
        CommittedChangesProvider provider,
        final RepositoryLocation location,
        @Nonnull LocalizeValue title,
        @Nullable final Component parent
    ) {
        showChangesBrowser(provider, location, title.get(), parent);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void showChangesBrowser(
        CommittedChangesProvider provider,
        final RepositoryLocation location,
        @Nls String title,
        @Nullable final Component parent
    ) {
        showChangesBrowser(provider, location, LocalizeValue.ofNullable(title), parent);
    }

    public void showWhatDiffersBrowser(@Nullable Component parent, Collection<Change> changes, @Nonnull LocalizeValue title) {
        showWhatDiffersBrowser(parent, changes, title.get());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void showWhatDiffersBrowser(@Nullable Component parent, Collection<Change> changes, @Nls String title) {
        showWhatDiffersBrowser(parent, changes, LocalizeValue.ofNullable(title));
    }

    @Nullable
    public abstract <T extends CommittedChangeList, U extends ChangeBrowserSettings> T chooseCommittedChangeList(
        @Nonnull CommittedChangesProvider<T, U> provider,
        RepositoryLocation location
    );

    public void openCommittedChangesTab(
        AbstractVcs vcs,
        VirtualFile root,
        ChangeBrowserSettings settings,
        int maxCount,
        @Nonnull LocalizeValue title
    ) {
        openCommittedChangesTab(vcs, root, settings, maxCount, title.get());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void openCommittedChangesTab(
        AbstractVcs vcs,
        VirtualFile root,
        ChangeBrowserSettings settings,
        int maxCount,
        final String title
    ) {
        openCommittedChangesTab(vcs, root, settings, maxCount, LocalizeValue.ofNullable(title));
    }

    public void openCommittedChangesTab(
        CommittedChangesProvider provider,
        RepositoryLocation location,
        ChangeBrowserSettings settings,
        int maxCount,
        @Nonnull LocalizeValue title
    ) {
        openCommittedChangesTab(provider, location, settings, maxCount, title.get());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void openCommittedChangesTab(
        CommittedChangesProvider provider,
        RepositoryLocation location,
        ChangeBrowserSettings settings,
        int maxCount,
        final String title
    ) {
        openCommittedChangesTab(provider, location, settings, maxCount, LocalizeValue.ofNullable(title));
    }

    /**
     * Shows the multiple file merge dialog for resolving conflicts in the specified set of virtual files.
     * Assumes all files are under the same VCS.
     *
     * @param files                 the files to show in the merge dialog.
     * @param provider              MergeProvider to be used for merging.
     * @param mergeDialogCustomizer custom container of titles, descriptions and messages for the merge dialog.
     * @return changed files for which the merge was actually performed.
     */
    @Nonnull
    public abstract List<VirtualFile> showMergeDialog(
        List<VirtualFile> files,
        MergeProvider provider,
        @Nonnull MergeDialogCustomizer mergeDialogCustomizer
    );

    /**
     * {@link #showMergeDialog(java.util.List, MergeProvider)} without description.
     */
    @Nonnull
    public final List<VirtualFile> showMergeDialog(List<VirtualFile> files, MergeProvider provider) {
        return showMergeDialog(files, provider, new MergeDialogCustomizer());
    }

    /**
     * {@link #showMergeDialog(java.util.List, MergeProvider)} without description and with default merge provider
     * for the current VCS.
     */
    @Nonnull
    public final List<VirtualFile> showMergeDialog(List<VirtualFile> files) {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        MergeProvider provider = null;
        for (VirtualFile virtualFile : files) {
            final AbstractVcs vcs = ProjectLevelVcsManager.getInstance(myProject).getVcsFor(virtualFile);
            if (vcs != null) {
                provider = vcs.getMergeProvider();
                if (provider != null) {
                    break;
                }
            }
        }
        if (provider == null) {
            return Collections.emptyList();
        }
        return showMergeDialog(files, provider);
    }

    public abstract void showFileHistory(
        @Nonnull VcsHistoryProvider historyProvider,
        @Nonnull FilePath path,
        @Nonnull AbstractVcs vcs,
        @Nullable String repositoryPath
    );

    public abstract void showFileHistory(
        @Nonnull VcsHistoryProvider historyProvider,
        @Nullable AnnotationProvider annotationProvider,
        @Nonnull FilePath path,
        @Nullable String repositoryPath,
        @Nonnull final AbstractVcs vcs
    );

    /**
     * Shows the "Rollback Changes" dialog with the specified list of changes.
     *
     * @param changes the changes to show in the dialog.
     */
    public abstract void showRollbackChangesDialog(List<Change> changes);

    @Nonnull
    public List<VirtualFile> selectFilesToProcess(
        List<VirtualFile> files,
        @Nonnull LocalizeValue title,
        @Nullable String prompt,
        @Nonnull LocalizeValue singleFileTitle,
        Function<Object, LocalizeValue> singleFilePromptGenerator,
        VcsShowConfirmationOption confirmationOption
    ) {
        return selectFilesToProcess(
            files,
            title.get(),
            prompt,
            singleFileTitle.get(),
            singleFilePromptGenerator == null ? null : singleFilePromptGenerator.apply("{0}").get(),
            confirmationOption
        );
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @Nonnull
    public List<VirtualFile> selectFilesToProcess(
        List<VirtualFile> files,
        String title,
        @Nullable final String prompt,
        String singleFileTitle,
        String singleFilePromptTemplate,
        VcsShowConfirmationOption confirmationOption
    ) {
        return selectFilesToProcess(
            files,
            LocalizeValue.of(title),
            prompt,
            LocalizeValue.of(singleFileTitle),
            param -> LocalizeValue.of(MessageFormat.format(singleFilePromptTemplate, param)),
            confirmationOption
        );
    }

    @Nonnull
    public List<FilePath> selectFilePathsToProcess(
        List<FilePath> files,
        @Nonnull LocalizeValue title,
        @Nullable String prompt,
        @Nonnull LocalizeValue singleFileTitle,
        Function<Object, LocalizeValue> singleFilePromptGenerator,
        VcsShowConfirmationOption confirmationOption
    ) {
        return selectFilePathsToProcess(
            files,
            title.get(),
            prompt,
            singleFileTitle.get(),
            singleFilePromptGenerator == null ? null : singleFilePromptGenerator.apply("{0}").get(),
            confirmationOption
        );
    }

    @Nonnull
    public List<FilePath> selectFilePathsToProcess(
        List<FilePath> files,
        final String title,
        @Nullable final String prompt,
        final String singleFileTitle,
        final String singleFilePromptTemplate,
        final VcsShowConfirmationOption confirmationOption
    ) {
        return selectFilePathsToProcess(
            files,
            LocalizeValue.of(title),
            prompt,
            LocalizeValue.of(singleFileTitle),
            param -> LocalizeValue.of(MessageFormat.format(singleFilePromptTemplate, param)),
            confirmationOption
        );
    }

    @Nonnull
    public Collection<FilePath> selectFilePathsToProcess(
        List<FilePath> files,
        @Nonnull LocalizeValue title,
        @Nullable String prompt,
        @Nonnull LocalizeValue singleFileTitle,
        Function<Object, LocalizeValue> singleFilePromptGenerator,
        VcsShowConfirmationOption confirmationOption,
        @Nonnull LocalizeValue okActionName,
        @Nonnull LocalizeValue cancelActionName
    ) {
        return selectFilePathsToProcess(
            files,
            title.get(),
            prompt,
            singleFileTitle.get(),
            singleFilePromptGenerator == null ? null : singleFilePromptGenerator.apply("{0}").get(),
            confirmationOption,
            okActionName.get(),
            cancelActionName.get()
        );
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @Nonnull
    public Collection<FilePath> selectFilePathsToProcess(
        List<FilePath> files,
        final String title,
        @Nullable final String prompt,
        final String singleFileTitle,
        final String singleFilePromptTemplate,
        final VcsShowConfirmationOption confirmationOption,
        @Nullable String okActionName,
        @Nullable String cancelActionName
    ) {
        return selectFilePathsToProcess(files, title, prompt, singleFileTitle, singleFilePromptTemplate, confirmationOption);
    }

    /**
     * <p>Shows commit dialog, fills it with the given changes and given commit message, initially selects the given changelist.</p>
     * <p>Note that the method is asynchronous: it returns right after user presses "Commit" or "Cancel" and after all pre-commit handlers
     * have been called. It doesn't wait for commit itself to succeed or fail - for this use the {@code customResultHandler}.</p>
     *
     * @return true if user decides to commit the changes, false if user presses Cancel.
     */
    public abstract boolean commitChanges(
        @Nonnull Collection<Change> changes,
        @Nonnull LocalChangeList initialChangeList,
        @Nonnull String commitMessage,
        @Nullable CommitResultHandler customResultHandler
    );

    public abstract void loadAndShowCommittedChangesDetails(
        @Nonnull Project project,
        @Nonnull VcsRevisionNumber revision,
        @Nonnull VirtualFile file,
        @Nonnull VcsKey key,
        @Nullable RepositoryLocation location,
        boolean local
    );
}
