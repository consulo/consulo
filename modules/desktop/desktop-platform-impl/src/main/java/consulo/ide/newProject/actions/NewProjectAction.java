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
import com.intellij.ide.impl.util.NewProjectUtilPlatform;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.PlatformProjectOpenProcessor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.annotations.RequiredDispatchThread;
import consulo.ide.newProject.NewProjectDialog;
import consulo.ide.newProject.NewProjectPanel;
import consulo.ide.welcomeScreen.FlatWelcomeScreen;
import consulo.ide.welcomeScreen.WelcomeScreenSlideAction;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.function.Consumer;

/**
 * @author VISTALL
 */
public class NewProjectAction extends WelcomeScreenSlideAction implements DumbAware {
  static class SlideNewProjectPanel extends NewProjectPanel {
    private JButton myOkButton;

    public SlideNewProjectPanel(@Nonnull Disposable parentDisposable, @Nullable Project project, @Nullable VirtualFile virtualFile) {
      super(parentDisposable, project, virtualFile);
    }

    @Override
    public void setOKActionEnabled(boolean enabled) {
      myOkButton.setEnabled(enabled);
    }

    @Override
    protected JPanel createSouthPanel() {
      JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, SystemInfo.isMacOSLeopard ? 0 : 5, 0));
      myOkButton = new JButton(CommonBundle.getOkButtonText()) {
        @Override
        public boolean isDefaultButton() {
          return true;
        }
      };
      myOkButton.setMargin(JBUI.insets(2, 16));
      myOkButton.setEnabled(false);

      myOkButton.addActionListener(e -> generateProject(null, this));
      buttonsPanel.add(myOkButton);

      JButton cancelButton = new JButton(CommonBundle.getCancelButtonText());
      cancelButton.setMargin(JBUI.insets(2, 16));
      cancelButton.addActionListener(e -> {
        FlatWelcomeScreen flatWelcomeScreen = (FlatWelcomeScreen)UIUtil.findParentByCondition(buttonsPanel, x -> x instanceof FlatWelcomeScreen);

        if (flatWelcomeScreen != null) {
          flatWelcomeScreen.replacePanel(this);
        }
      });
      buttonsPanel.add(cancelButton);

      return JBUI.Panels.simplePanel().addToRight(buttonsPanel);
    }
  }

  @RequiredDispatchThread
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
  public JComponent createSlide(@Nonnull Disposable parentDisposable, Consumer<String> titleChanger) {
    titleChanger.accept(IdeBundle.message("title.new.project"));

    return new SlideNewProjectPanel(parentDisposable, null, null);
  }

  @RequiredDispatchThread
  protected static void generateProject(Project project, @Nonnull final NewProjectPanel projectPanel) {
    final File location = new File(projectPanel.getLocationText());
    final int childCount = location.exists() ? location.list().length : 0;
    if (!location.exists() && !location.mkdirs()) {
      Messages.showErrorDialog(project, "Cannot create directory '" + location + "'", "Create Project");
      return;
    }

    final VirtualFile baseDir = ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
      @Override
      public VirtualFile compute() {
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(location);
      }
    });
    baseDir.refresh(false, true);

    if (childCount > 0) {
      int rc = Messages.showYesNoDialog(project, "The directory '" + location + "' is not empty. Continue?", "Create New Project", Messages.getQuestionIcon());
      if (rc == Messages.NO) {
        return;
      }
    }

    RecentProjectsManager.getInstance().setLastProjectCreationLocation(location.getParent());

    AsyncResult<Project> result = new AsyncResult<>();
    PlatformProjectOpenProcessor.getInstance().doOpenProjectAsync(result, baseDir, null, false, UIAccess.current());
    result.doWhenProcessed(newProject -> NewProjectUtilPlatform.doCreate(projectPanel, newProject, baseDir));
  }
}
