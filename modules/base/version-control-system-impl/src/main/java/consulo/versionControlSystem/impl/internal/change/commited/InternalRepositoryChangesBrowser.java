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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.dataContext.TypeSafeDataProviderAdapter;
import consulo.language.editor.PlatformDataKeys;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.RepositoryChangesBrowserApi;
import consulo.versionControlSystem.impl.internal.change.action.OpenRepositoryVersionAction;
import consulo.versionControlSystem.impl.internal.change.action.RevertSelectedChangesAction;
import consulo.versionControlSystem.impl.internal.ui.awt.InternalChangesBrowser;
import consulo.versionControlSystem.internal.CommittedChangesBrowserUseCase;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author yole
 */
public class InternalRepositoryChangesBrowser extends InternalChangesBrowser implements DataProvider, RepositoryChangesBrowserApi {

    private CommittedChangesBrowserUseCase myUseCase;
    private CommonEditSourceAction myEditSourceAction;

    public InternalRepositoryChangesBrowser(Project project, List<CommittedChangeList> changeLists) {
        this(project, changeLists, Collections.<Change>emptyList(), null);
    }

    public InternalRepositoryChangesBrowser(Project project, List<? extends ChangeList> changeLists, List<Change> changes, ChangeList initialListSelection) {
        this(project, changeLists, changes, initialListSelection, null);
    }

    public InternalRepositoryChangesBrowser(Project project, List<? extends ChangeList> changeLists, List<Change> changes, ChangeList initialListSelection, VirtualFile toSelect) {
        super(project, changeLists, changes, initialListSelection, false, false, null, MyUseCase.COMMITTED_CHANGES, toSelect);
    }

    @Override
    protected void buildToolBar(DefaultActionGroup toolBarGroup) {
        super.buildToolBar(toolBarGroup);

        toolBarGroup.add(ActionManager.getInstance().getAction("Vcs.ShowDiffWithLocal"));
        myEditSourceAction = new MyEditSourceAction();
        myEditSourceAction.registerCustomShortcutSet(CommonShortcuts.getEditSource(), this);
        toolBarGroup.add(myEditSourceAction);
        OpenRepositoryVersionAction action = new OpenRepositoryVersionAction();
        toolBarGroup.add(action);
        RevertSelectedChangesAction revertSelectedChangesAction = new RevertSelectedChangesAction();
        toolBarGroup.add(revertSelectedChangesAction);

        ActionGroup group = (ActionGroup) ActionManager.getInstance().getAction("RepositoryChangesBrowserToolbar");
        AnAction[] actions = group.getChildren(null);
        for (AnAction anAction : actions) {
            toolBarGroup.add(anAction);
        }
    }

    @Override
    public void setUseCase(CommittedChangesBrowserUseCase useCase) {
        myUseCase = useCase;
    }

    @Override
    public Object getData(@Nonnull Key<?> dataId) {
        if (CommittedChangesBrowserUseCase.DATA_KEY == dataId) {
            return myUseCase;
        }

        else if (VcsDataKeys.SELECTED_CHANGES == dataId) {
            List<Change> list = myViewer.getSelectedChanges();
            return list.toArray(new Change[list.size()]);
        }
        else if (VcsDataKeys.CHANGE_LEAD_SELECTION == dataId) {
            Change highestSelection = myViewer.getHighestLeadSelection();
            return (highestSelection == null) ? new Change[]{} : new Change[]{highestSelection};
        }
        else {
            TypeSafeDataProviderAdapter adapter = new TypeSafeDataProviderAdapter(this);
            return adapter.getData(dataId);
        }
    }

    @Override
    public CommonEditSourceAction getEditSourceAction() {
        return myEditSourceAction;
    }

    private class MyEditSourceAction extends CommonEditSourceAction {
        private MyEditSourceAction() {
            getTemplatePresentation().setTextValue(LocalizeValue.localizeTODO("Edit Source"));
            getTemplatePresentation().setIcon(PlatformIconGroup.actionsEditsource());
        }

        @Override
        public void update(@Nonnull AnActionEvent event) {
            super.update(event);
            boolean isModalContext = Objects.equals(event.getData(PlatformDataKeys.IS_MODAL_CONTEXT), Boolean.TRUE);
            event.getPresentation().setEnabled(
                !isModalContext && !CommittedChangesBrowserUseCase.IN_AIR.equals(event.getData(CommittedChangesBrowserUseCase.DATA_KEY))
            );
        }

        @Override
        protected Navigatable[] getNavigatables(DataContext dataContext) {
            Change[] changes = dataContext.getData(VcsDataKeys.SELECTED_CHANGES);
            if (changes != null) {
                Collection<Change> changeCollection = Arrays.asList(changes);
                return ChangesUtil.getNavigatableArray(myProject, ChangesUtil.getFilesFromChanges(changeCollection));
            }
            return null;
        }
    }
}
