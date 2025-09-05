/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.versionControlSystem.impl.internal.change.patch;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.CommonBundle;
import consulo.component.ProcessCanceledException;
import consulo.container.boot.ContainerPathManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.project.util.WaitForProgressToShow;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.ui.ex.awt.util.AskWithOpenFileInFileManager;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.patch.IdeaTextPatchBuilder;
import consulo.versionControlSystem.change.patch.PatchWriter;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelveChangesManager;
import consulo.versionControlSystem.impl.internal.change.ui.awt.SessionDialog;
import consulo.versionControlSystem.impl.internal.ui.awt.CreatePatchConfigurationPanel;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@ExtensionImpl
public class CreatePatchCommitExecutor extends LocalCommitExecutor {
    private static final Logger LOG = Logger.getInstance(CreatePatchCommitExecutor.class);

    private static final String VCS_PATCH_PATH_KEY = "vcs.patch.path";

    private final Project myProject;

    @Inject
    public CreatePatchCommitExecutor(Project project) {
        myProject = project;
    }

    @Override
    @Nls
    public String getActionText() {
        return "Create Patch...";
    }

    @Override
    public String getHelpId() {
        return "reference.dialogs.vcs.patch.create";
    }

    @Override
    @Nonnull
    public CommitSession createCommitSession() {
        return new CreatePatchCommitSession(myProject);
    }

    private class CreatePatchCommitSession implements CommitSession, CommitSessionContextAware {
        private final CreatePatchConfigurationPanel myPanel;
        private CommitContext myCommitContext;
        private final Project myProject;

        public CreatePatchCommitSession(Project project) {
            myProject = project;
            myPanel = new CreatePatchConfigurationPanel(myProject);
        }

        @Override
        public void setContext(CommitContext context) {
            myCommitContext = context;
        }

        @Override
        @Nullable
        public JComponent getAdditionalConfigurationUI() {
            return myPanel.getPanel();
        }

        @Override
        public JComponent getAdditionalConfigurationUI(Collection<Change> changes, String commitMessage) {
            String patchPath = ProjectPropertiesComponent.getInstance(myProject).getValue(VCS_PATCH_PATH_KEY);
            if (StringUtil.isEmptyOrSpaces(patchPath)) {
                VcsApplicationSettings settings = VcsApplicationSettings.getInstance();
                patchPath = settings.PATCH_STORAGE_LOCATION;
                if (patchPath == null) {
                    patchPath = myProject.getBaseDir() == null ? ContainerPathManager.get().getHomePath() : myProject.getBaseDir().getPresentableUrl();
                }
            }

            myPanel.setFileName(ShelveChangesManager.suggestPatchName(myProject, commitMessage, new File(patchPath), null));
            File commonAncestor = ChangesUtil.findCommonAncestor(changes);
            myPanel.setCommonParentPath(commonAncestor);
            Set<AbstractVcs> affectedVcses = ChangesUtil.getAffectedVcses(changes, myProject);
            if (affectedVcses.size() == 1 && commonAncestor != null) {
                VirtualFile vcsRoot = VcsUtil.getVcsRootFor(myProject, VcsUtil.getFilePath(commonAncestor));
                if (vcsRoot != null) {
                    myPanel.selectBasePath(vcsRoot);
                }
            }
            myPanel.setReversePatch(false);

            JComponent panel = myPanel.getPanel();
            panel.putClientProperty(SessionDialog.VCS_CONFIGURATION_UI_TITLE, "Patch File Settings");
            return panel;
        }

        @Override
        public boolean canExecute(Collection<Change> changes, String commitMessage) {
            return myPanel.isOkToExecute();
        }

        @Override
        public void execute(Collection<Change> changes, String commitMessage) {
            String fileName = myPanel.getFileName();
            final File file = new File(fileName).getAbsoluteFile();
            if (file.exists()) {
                final int[] result = new int[1];
                WaitForProgressToShow.runOrInvokeAndWaitAboveProgress(new Runnable() {
                    @Override
                    public void run() {
                        result[0] = Messages.showYesNoDialog(myProject, "File " + file.getName() + " (" + file.getParent() + ")" +
                                " already exists.\nDo you want to overwrite it?",
                            CommonBundle.getWarningTitle(), Messages.getWarningIcon());
                    }
                });
                if (Messages.NO == result[0]) {
                    return;
                }
            }
            if (file.getParentFile() == null) {
                WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
                    @Override
                    public void run() {
                        Messages.showErrorDialog(myProject, VcsBundle.message("create.patch.error.title", "Can not write patch to specified file: " +
                            file.getPath()), CommonBundle.getErrorTitle());
                    }
                }, ModalityState.nonModal(), myProject);
                return;
            }

            int binaryCount = 0;
            for (Change change : changes) {
                if (ChangesUtil.isBinaryChange(change)) {
                    binaryCount++;
                }
            }
            if (binaryCount == changes.size()) {
                WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
                    @Override
                    public void run() {
                        Messages.showInfoMessage(myProject, VcsBundle.message("create.patch.all.binary"),
                            VcsBundle.message("create.patch.commit.action.title"));
                    }
                }, null, myProject);
                return;
            }
            try {
                file.getParentFile().mkdirs();
                VcsConfiguration.getInstance(myProject).acceptLastCreatedPatchName(file.getName());
                String patchPath = file.getParent();
                VcsApplicationSettings.getInstance().PATCH_STORAGE_LOCATION = patchPath;
                ProjectPropertiesComponent.getInstance(myProject).setValue(VCS_PATCH_PATH_KEY, patchPath);
                boolean reversePatch = myPanel.isReversePatch();

                String baseDirName = myPanel.getBaseDirName();
                List<FilePatch> patches = IdeaTextPatchBuilder.buildPatch(myProject, changes, baseDirName, reversePatch);
                PatchWriter.writePatches(myProject, fileName, baseDirName, patches, myCommitContext, myPanel.getEncoding());
                LocalizeValue message;
                if (binaryCount == 0) {
                    message = VcsLocalize.createPatchSuccessConfirmation(file.getPath());
                }
                else {
                    message = VcsLocalize.createPatchPartialSuccessConfirmation(file.getPath(), binaryCount);
                }
                WaitForProgressToShow.runOrInvokeLaterAboveProgress(() -> {
                    VcsConfiguration configuration = VcsConfiguration.getInstance(myProject);
                    if (Boolean.TRUE.equals(configuration.SHOW_PATCH_IN_EXPLORER)) {
                        Platform.current().openFileInFileManager(file, UIAccess.current());
                    }
                    else if (Boolean.FALSE.equals(configuration.SHOW_PATCH_IN_EXPLORER)) {
                        return;
                    }
                    else {
                        configuration.SHOW_PATCH_IN_EXPLORER =
                            AskWithOpenFileInFileManager.showDialog(myProject, message, VcsLocalize.createPatchCommitActionTitle(), file);
                    }
                }, null, myProject);
            }
            catch (ProcessCanceledException e) {
                //
            }
            catch (final Exception ex) {
                LOG.info(ex);
                WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
                    @Override
                    public void run() {
                        Messages.showErrorDialog(myProject, VcsBundle.message("create.patch.error.title", ex.getMessage()),
                            CommonBundle.getErrorTitle());
                    }
                }, null, myProject);
            }
        }

        @Override
        public void executionCanceled() {
        }

        @RequiredUIAccess
        @Override
        @Nullable
        public ValidationInfo validateFields() {
            return myPanel.validateFields();
        }

        @Override
        public String getHelpId() {
            return null;
        }
    }
}
