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
package consulo.ide.impl.idea.util.indexing;

import consulo.content.ContentIterator;
import consulo.language.psi.stub.IndexableFileSet;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-08-08
 */
public class FileBaseIndexSet implements IndexableFileSet {
    private FileBasedIndexScanRunnableCollector myCollector;

    public FileBaseIndexSet(FileBasedIndexScanRunnableCollector collector) {
        myCollector = collector;
    }

    @Override
    public boolean isInSet(@Nonnull final VirtualFile file) {
        return myCollector.shouldCollect(file);
    }

    @Override
    public void iterateIndexableFilesIn(@Nonnull final VirtualFile file, @Nonnull final ContentIterator iterator) {
        VirtualFileUtil.visitChildrenRecursively(file, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@Nonnull VirtualFile file) {

                if (!isInSet(file)) return false;
                iterator.processFile(file);

                return true;
            }
        });
    }
}
