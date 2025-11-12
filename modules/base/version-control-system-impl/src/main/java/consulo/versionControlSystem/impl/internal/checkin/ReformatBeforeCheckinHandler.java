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
import consulo.language.codeStyle.FormatterUtil;
import consulo.language.editor.internal.SharedLayoutProcessors;
import consulo.project.Project;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinHandlerUtil;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class ReformatBeforeCheckinHandler extends CheckinHandler implements CheckinMetaHandler {
    public static final String COMMAND_NAME = FormatterUtil.REFORMAT_BEFORE_COMMIT_COMMAND_NAME;

    protected final Project myProject;
    private final CheckinProjectPanel myPanel;

    public ReformatBeforeCheckinHandler(Project project, CheckinProjectPanel panel) {
        myProject = project;
        myPanel = panel;
    }

    @Override
    @Nullable
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        final JCheckBox reformatBox = new JCheckBox(VcsLocalize.checkboxCheckinOptionsReformatCode().get());

        return new RefreshableOnComponent() {
            @Override
            public JComponent getComponent() {
                JPanel panel = new JPanel(new GridLayout(1, 0));
                panel.add(reformatBox);
                return panel;
            }

            @Override
            public void refresh() {
            }

            @Override
            public void saveState() {
                getSettings().REFORMAT_BEFORE_PROJECT_COMMIT = reformatBox.isSelected();
            }

            @Override
            public void restoreState() {
                reformatBox.setSelected(getSettings().REFORMAT_BEFORE_PROJECT_COMMIT);
            }
        };

    }

    protected VcsConfiguration getSettings() {
        return VcsConfiguration.getInstance(myProject);
    }

    @Override
    public void runCheckinHandlers(Runnable finishAction) {
        VcsConfiguration configuration = VcsConfiguration.getInstance(myProject);
        Collection<VirtualFile> files = myPanel.getVirtualFiles();

        Runnable performCheckoutAction = () -> {
            FileDocumentManager.getInstance().saveAllDocuments();
            finishAction.run();
        };

        if (reformat(configuration, true)) {
            SharedLayoutProcessors processors = myProject.getInstance(SharedLayoutProcessors.class);

            processors.createReformatCodeProcessor(
                CheckinHandlerUtil.getPsiFiles(myProject, files),
                COMMAND_NAME, performCheckoutAction
            ).run();
        }
        else {
            performCheckoutAction.run();
        }

    }

    private static boolean reformat(VcsConfiguration configuration, boolean checkinProject) {
        return checkinProject ? configuration.REFORMAT_BEFORE_PROJECT_COMMIT : configuration.REFORMAT_BEFORE_FILE_COMMIT;
    }
}
