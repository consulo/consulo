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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ui.VcsFileList;
import consulo.versionControlSystem.ui.VcsFileListBuilder;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedVcsFileListBuilder<T> implements VcsFileListBuilder<T> {
    private final Project myProject;
    private final List<T> myItems;
    private final Function<T, FilePath> myToPath;
    private final Predicate<T> myValid;

    private boolean mySelectable;
    private boolean myToolbar;
    private @Nullable Runnable myInclusionListener;

    public UnifiedVcsFileListBuilder(Project project, List<T> items, Function<T, FilePath> toPath, Predicate<T> valid) {
        myProject = project;
        myItems = List.copyOf(items);
        myToPath = toPath;
        myValid = valid;
    }

    @Override
    public VcsFileListBuilder<T> selectable() {
        mySelectable = true;
        return this;
    }

    @Override
    public VcsFileListBuilder<T> toolbar() {
        myToolbar = true;
        return this;
    }

    @Override
    public VcsFileListBuilder<T> inclusionListener(Runnable listener) {
        myInclusionListener = listener;
        return this;
    }

    @RequiredUIAccess
    @Override
    public VcsFileList<T> build(Disposable parentDisposable) {
        return new UnifiedVcsFileList<>(
            myProject,
            myItems,
            myToPath,
            myValid,
            mySelectable,
            myToolbar,
            null,
            myInclusionListener,
            parentDisposable
        );
    }
}
