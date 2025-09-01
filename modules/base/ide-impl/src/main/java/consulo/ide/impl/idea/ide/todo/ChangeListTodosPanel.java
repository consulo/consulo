/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.todo;

import consulo.application.ui.util.TodoPanelSettings;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListAdapter;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.project.ui.util.AppUIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.awt.util.Alarm;
import consulo.disposer.Disposer;

import java.util.Collection;

/**
 * @author anna
 * @since 2007-07-27
 */
public abstract class ChangeListTodosPanel extends TodoPanel {
    private final Alarm myAlarm;

    public ChangeListTodosPanel(Project project, TodoPanelSettings settings, Content content) {
        super(project, settings, false, content);
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        MyChangeListManagerListener myChangeListManagerListener = new MyChangeListManagerListener();
        changeListManager.addChangeListListener(myChangeListManagerListener);
        Disposer.register(this, () -> ChangeListManager.getInstance(myProject).removeChangeListListener(myChangeListManagerListener));
        myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    }

    private final class MyChangeListManagerListener extends ChangeListAdapter {
        @Override
        public void defaultListChanged(ChangeList oldDefaultList, ChangeList newDefaultList) {
            rebuildWithAlarm(myAlarm);
            AppUIUtil.invokeOnEdt(() -> setDisplayName(IdeLocalize.changelistTodoTitle(newDefaultList.getName()).get()));
        }

        @Override
        public void changeListRenamed(ChangeList list, String oldName) {
            AppUIUtil.invokeOnEdt(() -> setDisplayName(IdeLocalize.changelistTodoTitle(list.getName()).get()));
        }

        @Override
        public void changesMoved(Collection<Change> changes, ChangeList fromList, ChangeList toList) {
            rebuildWithAlarm(myAlarm);
        }
    }
}