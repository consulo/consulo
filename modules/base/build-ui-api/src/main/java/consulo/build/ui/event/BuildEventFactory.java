/*
 * Copyright 2013-2024 consulo.io
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
package consulo.build.ui.event;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.FilePosition;
import consulo.build.ui.issue.BuildIssue;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-11-11
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface BuildEventFactory {
    SkippedResult createSkippedResult();

    default SuccessResult createSuccessResult() {
        return createSuccessResult(false);
    }

    SuccessResult createSuccessResult(boolean isUpToDate);

    @Deprecated
    @DeprecationInfo("Use #newFailure()...createResult()")
    @SuppressWarnings("deprecation")
    default FailureResult createFailureResult() {
        return newFailure().createResult();
    }

    default FailureResult createFailureResult(@Nullable Throwable error) {
        return newFailure().optionalError(error).createResult();
    }

    @Deprecated
    @DeprecationInfo("Use #newFailure()...createResult()")
    @SuppressWarnings("deprecation")
    default FailureResult createFailureResult(@Nullable @BuildEventsNls.Message String message) {
        return newFailure().message(message).createResult();
    }

    @Deprecated
    @DeprecationInfo("Use #newFailure()...createResult()")
    @SuppressWarnings("deprecation")
    default FailureResult createFailureResult(@Nullable @BuildEventsNls.Message String message, @Nullable Throwable error) {
        return newFailure()
            .message(message)
            .optionalError(error)
            .createResult();
    }

    default DerivedResult createDerivedResult() {
        return createDerivedResult(null, null);
    }

    DerivedResult createDerivedResult(@Nullable Supplier<EventResult> onDefault, @Nullable Supplier<FailureResult> onFail);

    Failure.Builder newFailure();

    @Deprecated
    @DeprecationInfo("Use #newFailure()...create()")
    @SuppressWarnings("deprecation")
    default Failure createFailure(@BuildEventsNls.Message String message, Throwable error) {
        return newFailure()
            .message(message)
            .error(error)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newFailure()...create()")
    @SuppressWarnings("deprecation")
    default Failure createFailure(
        @Nullable @BuildEventsNls.Message String message,
        Throwable error,
        @Nullable Notification notification,
        @Nullable Navigatable navigatable
    ) {
        return newFailure()
            .message(message)
            .error(error)
            .optionalNotification(notification)
            .optionalNavigatable(navigatable)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newFailure()...create()")
    @SuppressWarnings("deprecation")
    default Failure createFailure(
        @Nullable @BuildEventsNls.Message String message,
        @Nullable @BuildEventsNls.Description String description
    ) {
        return newFailure()
            .message(message)
            .description(description)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newFailure()...create()")
    @SuppressWarnings("deprecation")
    default Failure createFailure(
        @Nullable @BuildEventsNls.Message String message,
        @Nullable @BuildEventsNls.Description String description,
        List<? extends Failure> causes
    ) {
        return newFailure()
            .message(message)
            .description(description)
            .causes(causes)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newFailure()...create()")
    @SuppressWarnings("deprecation")
    default Failure createFailure(
        @Nullable @BuildEventsNls.Message String message,
        @Nullable @BuildEventsNls.Description String description,
        List<? extends Failure> causes,
        @Nullable Throwable error,
        @Nullable Notification notification,
        @Nullable Navigatable navigatable
    ) {
        return newFailure()
            .message(message)
            .description(description)
            .optionalError(error)
            .optionalNotification(notification)
            .optionalNavigatable(navigatable)
            .create();
    }

    FailureResult createFailureResult(List<Failure> failures);

    FileMessageEvent createFileMessageEvent(
        Object parentId,
        MessageEvent.Kind kind,
        NotificationGroup group,
        LocalizeValue message,
        LocalizeValue detailedMessage,
        FilePosition filePosition
    );

    FinishEvent createFinishEvent(
        Object eventId,
        @Nullable Object parentId,
        long eventTime,
        LocalizeValue message,
        EventResult result
    );

    FinishBuildEvent createFinishBuildEvent(
        Object eventId,
        @Nullable Object parentId,
        long eventTime,
        LocalizeValue message,
        EventResult result
    );

    default MessageEvent createMessageEvent(
        Object parentId,
        MessageEvent.Kind kind,
        NotificationGroup group,
        LocalizeValue message,
        LocalizeValue detailedMessage
    ) {
        return createMessageEvent(parentId, kind, group, message, detailedMessage, null);
    }

    MessageEvent createMessageEvent(
        Object parentId,
        MessageEvent.Kind kind,
        NotificationGroup group,
        LocalizeValue message,
        LocalizeValue detailedMessage,
        @Nullable Navigatable navigatable
    );

    StartEvent createStartEvent(
        Object eventId,
        @Nullable Object parentId,
        long eventTime,
        LocalizeValue message
    );

    BuildIssueEvent createBuildIssueEvent(
        Object parentId,
        BuildIssue buildIssue,
        MessageEvent.Kind kind
    );

    StartBuildEvent createStartBuildEvent(BuildDescriptor descriptor, LocalizeValue message);

    default OutputBuildEvent createOutputBuildEvent(@Nullable Object parentId, String message, boolean stdOut) {
        return createOutputBuildEvent(new Object(), parentId, message, stdOut);
    }

    OutputBuildEvent createOutputBuildEvent(Object eventId, @Nullable Object parentId, String message, boolean stdOut);
}
