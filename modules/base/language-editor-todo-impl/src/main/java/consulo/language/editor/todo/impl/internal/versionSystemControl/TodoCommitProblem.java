/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor.todo.impl.internal.versionSystemControl;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CommitInfo;
import consulo.versionControlSystem.checkin.CommitProblemWithDetails;
import consulo.versionControlSystem.localize.VcsLocalize;

import java.util.concurrent.CompletableFuture;

public class TodoCommitProblem implements CommitProblemWithDetails {
    private final TodoCheckinHandlerWorker myWorker;

    public TodoCommitProblem(TodoCheckinHandlerWorker worker) {
        myWorker = worker;
    }

    @Override
    public LocalizeValue getText() {
        return VcsLocalize.labelTodoItemsFound(myWorker.inOneList().size());
    }

    @RequiredUIAccess
    @Override
    public void showDetails(Project project) {
        TodoCheckinHandler.showTodoItems(project, myWorker);
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<CheckinHandler.ReturnResult> showModalSolution(Project project, CommitInfo commitInfo) {
        return TodoCheckinHandler.showDialog(project, myWorker, commitInfo.getCommitActionText());
    }

    @Override
    public LocalizeValue getShowDetailsAction() {
        return VcsLocalize.todoInNewReviewButton();
    }
}
