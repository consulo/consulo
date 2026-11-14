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

import org.jspecify.annotations.Nullable;

/**
 * The user's answer to an {@link AIToolPermissionRequest}.
 *
 * @param allowed  whether the tool may run
 * @param always   allowed, and the same tool may run again in this conversation without asking
 * @param message  why it was denied, told to the model so it can change course
 * @param optionId the option of the request the user chose, when the request offered its own
 * @author VISTALL
 * @since 2026-10-13
 */
public record AIToolPermissionDecision(boolean allowed, boolean always, String message, @Nullable String optionId) {
    public static AIToolPermissionDecision allow() {
        return new AIToolPermissionDecision(true, false, "", null);
    }

    public static AIToolPermissionDecision allowAlways() {
        return new AIToolPermissionDecision(true, true, "", null);
    }

    public static AIToolPermissionDecision deny(String message) {
        return new AIToolPermissionDecision(false, false, message, null);
    }

    public static AIToolPermissionDecision option(AIToolPermissionOption option) {
        AIToolPermissionOptionKind kind = option.kind();
        return new AIToolPermissionDecision(!kind.isReject(), kind == AIToolPermissionOptionKind.ALLOW_ALWAYS, "", option.id());
    }
}
