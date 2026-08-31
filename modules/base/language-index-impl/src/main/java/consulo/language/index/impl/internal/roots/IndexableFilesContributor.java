// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.util.List;
import java.util.function.Predicate;

/**
 * A base interface to provide a files which should be indexed for a given project.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface IndexableFilesContributor {
    /**
     * Returns ordered list of logical file sets (module files, SDK files, etc) to be indexed. Note:
     * <ul>
     * <li>The method is called in read-action with valid {@code project}.</li>
     * <li>{@link IndexableFilesIterator}-s will be indexed in provided order.</li>
     * <li>Files in {@link IndexableFilesIterator} should be not evaluated eagerly for performance reasons.</li>
     * </ul>
     */
    List<IndexableFilesIterator> getIndexableFiles(Project project);

    /**
     * Quickly should answer does file belongs to files contributor.
     * Used to filter out file events which is required to update indexes.
     */
    Predicate<VirtualFile> getOwnFilePredicate(Project project);
}
