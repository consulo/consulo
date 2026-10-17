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
package consulo.versionControlSystem.impl.internal.commit;

import consulo.versionControlSystem.checkin.CommitCheck;
import consulo.versionControlSystem.checkin.CommitProblem;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public final class UnifiedCommitChecksResult {
    public enum Kind {
        PASSED,
        FAILED,
        CLOSE_WINDOW,
        CANCELLED
    }

    private final Kind myKind;
    private final @Nullable CommitProblem myProblem;
    private final List<CommitCheck> myPostponedChecks;

    private UnifiedCommitChecksResult(Kind kind, @Nullable CommitProblem problem, List<CommitCheck> postponedChecks) {
        myKind = kind;
        myProblem = problem;
        myPostponedChecks = postponedChecks;
    }

    public static UnifiedCommitChecksResult passed(List<CommitCheck> postponedChecks) {
        return new UnifiedCommitChecksResult(Kind.PASSED, null, List.copyOf(postponedChecks));
    }

    public static UnifiedCommitChecksResult failed(@Nullable CommitProblem problem) {
        return new UnifiedCommitChecksResult(Kind.FAILED, problem, List.of());
    }

    public static UnifiedCommitChecksResult closeWindow() {
        return new UnifiedCommitChecksResult(Kind.CLOSE_WINDOW, null, List.of());
    }

    public static UnifiedCommitChecksResult cancelled() {
        return new UnifiedCommitChecksResult(Kind.CANCELLED, null, List.of());
    }

    public Kind getKind() {
        return myKind;
    }

    public @Nullable CommitProblem getProblem() {
        return myProblem;
    }

    public List<CommitCheck> getPostponedChecks() {
        return myPostponedChecks;
    }
}
