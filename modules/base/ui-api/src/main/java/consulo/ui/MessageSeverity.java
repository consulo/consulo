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
 * The level a message box is shown at. NONE means no image at all, which is a state of its own and
 * the reason this is not {@link NotificationType}.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public enum MessageSeverity {
    NONE,
    INFO,
    WARNING,
    ERROR,
    QUESTION
}
