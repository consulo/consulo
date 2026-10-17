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
package consulo.versionControlSystem.impl.internal.commit;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CommitInfo;
import consulo.versionControlSystem.checkin.CommitProblem;
import consulo.versionControlSystem.localize.VcsLocalize;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnknownCommitProblem implements CommitProblem {
    private final CheckinHandler.ReturnResult myResult;

    public UnknownCommitProblem(CheckinHandler.ReturnResult result) {
        myResult = result;
    }

    public CheckinHandler.ReturnResult getResult() {
        return myResult;
    }

    @Override
    public LocalizeValue getText() {
        return VcsLocalize.beforeCheckinErrorUnknown();
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<CheckinHandler.ReturnResult> showModalSolution(Project project, CommitInfo commitInfo) {
        return CompletableFuture.completedFuture(myResult);
    }
}
