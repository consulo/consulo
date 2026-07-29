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

import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.SeparatorWithText;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.distributed.repository.Repository;

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

    protected List<Object> collectActions() {
        List<Object> preActions = new ArrayList<>();

        addRegisteredAction(preActions, "Vcs.UpdateProject");
        addRegisteredAction(preActions, "CheckinProject");
        addRegisteredAction(preActions, "Vcs.Push");

        List<Object> result = new ArrayList<>(preActions);
        if (!preActions.isEmpty()) {
            result.add(new SeparatorWithText());
        }

        appendFirstPopupActions(result::add);

        return result;
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
