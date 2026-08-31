// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.content.ContentIterator;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.ProjectIndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.ProjectFileOrDirOrigin;
import consulo.localize.LocalizeValue;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;

public class ProjectIndexableFilesIteratorImpl implements ProjectIndexableFilesIterator {
    private final VirtualFile myFileOrDir;

    public ProjectIndexableFilesIteratorImpl(VirtualFile fileOrDir) {
        myFileOrDir = fileOrDir;
    }

    @Override
    public String getDebugName() {
        return "Files under `" + myFileOrDir.getPath() + "`";
    }

    @Override
    public LocalizeValue getIndexingProgressText() {
        return IndexingLocalize.indexableFilesProviderIndexingFileordirName(myFileOrDir.getName());
    }

    @Override
    public LocalizeValue getRootsScanningProgressText() {
        return IndexingLocalize.indexableFilesProviderScanningFileordirName(myFileOrDir.getName());
    }

    @Override
    public ProjectFileOrDirOrigin getOrigin() {
        return new ProjectFileOrDirOriginImpl(myFileOrDir);
    }

    @Override
    public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
        return ProjectFileIndex.getInstance(project).iterateContentUnderDirectory(myFileOrDir, fileIterator, fileFilter);
    }
}
