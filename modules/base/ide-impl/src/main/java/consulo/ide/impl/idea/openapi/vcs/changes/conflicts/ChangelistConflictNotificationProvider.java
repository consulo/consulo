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
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.FileEditor;
import consulo.application.dumb.DumbAware;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.virtualFileSystem.VirtualFile;
import consulo.annotation.access.RequiredReadAction;

import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class ChangelistConflictNotificationProvider implements EditorNotificationProvider, DumbAware {

  private final ChangelistConflictTracker myConflictTracker;

  @Inject
  public ChangelistConflictNotificationProvider(ChangeListManager changeListManager) {
    myConflictTracker = ((ChangeListManagerImpl)changeListManager).getConflictTracker();
  }

  @Nonnull
  @Override
  public String getId() {
    return "changelist-conflict";
  }

  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationBuilder buildNotification(@Nonnull VirtualFile file, @Nonnull FileEditor fileEditor, @Nonnull Supplier<EditorNotificationBuilder> builderFactory) {
    return myConflictTracker.hasConflict(file) ? ChangelistConflictNotificationPanel.create(myConflictTracker, file, builderFactory) : null;
  }
}
