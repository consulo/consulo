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
import consulo.build.ui.issue.BuildIssue;
import consulo.navigation.Navigatable;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-11-11
 */
@ServiceImpl
@Singleton
public class BuildEventFactoryImpl implements BuildEventFactory {
    
    @Override
    public SkippedResult createSkippedResult() {
        return new SkippedResultImpl();
    }

    
    @Override
    public SuccessResult createSuccessResult(boolean isUpToDate) {
        return new SuccessResultImpl(isUpToDate);
    }

    
    @Override
    public DerivedResult createDerivedResult(@Nullable Supplier<EventResult> onDefault, @Nullable Supplier<FailureResult> onFail) {
        return new DerivedResultImpl(onDefault, onFail);
    }

    
    @Override
    public Failure createFailure(String message, String description, List<? extends Failure> causes, @Nullable Throwable error, @Nullable Notification notification, @Nullable Navigatable navigatable) {
        return new FailureImpl(message, description, causes, error, notification, navigatable);
    }

    
    @Override
    public FailureResult createFailureResult(List<Failure> failures) {
        return new FailureResultImpl(failures);
    }

    
    @Override
    public FileMessageEvent createFileMessageEvent(Object parentId,
                                                   MessageEvent.Kind kind,
                                                   NotificationGroup group,
                                                   String message,
                                                   @Nullable String detailedMessage,
                                                   FilePosition filePosition) {
        return new FileMessageEventImpl(parentId, kind, group, message, detailedMessage, filePosition);
    }

    
    @Override
    public FinishEvent createFinishEvent(Object eventId, @Nullable Object parentId, long eventTime, String message, EventResult result) {
        return new FinishEventImpl(eventId, parentId, eventTime, message, result);
    }

    
    @Override
    public FinishBuildEvent createFinishBuildEvent(Object eventId, @Nullable Object parentId, long eventTime, String message, EventResult result) {
        return new FinishBuildEventImpl(eventId, parentId, eventTime, message, result);
    }

    
    @Override
    public MessageEvent createMessageEvent(Object parentId,
                                           MessageEvent.Kind kind,
                                           NotificationGroup group,
                                           String message,
                                           @Nullable String detailedMessage,
                                           @Nullable Navigatable navigatable) {
        return new MessageEventImpl(parentId, kind, group, message, detailedMessage, navigatable);
    }

    
    @Override
    public StartEvent createStartEvent(Object eventId, @Nullable Object parentId, long eventTime, String message) {
        return new StartEventImpl(eventId, parentId, eventTime, message);
    }

    
    @Override
    public BuildIssueEvent createBuildIssueEvent(Object parentId, BuildIssue buildIssue, MessageEvent.Kind kind) {
        return new BuildIssueEventImpl(parentId, buildIssue, kind);
    }

    
    @Override
    public StartBuildEvent createStartBuildEvent(BuildDescriptor descriptor, String message) {
        return new StartBuildEventImpl(descriptor, message);
    }

    
    @Override
    public OutputBuildEvent createOutputBuildEvent(Object eventId, @Nullable Object parentId, String message, boolean stdOut) {
        return new OutputBuildEventImpl(eventId, parentId, message, false);
    }
}
