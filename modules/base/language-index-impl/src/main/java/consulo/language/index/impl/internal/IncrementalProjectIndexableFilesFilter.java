// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
package consulo.language.index.impl.internal;

import consulo.language.psi.stub.IdFilter;
import consulo.util.collection.ConcurrentBitSet;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

class IncrementalProjectIndexableFilesFilter extends IdFilter {
    private volatile ConcurrentBitSet myFileIds = new ConcurrentBitSet();
    private volatile @Nullable ConcurrentBitSet myPreviousFileIds;

    @Override
    public boolean containsFileId(int fileId) {
        return myFileIds.get(fileId);
    }

    boolean ensureFileIdPresent(int fileId, BooleanSupplier add) {
        assert fileId > 0;

        ConcurrentBitSet fileIds = myFileIds;
        if (fileIds.get(fileId)) {
            return false;
        }

        if (add.getAsBoolean()) {
            fileIds.set(fileId);
            ConcurrentBitSet previousFileIds = myPreviousFileIds;
            return previousFileIds == null || !previousFileIds.get(fileId);
        }
        return false;
    }

    void removeFileId(int fileId) {
        assert fileId > 0;
        myFileIds.clear(fileId);
    }

    void memoizeAndResetFileIds() {
        // called in sequential UnindexedFilesScanner tasks
        myPreviousFileIds = myFileIds;
        myFileIds = new ConcurrentBitSet();
    }

    void resetPreviousFileIds() {
        // called in sequential UnindexedFilesScanner tasks
        myPreviousFileIds = null;
    }
}
