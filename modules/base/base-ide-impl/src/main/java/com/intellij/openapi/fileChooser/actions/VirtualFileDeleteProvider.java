/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.fileChooser.actions;

import com.intellij.CommonBundle;
import com.intellij.ide.DeleteProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.UIBundle;
import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class VirtualFileDeleteProvider implements DeleteProvider {
  private static final Logger LOG = Logger.getInstance(VirtualFileDeleteProvider.class);

  @Override
  public boolean canDeleteElement(@Nonnull DataContext dataContext) {
    final VirtualFile[] files = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    return files != null && files.length > 0;
  }

  @Override
  public void deleteElement(@Nonnull DataContext dataContext) {
    final VirtualFile[] files = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (files == null || files.length == 0) return;
    Project project = dataContext.getData(CommonDataKeys.PROJECT);

    String message = createConfirmationMessage(files);
    int returnValue = Messages.showOkCancelDialog(message, UIBundle.message("delete.dialog.title"), ApplicationBundle.message("button.delete"),
                                                  CommonBundle.getCancelButtonText(), Messages.getQuestionIcon());
    if (returnValue != Messages.OK) return;

    Arrays.sort(files, FileComparator.getInstance());

    List<String> problems = ContainerUtil.newLinkedList();
    CommandProcessor.getInstance().executeCommand(project, () -> {
      new Task.Modal(project, "Deleting Files...", true) {
        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          indicator.setIndeterminate(false);
          int i = 0;
          for (VirtualFile file : files) {
            indicator.checkCanceled();
            indicator.setText2(file.getPresentableUrl());
            indicator.setFraction((double)i / files.length);
            i++;

            try {
              WriteAction.run(() -> file.delete(this));
            }
            catch (Exception e) {
              LOG.info("Error when deleting " + file, e);
              problems.add(file.getName());
            }
          }
        }

        @RequiredUIAccess
        @Override
        public void onSuccess() {
          reportProblems();
        }

        @RequiredUIAccess
        @Override
        public void onCancel() {
          reportProblems();
        }

        private void reportProblems() {
          if (!problems.isEmpty()) {
            reportDeletionProblem(problems);
          }
        }
      }.queue();
    }, "Deleting files", null);
  }

  private static void reportDeletionProblem(List<String> problems) {
    boolean more = false;
    if (problems.size() > 10) {
      problems = problems.subList(0, 10);
      more = true;
    }
    Messages.showMessageDialog("Could not erase files or folders:\n  " + StringUtil.join(problems, ",\n  ") + (more ? "\n  ..." : ""),
                               UIBundle.message("error.dialog.title"), Messages.getErrorIcon());
  }

  private static final class FileComparator implements Comparator<VirtualFile> {
    private static final FileComparator ourInstance = new FileComparator();

    public static FileComparator getInstance() {
      return ourInstance;
    }

    @Override
    public int compare(final VirtualFile o1, final VirtualFile o2) {
      // files first
      return o2.getPath().compareTo(o1.getPath());
    }
  }

  private static String createConfirmationMessage(VirtualFile[] filesToDelete) {
    if (filesToDelete.length == 1) {
      if (filesToDelete[0].isDirectory()) {
        return UIBundle.message("are.you.sure.you.want.to.delete.selected.folder.confirmation.message", filesToDelete[0].getName());
      }
      else {
        return UIBundle.message("are.you.sure.you.want.to.delete.selected.file.confirmation.message", filesToDelete[0].getName());
      }
    }
    else {
      boolean hasFiles = false;
      boolean hasFolders = false;
      for (VirtualFile file : filesToDelete) {
        boolean isDirectory = file.isDirectory();
        hasFiles |= !isDirectory;
        hasFolders |= isDirectory;
      }
      LOG.assertTrue(hasFiles || hasFolders);
      if (hasFiles && hasFolders) {
        return UIBundle.message("are.you.sure.you.want.to.delete.selected.files.and.directories.confirmation.message", filesToDelete.length);
      }
      else if (hasFolders) {
        return UIBundle.message("are.you.sure.you.want.to.delete.selected.folders.confirmation.message", filesToDelete.length);
      }
      else {
        return UIBundle.message("are.you.sure.you.want.to.delete.selected.files.and.files.confirmation.message", filesToDelete.length);
      }
    }
  }
}