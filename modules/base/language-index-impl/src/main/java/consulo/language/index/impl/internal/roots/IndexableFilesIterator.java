// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.content.ContentIterator;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFileFilter;

/**
 * Provides a named file set to be indexed for a single project structure entity (module, library, SDK, etc.)
 * Allows the indexing infrastructure to prioritize indexing by some predicate.
 */
public interface IndexableFilesIterator {
    /**
     * Presentable name that can be shown in logs and used for debugging purposes.
     */
    String getDebugName();

    /**
     * Presentable text shown in progress indicator during indexing of files of this provider.
     */
    LocalizeValue getIndexingProgressText();

    /**
     * Presentable text shown in progress indicator during traversing of files of this provider.
     */
    LocalizeValue getRootsScanningProgressText();

    /**
     * Represents origins (module, library, etc) of indexable file iterator.
     */
    IndexableSetOrigin getOrigin();

    /**
     * Iterates through all files and directories corresponding to this iterator.
     * <br />
     * The {@code fileFilter} is used to not process some files.
     * <br />
     * It is common to pass {@link IndexableFilesDeduplicateFilter} as the {@code fileFilter}
     * to avoid processing the same files twice. Several {@code IndexableFilesIterator}-s
     * may iterate the same roots (probably in different threads).
     *
     * @return {@code false} if the {@code fileIterator} has stopped the iteration by returning {@code false}, {@code true} otherwise.
     */
    boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter);
}
