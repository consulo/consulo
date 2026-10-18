// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.review.details.model;

import consulo.versionControlSystem.change.RefComparisonChange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeReviewChangesContainer {
    private final List<RefComparisonChange> mySummaryChanges;
    private final List<String> myCommits;
    private final Map<String, List<RefComparisonChange>> myChangesByCommits;
    private final Map<RefComparisonChange, String> myCommitsByChange;

    public CodeReviewChangesContainer(List<RefComparisonChange> summaryChanges,
                                      List<String> commits,
                                      Map<String, List<RefComparisonChange>> changesByCommits) {
        mySummaryChanges = summaryChanges;
        myCommits = commits;
        myChangesByCommits = changesByCommits;

        Map<RefComparisonChange, String> commitsByChange = new HashMap<>();
        for (Map.Entry<String, List<RefComparisonChange>> entry : changesByCommits.entrySet()) {
            for (RefComparisonChange change : entry.getValue()) {
                commitsByChange.put(change, entry.getKey());
            }
        }
        myCommitsByChange = commitsByChange;
    }

    public List<RefComparisonChange> getSummaryChanges() {
        return mySummaryChanges;
    }

    public List<String> getCommits() {
        return myCommits;
    }

    public Map<String, List<RefComparisonChange>> getChangesByCommits() {
        return myChangesByCommits;
    }

    public Map<RefComparisonChange, String> getCommitsByChange() {
        return myCommitsByChange;
    }
}
