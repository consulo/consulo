/*
 * Copyright 2013-2016 must-be.org
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

import com.intellij.icons.AllIcons;
import com.intellij.ide.GeneralSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.welcomeScreen.NewWelcomeScreen;
import com.intellij.platform.PlatformProjectOpenProcessor;
import com.intellij.util.ui.UIUtil;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;
import consulo.annotations.RequiredWriteAction;
import consulo.ide.impl.NewModuleBuilderProcessor;
import consulo.ide.impl.NewProjectDialog;
import consulo.ide.newProject.NewProjectPanel;
import consulo.ide.welcomeScreen.FlatWelcomeScreen;
import consulo.ide.welcomeScreen.WelcomeScreenSlideAction;
import consulo.roots.impl.ExcludedContentFolderTypeProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * @author VISTALL
 */
public class NewProjectAction extends WelcomeScreenSlideAction implements DumbAware {
  static class SlideNewProjectPanel extends NewProjectPanel {

    public SlideNewProjectPanel(@NotNull Disposable parentDisposable, @Nullable Project project, @Nullable VirtualFile virtualFile) {
      super(parentDisposable, project, virtualFile);
    }

    @Override
    protected JPanel createSouthPanel() {
      JPanel buttonsPanel = new JPanel(new BorderLayout());
      JButton comp = new JButton("Cancel");
      comp.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JComponent parent = (JComponent)UIUtil.findParentByCondition(buttonsPanel, x -> x instanceof FlatWelcomeScreen);

          if(parent != null) {
            parent.remove(SlideNewProjectPanel.this);
          }
        }
      });
      buttonsPanel.add(comp, BorderLayout.EAST);

      return buttonsPanel;
    }
  }
  @RequiredDispatchThread
  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    Project project = e.getProject();
    NewProjectDialog dialog = new NewProjectDialog(project, null);

    if (dialog.showAndGet()) {
      generateProject(project, dialog.getProjectPanel());
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    if (NewWelcomeScreen.isNewWelcomeScreen(e)) {
      e.getPresentation().setIcon(AllIcons.Welcome.CreateNewProject);
    }
  }

  @NotNull
  @Override
  public JComponent createSlide(@NotNull Disposable parentDisposable) {
    return new SlideNewProjectPanel(parentDisposable, null, null);
  }

  @Nullable
  @RequiredDispatchThread
  protected Project generateProject(Project project, @NotNull final NewProjectPanel projectPanel) {
    final File location = new File(projectPanel.getLocationText());
    final int childCount = location.exists() ? location.list().length : 0;
    if (!location.exists() && !location.mkdirs()) {
      Messages.showErrorDialog(project, "Cannot create directory '" + location + "'", "Create Project");
      return null;
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
        return null;
      }
    }

    GeneralSettings.getInstance().setLastProjectCreationLocation(location.getParent());
    return PlatformProjectOpenProcessor.doOpenProject(baseDir, null, false, -1, project1 -> doCreate(projectPanel, project1, baseDir));
  }

  @NotNull
  @RequiredReadAction
  public static Module doCreate(@NotNull NewProjectPanel projectPanel, @NotNull final Project project, @NotNull final VirtualFile baseDir) {
    return doCreate(projectPanel, ModuleManager.getInstance(project).getModifiableModel(), baseDir, true);
  }

  @NotNull
  public static Module doCreate(@NotNull NewProjectPanel projectPanel,
                                @NotNull final ModifiableModuleModel modifiableModel,
                                @NotNull final VirtualFile baseDir,
                                final boolean requireModelCommit) {
    return new WriteAction<Module>() {
      @Override
      protected void run(Result<Module> result) throws Throwable {
        result.setResult(doCreateImpl(projectPanel, modifiableModel, baseDir, requireModelCommit));
      }
    }.execute().getResultObject();
  }

  @SuppressWarnings("unchecked")
  @NotNull
  @RequiredWriteAction
  private static Module doCreateImpl(@NotNull NewProjectPanel projectPanel,
                                     @NotNull final ModifiableModuleModel modifiableModel,
                                     @NotNull final VirtualFile baseDir,
                                     boolean requireModelCommit) {
    String name = StringUtil.notNullize(projectPanel.getNameText(), baseDir.getName());

    Module newModule = modifiableModel.newModule(name, baseDir.getPath());

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(newModule);
    ModifiableRootModel modifiableModelForModule = moduleRootManager.getModifiableModel();
    ContentEntry contentEntry = modifiableModelForModule.addContentEntry(baseDir);

    if (!projectPanel.isModuleCreation()) {
      contentEntry.addFolder(contentEntry.getUrl() + "/" + Project.DIRECTORY_STORE_FOLDER, ExcludedContentFolderTypeProvider.getInstance());
    }

    NewModuleBuilderProcessor processor = projectPanel.getProcessor();
    if (processor != null) {
      processor.setupModule(projectPanel.getConfigurationPanel(), contentEntry, modifiableModelForModule);
    }

    modifiableModelForModule.commit();

    if (requireModelCommit) {
      modifiableModel.commit();
    }

    baseDir.refresh(true, true);
    return newModule;
  }
}
