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
package consulo.continuousIntegration;

/**
 * Part of a build log.
 *
 * @param text       new text since the requested offset
 * @param nextOffset offset for the next request
 * @param complete   {@code true} when the log will not grow anymore (the build or stage is finished)
 * @since 2026-11-09
 */
public record CILogChunk(String text, long nextOffset, boolean complete) {
    public static CILogChunk completed(String text) {
        return new CILogChunk(text, text.length(), true);
    }
}
