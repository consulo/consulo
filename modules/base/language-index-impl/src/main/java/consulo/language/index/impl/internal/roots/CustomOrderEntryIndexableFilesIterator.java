/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.index.impl.internal.roots;

import consulo.application.ReadAction;
import consulo.content.ContentIterator;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Iterates roots of Consulo custom order entries (module-bound entries with own roots, e.g. provided by plugins).
 */
public class CustomOrderEntryIndexableFilesIterator implements IndexableFilesIterator {
    private final OrderEntry myOrderEntry;

    CustomOrderEntryIndexableFilesIterator(OrderEntry orderEntry) {
        myOrderEntry = orderEntry;
    }

    @Override
    public String getDebugName() {
        return "Order entry " + myOrderEntry.getPresentableName();
    }

    @Override
    public LocalizeValue getIndexingProgressText() {
        return IndexingLocalize.indexableFilesProviderIndexingLibraryName(myOrderEntry.getPresentableName());
    }

    @Override
    public LocalizeValue getRootsScanningProgressText() {
        return IndexingLocalize.indexableFilesProviderScanningLibraryName(myOrderEntry.getPresentableName());
    }

    @Override
    public IndexableSetOrigin getOrigin() {
        return new OrderEntryOriginImpl(myOrderEntry);
    }

    @Override
    public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
        List<VirtualFile> roots = ReadAction.compute(() -> {
            if (!myOrderEntry.isValid()) {
                return List.of();
            }
            List<VirtualFile> files = new ArrayList<>();
            files.addAll(List.of(myOrderEntry.getFiles(SourcesOrderRootType.ID)));
            files.addAll(List.of(myOrderEntry.getFiles(BinariesOrderRootType.ID)));
            return files;
        });
        return IndexableFilesIterationMethods.iterateRoots(project, roots, fileIterator, fileFilter);
    }
}
