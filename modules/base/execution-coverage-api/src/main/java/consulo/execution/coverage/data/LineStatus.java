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
package consulo.execution.coverage.data;

import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.localize.LocalizeValue;

public enum LineStatus {
    NOT_COVERED(ExecutionCoverageLocalize.coverageNextChangeUncovered(), CodeInsightColors.LINE_NONE_COVERAGE),
    PARTIALLY_COVERED(ExecutionCoverageLocalize.coverageNextChangePartialCovered(), CodeInsightColors.LINE_PARTIAL_COVERAGE),
    COVERED(ExecutionCoverageLocalize.coverageNextChangeFullyCovered(), CodeInsightColors.LINE_FULL_COVERAGE);

    private final LocalizeValue myDisplayName;
    private final TextAttributesKey myAttributesKey;

    LineStatus(LocalizeValue displayName, TextAttributesKey attributesKey) {
        myDisplayName = displayName;
        myAttributesKey = attributesKey;
    }

    public LocalizeValue getDisplayName() {
        return myDisplayName;
    }

    public TextAttributesKey getAttributesKey() {
        return myAttributesKey;
    }
}
