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
import consulo.dataContext.DataSink;
import consulo.language.editor.PlatformDataKeys;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.RepositoryChangesBrowser;
import consulo.versionControlSystem.impl.internal.change.action.OpenRepositoryVersionAction;
import consulo.versionControlSystem.impl.internal.change.action.RevertSelectedChangesAction;
import consulo.versionControlSystem.impl.internal.ui.awt.InternalChangesBrowser;
import consulo.versionControlSystem.internal.CommittedChangesBrowserUseCase;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;

import java.util.*;

/**
 * @author yole
 */
public class InternalRepositoryChangesBrowser extends InternalChangesBrowser implements RepositoryChangesBrowser {

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
    public void uiDataSnapshot(DataSink sink) {
        super.uiDataSnapshot(sink);
        sink.set(CommittedChangesBrowserUseCase.DATA_KEY, myUseCase);
        List<Change> list = myViewer.getSelectedChanges();
        sink.set(VcsDataKeys.SELECTED_CHANGES, list.toArray(new Change[list.size()]));
        Change highestSelection = myViewer.getHighestLeadSelection();
        sink.set(VcsDataKeys.CHANGE_LEAD_SELECTION, (highestSelection == null) ? new Change[]{} : new Change[]{highestSelection});
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
        public void update(AnActionEvent event) {
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
