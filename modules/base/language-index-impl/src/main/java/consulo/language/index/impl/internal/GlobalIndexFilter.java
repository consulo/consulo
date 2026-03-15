// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.index.io.IndexId;
import consulo.virtualFileSystem.VirtualFile;

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
    boolean isExcludedFromIndex(VirtualFile virtualFile, IndexId<?, ?> indexId);

    int getVersion();

    boolean affectsIndex(IndexId<?, ?> indexId);

    ExtensionPointName<GlobalIndexFilter> EP_NAME = ExtensionPointName.create(GlobalIndexFilter.class);

    /**
     * Returns true if the given file should be excluded from indexing by any of the registered filters.
     */
    static boolean isExcludedFromIndexViaFilters(VirtualFile file, IndexId<?, ?> indexId) {
        for (GlobalIndexFilter filter : EP_NAME.getExtensionList()) {
            if (filter.isExcludedFromIndex(file, indexId)) {
                return true;
            }
        }
        return false;
    }

    static int getFiltersVersion(IndexId<?, ?> indexId) {
        int result = 0;
        for (GlobalIndexFilter extension : EP_NAME.getExtensionList()) {
            if (extension.affectsIndex(indexId)) {
                result += extension.getVersion();
            }
        }
        return result;
    }
}
