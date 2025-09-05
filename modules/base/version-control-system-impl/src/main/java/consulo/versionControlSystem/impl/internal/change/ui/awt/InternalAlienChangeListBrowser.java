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
package consulo.versionControlSystem.impl.internal.change.ui.awt;

import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangesBrowserApi;
import consulo.versionControlSystem.impl.internal.ui.awt.InternalChangesBrowser;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InternalAlienChangeListBrowser extends InternalChangesBrowser {
  private final List<Change> myChanges;
  private final AbstractVcs myVcs;

  public InternalAlienChangeListBrowser(Project project, List<? extends ChangeList> changeLists, List<Change> changes,
                                        ChangeList initialListSelection, boolean capableOfExcludingChanges,
                                        boolean highlightProblems, AbstractVcs vcs) {
    super(project, changeLists, changes, initialListSelection, capableOfExcludingChanges, highlightProblems, null, ChangesBrowserApi.MyUseCase.LOCAL_CHANGES, null);
    myChanges = changes;
    myVcs = vcs;
    rebuildList();
  }

  @Override
  public void rebuildList() {
    // dont change lists
    myViewer.setChangesToDisplay(myChanges ==  null ? Collections.<Change>emptyList() : myChanges);
  }

  protected void setInitialSelection(List<? extends ChangeList> changeLists, @Nonnull List<Change> changes, ChangeList initialListSelection) {
    if (! changeLists.isEmpty()) {
      mySelectedChangeList = changeLists.get(0);
    }
  }

  @Override
  protected void buildToolBar(DefaultActionGroup toolBarGroup) {
    super.buildToolBar(toolBarGroup);

    toolBarGroup.add(ActionManager.getInstance().getAction("AlienCommitChangesDialog.AdditionalActions"));
  }

  @Override
  @Nonnull
  public Set<AbstractVcs> getAffectedVcses() {
    return Set.of(myVcs);
  }

  @Override
  @Nonnull
  public List<Change> getCurrentIncludedChanges() {
    return new ArrayList<>(myChanges);
  }
}
