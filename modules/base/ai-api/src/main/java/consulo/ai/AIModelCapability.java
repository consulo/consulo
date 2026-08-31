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
 * What a model can do. Callers should check these rather than special casing model names, since the
 * set of models changes far more often than the set of capabilities.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public enum AIModelCapability {
    /**
     * Can stream the answer token by token instead of only returning it whole.
     */
    STREAMING,
    /**
     * Can be given tools and ask for them to be called.
     */
    TOOL_USE,
    /**
     * Accepts images as input.
     */
    VISION,
    /**
     * Can expose intermediate reasoning separately from the answer.
     */
    THINKING,
    /**
     * Supports reusing a previously sent prefix of the conversation at reduced cost.
     */
    CACHING
}
