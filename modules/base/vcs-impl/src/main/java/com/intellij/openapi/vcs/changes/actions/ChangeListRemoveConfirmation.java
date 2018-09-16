/*
 * Copyright 2013-2016 consulo.io
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
package com.intellij.openapi.vcs.changes.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.*;

/**
 * @author VISTALL
 * @since 18-Aug-16
 * <p>
 * from kotlin platform\vcs-impl\src\com\intellij\openapi\vcs\changes\actions\ChangeListRemoveConfirmation.kt
 */
public abstract class ChangeListRemoveConfirmation {
  public abstract boolean askIfShouldRemoveChangeLists(@Nonnull List<? extends LocalChangeList> toAsk);

  public static void processLists(Project project, boolean explicitly, Collection<LocalChangeList> allLists, ChangeListRemoveConfirmation ask) {
    List<String> allIds = ContainerUtil.map(allLists, LocalChangeList::getId);
    Set<String> confirmationAsked = new HashSet<>();
    Set<String> doNotRemove = new HashSet<>();

    ChangeListManager manager = ChangeListManager.getInstance(project);
    for (String id : allIds) {
      for (AbstractVcs vcs : ProjectLevelVcsManager.getInstance(project).getAllActiveVcss()) {
        LocalChangeList list = manager.getChangeList(id);

        ThreeState permission;
        if (list == null) {
          permission = ThreeState.NO;
        }
        else {
          permission = vcs.mayRemoveChangeList(list, explicitly);
        }

        if (permission != ThreeState.UNSURE) {
          confirmationAsked.add(id);
        }

        if (permission == ThreeState.NO) {
          doNotRemove.add(id);
          break;
        }
      }

      List<String> toAsk = ContainerUtil.filter(allIds, it -> !confirmationAsked.contains(it) && !doNotRemove.contains(it));
      if (!toAsk.isEmpty() && !ask.askIfShouldRemoveChangeLists(ContainerUtil.mapNotNull(toAsk, manager::getChangeList))) {
        doNotRemove.addAll(toAsk);
      }

      List<LocalChangeList> toRemove = ContainerUtil.mapNotNull(ContainerUtil.filter(allIds, it -> !doNotRemove.contains(it)), manager::getChangeList);
      LocalChangeList active = ContainerUtil.find(toRemove, LocalChangeList::isDefault);

      for (LocalChangeList it : toRemove) {
        if (it != active) {
          manager.removeChangeList(it.getName());
        }
      }

      if (active != null && RemoveChangeListAction.confirmActiveChangeListRemoval(project, Collections.singletonList(active), active.getChanges().isEmpty())) {
        manager.removeChangeList(active.getName());
      }
    }
  }
}
