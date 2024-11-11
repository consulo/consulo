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
package consulo.build.ui.impl.internal.event;

import consulo.annotation.component.ServiceImpl;
import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.FilePosition;
import consulo.build.ui.event.*;
import consulo.navigation.Navigatable;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-11-11
 */
@ServiceImpl
@Singleton
public class BuildEventFactoryImpl implements BuildEventFactory {
    @Nonnull
    @Override
    public SuccessResult createSuccessResult(boolean isUpToDate) {
        return new SuccessResultImpl(isUpToDate);
    }

    @Nonnull
    @Override
    public Failure createFailure(String message, String description, @Nonnull List<? extends Failure> causes, @Nullable Throwable error, @Nullable Notification notification, @Nullable Navigatable navigatable) {
        return new FailureImpl(message, description, causes, error, notification, navigatable);
    }

    @Nonnull
    @Override
    public FailureResult createFailureResult(@Nonnull List<Failure> failures) {
        return new FailureResultImpl(failures);
    }

    @Nonnull
    @Override
    public FileMessageEvent createFileMessageEvent(@Nonnull Object parentId,
                                                   @Nonnull MessageEvent.Kind kind,
                                                   @Nonnull NotificationGroup group,
                                                   @Nonnull String message,
                                                   @Nullable String detailedMessage,
                                                   @Nonnull FilePosition filePosition) {
        return new FileMessageEventImpl(parentId, kind, group, message, detailedMessage, filePosition);
    }

    @Nonnull
    @Override
    public FinishEvent createFinishEvent(@Nonnull Object eventId, @Nullable Object parentId, long eventTime, @Nonnull String message, @Nonnull EventResult result) {
        return new FinishEventImpl(eventId, parentId, eventTime, message, result);
    }

    @Nonnull
    @Override
    public FinishBuildEvent createFinishBuildEvent(@Nonnull Object eventId, @Nullable Object parentId, long eventTime, @Nonnull String message, @Nonnull EventResult result) {
        return new FinishBuildEventImpl(eventId, parentId, eventTime, message, result);
    }

    @Nonnull
    @Override
    public MessageEvent createMessageEvent(@Nonnull Object parentId, @Nonnull MessageEvent.Kind kind, @Nonnull NotificationGroup group, @Nonnull String message, @Nullable String detailedMessage) {
        return new MessageEventImpl(parentId, kind, group, message, detailedMessage);
    }

    @Nonnull
    @Override
    public StartEvent createStartEvent(@Nonnull Object eventId, @Nullable Object parentId, long eventTime, @Nonnull String message) {
        return new StartEventImpl(eventId, parentId, eventTime, message);
    }

    @Nonnull
    @Override
    public StartBuildEvent createStartBuildEvent(@Nonnull BuildDescriptor descriptor, @Nonnull String message) {
        return new StartBuildEventImpl(descriptor, message);
    }

    @Nonnull
    @Override
    public OutputBuildEvent createOutputBuildEvent(@Nonnull Object eventId, @Nullable Object parentId, @Nonnull String message, boolean stdOut) {
        return new OutputBuildEventImpl(eventId, parentId, message, false);
    }
}
