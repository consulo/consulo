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

import consulo.disposer.Disposable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.change.RefComparisonChange;
import consulo.versionControlSystem.review.util.ChangesSelection;
import consulo.versionControlSystem.review.util.RefComparisonChangeUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public interface CodeReviewChangeListViewModel {
    Key<CodeReviewChangeListViewModel> DATA_KEY = Key.create("Code.Review.Changes.List.ViewModel");

    Project getProject();

    List<RefComparisonChange> getChanges();

    /**
     * Flow of selection requests to be handled
     */
    void addSelectionRequestListener(Disposable parentDisposable, Consumer<CodeReviewChangeListSelectionRequest> listener);

    /**
     * Uni-directional state of changelist selection (changelist presentation should not collect it)
     */
    ObservableValue<@Nullable ChangesSelection> getChangesSelection();

    /**
     * Publish changelist selection to {@link #getChangesSelection()}
     */
    void updateSelectedChanges(@Nullable ChangesSelection selection);

    /**
     * Show diff preview for {@link #getChangesSelection()}
     */
    void showDiffPreview();

    /**
     * Request standalone diff for {@link #getChangesSelection()}
     */
    void showDiff();

    default boolean canEdit(RefComparisonChange change) {
        return RefComparisonChangeUtil.getFilePath(change).getVirtualFile() != null;
    }

    @RequiredUIAccess
    default void startEditing(RefComparisonChange change) {
        VirtualFile file = RefComparisonChangeUtil.getFilePath(change).getVirtualFile();
        if (file == null) {
            return;
        }
        OpenFileDescriptorFactory.getInstance(getProject()).builder(file).build().navigate(true);
    }
}
