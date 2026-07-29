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
package consulo.desktop.awt.versionSystemControl.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopup;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupFactory;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-07-30
 */
@Singleton
@ServiceImpl
public class DvcsBranchesTreePopupFactoryImpl implements DvcsBranchesTreePopupFactory {
    private final Project myProject;

    @Inject
    public DvcsBranchesTreePopupFactoryImpl(Project project) {
        myProject = project;
    }

    @Override
    public <R extends Repository> DvcsBranchesTreePopup<R> createTreePopup(DvcsBranchesTreePopupModel<R> model) {
        return new DvcsBranchesTreePopupImpl<>(myProject, model);
    }
}
