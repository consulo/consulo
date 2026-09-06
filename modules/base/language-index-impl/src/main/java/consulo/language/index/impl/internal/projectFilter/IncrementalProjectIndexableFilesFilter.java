// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.projectFilter;

import consulo.project.Project;

import java.util.function.BooleanSupplier;

class IncrementalProjectIndexableFilesFilterFactory extends ProjectIndexableFilesFilterFactory {
    @Override
    ProjectIndexableFilesFilter create(Project project, long currentVfsCreationTimestamp) {
        return new IncrementalProjectIndexableFilesFilter();
    }
}

class IncrementalProjectIndexableFilesFilter extends ProjectIndexableFilesFilter {
    protected final ConcurrentFileIds myFileIds;

    IncrementalProjectIndexableFilesFilter() {
        this(new ConcurrentFileIds());
    }

    IncrementalProjectIndexableFilesFilter(ConcurrentFileIds fileIds) {
        myFileIds = fileIds;
    }

    @Override
    public boolean containsFileId(int fileId) {
        return myFileIds.get(fileId);
    }

    @Override
    boolean ensureFileIdPresent(int fileId, BooleanSupplier add) {
        assert fileId > 0;

        return runUpdate(() -> {
            ConcurrentFileIds fileIds = myFileIds;
            if (fileIds.get(fileId)) {
                return true;
            }
            else if (add.getAsBoolean()) {
                fileIds.set(fileId, true);
                return true;
            }
            else {
                return false;
            }
        });
    }

    @Override
    void removeFileId(int fileId) {
        assert fileId > 0;
        runUpdate(() -> {
            myFileIds.set(fileId, false);
            return null;
        });
    }

    @Override
    void resetFileIds() {
        myFileIds.clear();
    }
}
