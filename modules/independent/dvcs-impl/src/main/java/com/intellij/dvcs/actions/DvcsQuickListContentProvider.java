/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.dvcs.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.actions.VcsQuickListContentProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class DvcsQuickListContentProvider implements VcsQuickListContentProvider {

  @Nullable
  public List<AnAction> getVcsActions(@Nullable Project project, @javax.annotation.Nullable AbstractVcs activeVcs,
                                      @javax.annotation.Nullable DataContext dataContext) {

    if (activeVcs == null || !getVcsName().equals(activeVcs.getName())) {
      return null;
    }

    final ActionManager manager = ActionManager.getInstance();
    final List<AnAction> actions = new ArrayList<>();

    actions.add(new AnSeparator(activeVcs.getDisplayName()));
    add("CheckinProject", manager, actions);
    add("CheckinFiles", manager, actions);
    add("ChangesView.Revert", manager, actions);

    addSeparator(actions);
    add("Vcs.ShowTabbedFileHistory", manager, actions);
    add("Annotate", manager, actions);
    add("Compare.SameVersion", manager, actions);

    addSeparator(actions);
    addVcsSpecificActions(manager, actions);
    return actions;
  }

  @Nonnull
  protected abstract String getVcsName();

  protected abstract void addVcsSpecificActions(@Nonnull ActionManager manager, @Nonnull List<AnAction> actions);

  @Override
  public boolean replaceVcsActionsFor(@Nonnull AbstractVcs activeVcs, @javax.annotation.Nullable DataContext dataContext) {
    return getVcsName().equals(activeVcs.getName());
  }

  protected static void addSeparator(@Nonnull final List<AnAction> actions) {
    actions.add(new AnSeparator());
  }

  protected static void add(String actionName, ActionManager manager, List<AnAction> actions) {
    final AnAction action = manager.getAction(actionName);
    assert action != null : "Can not find action " + actionName;
    actions.add(action);
  }
}
