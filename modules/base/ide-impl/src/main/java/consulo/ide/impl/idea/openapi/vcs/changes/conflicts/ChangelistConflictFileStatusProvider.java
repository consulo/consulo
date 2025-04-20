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

package consulo.ide.impl.idea.openapi.vcs.changes.conflicts;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusFactory;
import consulo.virtualFileSystem.status.FileStatusProvider;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class ChangelistConflictFileStatusProvider implements FileStatusProvider {

  private static final FileStatus MODIFIED_OUTSIDE = FileStatusFactory.getInstance().createFileStatus("modifiedOutside", LocalizeValue.localizeTODO("Modified in not active changelist"), null);
  private static final FileStatus ADDED_OUTSIDE = FileStatusFactory.getInstance().createFileStatus("addedOutside", LocalizeValue.localizeTODO("Added in not active changelist"), null);
  private static final FileStatus CHANGELIST_CONFLICT = FileStatusFactory.getInstance().createFileStatus("changelistConflict", LocalizeValue.localizeTODO("Changelist conflict"), null);

  private final ChangelistConflictTracker myConflictTracker;
  private final ChangeListManager myChangeListManager;

  @Inject
  public ChangelistConflictFileStatusProvider(ChangeListManager changeListManager) {
    myChangeListManager = changeListManager;
    myConflictTracker = ((ChangeListManagerImpl)changeListManager).getConflictTracker();
  }

  @Override
  @Nullable
  public FileStatus getFileStatus(VirtualFile virtualFile) {
    ChangelistConflictTracker.Options options = myConflictTracker.getOptions();
    if (!options.TRACKING_ENABLED) {
      return null;
    }
    boolean conflict = myConflictTracker.hasConflict(virtualFile);
    if (conflict && options.HIGHLIGHT_CONFLICTS) {
      return CHANGELIST_CONFLICT;
    }
    else if (options.HIGHLIGHT_NON_ACTIVE_CHANGELIST) {
      FileStatus status = myChangeListManager.getStatus(virtualFile);
      if (status == FileStatus.MODIFIED || status == FileStatus.ADDED) {
        if (!myConflictTracker.isFromActiveChangelist(virtualFile)) {
          return status == FileStatus.MODIFIED ? MODIFIED_OUTSIDE : ADDED_OUTSIDE;
        }
      }
    }
    return null;
  }
}
