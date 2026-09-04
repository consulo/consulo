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
package consulo.ui;

import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

/**
 * The "do not ask again" affordance of a message box. The remember object stores and loads the
 * answer itself, so a box whose answer is already remembered never opens at all and its result is
 * available immediately.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public interface MessageBoxRemember<V extends @Nullable Object> {
    void setValue(V value);

    /**
     * @return null when nothing is remembered yet, in which case the box is shown
     */
    @Nullable
    V getValue();

    LocalizeValue getMessageText();

    default boolean isRememberByDefault() {
        return false;
    }

    /**
     * When the checkbox is honoured.
     */
    default RememberScope getScope() {
        return RememberScope.ON_ANSWER;
    }

    /**
     * Whether the checkbox is offered at all. A hidden option still remembers, silently.
     */
    default boolean isVisible() {
        return true;
    }
}
