// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.indexing;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.index.io.IndexId;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * Allows to exclude files from indexing, on a per-index basis.
 *
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface GlobalIndexFilter {
    /**
     * Returns true if the given file should be excluded from indexing by the given index.
     */
    boolean isExcludedFromIndex(@Nonnull VirtualFile virtualFile, @Nonnull IndexId<?, ?> indexId);

    int getVersion();

    boolean affectsIndex(@Nonnull IndexId<?, ?> indexId);

    ExtensionPointName<GlobalIndexFilter> EP_NAME = ExtensionPointName.create(GlobalIndexFilter.class);

    /**
     * Returns true if the given file should be excluded from indexing by any of the registered filters.
     */
    static boolean isExcludedFromIndexViaFilters(@Nonnull VirtualFile file, @Nonnull IndexId<?, ?> indexId) {
        for (GlobalIndexFilter filter : EP_NAME.getExtensionList()) {
            if (filter.isExcludedFromIndex(file, indexId)) {
                return true;
            }
        }
        return false;
    }

    static int getFiltersVersion(@Nonnull IndexId<?, ?> indexId) {
        int result = 0;
        for (GlobalIndexFilter extension : EP_NAME.getExtensionList()) {
            if (extension.affectsIndex(indexId)) {
                result += extension.getVersion();
            }
        }
        return result;
    }
}
