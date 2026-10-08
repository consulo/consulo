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

import org.jspecify.annotations.Nullable;

import java.util.List;

public interface CoverageLine {
    int getLineNumber();

    LineStatus getStatus();

    void setStatus(LineStatus status);

    int getHits();

    @Nullable
    String getMethodSignature();

    boolean isCoveredBySingleTest();

    @Nullable
    String getUniqueTestName();

    List<BranchCoverage> getBranches();

    static LineStatus getStatus(@Nullable CoverageLine coverageLine) {
        return coverageLine == null ? LineStatus.NOT_COVERED : coverageLine.getStatus();
    }

    static boolean isSomewhatCovered(@Nullable CoverageLine coverageLine) {
        return coverageLine != null && coverageLine.getStatus() != LineStatus.NOT_COVERED;
    }

    static boolean isNotCovered(@Nullable CoverageLine coverageLine) {
        return !isSomewhatCovered(coverageLine);
    }
}
