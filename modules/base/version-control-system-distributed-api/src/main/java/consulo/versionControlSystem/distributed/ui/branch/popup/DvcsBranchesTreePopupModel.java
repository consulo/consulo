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
package consulo.versionControlSystem.distributed.ui.branch.popup;

import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.Presentation;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.distributed.repository.Repository;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-07-29
 */
public abstract class DvcsBranchesTreePopupModel<R extends Repository> {
    protected final ActionManager myActionManager;
    protected final R myRepository;

    protected DvcsBranchesTreePopupModel(ActionManager actionManager, R repository) {
        myActionManager = actionManager;
        myRepository = repository;
    }

    public AbstractVcs<?> getVcs() {
        return myRepository.getVcs();
    }

    public abstract DvcsBranchesTreeModel createTreeModel();

    public abstract DvcsBranchesTreePopupStepBase createTreePopupStep(DvcsBranchesTreeModel treeModel);

    public abstract DvcsBranchesTreeModel createRefTreeModel();

    public @Nullable ActionGroup createRefActionGroup(DvcsRef ref) {
        return null;
    }

    public @Nullable AnAction createNewBranchAction() {
        return null;
    }

    public @Nullable AnAction createCheckoutAction(DvcsRef ref) {
        return null;
    }

    /**
     * The registered group whose actions head the popup, or {@code null} for Update, Commit and Push.
     */
    protected @Nullable String getTopLevelActionGroupId() {
        return null;
    }

    protected List<Object> collectActions() {
        List<Object> preActions = new ArrayList<>();

        String groupId = getTopLevelActionGroupId();
        if (groupId != null && myActionManager.getAction(groupId) instanceof ActionGroup group) {
            collectGroupActions(group, preActions);
            while (!preActions.isEmpty() && preActions.get(preActions.size() - 1) instanceof DvcsBranchesTreeSeparator) {
                preActions.remove(preActions.size() - 1);
            }
        }
        else {
            addRegisteredAction(preActions, "Vcs.UpdateProject");
            addRegisteredAction(preActions, "CheckinProject");
            addRegisteredAction(preActions, "Vcs.Push");
        }

        List<Object> result = new ArrayList<>(preActions);
        if (!preActions.isEmpty()) {
            result.add(new DvcsBranchesTreeSeparator());
        }

        appendFirstPopupActions(result::add);

        return result;
    }

    private void collectGroupActions(ActionGroup group, List<Object> target) {
        for (AnAction child : group.getChildren(null)) {
            if (child instanceof AnSeparator) {
                if (!target.isEmpty() && !(target.get(target.size() - 1) instanceof DvcsBranchesTreeSeparator)) {
                    target.add(new DvcsBranchesTreeSeparator());
                }
            }
            else if (child instanceof ActionGroup childGroup && !childGroup.isPopup()) {
                collectGroupActions(childGroup, target);
            }
            else {
                AnAction updated = updateAction(child);
                if (updated != null) {
                    target.add(updated);
                }
            }
        }
    }

    private @Nullable AnAction updateAction(AnAction action) {
        if (action instanceof ActionGroup || !(action instanceof AnActionWithSyncUpdate syncUpdate)) {
            return action;
        }
        Presentation presentation = action.getTemplatePresentation().clone();
        DataContext dataContext = DataContext.builder().add(Project.KEY, myRepository.getProject()).build();
        syncUpdate.update(AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, presentation, dataContext));
        if (!presentation.isVisible()) {
            return null;
        }
        return new DvcsBranchesTreeUpdatedAction(action, presentation);
    }

    private void addRegisteredAction(List<Object> target, String actionId) {
        AnAction action = myActionManager.getAction(actionId);
        if (action != null) {
            target.add(action);
        }
    }

    public abstract void appendFirstPopupActions(Consumer<AnAction> consumer);

    public void appendHeaderActions(Consumer<AnAction> consumer) {
    }

    public final R getRepository() {
        return myRepository;
    }
}
