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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.FilePosition;
import consulo.build.ui.issue.BuildIssue;
import consulo.navigation.Navigatable;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
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

    
    default FailureResult createFailureResult() {
        return createFailureResult(null, null);
    }

    
    default FailureResult createFailureResult(@Nullable Throwable error) {
        return createFailureResult(null, error);
    }

    
    default FailureResult createFailureResult(@Nullable @BuildEventsNls.Message String message) {
        return createFailureResult(message, null);
    }

    
    default FailureResult createFailureResult(@Nullable @BuildEventsNls.Message String message, @Nullable Throwable error) {
        List<Failure> failures = List.of();
        if (message != null || error != null) {
            failures = List.of(createFailure(message, error));
        }
        return createFailureResult(failures);
    }

    
    default DerivedResult createDerivedResult() {
        return createDerivedResult(null, null);
    }

    
    DerivedResult createDerivedResult(@Nullable Supplier<EventResult> onDefault, @Nullable Supplier<FailureResult> onFail);

    
    default Failure createFailure(@BuildEventsNls.Message String message, Throwable error) {
        return createFailure(message, null, Collections.emptyList(), error, null, null);
    }

    
    default Failure createFailure(@BuildEventsNls.Message String message,
                                  Throwable error,
                                  @Nullable Notification notification,
                                  @Nullable Navigatable navigatable) {
        return createFailure(message, null, Collections.emptyList(), error, notification, navigatable);
    }

    
    default Failure createFailure(@BuildEventsNls.Message String message, @BuildEventsNls.Description String description) {
        return createFailure(message, description, List.of(), null, null, null);
    }

    
    default Failure createFailure(@BuildEventsNls.Message String message,
                                  @BuildEventsNls.Description String description,
                                  List<? extends Failure> causes) {
        return createFailure(message, description, causes, null, null, null);
    }

    
    Failure createFailure(@BuildEventsNls.Message String message,
                          @BuildEventsNls.Description String description,
                          List<? extends Failure> causes,
                          @Nullable Throwable error,
                          @Nullable Notification notification,
                          @Nullable Navigatable navigatable);

    
    FailureResult createFailureResult(List<Failure> failures);

    
    FileMessageEvent createFileMessageEvent(Object parentId,
                                            MessageEvent.Kind kind,
                                            NotificationGroup group,
                                            @BuildEventsNls.Message String message,
                                            @Nullable @BuildEventsNls.Description String detailedMessage,
                                            FilePosition filePosition);

    
    FinishEvent createFinishEvent(Object eventId,
                                  @Nullable Object parentId,
                                  long eventTime,
                                  @BuildEventsNls.Message String message,
                                  EventResult result);

    
    FinishBuildEvent createFinishBuildEvent(Object eventId,
                                            @Nullable Object parentId,
                                            long eventTime,
                                            @BuildEventsNls.Message String message,
                                            EventResult result);

    
    default MessageEvent createMessageEvent(Object parentId,
                                            MessageEvent.Kind kind,
                                            NotificationGroup group,
                                            String message,
                                            @Nullable String detailedMessage) {
        return createMessageEvent(parentId, kind, group, message, detailedMessage, null);
    }

    
    MessageEvent createMessageEvent(Object parentId,
                                    MessageEvent.Kind kind,
                                    NotificationGroup group,
                                    String message,
                                    @Nullable String detailedMessage,
                                    @Nullable Navigatable navigatable);

    
    StartEvent createStartEvent(Object eventId,
                                @Nullable Object parentId,
                                long eventTime,
                                @BuildEventsNls.Message String message);

    
    BuildIssueEvent createBuildIssueEvent(
        Object parentId,
        BuildIssue buildIssue,
        MessageEvent.Kind kind
    );

    
    StartBuildEvent createStartBuildEvent(BuildDescriptor descriptor, @BuildEventsNls.Message String message);

    
    default OutputBuildEvent createOutputBuildEvent(@Nullable Object parentId, @BuildEventsNls.Message String message, boolean stdOut) {
        return createOutputBuildEvent(new Object(), parentId, message, stdOut);
    }

    
    OutputBuildEvent createOutputBuildEvent(Object eventId, @Nullable Object parentId, @BuildEventsNls.Message String message, boolean stdOut);
}
