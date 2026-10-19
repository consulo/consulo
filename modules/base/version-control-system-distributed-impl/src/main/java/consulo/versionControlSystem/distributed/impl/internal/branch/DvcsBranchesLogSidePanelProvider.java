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
package consulo.versionControlSystem.distributed.impl.internal.branch;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryManager;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupModel;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupModelFactory;
import consulo.versionControlSystem.log.VcsLogSidePanelProvider;
import consulo.versionControlSystem.log.VcsLogUi;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@ExtensionImpl
public class DvcsBranchesLogSidePanelProvider implements VcsLogSidePanelProvider {
    private final Project myProject;

    @Inject
    public DvcsBranchesLogSidePanelProvider(Project project) {
        myProject = project;
    }

    @Override
    public String getId() {
        return "Dvcs.Branches";
    }

    @Override
    public LocalizeValue getTitle() {
        return DistributedVcsLocalize.branchesPaneTitle();
    }

    @Override
    public boolean isAvailable() {
        return findModel() != null;
    }

    @Override
    public @Nullable ActionGroup getToolbarActions() {
        return ActionManager.getInstance().getAction(DvcsLogBranchesToolbarGroup.class);
    }

    @Override
    @RequiredUIAccess
    public Component createComponent(VcsLogUi logUi, Disposable parentDisposable) {
        DvcsBranchesTreePopupModel<?> model = findModel();
        if (model == null) {
            return Label.create(LocalizeValue.empty());
        }
        return new DvcsBranchesLogSidePanel(myProject, logUi, model, parentDisposable).getComponent();
    }

    private @Nullable DvcsBranchesTreePopupModel<?> findModel() {
        for (Repository repository : VcsRepositoryManager.getInstance(myProject).getRepositories()) {
            DvcsBranchesTreePopupModel<?> model = myProject.getExtensionPoint(DvcsBranchesTreePopupModelFactory.class)
                .computeSafeIfAny(factory -> factory.create(repository));
            if (model != null) {
                return model;
            }
        }
        return null;
    }
}
