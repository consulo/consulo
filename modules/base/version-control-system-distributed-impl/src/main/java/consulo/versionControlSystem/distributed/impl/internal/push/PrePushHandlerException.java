/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.impl.internal.push;

import consulo.localize.LocalizeValue;

import java.util.List;

/**
 * An exception thrown if a {@link consulo.versionControlSystem.distributed.push.PrePushHandler} has failed to make the decision
 * by whatever reason: either it had been cancelled, or an execution exception had occurred.
 */
class PrePushHandlerException extends RuntimeException {
    /**
     * Name of the handler on which an exception happened.
     */
    private final LocalizeValue myFailedHandlerName;

    /**
     * Names of handlers which were skipped because {@link #myFailedHandlerName} had failed.
     */
    private final List<LocalizeValue> mySkippedHandlers;

    PrePushHandlerException(LocalizeValue failedHandlerName, List<LocalizeValue> skippedHandlers, Throwable cause) {
        super(cause);
        myFailedHandlerName = failedHandlerName;
        mySkippedHandlers = skippedHandlers;
    }

    public LocalizeValue getFailedHandlerName() {
        return myFailedHandlerName;
    }

    public List<LocalizeValue> getSkippedHandlers() {
        return mySkippedHandlers;
    }
}
