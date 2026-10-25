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

import consulo.diff.DiffContentFactory;
import consulo.diff.content.DiffContent;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.project.Project;
import consulo.review.util.ComputedResult;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.PresentableChange;
import consulo.versionControlSystem.change.RefComparisonChange;
import consulo.versionControlSystem.review.diff.model.AsyncDiffViewModel;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public class SandReviewDiffChangeViewModel implements AsyncDiffViewModel, PresentableChange {
    private static final String OBSOLETE_TEXT = "This file is not used by the demo anymore.\n";

    private final Project myProject;
    private final ScheduledExecutorService myScheduler;
    private final RefComparisonChange myChange;
    private final MutableObservableValue<@Nullable ComputedResult<DiffRequest>> myRequest = MutableObservableValue.of(null);

    public SandReviewDiffChangeViewModel(Project project, ScheduledExecutorService scheduler, RefComparisonChange change) {
        myProject = project;
        myScheduler = scheduler;
        myChange = change;
        reloadRequest();
    }

    public RefComparisonChange getChange() {
        return myChange;
    }

    @Override
    public ObservableValue<@Nullable ComputedResult<DiffRequest>> getRequest() {
        return myRequest;
    }

    @Override
    public void reloadRequest() {
        myRequest.set(ComputedResult.loading());
        myScheduler.schedule(() -> myRequest.set(ComputedResult.compute(this::createRequest)), 400, TimeUnit.MILLISECONDS);
    }

    private DiffRequest createRequest() {
        FilePath before = myChange.filePathBefore();
        FilePath after = myChange.filePathAfter();
        String afterText = after == null ? null : readText(after);
        String beforeText;
        if (before == null) {
            beforeText = null;
        }
        else if (SandReviewService.DEMO_FILE_NAME.equals(before.getName()) && afterText != null) {
            beforeText = String.join("\n", SandReviewEditorModel.baseLines(afterText));
        }
        else {
            beforeText = OBSOLETE_TEXT;
        }

        FilePath path = getFilePath();
        DiffRequest request = new SimpleDiffRequest(path.getName(), content(beforeText, path), content(afterText, path),
            myChange.revisionNumberBefore().toShortString(), myChange.revisionNumberAfter().toShortString());
        request.putUserData(RefComparisonChange.KEY, myChange);
        return request;
    }

    private DiffContent content(@Nullable String text, FilePath path) {
        DiffContentFactory factory = DiffContentFactory.getInstance();
        return text == null ? factory.createEmpty() : factory.create(myProject, text, path.getFileType());
    }

    private static @Nullable String readText(FilePath path) {
        try {
            return Files.readString(Path.of(path.getPath()), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            return null;
        }
    }

    @Override
    public FilePath getFilePath() {
        return Objects.requireNonNull(myChange.filePathAfter() != null ? myChange.filePathAfter() : myChange.filePathBefore());
    }

    @Override
    public FileStatus getFileStatus() {
        if (myChange.filePathBefore() == null) {
            return FileStatus.ADDED;
        }
        if (myChange.filePathAfter() == null) {
            return FileStatus.DELETED;
        }
        return FileStatus.MODIFIED;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SandReviewDiffChangeViewModel that && myChange.equals(that.myChange);
    }

    @Override
    public int hashCode() {
        return myChange.hashCode();
    }
}
