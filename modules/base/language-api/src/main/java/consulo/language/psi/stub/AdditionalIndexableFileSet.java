/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.psi.stub;

import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * @author peter
 */
public class AdditionalIndexableFileSet implements IndexableFileSet {
    private volatile @Nullable Set<VirtualFile> cachedFiles = null;
    private volatile @Nullable Set<VirtualFile> cachedDirectories = null;
    private final @Nullable List<IndexableSetContributor> myExtensions;
    private volatile long myContributorsModificationCount = -1;

    public AdditionalIndexableFileSet(@Nullable IndexableSetContributor extension) {
        myExtensions = extension == null ? null : List.of(extension);
    }

    private Set<VirtualFile> getDirectories() {
        Set<VirtualFile> directories = cachedDirectories;
        // all contributors: plugins loaded or unloaded at runtime change them
        boolean contributorsChanged = myExtensions == null
            && myContributorsModificationCount != Application.get().getExtensionPoint(IndexableSetContributor.class).getModificationCount();
        if (directories == null
            || contributorsChanged
            || filesInvalidated(directories)
            || filesInvalidated(Objects.requireNonNull(cachedFiles))) {
            directories = collectFilesAndDirectories();
        }
        return directories;
    }

    private Set<VirtualFile> collectFilesAndDirectories() {
        Set<VirtualFile> files = new HashSet<>();
        Set<VirtualFile> directories = new HashSet<>();
        List<IndexableSetContributor> extensionList = myExtensions;
        if (extensionList == null) {
            ExtensionPoint<IndexableSetContributor> point = Application.get().getExtensionPoint(IndexableSetContributor.class);
            myContributorsModificationCount = point.getModificationCount();
            extensionList = point.getExtensionList();
        }
        for (IndexableSetContributor provider : extensionList) {
            for (VirtualFile file : provider.getAdditionalRootsToIndex()) {
                (file.isDirectory() ? directories : files).add(file);
            }
        }
        cachedFiles = files;
        cachedDirectories = directories;
        return directories;
    }

    public static boolean filesInvalidated(Set<VirtualFile> files) {
        for (VirtualFile file : files) {
            if (!file.isValid()) {
                return true;
            }
        }
        return false;
    }

    public AdditionalIndexableFileSet() {
        this(null);
    }

    @Override
    public boolean isInSet(VirtualFile file) {
        for (VirtualFile root : getDirectories()) {
            if (VirtualFileUtil.isAncestor(root, file, false)) {
                return true;
            }
        }
        return Objects.requireNonNull(cachedFiles).contains(file);
    }
}
