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
import consulo.component.ProcessCanceledException;
import consulo.container.boot.ContainerPathManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.Component;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageBoxRemember;
import consulo.ui.MessageBoxes;
import consulo.ui.MessageButtonRole;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.patch.IdeaTextPatchBuilder;
import consulo.versionControlSystem.change.patch.PatchWriter;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelveChangesManagerImpl;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
    public LocalizeValue getActionText() {
        return LocalizeValue.localizeTODO("Create Patch…");
    }

    @Override
    public String getHelpId() {
        return "reference.dialogs.vcs.patch.create";
    }

    @Override
    public CommitSession createCommitSession(CommitContext commitContext) {
        return new CreatePatchCommitSession(myProject);
    }

    private class CreatePatchCommitSession implements CommitSession, CommitSessionContextAware {
        private final Project myProject;
        private @Nullable CreatePatchConfigurationPanel myPanel;
        private @Nullable UIAccess myUIAccess;
        private CommitContext myCommitContext = new CommitContext();

        public CreatePatchCommitSession(Project project) {
            myProject = project;
        }

        @Override
        public void setContext(CommitContext context) {
            myCommitContext = context;
        }

        @Override
        @RequiredUIAccess
        public Component getAdditionalConfigurationUI(Collection<Change> changes, String commitMessage, Runnable stateChanged) {
            myUIAccess = UIAccess.current();
            CreatePatchConfigurationPanel panel = new CreatePatchConfigurationPanel(myProject);
            myPanel = panel;

            String patchPath = ProjectPropertiesComponent.getInstance(myProject).getValue(VCS_PATCH_PATH_KEY);
            if (StringUtil.isEmptyOrSpaces(patchPath)) {
                VcsApplicationSettings settings = VcsApplicationSettings.getInstance();
                patchPath = settings.getState().PATCH_STORAGE_LOCATION;
                if (patchPath == null) {
                    patchPath = myProject.getBaseDir() == null ? ContainerPathManager.get().getHomePath() : myProject.getBaseDir()
                        .getPresentableUrl();
                }
            }

            panel.setFileName(ShelveChangesManagerImpl.suggestPatchName(myProject, commitMessage, new File(patchPath), null));
            File commonAncestor = ChangesUtil.findCommonAncestor(changes);
            panel.setCommonParentPath(commonAncestor);
            Set<AbstractVcs> affectedVcses = ChangesUtil.getAffectedVcses(changes, myProject);
            if (affectedVcses.size() == 1 && commonAncestor != null) {
                VirtualFile vcsRoot = VcsUtil.getVcsRootFor(myProject, VcsUtil.getFilePath(commonAncestor));
                if (vcsRoot != null) {
                    panel.selectBasePath(vcsRoot);
                }
            }
            panel.setReversePatch(false);
            panel.setStateChanged(stateChanged);
            return panel.getComponent();
        }

        @Override
        public LocalizeValue getAdditionalConfigurationTitle() {
            return VcsLocalize.createPatchSettingsDialogTitle();
        }

        @Override
        public boolean canExecute(Collection<Change> changes, String commitMessage) {
            return myPanel != null && myPanel.isOkToExecute();
        }

        @Override
        public void execute(Collection<Change> changes, String commitMessage) {
            CreatePatchConfigurationPanel panel = myPanel;
            UIAccess uiAccess = myUIAccess;
            if (panel == null || uiAccess == null) {
                return;
            }
            String fileName = panel.getFileName();
            File file = new File(fileName).getAbsoluteFile();
            if (file.exists() && !confirmOverwrite(uiAccess, file)) {
                return;
            }
            if (file.getParentFile() == null) {
                showError(uiAccess, VcsLocalize.createPatchErrorTitle(VcsLocalize.patchCreationCanNotWritePatchError(file.getPath())));
                return;
            }

            int binaryCount = 0;
            for (Change change : changes) {
                if (ChangesUtil.isBinaryChange(change)) {
                    binaryCount++;
                }
            }
            if (binaryCount == changes.size()) {
                uiAccess.give(() -> MessageBoxes.okInfo(VcsLocalize.createPatchAllBinary())
                    .title(VcsLocalize.createPatchCommitActionTitle())
                    .showAsync(myProject));
                return;
            }
            try {
                file.getParentFile().mkdirs();
                VcsConfiguration.getInstance(myProject).acceptLastCreatedPatchName(file.getName());
                String patchPath = file.getParent();
                VcsApplicationSettings.getInstance().getState().PATCH_STORAGE_LOCATION = patchPath;
                ProjectPropertiesComponent.getInstance(myProject).setValue(VCS_PATCH_PATH_KEY, patchPath);
                boolean reversePatch = panel.isReversePatch();

                String baseDirName = panel.getBaseDirName();
                List<FilePatch> patches = IdeaTextPatchBuilder.buildPatch(myProject, changes, baseDirName, reversePatch);
                PatchWriter.writePatches(myProject, fileName, baseDirName, patches, myCommitContext, panel.getEncoding());
                LocalizeValue message = binaryCount == 0
                    ? VcsLocalize.createPatchSuccessConfirmation(file.getPath())
                    : VcsLocalize.createPatchPartialSuccessConfirmation(file.getPath(), binaryCount);
                uiAccess.give(() -> offerToOpenPatch(uiAccess, message, file));
            }
            catch (ProcessCanceledException ignored) {
            }
            catch (Exception ex) {
                LOG.info(ex);
                showError(uiAccess, VcsLocalize.createPatchErrorTitle(ex.getMessage()));
            }
        }

        private boolean confirmOverwrite(UIAccess uiAccess, File file) {
            CompletableFuture<Boolean> answer = new CompletableFuture<>();
            uiAccess.give(() -> MessageBoxes.yesNo()
                .asWarning()
                .title(CommonLocalize.titleWarning())
                .text(VcsLocalize.patchApplyAlreadyExistsOverwritePrompt(file.getName(), file.getParent()))
                .showAsync(myProject)
                .whenComplete((yes, error) -> answer.complete(Boolean.TRUE.equals(yes))));
            return answer.join();
        }

        private void showError(UIAccess uiAccess, LocalizeValue message) {
            uiAccess.give(() -> MessageBoxes.okError(message).title(CommonLocalize.titleError()).showAsync(myProject));
        }

        @RequiredUIAccess
        private void offerToOpenPatch(UIAccess uiAccess, LocalizeValue message, File file) {
            VcsConfiguration configuration = VcsConfiguration.getInstance(myProject);
            MessageBoxBuilder.<Boolean>create()
                .asInfo()
                .title(VcsLocalize.createPatchCommitActionTitle())
                .text(message)
                .button(MessageButtonRole.YES, LocalizeValue.of(Platform.current().fileManagerName()), Boolean.TRUE)
                .asDefaultButton()
                .button(MessageButtonRole.NO, CommonLocalize.actionClose(), Boolean.FALSE)
                .asExitButton()
                .remember(new MessageBoxRemember<>() {
                    @Override
                    public void setValue(Boolean value) {
                        configuration.SHOW_PATCH_IN_EXPLORER = value;
                    }

                    @Override
                    public @Nullable Boolean getValue() {
                        return configuration.SHOW_PATCH_IN_EXPLORER;
                    }

                    @Override
                    public LocalizeValue getMessageText() {
                        return CommonLocalize.dialogOptionsDoNotAsk();
                    }
                })
                .showAsync(myProject)
                .whenComplete((open, error) -> {
                    if (Boolean.TRUE.equals(open)) {
                        Platform.current().openFileInFileManager(file, uiAccess);
                    }
                });
        }

        @Override
        public void executionCanceled() {
        }

        @Override
        @RequiredUIAccess
        public LocalizeValue validateFields() {
            return myPanel == null ? LocalizeValue.empty() : myPanel.validateFields();
        }

        @Override
        public @Nullable String getHelpId() {
            return null;
        }
    }
}
