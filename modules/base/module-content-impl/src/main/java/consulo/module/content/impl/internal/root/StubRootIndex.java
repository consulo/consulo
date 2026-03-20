/*
 * Copyright 2013-2024 consulo.io
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
package consulo.module.content.impl.internal.root;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.query.EmptyQuery;
import consulo.application.util.query.Query;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.content.DirectoryInfo;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2024-12-09
 */
public class StubRootIndex implements RootIndex {
    public static final StubRootIndex INSTANCE = new StubRootIndex();

    
    @Override
    public Query<VirtualFile> getDirectoriesByPackageName(String packageName, boolean includeLibrarySources) {
        return EmptyQuery.getEmptyQuery();
    }

    
    @Override
    public DirectoryInfo getInfoForFile(VirtualFile file) {
        return NonProjectDirectoryInfo.INVALID;
    }

    @Override
    public @Nullable ContentFolderTypeProvider getContentFolderType(DirectoryInfo directoryInfo) {
        return null;
    }

    @Override
    public @Nullable String getPackageName(VirtualFile dir) {
        return null;
    }

    @RequiredReadAction
    
    @Override
    public OrderEntry[] getOrderEntries(DirectoryInfo info) {
        return OrderEntry.EMPTY_ARRAY;
    }
}
