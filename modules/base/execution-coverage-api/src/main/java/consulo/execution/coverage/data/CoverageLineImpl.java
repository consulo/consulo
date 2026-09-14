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

public class CoverageLineImpl implements CoverageLine {
    private final int myLineNumber;
    private final String myMethodSignature;
    private LineStatus myStatus = LineStatus.NOT_COVERED;
    private int myHits;

    public CoverageLineImpl(int lineNumber, @Nullable String methodSignature) {
        myLineNumber = lineNumber;
        myMethodSignature = methodSignature;
    }

    @Override
    public int getLineNumber() {
        return myLineNumber;
    }

    @Override
    public LineStatus getStatus() {
        return myStatus;
    }

    @Override
    public void setStatus(LineStatus status) {
        myStatus = status;
    }

    @Override
    public int getHits() {
        return myHits;
    }

    public void setHits(int hits) {
        myHits = hits;
    }

    @Nullable
    @Override
    public String getMethodSignature() {
        return myMethodSignature;
    }

    @Override
    public boolean isCoveredBySingleTest() {
        return false;
    }
}
