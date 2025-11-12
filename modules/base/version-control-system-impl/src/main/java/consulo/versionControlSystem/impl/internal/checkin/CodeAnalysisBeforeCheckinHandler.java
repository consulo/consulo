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
package consulo.versionControlSystem.impl.internal.checkin;

import consulo.application.Application;
import consulo.component.ProcessCanceledException;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.CodeSmellDetector;
import consulo.versionControlSystem.CodeSmellInfo;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinHandlerUtil;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The check-in handler which performs code analysis before check-in. Source code for this class
 * is provided as a sample of using the {@link CheckinHandler} API.
 *
 * @author lesya
 * @since 5.1
 */
public class CodeAnalysisBeforeCheckinHandler extends CheckinHandler {
    private final Project myProject;
    private final CheckinProjectPanel myCheckinPanel;
    private static final Logger LOG = Logger.getInstance(CodeAnalysisBeforeCheckinHandler.class);

    public CodeAnalysisBeforeCheckinHandler(Project project, CheckinProjectPanel panel) {
        myProject = project;
        myCheckinPanel = panel;
    }

    @Override
    @Nullable
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        final JCheckBox checkBox = new JCheckBox(VcsLocalize.beforeCheckinStandardOptionsCheckSmells().get());
        return new RefreshableOnComponent() {
            @Override
            public JComponent getComponent() {
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(checkBox);
                CheckinHandlerUtil.disableWhenDumb(myProject, checkBox, "Code analysis is impossible until indices are up-to-date");
                return panel;
            }

            @Override
            public void refresh() {
            }

            @Override
            public void saveState() {
                getSettings().CHECK_CODE_SMELLS_BEFORE_PROJECT_COMMIT = checkBox.isSelected();
            }

            @Override
            public void restoreState() {
                checkBox.setSelected(getSettings().CHECK_CODE_SMELLS_BEFORE_PROJECT_COMMIT);
            }
        };
    }

    private VcsConfiguration getSettings() {
        return VcsConfiguration.getInstance(myProject);
    }

    @RequiredUIAccess
    private ReturnResult processFoundCodeSmells(List<CodeSmellInfo> codeSmells, @Nullable CommitExecutor executor) {
        int errorCount = collectErrors(codeSmells);
        int warningCount = codeSmells.size() - errorCount;
        String commitButtonText = executor != null ? executor.getActionText().get() : myCheckinPanel.getCommitActionName().get();
        if (commitButtonText.endsWith("...")) {
            commitButtonText = commitButtonText.substring(0, commitButtonText.length() - 3);
        }

        int answer = Messages.showYesNoCancelDialog(
            myProject,
            VcsLocalize.beforeCommitFilesContainCodeSmellsEditThemConfirmText(errorCount, warningCount).get(),
            VcsLocalize.codeSmellsErrorMessagesTabName().get(),
            VcsLocalize.codeSmellsReviewButton().get(),
            commitButtonText,
            CommonLocalize.buttonCancel().get(),
            UIUtil.getWarningIcon()
        );
        if (answer == 0) {
            CodeSmellDetector.getInstance(myProject).showCodeSmellErrors(codeSmells);
            return ReturnResult.CLOSE_WINDOW;
        }
        else if (answer == 2 || answer == -1) {
            return ReturnResult.CANCEL;
        }
        else {
            return ReturnResult.COMMIT;
        }
    }

    private static int collectErrors(List<CodeSmellInfo> codeSmells) {
        int result = 0;
        for (CodeSmellInfo codeSmellInfo : codeSmells) {
          if (codeSmellInfo.getSeverity() == HighlightSeverity.ERROR) {
            result++;
          }
        }
        return result;
    }

    @Override
    @RequiredUIAccess
    public ReturnResult beforeCheckin(CommitExecutor executor, BiConsumer<Object, Object> additionalDataConsumer) {
        if (getSettings().CHECK_CODE_SMELLS_BEFORE_PROJECT_COMMIT) {
            if (DumbService.getInstance(myProject).isDumb()) {
                if (Messages.showOkCancelDialog(
                    myProject,
                    "Code analysis can't be performed while " + Application.get().getName() + " updates the indices in background.\n" +
                        "You can commit the changes without running inspections, or you can wait until indices are built.",
                    "Code analysis is not possible right now",
                    "&Wait",
                    "&Commit",
                    null
                ) == DialogWrapper.OK_EXIT_CODE) {
                    return ReturnResult.CANCEL;
                }
                return ReturnResult.COMMIT;
            }

            try {
                List<CodeSmellInfo> codeSmells =
                    CodeSmellDetector.getInstance(myProject).findCodeSmells(new ArrayList<>(myCheckinPanel.getVirtualFiles()));
                if (!codeSmells.isEmpty()) {
                    return processFoundCodeSmells(codeSmells, executor);
                }
                else {
                    return ReturnResult.COMMIT;
                }
            }
            catch (ProcessCanceledException e) {
                return ReturnResult.CANCEL;
            }
            catch (Exception e) {
                LOG.error(e);
                if (Messages.showOkCancelDialog(myProject,
                    "Code analysis failed with exception: " + e.getClass().getName() + ": " + e.getMessage(),
                    "Code analysis failed",
                    "&Commit",
                    "&Cancel",
                    null
                ) == DialogWrapper.OK_EXIT_CODE) {
                    return ReturnResult.COMMIT;
                }
                return ReturnResult.CANCEL;
            }
        }
        else {
            return ReturnResult.COMMIT;
        }
    }
}
