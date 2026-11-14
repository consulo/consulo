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
package consulo.ai;

/**
 * What choosing an option of a permission request means.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public enum AIToolPermissionOptionKind {
    ALLOW_ONCE,
    ALLOW_ALWAYS,
    REJECT_ONCE,
    REJECT_ALWAYS,
    /**
     * A choice which is not a yes or no - an answer to a question the agent asks through a permission request
     */
    OTHER;

    public boolean isAllow() {
        return this == ALLOW_ONCE || this == ALLOW_ALWAYS;
    }

    public boolean isReject() {
        return this == REJECT_ONCE || this == REJECT_ALWAYS;
    }
}
