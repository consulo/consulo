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

public class BranchCoverageImpl implements BranchCoverage {
    private final int[] myHits;

    public BranchCoverageImpl(int outcomeCount) {
        myHits = new int[outcomeCount];
    }

    @Override
    public int getOutcomeCount() {
        return myHits.length;
    }

    @Override
    public int getHits(int outcome) {
        return myHits[outcome];
    }

    public void touch(int outcome) {
        myHits[outcome]++;
    }

    public void merge(BranchCoverage data) {
        int count = Math.min(myHits.length, data.getOutcomeCount());
        for (int i = 0; i < count; i++) {
            myHits[i] += data.getHits(i);
        }
    }
}
