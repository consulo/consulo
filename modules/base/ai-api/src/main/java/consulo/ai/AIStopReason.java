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
 * @author VISTALL
 * @since 2026-08-04
 */
public enum AIStopReason {
    /**
     * The model finished its answer.
     */
    END_TURN,
    /**
     * The model wants tools to be called; the answer is incomplete until results are sent back.
     */
    TOOL_USE,
    /**
     * The token budget ran out before the model was done.
     */
    MAX_TOKENS,
    /**
     * The caller cancelled the request.
     */
    CANCELLED,
    OTHER
}
