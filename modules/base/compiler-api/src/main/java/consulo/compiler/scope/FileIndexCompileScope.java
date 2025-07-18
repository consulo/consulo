/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler.scope;

import consulo.compiler.util.ExportableUserDataHolderBase;
import consulo.content.FileIndex;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 * @since 2003-12-18
 */
public abstract class FileIndexCompileScope extends ExportableUserDataHolderBase implements CompileScope {
    protected abstract FileIndex[] getFileIndices();

    @Override
    @Nonnull
    public VirtualFile[] getFiles(final FileType fileType) {
        final List<VirtualFile> files = new ArrayList<>();
        final FileIndex[] fileIndices = getFileIndices();
        for (final FileIndex fileIndex : fileIndices) {
            fileIndex.iterateContent(new CompilerContentIterator(fileType, fileIndex, !includeTestScope(), files));
        }
        return VirtualFileUtil.toVirtualFileArray(files);
    }
}
