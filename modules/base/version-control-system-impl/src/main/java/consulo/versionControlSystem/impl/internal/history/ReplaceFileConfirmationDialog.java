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
package consulo.versionControlSystem.impl.internal.history;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.FilePathSplittingPolicy;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReplaceFileConfirmationDialog {
  private final FileStatusManager myFileStatusManager;
  ProgressIndicator myProgressIndicator = ProgressManager.getInstance().getProgressIndicator();
  private final String myActionName;

  public ReplaceFileConfirmationDialog(Project project, String actionName) {
    myFileStatusManager = FileStatusManager.getInstance(project);
    myActionName = actionName;
  }

  public boolean confirmFor(VirtualFile[] files) {
    if (Application.get().isUnitTestMode()) {
      return true;
    }
    if (myProgressIndicator != null) myProgressIndicator.pushState();
    try {
      Collection modifiedFiles = collectModifiedFiles(files);
      if (modifiedFiles.isEmpty()) return true;
      return requestConfirmation(modifiedFiles);
    }
    finally {
      if (myProgressIndicator != null) myProgressIndicator.popState();
    }
  }

  public boolean requestConfirmation(Collection modifiedFiles) {
    if (modifiedFiles.isEmpty()) return true;

    return Messages.showOkCancelDialog(
        createMessage(modifiedFiles),
        myActionName,
        createOverwriteButtonName(modifiedFiles),
        getCancelButtonText(),
        UIUtil.getWarningIcon()
    ) == DialogWrapper.OK_EXIT_CODE;
  }

  protected String getCancelButtonText() {
    return CommonLocalize.buttonCancel().get();
  }

  private String createOverwriteButtonName(Collection modifiedFiles) {
    return modifiedFiles.size() > 1 ? getOkButtonTextForFiles() : getOkButtonTextForOneFile();
  }

  protected String getOkButtonTextForOneFile() {
    return VcsLocalize.buttonTextOverwriteModifiedFile().get();
  }

  protected String getOkButtonTextForFiles() {
    return VcsLocalize.buttonTextOverwriteModifiedFiles().get();
  }

  protected String createMessage(Collection modifiedFiles) {
    if (modifiedFiles.size() == 1) {
      VirtualFile virtualFile = ((VirtualFile)modifiedFiles.iterator().next());
      return VcsLocalize.messageTextFileLocallyModified(
          FilePathSplittingPolicy.SPLIT_BY_LETTER.getPresentableName(new File(virtualFile.getPath()), 40)
      ).get();
    }
    else {
      return VcsLocalize.messageTextSeveralFilesLocallyModified().get();
    }
  }

  public Collection<VirtualFile> collectModifiedFiles(VirtualFile @Nullable [] files) {
    List<VirtualFile> result = new ArrayList<>();

    if (files == null) return result;

    for (int i = 0; i < files.length; i++) {
      VirtualFile file = files[i];
      if (myProgressIndicator != null) {
        myProgressIndicator.setText(VcsLocalize.progressTextSearchingForModifiedFiles());
        myProgressIndicator.setText2(LocalizeValue.of(file.getPresentableUrl()));
      }
      FileStatus status = myFileStatusManager.getStatus(file);
      if (status != FileStatus.NOT_CHANGED) {
        result.add(file);
        if (result.size() > 1) return result;
      }
      result.addAll(collectModifiedFiles(file.getChildren()));
    }
    return result;
  }
}
