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
 * Standard parameter kinds. The impl has editors for all of them; provider-specific types (Jenkins plugin parameters)
 * get editors from {@link consulo.continuousIntegration.ui.CIParameterEditorProvider}.
 *
 * @since 2026-11-09
 */
public enum CIParameterKind {
    STRING,
    TEXT,
    BOOLEAN,
    CHOICE,
    PASSWORD,
    FILE,
    /**
     * Shown as a string unless a {@link consulo.continuousIntegration.ui.CIParameterEditorProvider} accepts it.
     */
    OTHER
}
