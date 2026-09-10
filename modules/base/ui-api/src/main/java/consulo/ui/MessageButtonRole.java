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
 * An answer the platform already has a name for, and already knows where to place. Every member
 * resolves to a standard label, so a role can never render as "label not found", and the set is
 * deliberately small - only answers a platform is expected to name for itself belong here.
 * <p>
 * There is no button without a role. A dialog which needs an answer outside this set is a dialog
 * rather than a message box, and belongs in a dialog descriptor.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public enum MessageButtonRole {
    OK,
    YES,
    NO,
    CANCEL,
    CLOSE,
    RETRY,
    YES_TO_ALL,
    NO_TO_ALL,
    HELP
}
