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
package consulo.ide.newProject.actions;

import com.intellij.CommonBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.impl.util.NewOrImportModuleUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import consulo.disposer.Disposable;
import consulo.ide.newProject.NewModuleBuilderProcessor;
import consulo.ide.newProject.ui.NewProjectDialog;
import consulo.ide.newProject.ui.NewProjectPanel;
import consulo.ide.welcomeScreen.WelcomeScreenSlideAction;
import consulo.ide.welcomeScreen.WelcomeScreenSlider;
import consulo.ide.wizard.newModule.NewModuleWizardContext;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * @author VISTALL
 */
public class NewProjectAction extends WelcomeScreenSlideAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(NewProjectAction.class);

  static class SlideNewProjectPanel extends NewProjectPanel {
    private final WelcomeScreenSlider owner;
    private JButton myOkButton;
    private JButton myCancelButton;

    private Runnable myOkAction;
    private Runnable myCancelAction;

    @RequiredUIAccess
    public SlideNewProjectPanel(@Nonnull Disposable parentDisposable, WelcomeScreenSlider owner, @Nullable Project project, @Nullable VirtualFile virtualFile) {
      super(parentDisposable, project, virtualFile);
      this.owner = owner;
    }

    @Override
    public void setOKActionEnabled(boolean enabled) {
      myOkButton.setEnabled(enabled);
    }

    @Override
    public void setOKActionText(@Nonnull String text) {
      myOkButton.setText(text);
    }

    @Override
    public void setCancelText(@Nonnull String text) {
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
    protected JPanel createSouthPanel() {
      JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, SystemInfo.isMacOSLeopard ? 0 : 5, 0));

      myCancelButton = new JButton(CommonBundle.getCancelButtonText());
      myCancelButton.addActionListener(e -> doCancelAction());

      buttonsPanel.add(myCancelButton);

      myOkButton = new JButton(CommonBundle.getOkButtonText()) {
        @Override
        public boolean isDefaultButton() {
          return true;
        }
      };
      myOkButton.setEnabled(false);

      myOkButton.addActionListener(e -> doOkAction());
      buttonsPanel.add(myOkButton);

      return JBUI.Panels.simplePanel().addToRight(buttonsPanel);
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

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    Project project = e.getProject();
    NewProjectDialog dialog = new NewProjectDialog(project, null);

    if (dialog.showAndGet()) {
      generateProject(project, dialog.getProjectPanel());
    }
  }

  @Nonnull
  @Override
  public JComponent createSlide(@Nonnull Disposable parentDisposable, @Nonnull WelcomeScreenSlider owner) {
    owner.setTitle(IdeBundle.message("title.new.project"));

    return new SlideNewProjectPanel(parentDisposable, owner, null, null);
  }

  @RequiredUIAccess
  protected static void generateProject(Project project, @Nonnull final NewProjectPanel projectPanel) {
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

    final File location = new File(context.getPath());
    final int childCount = location.exists() ? location.list().length : 0;
    if (!location.exists() && !location.mkdirs()) {
      Messages.showErrorDialog(project, "Cannot create directory '" + location + "'", "Create Project");
      return;
    }

    final VirtualFile baseDir = WriteAction.compute(() -> LocalFileSystem.getInstance().refreshAndFindFileByIoFile(location));
    baseDir.refresh(false, true);

    if (childCount > 0) {
      int rc = Messages.showYesNoDialog(project, "The directory '" + location + "' is not empty. Continue?", "Create New Project", Messages.getQuestionIcon());
      if (rc == Messages.NO) {
        return;
      }
    }

    RecentProjectsManager.getInstance().setLastProjectCreationLocation(location.getParent());

    UIAccess uiAccess = UIAccess.current();
    ProjectManager.getInstance().openProjectAsync(baseDir, uiAccess).doWhenDone((openedProject) -> {
      uiAccess.give(() -> NewOrImportModuleUtil.doCreate(panel, openedProject, baseDir));
    });
  }
}
