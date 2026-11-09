/*
 * Copyright 2013-2026 consulo.io
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
package consulo.task.impl.internal.issue;

import consulo.accountManagement.Account;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.task.TaskManager;
import consulo.task.TaskRepository;
import consulo.task.TaskRepositoryOptionsUtil;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
public final class IssueTabsSync {
    private final Project myProject;
    private final ToolWindow myToolWindow;
    private final Map<String, Content> myContents = new LinkedHashMap<>();

    public IssueTabsSync(Project project, ToolWindow toolWindow) {
        myProject = project;
        myToolWindow = toolWindow;
    }

    @RequiredUIAccess
    public void install(Disposable parentDisposable) {
        TaskManagerImpl manager = (TaskManagerImpl) TaskManager.getManager(myProject);
        manager.addRepositoriesListener(
            () -> myProject.getApplication().invokeLater(this::sync, myProject.getDisposed()),
            parentDisposable
        );
        sync();
    }

    private static String getTabName(TaskRepository repository) {
        Account account = repository.getAccount();
        if (account != null) {
            return account.getName();
        }
        return repository.getServerUrl().isEmpty() ? repository.getRepositoryType().getPresentableName().get() : repository.getServerUrl();
    }

    @RequiredUIAccess
    private void sync() {
        ContentManager contentManager = myToolWindow.getContentManager();
        Map<String, TaskRepository> repositories = new LinkedHashMap<>();
        for (TaskRepository repository : TaskManager.getManager(myProject).getAllRepositories()) {
            repositories.put(repository.getRepositoryType().getDetectionKeyUnchecked(repository.getOptions()), repository);
        }

        for (Map.Entry<String, Content> entry : Map.copyOf(myContents).entrySet()) {
            TaskRepository repository = repositories.get(entry.getKey());
            IssueListPanel panel = (IssueListPanel) entry.getValue().getUserData(IssueListPanelKey.KEY);
            if (repository == null || panel == null
                || !TaskRepositoryOptionsUtil.isEqual(repository.getOptions(), panel.getRepository().getOptions())) {
                contentManager.removeContent(entry.getValue(), true);
                myContents.remove(entry.getKey());
            }
        }

        for (Map.Entry<String, TaskRepository> entry : repositories.entrySet()) {
            if (myContents.containsKey(entry.getKey())) {
                continue;
            }
            IssueListPanel panel = new IssueListPanel(myProject, entry.getValue());
            Content content = ContentFactory.getInstance().createUIContent(panel.getComponent(), getTabName(entry.getValue()), false);
            content.setDisposer(panel);
            content.putUserData(IssueListPanelKey.KEY, panel);
            contentManager.addContent(content);
            myContents.put(entry.getKey(), content);
        }
    }
}
