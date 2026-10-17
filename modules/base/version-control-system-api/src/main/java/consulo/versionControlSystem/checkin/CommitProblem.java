/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors.
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
package consulo.versionControlSystem.checkin;

import consulo.component.util.PluginExceptionUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageButtonRole;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ExceptionUtil;
import consulo.versionControlSystem.localize.VcsLocalize;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Represents some problem found in the commit.
 *
 * @see CommitProblemWithDetails
 */
public interface CommitProblem {
    /**
     * Short problem description to show to the user.
     * <p>
     * HTML markup is not supported, use {@link CommitProblemWithDetails}.
     */
    LocalizeValue getText();

    /**
     * Suggest a solution for the found problem in modal commit mode, if needed.
     * Typically, this is a {@code Problem found, commit anyway? [Yes/No]} message dialog.
     * <p>
     * Method is not called for non-modal commit modes.
     */
    @RequiredUIAccess
    default CompletableFuture<CheckinHandler.ReturnResult> showModalSolution(Project project, CommitInfo commitInfo) {
        if (this instanceof CommitProblemWithDetails withDetails) {
            return MessageBoxBuilder.<CheckinHandler.ReturnResult>create()
                .asWarning()
                .title(VcsLocalize.checkinCommitChecksFailed())
                .text(getText())
                .button(MessageButtonRole.YES, withDetails.getShowDetailsAction(), () -> {
                    withDetails.showDetails(project);
                    return CheckinHandler.ReturnResult.CLOSE_WINDOW;
                })
                .asDefaultButton()
                .button(MessageButtonRole.NO, commitInfo.getCommitActionText(), CheckinHandler.ReturnResult.COMMIT)
                .button(MessageButtonRole.CANCEL, VcsLocalize.checkinCommitChecksFailedCancelButton(), CheckinHandler.ReturnResult.CANCEL)
                .asExitButton()
                .showAsync();
        }

        return MessageBoxBuilder.<CheckinHandler.ReturnResult>create()
            .asWarning()
            .title(VcsLocalize.checkinCommitChecksFailed())
            .text(getText())
            .button(MessageButtonRole.YES, commitInfo.getCommitActionText(), CheckinHandler.ReturnResult.COMMIT)
            .asDefaultButton()
            .button(MessageButtonRole.NO, VcsLocalize.checkinCommitChecksFailedCancelButton(), CheckinHandler.ReturnResult.CANCEL)
            .asExitButton()
            .showAsync();
    }

    static CommitProblem createError(Throwable e) {
        PluginId pluginId = PluginExceptionUtil.findFirstPluginId(e);
        PluginDescriptor plugin = pluginId == null ? null : PluginManager.findPlugin(pluginId);

        LocalizeValue message = plugin != null
            ? VcsLocalize.beforeCheckinErrorInPlugin(plugin.getName())
            : VcsLocalize.beforeCheckinErrorInternal();

        IOException ioException = ExceptionUtil.findCause(e, IOException.class);
        if (ioException != null) {
            message = LocalizeValue.join(message, LocalizeValue.of(". " + ioException.getMessage()));
        }
        return new TextCommitProblem(message);
    }
}
