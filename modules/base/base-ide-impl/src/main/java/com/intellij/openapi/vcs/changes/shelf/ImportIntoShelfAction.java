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
package com.intellij.openapi.vcs.changes.shelf;

import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.application.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import consulo.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.util.Consumer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.fileChooser.FileChooser;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author irengrig
 * Date: 2/25/11
 * Time: 2:23 PM
 */
public class ImportIntoShelfAction extends DumbAwareAction {
  public ImportIntoShelfAction() {
    super("Import patches...", "Copies patch file to shelf", null);
  }

  @Override
  public void update(AnActionEvent e) {
    final Project project = e.getDataContext().getData(CommonDataKeys.PROJECT);
    e.getPresentation().setEnabled(project != null);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Project project = e.getDataContext().getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, true);
    FileChooser.chooseFiles(descriptor, project, null).doWhenDone(files -> {
      //gatherPatchFiles
      final ProgressManager pm = ProgressManager.getInstance();
      final ShelveChangesManager shelveChangesManager = ShelveChangesManager.getInstance(project);

      final List<VirtualFile> patchTypeFiles = new ArrayList<VirtualFile>();
      final boolean filesFound = pm.runProcessWithProgressSynchronously(new Runnable() {
        @Override
        public void run() {
          patchTypeFiles.addAll(shelveChangesManager.gatherPatchFiles(Arrays.asList(files)));
        }
      }, "Looking for patch files...", true, project);
      if (!filesFound || patchTypeFiles.isEmpty()) return;
      if (!patchTypeFiles.equals(files)) {
        final String message =
                "Found " + (patchTypeFiles.size() == 1 ? "one patch file (" + patchTypeFiles.get(0).getPath() + ")." : (patchTypeFiles.size() + " patch files.")) + "\nContinue with import?";
        final int toImport = Messages.showYesNoDialog(project, message, "Import Patches", Messages.getQuestionIcon());
        if (toImport == Messages.NO) return;
      }
      pm.runProcessWithProgressSynchronously(new Runnable() {
        @Override
        public void run() {
          final List<VcsException> exceptions = new ArrayList<VcsException>();
          final List<ShelvedChangeList> lists = shelveChangesManager.importChangeLists(patchTypeFiles, new Consumer<VcsException>() {
            @Override
            public void consume(VcsException e) {
              exceptions.add(e);
            }
          });
          if (!lists.isEmpty()) {
            ShelvedChangesViewManager.getInstance(project).activateView(lists.get(lists.size() - 1));
          }
          if (!exceptions.isEmpty()) {
            AbstractVcsHelper.getInstance(project).showErrors(exceptions, "Import patches into shelf");
          }
          if (lists.isEmpty() && exceptions.isEmpty()) {
            VcsBalloonProblemNotifier.showOverChangesView(project, "No patches found", MessageType.WARNING);
          }
        }
      }, "Import patches into shelf", true, project);
    });
  }
}
