// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.stub;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.index.io.DataIndexer;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

/**
 * @author traff
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface IdIndexer extends DataIndexer<IdIndexEntry, Integer, FileContent> {
    ExtensionPointCacheKey<IdIndexer, Map<FileType, IdIndexer>> KEY = ExtensionPointCacheKey.groupBy("IdIndexer", IdIndexer::getFileType);

    @Nullable
    static IdIndexer forFileType(FileType fileType) {
        ExtensionPoint<IdIndexer> extensionPoint = Application.get().getExtensionPoint(IdIndexer.class);
        Map<FileType, IdIndexer> map = extensionPoint.getOrBuildCache(KEY);
        return map.get(fileType);
    }

    @Nonnull
    FileType getFileType();

    default int getVersion() {
        return 1;
    }
}
