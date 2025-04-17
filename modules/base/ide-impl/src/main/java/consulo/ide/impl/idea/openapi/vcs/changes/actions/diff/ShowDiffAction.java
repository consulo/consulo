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
package consulo.ide.impl.idea.openapi.vcs.changes.actions.diff;

import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.diff.DiffManager;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.chain.DiffRequestChain;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.versionControlSystem.change.diff.ChangeDiffRequestProducer;
import consulo.versionControlSystem.impl.internal.change.FakeRevision;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.dataholder.Key;
import consulo.util.lang.function.Condition;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ShowDiffAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(ShowDiffAction.class);

    public void update(@Nonnull AnActionEvent e) {
        Change[] changes = e.getData(VcsDataKeys.CHANGES);
        Project project = e.getData(Project.KEY);
        if (ActionPlaces.MAIN_MENU.equals(e.getPlace())) {
            e.getPresentation().setEnabled(project != null && changes != null && changes.length > 0);
        }
        else {
            e.getPresentation().setEnabled(project != null && canShowDiff(project, changes));
        }
    }

    public static boolean canShowDiff(@Nullable Project project, @Nullable Change[] changes) {
        return changes != null && canShowDiff(project, Arrays.asList(changes));
    }

    public static boolean canShowDiff(@Nullable Project project, @Nullable List<Change> changes) {
        if (changes == null || changes.size() == 0) {
            return false;
        }
        for (Change change : changes) {
            if (ChangeDiffRequestProducer.canCreate(project, change)) {
                return true;
            }
        }
        return false;
    }

    public void actionPerformed(@Nonnull final AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        final Change[] changes = e.getData(VcsDataKeys.CHANGES);
        if (project == null || !canShowDiff(project, changes)) {
            return;
        }
        if (ChangeListManager.getInstance(project).isFreezedWithNotification(null)) {
            return;
        }

        final boolean needsConversion = checkIfThereAreFakeRevisions(project, changes);
        final List<Change> changesInList = e.getData(VcsDataKeys.CHANGES_IN_LIST_KEY);

        // this trick is essential since we are under some conditions to refresh changes;
        // but we can only rely on callback after refresh
        final Runnable performer = () -> {
            Change[] convertedChanges;
            if (needsConversion) {
                convertedChanges = loadFakeRevisions(project, changes);
            }
            else {
                convertedChanges = changes;
            }

            if (convertedChanges == null || convertedChanges.length == 0) {
                return;
            }

            Change selectedChane = null;
            List<Change> result = null;

            if (convertedChanges.length == 1) {
                selectedChane = convertedChanges[0];
                ChangeList changeList =
                    ((ChangeListManagerImpl)ChangeListManager.getInstance(project)).getIdentityChangeList(selectedChane);
                if (changeList != null) {
                    result = changesInList != null ? changesInList : new ArrayList<>(changeList.getChanges());
                }
            }
            if (result == null) {
                result = ContainerUtil.newArrayList(convertedChanges);
            }

            //ContainerUtil.sort(result, ChangesComparator.getInstance(false));
            int index = selectedChane == null ? 0 : Math.max(0, ContainerUtil.indexOfIdentity(result, selectedChane));

            showDiffForChange(project, result, index);
        };

        if (needsConversion) {
            ChangeListManager.getInstance(project).invokeAfterUpdate(
                performer,
                InvokeAfterUpdateMode.BACKGROUND_CANCELLABLE,
                ActionLocalize.actionChangesviewDiffText().get(),
                IdeaModalityState.current()
            );
        }
        else {
            performer.run();
        }
    }

    private static boolean checkIfThereAreFakeRevisions(@Nonnull Project project, @Nonnull Change[] changes) {
        boolean needsConversion = false;
        for (Change change : changes) {
            final ContentRevision beforeRevision = change.getBeforeRevision();
            final ContentRevision afterRevision = change.getAfterRevision();
            if (beforeRevision instanceof FakeRevision) {
                VcsDirtyScopeManager.getInstance(project).fileDirty(beforeRevision.getFile());
                needsConversion = true;
            }
            if (afterRevision instanceof FakeRevision) {
                VcsDirtyScopeManager.getInstance(project).fileDirty(afterRevision.getFile());
                needsConversion = true;
            }
        }
        return needsConversion;
    }

    @Nullable
    private static Change[] loadFakeRevisions(@Nonnull Project project, @Nonnull Change[] changes) {
        List<Change> matchingChanges = new ArrayList<>();
        for (Change change : changes) {
            matchingChanges.addAll(ChangeListManager.getInstance(project).getChangesIn(ChangesUtil.getFilePath(change)));
        }
        return matchingChanges.toArray(new Change[matchingChanges.size()]);
    }

    //
    // Impl
    //

    public static void showDiffForChange(@Nullable Project project, @Nonnull Iterable<Change> changes) {
        showDiffForChange(project, changes, 0);
    }

    public static void showDiffForChange(@Nullable Project project, @Nonnull Iterable<Change> changes, int index) {
        showDiffForChange(project, changes, index, new ShowDiffContext());
    }

    public static void showDiffForChange(
        @Nullable Project project,
        @Nonnull Iterable<Change> changes,
        @Nonnull Condition<Change> condition,
        @Nonnull ShowDiffContext context
    ) {
        int index = 0;
        List<ChangeDiffRequestProducer> presentables = new ArrayList<>();
        for (Change change : changes) {
            if (condition.value(change)) {
                index = presentables.size();
            }
            ChangeDiffRequestProducer presentable = ChangeDiffRequestProducer.create(project, change, context.getChangeContext(change));
            if (presentable != null) {
                presentables.add(presentable);
            }
        }

        showDiffForChange(project, presentables, index, context);
    }

    public static void showDiffForChange(
        @Nullable Project project,
        @Nonnull Iterable<Change> changes,
        int index,
        @Nonnull ShowDiffContext context
    ) {
        int i = 0;
        int newIndex = 0;
        List<ChangeDiffRequestProducer> presentables = new ArrayList<>();
        for (Change change : changes) {
            if (i == index) {
                newIndex = presentables.size();
            }
            ChangeDiffRequestProducer presentable = ChangeDiffRequestProducer.create(project, change, context.getChangeContext(change));
            if (presentable != null) {
                presentables.add(presentable);
            }
            i++;
        }

        showDiffForChange(project, presentables, newIndex, context);
    }

    private static void showDiffForChange(
        @Nullable Project project,
        @Nonnull List<ChangeDiffRequestProducer> presentables,
        int index,
        @Nonnull ShowDiffContext context
    ) {
        if (presentables.isEmpty()) {
            return;
        }
        if (index < 0 || index >= presentables.size()) {
            index = 0;
        }

        DiffRequestChain chain = new ChangeDiffRequestChain(presentables);
        chain.setIndex(index);

        for (Map.Entry<Key, Object> entry : context.getChainContext().entrySet()) {
            chain.putUserData(entry.getKey(), entry.getValue());
        }
        chain.putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, context.getActions());

        DiffManager.getInstance().showDiff(project, chain, context.getDialogHints());
    }
}
