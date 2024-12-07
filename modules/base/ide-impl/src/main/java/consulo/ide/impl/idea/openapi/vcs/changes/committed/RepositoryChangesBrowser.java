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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.application.AllIcons;
import consulo.ide.impl.idea.ide.actions.EditSourceAction;
import consulo.dataContext.TypeSafeDataProviderAdapter;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.OpenRepositoryVersionAction;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.RevertSelectedChangesAction;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesBrowser;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import consulo.navigation.Navigatable;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
public class RepositoryChangesBrowser extends ChangesBrowser implements DataProvider {

  private CommittedChangesBrowserUseCase myUseCase;
  private EditSourceAction myEditSourceAction;

  public RepositoryChangesBrowser(final Project project, final List<CommittedChangeList> changeLists) {
    this(project, changeLists, Collections.<Change>emptyList(), null);
  }

  public RepositoryChangesBrowser(final Project project, final List<? extends ChangeList> changeLists, final List<Change> changes, final ChangeList initialListSelection) {
    this(project, changeLists, changes, initialListSelection, null);
  }

  public RepositoryChangesBrowser(final Project project, final List<? extends ChangeList> changeLists, final List<Change> changes, final ChangeList initialListSelection, VirtualFile toSelect) {
    super(project, changeLists, changes, initialListSelection, false, false, null, MyUseCase.COMMITTED_CHANGES, toSelect);
  }

  @Override
  protected void buildToolBar(final DefaultActionGroup toolBarGroup) {
    super.buildToolBar(toolBarGroup);

    toolBarGroup.add(ActionManager.getInstance().getAction("Vcs.ShowDiffWithLocal"));
    myEditSourceAction = new MyEditSourceAction();
    myEditSourceAction.registerCustomShortcutSet(CommonShortcuts.getEditSource(), this);
    toolBarGroup.add(myEditSourceAction);
    OpenRepositoryVersionAction action = new OpenRepositoryVersionAction();
    toolBarGroup.add(action);
    final RevertSelectedChangesAction revertSelectedChangesAction = new RevertSelectedChangesAction();
    toolBarGroup.add(revertSelectedChangesAction);

    ActionGroup group = (ActionGroup)ActionManager.getInstance().getAction("RepositoryChangesBrowserToolbar");
    final AnAction[] actions = group.getChildren(null);
    for (AnAction anAction : actions) {
      toolBarGroup.add(anAction);
    }
  }

  public void setUseCase(final CommittedChangesBrowserUseCase useCase) {
    myUseCase = useCase;
  }

  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    if (CommittedChangesBrowserUseCase.DATA_KEY == dataId) {
      return myUseCase;
    }

    else if (VcsDataKeys.SELECTED_CHANGES == dataId) {
      final List<Change> list = myViewer.getSelectedChanges();
      return list.toArray(new Change[list.size()]);
    }
    else if (VcsDataKeys.CHANGE_LEAD_SELECTION == dataId) {
      final Change highestSelection = myViewer.getHighestLeadSelection();
      return (highestSelection == null) ? new Change[]{} : new Change[]{highestSelection};
    }
    else {
      final TypeSafeDataProviderAdapter adapter = new TypeSafeDataProviderAdapter(this);
      return adapter.getData(dataId);
    }
  }

  public EditSourceAction getEditSourceAction() {
    return myEditSourceAction;
  }

  private class MyEditSourceAction extends EditSourceAction {
    @RequiredUIAccess
    @Override
    public void update(@Nonnull final AnActionEvent event) {
      super.update(event);
      event.getPresentation().setIcon(AllIcons.Actions.EditSource);
      event.getPresentation().setText("Edit Source");
      if ((!IdeaModalityState.nonModal().equals(IdeaModalityState.current())) || CommittedChangesBrowserUseCase.IN_AIR.equals(event.getData(CommittedChangesBrowserUseCase.DATA_KEY))) {
        event.getPresentation().setEnabled(false);
      }
      else {
        event.getPresentation().setEnabled(true);
      }
    }

    @Override
    protected Navigatable[] getNavigatables(final DataContext dataContext) {
      Change[] changes = dataContext.getData(VcsDataKeys.SELECTED_CHANGES);
      if (changes != null) {
        Collection<Change> changeCollection = Arrays.asList(changes);
        return ChangesUtil.getNavigatableArray(myProject, ChangesUtil.getFilesFromChanges(changeCollection));
      }
      return null;
    }
  }
}
