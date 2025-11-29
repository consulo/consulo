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
package consulo.versionControlSystem.impl.internal.checkin;

import consulo.document.FileDocumentManager;
import consulo.language.editor.internal.SharedLayoutProcessors;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinHandlerUtil;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.CheckBoxRefreshableOnComponent;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import java.util.Collection;

public class OptimizeImportsBeforeCheckinHandler extends CheckinHandler implements CheckinMetaHandler {
    protected final Project myProject;
    private final CheckinProjectPanel myPanel;

    public OptimizeImportsBeforeCheckinHandler(Project project, CheckinProjectPanel panel) {
        myProject = project;
        myPanel = panel;
    }

    @RequiredUIAccess
    @Override
    @Nullable
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        VcsConfiguration settings = VcsConfiguration.getInstance(myProject);
        return new CheckBoxRefreshableOnComponent(
                VcsLocalize.checkboxCheckinOptionsOptimizeImports(),
                myProject,
                LocalizeValue.localizeTODO("Impossible until indices are up-to-date"),
                () -> {
                    return settings.OPTIMIZE_IMPORTS_BEFORE_PROJECT_COMMIT;
                },
                value -> settings.OPTIMIZE_IMPORTS_BEFORE_PROJECT_COMMIT = value
        );
    }

    @Override
    public void runCheckinHandlers(Runnable finishAction) {
        VcsConfiguration configuration = VcsConfiguration.getInstance(myProject);
        Collection<VirtualFile> files = myPanel.getVirtualFiles();

        Runnable performCheckoutAction = () -> {
            FileDocumentManager.getInstance().saveAllDocuments();
            finishAction.run();
        };

        if (configuration.OPTIMIZE_IMPORTS_BEFORE_PROJECT_COMMIT) {
            SharedLayoutProcessors processors = myProject.getInstance(SharedLayoutProcessors.class);

            processors.createOptimizeImportsProcessor(
                CheckinHandlerUtil.getPsiFiles(myProject, files),
                CodeInsightLocalize.processOptimizeImportsBeforeCommit().get(),
                performCheckoutAction
            ).run();
        }
        else {
            finishAction.run();
        }
    }
}
