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

import consulo.application.progress.ProgressIndicator;
import consulo.project.DumbService;
import consulo.ui.UIAccess;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinModificationHandler;
import consulo.versionControlSystem.checkin.CommitCheck;
import consulo.versionControlSystem.checkin.CommitInfo;
import consulo.versionControlSystem.checkin.CommitProblem;
import consulo.versionControlSystem.impl.internal.change.PseudoMap;
import org.jspecify.annotations.Nullable;

/**
 * Runs a {@link CheckinHandler} which does not implement {@link CommitCheck} as a commit check, on the UI thread.
 *
 * @author VISTALL
 * @since 2026-10-17
 */
public class ProxyCommitCheck implements CommitCheck {
    private final CheckinHandler myCheckinHandler;
    private final UIAccess myUIAccess;
    private final PseudoMap<Object, Object> myAdditionalData;

    public ProxyCommitCheck(CheckinHandler checkinHandler, UIAccess uiAccess, PseudoMap<Object, Object> additionalData) {
        myCheckinHandler = checkinHandler;
        myUIAccess = uiAccess;
        myAdditionalData = additionalData;
    }

    public CheckinHandler getCheckinHandler() {
        return myCheckinHandler;
    }

    @Override
    public ExecutionOrder getExecutionOrder() {
        return myCheckinHandler instanceof CheckinModificationHandler ? ExecutionOrder.MODIFICATION : ExecutionOrder.LATE;
    }

    @Override
    public boolean isDumbAware() {
        return DumbService.isDumbAware(myCheckinHandler);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nullable CommitProblem runCheck(CommitInfo commitInfo, ProgressIndicator indicator) {
        CheckinHandler.ReturnResult result = myUIAccess.giveAsync(() -> myCheckinHandler.beforeCheckin(commitInfo.getExecutor(), myAdditionalData)).join();
        if (result == null || result == CheckinHandler.ReturnResult.COMMIT) {
            return null;
        }
        return new UnknownCommitProblem(result);
    }

    @Override
    public String toString() {
        return "ProxyCommitCheck: " + myCheckinHandler;
    }
}
