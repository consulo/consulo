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

import consulo.fileEditor.EditorNotificationBuilder;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.ide.impl.idea.openapi.vcs.changes.LocalChangeListImpl;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.Messages;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * @author Dmitry Avdeev
 */
public class ChangelistConflictNotificationPanel {
  @Nullable
  public static EditorNotificationBuilder create(ChangelistConflictTracker tracker, VirtualFile file, Supplier<EditorNotificationBuilder> builderFactory) {
    final ChangeListManager manager = tracker.getChangeListManager();
    final Change change = manager.getChange(file);
    if (change == null) return null;
    final LocalChangeList changeList = manager.getChangeList(change);
    if (changeList == null) return null;
    EditorNotificationBuilder builder = builderFactory.get();
    build(tracker, file, LocalChangeListImpl.createEmptyChangeList(tracker.getProject(), "Test"), builder);
    return builder;
  }

  static void build(ChangelistConflictTracker tracker, VirtualFile file, LocalChangeList changeList, EditorNotificationBuilder builder) {
    final ChangeListManager manager = tracker.getChangeListManager();
    builder.withText(LocalizeValue.localizeTODO("File from non-active changelist is modified"));

    builder.withAction(LocalizeValue.localizeTODO("Move changes"), LocalizeValue.localizeTODO("Move changes to active changelist (" + manager.getDefaultChangeList().getName() + ")"), (i) -> {
      ChangelistConflictResolution.MOVE.resolveConflict(tracker.getProject(), changeList.getChanges(), file);
    });

    builder.withAction(LocalizeValue.localizeTODO("Switch changelist"), LocalizeValue.localizeTODO("Set active changelist to '" + changeList.getName() + "'"), (i) -> {
      Change change = tracker.getChangeListManager().getChange(file);
      if (change == null) {
        Messages.showInfoMessage("No changes for this file", "ReflectionMessage");
      }
      else {
        ChangelistConflictResolution.SWITCH.resolveConflict(tracker.getProject(), Collections.singletonList(change), null);
      }
    });

    builder.withAction(LocalizeValue.localizeTODO("Ignore"), LocalizeValue.localizeTODO("Hide this notification"), (i) -> {
      tracker.ignoreConflict(file, true);
    });

    builder.withGearAction(LocalizeValue.localizeTODO("Show options dialog"), PlatformIconGroup.generalSettings(), (i) -> {
      ShowSettingsUtil.getInstance().showAndSelect(tracker.getProject(), ChangelistConflictConfigurable.class);
    });
  }
}
