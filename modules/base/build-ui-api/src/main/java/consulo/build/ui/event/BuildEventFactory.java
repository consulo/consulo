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
import consulo.navigation.Navigatable;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-11-11
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface BuildEventFactory {
    @Nonnull
    SkippedResult createSkippedResult();

    @Nonnull
    default SuccessResult createSuccessResult() {
        return createSuccessResult(false);
    }

    @Nonnull
    SuccessResult createSuccessResult(boolean isUpToDate);

    @Nonnull
    default FailureResult createFailureResult() {
        return createFailureResult(null, null);
    }

    @Nonnull
    default FailureResult createFailureResult(@Nullable Throwable error) {
        return createFailureResult(null, error);
    }

    @Nonnull
    default FailureResult createFailureResult(@Nullable @BuildEventsNls.Message String message) {
        return createFailureResult(message, null);
    }

    @Nonnull
    default FailureResult createFailureResult(@Nullable @BuildEventsNls.Message String message, @Nullable Throwable error) {
        List<Failure> failures = List.of();
        if (message != null || error != null) {
            failures = List.of(createFailure(message, error));
        }
        return createFailureResult(failures);
    }

    @Nonnull
    default Failure createFailure(@BuildEventsNls.Message String message, Throwable error) {
        return createFailure(message, null, Collections.emptyList(), error, null, null);
    }

    @Nonnull
    default Failure createFailure(@BuildEventsNls.Message String message,
                                  Throwable error,
                                  @Nullable Notification notification,
                                  @Nullable Navigatable navigatable) {
        return createFailure(message, null, Collections.emptyList(), error, notification, navigatable);
    }

    @Nonnull
    default Failure createFailure(@BuildEventsNls.Message String message, @BuildEventsNls.Description String description) {
        return createFailure(message, description, List.of(), null, null, null);
    }

    @Nonnull
    default Failure createFailure(@BuildEventsNls.Message String message,
                                  @BuildEventsNls.Description String description,
                                  @Nonnull List<? extends Failure> causes) {
        return createFailure(message, description, causes, null, null, null);
    }

    @Nonnull
    Failure createFailure(@BuildEventsNls.Message String message,
                          @BuildEventsNls.Description String description,
                          @Nonnull List<? extends Failure> causes,
                          @Nullable Throwable error,
                          @Nullable Notification notification,
                          @Nullable Navigatable navigatable);

    @Nonnull
    FailureResult createFailureResult(@Nonnull List<Failure> failures);

    @Nonnull
    FileMessageEvent createFileMessageEvent(@Nonnull Object parentId,
                                            @Nonnull MessageEvent.Kind kind,
                                            @Nonnull NotificationGroup group,
                                            @Nonnull @BuildEventsNls.Message String message,
                                            @Nullable @BuildEventsNls.Description String detailedMessage,
                                            @Nonnull FilePosition filePosition);

    @Nonnull
    FinishEvent createFinishEvent(@Nonnull Object eventId,
                                  @Nullable Object parentId,
                                  long eventTime,
                                  @Nonnull @BuildEventsNls.Message String message,
                                  @Nonnull EventResult result);

    @Nonnull
    FinishBuildEvent createFinishBuildEvent(@Nonnull Object eventId,
                                            @Nullable Object parentId,
                                            long eventTime,
                                            @Nonnull @BuildEventsNls.Message String message,
                                            @Nonnull EventResult result);

    @Nonnull
    MessageEvent createMessageEvent(@Nonnull Object parentId,
                                    @Nonnull MessageEvent.Kind kind,
                                    @Nonnull NotificationGroup group,
                                    @Nonnull @BuildEventsNls.Message String message,
                                    @Nullable @BuildEventsNls.Description String detailedMessage);

    @Nonnull
    StartEvent createStartEvent(@Nonnull Object eventId,
                                @Nullable Object parentId,
                                long eventTime,
                                @Nonnull @BuildEventsNls.Message String message);

    @Nonnull
    StartBuildEvent createStartBuildEvent(@Nonnull BuildDescriptor descriptor, @Nonnull @BuildEventsNls.Message String message);

    @Nonnull
    default OutputBuildEvent createOutputBuildEvent(@Nullable Object parentId, @Nonnull @BuildEventsNls.Message String message, boolean stdOut) {
        return createOutputBuildEvent(new Object(), parentId, message, stdOut);
    }

    @Nonnull
    OutputBuildEvent createOutputBuildEvent(@Nonnull Object eventId, @Nullable Object parentId, @Nonnull @BuildEventsNls.Message String message, boolean stdOut);
}
