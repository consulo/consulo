/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.push;

import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import org.jspecify.annotations.Nullable;

public class VcsError {
    private final LocalizeValue myErrorText;
    private final LocalizeValue myHandlerText;
    private final @Nullable VcsErrorHandler myErrorHandleListener;

    public VcsError(LocalizeValue text) {
        this(text, LocalizeValue.empty(), null);
    }

    public VcsError(LocalizeValue text, @Nullable VcsErrorHandler listener) {
        this(text, LocalizeValue.empty(), listener);
    }

    public VcsError(LocalizeValue text, LocalizeValue handlerText, @Nullable VcsErrorHandler listener) {
        myErrorText = text;
        myHandlerText = handlerText;
        myErrorHandleListener = listener;
    }

    public LocalizeValue getText() {
        return myErrorText;
    }

    public LocalizeValue getHandlerText() {
        return myHandlerText;
    }

    public boolean hasHandler() {
        return myErrorHandleListener != null;
    }

    public void handleError(CommitLoader loader) {
        if (myErrorHandleListener != null) {
            myErrorHandleListener.handleError(loader);
        }
    }

    public static VcsError createEmptyTargetError(String name) {
        return new VcsError(DistributedVcsLocalize.pushErrorSpecifyNotEmptyRemotePushPath0(name));
    }
}
