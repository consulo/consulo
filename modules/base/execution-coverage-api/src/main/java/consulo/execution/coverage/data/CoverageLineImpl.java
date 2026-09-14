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

import java.util.ArrayList;
import java.util.List;

public class CoverageLineImpl implements CoverageLine {
    private final int myLineNumber;
    private String myMethodSignature;
    private LineStatus myStatus = LineStatus.NOT_COVERED;
    private int myHits;
    private String myUniqueTestName;
    private final List<BranchCoverage> myBranches = new ArrayList<>();

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

    public void touch() {
        myHits++;
    }

    @Nullable
    @Override
    public String getMethodSignature() {
        return myMethodSignature;
    }

    @Override
    public boolean isCoveredBySingleTest() {
        return myUniqueTestName != null && !myUniqueTestName.isEmpty();
    }

    @Nullable
    @Override
    public String getUniqueTestName() {
        return myUniqueTestName;
    }

    public void setUniqueTestName(@Nullable String testName) {
        myUniqueTestName = testName;
    }

    @Override
    public List<BranchCoverage> getBranches() {
        return myBranches;
    }

    public void addBranch(BranchCoverage branch) {
        myBranches.add(branch);
    }

    public void merge(CoverageLine data) {
        myHits += data.getHits();

        List<BranchCoverage> otherBranches = data.getBranches();
        for (int i = 0; i < otherBranches.size(); i++) {
            BranchCoverage other = otherBranches.get(i);
            if (i < myBranches.size()) {
                BranchCoverage mine = myBranches.get(i);
                if (mine instanceof BranchCoverageImpl branchImpl) {
                    branchImpl.merge(other);
                    continue;
                }
            }
            BranchCoverageImpl copy = new BranchCoverageImpl(other.getOutcomeCount());
            copy.merge(other);
            myBranches.add(copy);
        }

        if (data.getMethodSignature() != null) {
            myMethodSignature = data.getMethodSignature();
        }
        if (data.getStatus().ordinal() > myStatus.ordinal()) {
            myStatus = data.getStatus();
        }
        if (myUniqueTestName == null) {
            myUniqueTestName = data.getUniqueTestName();
        }
        else if (data.getUniqueTestName() != null && !myUniqueTestName.equals(data.getUniqueTestName())) {
            myUniqueTestName = "";
        }
    }
}
