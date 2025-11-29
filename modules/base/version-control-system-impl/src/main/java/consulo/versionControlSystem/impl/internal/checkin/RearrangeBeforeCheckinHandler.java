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
package consulo.versionControlSystem.impl.internal.checkin;

import consulo.document.FileDocumentManager;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.internal.SharedLayoutProcessors;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.NonFocusableCheckBox;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinHandlerUtil;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.CheckBoxRefreshableOnComponent;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public class RearrangeBeforeCheckinHandler extends CheckinHandler implements CheckinMetaHandler {
    public static final String COMMAND_NAME = CodeInsightBundle.message("process.rearrange.code.before.commit");

    private final Project myProject;
    private final CheckinProjectPanel myPanel;

    public RearrangeBeforeCheckinHandler(@Nonnull Project project, @Nonnull CheckinProjectPanel panel) {
        myProject = project;
        myPanel = panel;
    }

    @RequiredUIAccess
    @Override
    @Nullable
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        VcsConfiguration configuration = VcsConfiguration.getInstance(myProject);

        return new CheckBoxRefreshableOnComponent(
            VcsLocalize.checkboxCheckinOptionsRearrangeCode(),
            myProject,
            LocalizeValue.localizeTODO("Impossible until indices are up-to-date"),
            () -> configuration.REARRANGE_BEFORE_PROJECT_COMMIT,
            value -> configuration.REARRANGE_BEFORE_PROJECT_COMMIT = value
        );
    }

    @Override
    public void runCheckinHandlers(Runnable finishAction) {
        Runnable performCheckoutAction = () -> {
            FileDocumentManager.getInstance().saveAllDocuments();
            finishAction.run();
        };

        if (VcsConfiguration.getInstance(myProject).REARRANGE_BEFORE_PROJECT_COMMIT && !DumbService.isDumb(myProject)) {
            SharedLayoutProcessors processors = myProject.getInstance(SharedLayoutProcessors.class);

            processors.createRearrangeCodeProcessor(
                CheckinHandlerUtil.getPsiFiles(myProject, myPanel.getVirtualFiles()),
                COMMAND_NAME,
                performCheckoutAction
            ).run();
        }
        else {
            performCheckoutAction.run();
        }
    }
}
