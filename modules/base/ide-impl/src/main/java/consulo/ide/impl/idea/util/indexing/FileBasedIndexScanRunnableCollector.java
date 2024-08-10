/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.indexing;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.progress.ProgressIndicator;
import consulo.content.ContentIterator;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.List;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class FileBasedIndexScanRunnableCollector {
    public static FileBasedIndexScanRunnableCollector getInstance(@Nonnull Project project) {
        return project.getInstance(FileBasedIndexScanRunnableCollector.class);
    }

    // Returns true if file should be indexed
    public abstract boolean shouldCollect(@Nonnull final VirtualFile file);

    // Collect all roots for indexing
    public abstract List<Runnable> collectScanRootRunnables(@Nonnull final ContentIterator processor, final ProgressIndicator indicator);
}
