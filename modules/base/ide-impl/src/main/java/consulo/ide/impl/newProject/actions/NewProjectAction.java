/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.newProject.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.WriteAction;
import consulo.disposer.Disposable;
import consulo.ide.impl.newProject.ui.NewProjectDialog;
import consulo.ide.impl.newProject.ui.NewProjectPanel;
import consulo.ide.impl.welcomeScreen.WelcomeScreenSlider;
import consulo.ide.newModule.NewModuleBuilderProcessor;
import consulo.ide.newModule.NewModuleWizardContext;
import consulo.ide.newModule.NewOrImportModuleUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.internal.RecentProjectsManager;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.TitlelessDecorator;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.File;

/**
 * @author VISTALL
 */
@ActionImpl(id = "NewProject")
public class NewProjectAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(NewProjectAction.class);

    static class SlideNewProjectPanel extends NewProjectPanel {
        private final WelcomeScreenSlider owner;
        private Button myOkButton;
        private Button myCancelButton;

        private Runnable myOkAction;
        private Runnable myCancelAction;

        @RequiredUIAccess
        public SlideNewProjectPanel(
            @Nonnull Disposable parentDisposable,
            WelcomeScreenSlider owner,
            @Nullable Project project,
            @Nullable VirtualFile virtualFile,
            @Nonnull TitlelessDecorator titlelessDecorator
        ) {
            super(parentDisposable, project, virtualFile, titlelessDecorator);
            this.owner = owner;
        }

        @Override
        @RequiredUIAccess
        public void setOKActionEnabled(boolean enabled) {
            myOkButton.setEnabled(enabled);
        }

        @Override
        @RequiredUIAccess
        public void setOKActionText(@Nonnull LocalizeValue text) {
            myOkButton.setText(text);
        }

        @Override
        @RequiredUIAccess
        public void setCancelText(@Nonnull LocalizeValue text) {
            myCancelButton.setText(text);
        }

        @Override
        public void setCancelAction(Runnable backAction) {
            myCancelAction = backAction;
        }

        @Override
        public void setOKAction(@Nullable Runnable action) {
            myOkAction = action;
        }

        @RequiredUIAccess
        @Nonnull
        @Override
        protected JComponent createSouthPanel() {
            HorizontalLayout buttonsPanel = HorizontalLayout.create();

            myCancelButton = Button.create(CommonLocalize.buttonCancel());
            myCancelButton.addClickListener(e -> doCancelAction());

            buttonsPanel.add(myCancelButton);

            myOkButton = Button.create(CommonLocalize.buttonOk());
            myOkButton.addStyle(ButtonStyle.PRIMARY);
            myOkButton.setEnabled(false);

            myOkButton.addClickListener(e -> doOkAction());
            buttonsPanel.add(myOkButton);

            return (JComponent) TargetAWT.to(DockLayout.create().right(buttonsPanel));
        }

        private void doCancelAction() {
            if (myCancelAction != null) {
                myCancelAction.run();
            }
            else {
                owner.removeSlide(this);
            }
        }

        @RequiredUIAccess
        private void doOkAction() {
            if (myOkAction != null) {
                myOkAction.run();
            }
            else {
                generateProject(null, this);
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        NewProjectDialog dialog = new NewProjectDialog(project, null);

        if (dialog.showAndGet()) {
            generateProject(project, dialog.getProjectPanel());
        }
    }

    @RequiredUIAccess
    protected static void generateProject(Project project, @Nonnull NewProjectPanel projectPanel) {
        NewModuleWizardContext context = projectPanel.getWizardContext();
        NewModuleBuilderProcessor<NewModuleWizardContext> processor = projectPanel.getProcessor();
        if (processor == null || context == null) {
            LOG.error("Impossible situation. Calling generate project with null data: " + processor + "/" + context);
            return;
        }

        generateProjectAsync(project, projectPanel);
    }

    @RequiredUIAccess
    private static void generateProjectAsync(Project project, @Nonnull NewProjectPanel panel) {
        // leave current step
        panel.finish();

        NewModuleWizardContext context = panel.getWizardContext();

        File location = new File(context.getPath());
        int childCount = location.exists() ? location.list().length : 0;
        if (!location.exists() && !location.mkdirs()) {
            Messages.showErrorDialog(project, "Cannot create directory '" + location + "'", "Create Project");
            return;
        }

        VirtualFile baseDir = WriteAction.compute(() -> LocalFileSystem.getInstance().refreshAndFindFileByIoFile(location));
        baseDir.refresh(false, true);

        if (childCount > 0) {
            int rc = Messages.showYesNoDialog(
                project,
                "The directory '" + location + "' is not empty. Continue?",
                "Create New Project",
                UIUtil.getQuestionIcon()
            );
            if (rc == Messages.NO) {
                return;
            }
        }

        RecentProjectsManager.getInstance().setLastProjectCreationLocation(location.getParent());

        UIAccess uiAccess = UIAccess.current();
        ProjectManager.getInstance()
            .openProjectAsync(baseDir, uiAccess)
            .doWhenDone((openedProject) -> uiAccess.give(() -> NewOrImportModuleUtil.doCreate(
                panel.getProcessor(),
                panel.getWizardContext(),
                openedProject,
                baseDir
            )));
    }
}
