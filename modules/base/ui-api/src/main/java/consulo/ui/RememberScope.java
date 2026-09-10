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

/**
 * When a remembered answer is actually written.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public enum RememberScope {
    /**
     * Only when the box was accepted.
     */
    ON_ACCEPT,
    /**
     * Any button press, but not dismissal. The default.
     */
    ON_ANSWER,
    /**
     * Any button press, dismissal included.
     */
    ON_ANY_ANSWER
}
