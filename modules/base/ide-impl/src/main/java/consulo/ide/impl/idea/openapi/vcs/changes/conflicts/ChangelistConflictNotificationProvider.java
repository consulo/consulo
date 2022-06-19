/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import consulo.fileEditor.FileEditor;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManager;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.virtualFileSystem.VirtualFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.ide.impl.codeEditor.EditorNotificationProvider;

import jakarta.inject.Inject;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class ChangelistConflictNotificationProvider implements EditorNotificationProvider<ChangelistConflictNotificationPanel>, DumbAware {

  private final ChangelistConflictTracker myConflictTracker;

  @Inject
  public ChangelistConflictNotificationProvider(ChangeListManager changeListManager) {
    myConflictTracker = ((ChangeListManagerImpl)changeListManager).getConflictTracker();
  }

  @RequiredReadAction
  public ChangelistConflictNotificationPanel createNotificationPanel(VirtualFile file, FileEditor fileEditor) {
    return myConflictTracker.hasConflict(file) ? ChangelistConflictNotificationPanel.create(myConflictTracker, file) : null;
  }
}
