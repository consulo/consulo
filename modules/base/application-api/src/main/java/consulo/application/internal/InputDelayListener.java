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
package consulo.application.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Input given to {@link ApplicationEx#runInput} which waits for a background write action.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@TopicAPI(ComponentScope.APPLICATION)
public interface InputDelayListener {
    /**
     * Called on the ui thread of {@code uiAccess} once its input has waited for a background write action for a while.
     */
    @RequiredUIAccess
    default void inputDelayed(UIAccess uiAccess) {
    }

    /**
     * Called on the ui thread of {@code uiAccess} after {@link #inputDelayed}, once all its waiting input ran.
     */
    @RequiredUIAccess
    default void inputResumed(UIAccess uiAccess) {
    }
}
