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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.ApplicationNamesInfo;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.PathChooserDialog;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorProviderManager;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.impl.ProjectUtil;
import consulo.ide.impl.idea.openapi.fileChooser.FileElement;
import consulo.ide.impl.idea.openapi.fileChooser.impl.FileChooserUtil;
import consulo.ide.impl.idea.openapi.fileEditor.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import consulo.ide.impl.idea.openapi.fileTypes.ex.FileTypeChooser;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.projectImport.ProjectOpenProcessor;
import consulo.ide.impl.project.ProjectOpenProcessors;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class OpenFileAction extends AnAction implements DumbAware {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    @Nullable final Project project = e.getData(CommonDataKeys.PROJECT);
    final boolean showFiles = project != null;

    final FileChooserDescriptor descriptor = new OpenProjectFileChooserDescriptor(true) {
      @RequiredUIAccess
      @Override
      public boolean isFileSelectable(VirtualFile file) {
        if (super.isFileSelectable(file)) {
          return true;
        }
        if (file.isDirectory()) {
          return false;
        }
        return showFiles && !FileElement.isArchive(file);
      }

      @Override
      public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        if (!file.isDirectory() && isFileSelectable(file)) {
          if (!showHiddenFiles && FileElement.isFileHidden(file)) return false;
          return true;
        }
        return super.isFileVisible(file, showHiddenFiles);
      }

      @Override
      public boolean isChooseMultiple() {
        return showFiles;
      }
    };
    descriptor.setTitle(showFiles ? "Open File or Project" : "Open Project");
    // FIXME [VISTALL] we need this? descriptor.setDescription(getFileChooserDescription());

    descriptor.putUserData(PathChooserDialog.PREFER_LAST_OVER_EXPLICIT, Boolean.TRUE);

    FileChooser.chooseFiles(descriptor, project, VfsUtil.getUserHomeDir()).doWhenDone(files -> {
      for (VirtualFile file : files) {
        if (!descriptor.isFileSelectable(file)) { // on Mac, it could be selected anyway
          Messages.showInfoMessage(project, file.getPresentableUrl() + " contains no " + ApplicationNamesInfo.getInstance().getFullProductName() + " project", "Cannot Open Project");
          return;
        }
      }
      doOpenFile(project, files);
    });
  }

  @Nonnull
  private static String getFileChooserDescription() {
    List<ProjectOpenProcessor> providers = ProjectOpenProcessors.getInstance().getProcessors();
    List<String> fileSamples = new ArrayList<>();
    for (ProjectOpenProcessor processor : providers) {
      processor.collectFileSamples(fileSamples::add);
    }
    return IdeBundle.message("import.project.chooser.header", StringUtil.join(fileSamples, ", <br>"));
  }

  @RequiredUIAccess
  private static void doOpenFile(@Nullable final Project project, @Nonnull final VirtualFile[] result) {
    for (final VirtualFile file : result) {
      if (file.isDirectory()) {
        ProjectUtil.openAsync(file.getPath(), project, false, UIAccess.current()).doWhenDone(openedProject -> FileChooserUtil.setLastOpenedFile(openedProject, file));
        return;
      }

      if (OpenProjectFileChooserDescriptor.canOpen(file)) {
        int answer = Messages.showYesNoDialog(project, IdeBundle.message("message.open.file.is.project", file.getName()), IdeBundle.message("title.open.project"), Messages.getQuestionIcon());
        if (answer == 0) {
          ProjectUtil.openAsync(file.getPath(), project, false, UIAccess.current()).doWhenDone(openedProject -> FileChooserUtil.setLastOpenedFile(openedProject, file));
          return;
        }
      }

      FileType type = FileTypeChooser.getKnownFileTypeOrAssociate(file, project);
      if (type == null) return;

      if (project != null) {
        openFile(file, project);
      }
    }
  }

  public static void openFile(final String filePath, final Project project) {
    final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
    if (file != null && file.isValid()) {
      openFile(file, project);
    }
  }

  public static void openFile(final VirtualFile virtualFile, final Project project) {
    FileEditorProviderManager editorProviderManager = FileEditorProviderManager.getInstance();
    if (editorProviderManager.getProviders(project, virtualFile).length == 0) {
      Messages.showMessageDialog(project, IdeBundle.message("error.files.of.this.type.cannot.be.opened", ApplicationNamesInfo.getInstance().getProductName()),
                                 IdeBundle.message("title.cannot.open.file"), Messages.getErrorIcon());
      return;
    }

    NonProjectFileWritingAccessProvider.allowWriting(virtualFile);
    OpenFileDescriptorImpl descriptor = new OpenFileDescriptorImpl(project, virtualFile);
    FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
  }
}
