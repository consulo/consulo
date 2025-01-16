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
import consulo.application.util.query.Query;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.content.DirectoryInfo;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-12-09
 */
public interface RootIndex {
    @Nonnull
    Query<VirtualFile> getDirectoriesByPackageName(@Nonnull final String packageName, final boolean includeLibrarySources);

    @Nonnull
    DirectoryInfo getInfoForFile(@Nonnull VirtualFile file);

    @Nullable
    ContentFolderTypeProvider getContentFolderType(@Nonnull DirectoryInfo directoryInfo);

    @Nullable
    String getPackageName(@Nonnull final VirtualFile dir);

    @Nonnull
    @RequiredReadAction
    OrderEntry[] getOrderEntries(@Nonnull DirectoryInfo info);
}
