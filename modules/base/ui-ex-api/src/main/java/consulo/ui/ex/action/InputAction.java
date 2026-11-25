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
package consulo.ui.ex.action;

/**
 * An action performed like typed input: it does nothing visible before its write action. Invoked from the keyboard while a
 * background write action holds the write-intent lock, it waits in the input queue of its ui instead of blocking the ui
 * thread, and runs in order with the keys typed around it.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public interface InputAction {
    /**
     * @return false when the action is not input after all - for example when it opens a dialog before it writes
     */
    default boolean isInputAction() {
        return true;
    }
}
