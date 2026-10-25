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
package consulo.sandboxPlugin.ide.review;

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.review.util.ComputedResult;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.RefComparisonChange;
import consulo.versionControlSystem.review.details.model.CodeReviewChangeDetails;
import consulo.versionControlSystem.review.details.model.CodeReviewChangesContainer;
import consulo.versionControlSystem.review.details.model.CodeReviewChangesViewModel;
import consulo.versionControlSystem.review.details.model.CodeReviewChangesViewModelDelegate;
import consulo.versionControlSystem.util.VcsUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewChangesViewModel implements CodeReviewChangesViewModel<SandReviewCommit> {
    private static final String BASE_SHA = "6f2a1c0e9b7d4a31";
    private static final String CLEANUP_SHA = "c3d4e5f60718293a";

    private final List<SandReviewCommit> myCommits;
    private final MutableObservableValue<List<SandReviewCommit>> myReviewCommits;
    private final CodeReviewChangesViewModelDelegate<SandReviewChangeListViewModel> myDelegate;
    private final ObservableValue<@Nullable SandReviewCommit> mySelectedCommit;
    private final ObservableValue<Integer> mySelectedCommitIndex;

    public SandReviewChangesViewModel(Project project, Disposable parentDisposable, String reviewNumber, IntSupplier demoDiscussions) {
        long now = System.currentTimeMillis();
        SandReviewCommit addNotes = new SandReviewCommit("a1b2c3d4e5f60718", "Add the notes file",
            "The notes collect what the demo should show.", "Valery Semenchuk", new Date(now - 3 * 3600_000L));
        SandReviewCommit explain = new SandReviewCommit("b2c3d4e5f6071829", "Explain review threads in the demo",
            "Describes how threads anchor to lines and how ranges are commented.", "Valery Semenchuk", new Date(now - 2 * 3600_000L));
        SandReviewCommit cleanup = new SandReviewCommit(CLEANUP_SHA, "Remove the obsolete file",
            "<b>Obsolete.txt</b> is not used by the demo anymore.", "Sand Reviewer", new Date(now - 3600_000L));
        myCommits = List.of(addNotes, explain, cleanup);
        myReviewCommits = MutableObservableValue.of(myCommits);

        String basePath = project.getBasePath();
        FilePath notes = VcsUtil.getFilePath(basePath + "/a.txt", false);
        FilePath demo = VcsUtil.getFilePath(basePath + "/" + SandReviewService.DEMO_FILE_NAME, false);
        FilePath obsolete = VcsUtil.getFilePath(basePath + "/Obsolete.txt", false);

        Map<String, List<RefComparisonChange>> changesByCommits = new LinkedHashMap<>();
        changesByCommits.put(addNotes.sha(), List.of(change(BASE_SHA, null, addNotes.sha(), notes)));
        changesByCommits.put(explain.sha(), List.of(change(addNotes.sha(), demo, explain.sha(), demo)));
        changesByCommits.put(cleanup.sha(), List.of(change(explain.sha(), obsolete, cleanup.sha(), null), change(explain.sha(), demo, cleanup.sha(), demo)));

        List<RefComparisonChange> summary = summaryChanges(project);

        List<String> commitShas = new ArrayList<>();
        for (SandReviewCommit commit : myCommits) {
            commitShas.add(commit.sha());
        }
        CodeReviewChangesContainer container = new CodeReviewChangesContainer(summary, commitShas, changesByCommits);

        myDelegate = CodeReviewChangesViewModelDelegate.create(parentDisposable,
            ObservableValue.of(ComputedResult.success(container)),
            (cs, changes, changeList) -> {
                Map<RefComparisonChange, CodeReviewChangeDetails> details = new HashMap<>();
                for (RefComparisonChange change : changeList.changes()) {
                    boolean isDemo = demo.equals(change.filePathAfter());
                    details.put(change, new CodeReviewChangeDetails(!isDemo && change.filePathBefore() == null, isDemo ? demoDiscussions.getAsInt() : 0));
                }
                return new SandReviewChangeListViewModel(project, cs, reviewNumber, changeList, details);
            });

        mySelectedCommit = myDelegate.getSelectedCommit().<@Nullable SandReviewCommit>map(sha -> {
            for (SandReviewCommit commit : myCommits) {
                if (commit.sha().equals(sha)) {
                    return commit;
                }
            }
            return null;
        });
        mySelectedCommitIndex = mySelectedCommit.map(commit -> commit == null ? -1 : myCommits.indexOf(commit));
    }

    public static List<RefComparisonChange> summaryChanges(Project project) {
        String basePath = project.getBasePath();
        FilePath notes = VcsUtil.getFilePath(basePath + "/a.txt", false);
        FilePath demo = VcsUtil.getFilePath(basePath + "/" + SandReviewService.DEMO_FILE_NAME, false);
        FilePath obsolete = VcsUtil.getFilePath(basePath + "/Obsolete.txt", false);
        return List.of(
            change(BASE_SHA, null, CLEANUP_SHA, notes),
            change(BASE_SHA, demo, CLEANUP_SHA, demo),
            change(BASE_SHA, obsolete, CLEANUP_SHA, null)
        );
    }

    private static RefComparisonChange change(String beforeSha, @Nullable FilePath before, String afterSha, @Nullable FilePath after) {
        return new RefComparisonChange(new SandRevisionNumber(beforeSha), before, new SandRevisionNumber(afterSha), after);
    }

    public ObservableValue<ComputedResult<SandReviewChangeListViewModel>> getChangeListVm() {
        return myDelegate.getChangeListVm();
    }

    @Override
    public ObservableValue<List<SandReviewCommit>> getReviewCommits() {
        return myReviewCommits;
    }

    @Override
    public ObservableValue<@Nullable SandReviewCommit> getSelectedCommit() {
        return mySelectedCommit;
    }

    @Override
    public ObservableValue<Integer> getSelectedCommitIndex() {
        return mySelectedCommitIndex;
    }

    @Override
    public void selectCommit(int index) {
        myDelegate.selectCommit(index);
    }

    @Override
    public void selectNextCommit() {
        myDelegate.selectNextCommit();
    }

    @Override
    public void selectPreviousCommit() {
        myDelegate.selectPreviousCommit();
    }

    @Override
    public String commitHash(SandReviewCommit commit) {
        return commit.sha().substring(0, 8);
    }
}
