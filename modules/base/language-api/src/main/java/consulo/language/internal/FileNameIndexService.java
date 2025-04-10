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
package consulo.language.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.language.psi.stub.IdFilter;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

@ServiceAPI(ComponentScope.APPLICATION)
public interface FileNameIndexService {
    @Nonnull
    Collection<VirtualFile> getVirtualFilesByName(
        Project project,
        @Nonnull String name,
        @Nonnull SearchScope scope,
        @Nullable IdFilter idFilter
    );

    void processAllFileNames(@Nonnull Processor<? super String> processor, @Nonnull SearchScope scope, @Nullable IdFilter filter);

    @Nonnull
    Collection<VirtualFile> getFilesWithFileType(@Nonnull FileType type, @Nonnull SearchScope scope);

    boolean processFilesWithFileType(@Nonnull FileType type, @Nonnull Processor<? super VirtualFile> processor, @Nonnull SearchScope scope);
}
