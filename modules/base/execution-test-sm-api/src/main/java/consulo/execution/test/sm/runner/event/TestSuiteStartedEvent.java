/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.test.sm.runner.event;

import jetbrains.buildServer.messages.serviceMessages.TestSuiteStarted;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class TestSuiteStartedEvent extends BaseStartedNodeEvent {
    public TestSuiteStartedEvent(
        @Nullable String name,
        @Nullable String id,
        @Nullable String parentId,
        @Nullable String locationUrl,
        @Nullable String metainfo,
        @Nullable String nodeType,
        @Nullable String nodeArgs,
        boolean running
    ) {
        super(name, id, parentId, locationUrl, metainfo, nodeType, nodeArgs, running);
    }

    public TestSuiteStartedEvent(@Nonnull TestSuiteStarted suiteStarted, @Nullable String locationUrl) {
        this(suiteStarted, locationUrl, BaseStartedNodeEvent.getMetainfo(suiteStarted));
    }

    public TestSuiteStartedEvent(@Nonnull TestSuiteStarted suiteStarted, @Nullable String locationUrl, @Nullable String metainfo) {
        super(
            suiteStarted.getSuiteName(),
            TreeNodeEvent.getNodeId(suiteStarted),
            getParentNodeId(suiteStarted),
            locationUrl,
            metainfo,
            BaseStartedNodeEvent.getNodeType(suiteStarted),
            BaseStartedNodeEvent.getNodeArgs(suiteStarted),
            BaseStartedNodeEvent.isRunning(suiteStarted)
        );
    }

    public TestSuiteStartedEvent(@Nonnull String name, @Nullable String locationUrl) {
        this(name, locationUrl, null);
    }

    public TestSuiteStartedEvent(@Nonnull String name, @Nullable String locationUrl, @Nullable String metainfo) {
        super(name, null, null, locationUrl, metainfo, null, null, true);
    }
}
