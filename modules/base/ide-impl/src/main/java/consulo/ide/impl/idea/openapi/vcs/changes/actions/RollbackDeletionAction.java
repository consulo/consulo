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
package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.RollbackProgressModifier;
import consulo.versionControlSystem.rollback.RollbackEnvironment;
import consulo.virtualFileSystem.LocalFileSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 * @since 2006-11-02
 */
public class RollbackDeletionAction extends AbstractMissingFilesAction {
  protected List<VcsException> processFiles(AbstractVcs vcs, List<FilePath> files) {
    RollbackEnvironment environment = vcs.getRollbackEnvironment();
    if (environment == null) return Collections.emptyList();
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.setText(vcs.getDisplayName() + ": performing rollback...");
    }
    List<VcsException> result = new ArrayList<VcsException>(0);
    try {
      environment.rollbackMissingFileDeletion(files, result, new RollbackProgressModifier(files.size(), indicator));
    }
    catch (ProcessCanceledException e) {
      // for files refresh
    }
    LocalFileSystem.getInstance().refreshIoFiles(ChangesUtil.filePathsToFiles(files));
    return result;
  }

  protected String getName() {
    return VcsBundle.message("changes.action.rollback.text");
  }

  protected boolean synchronously() {
    return false;
  }
}
