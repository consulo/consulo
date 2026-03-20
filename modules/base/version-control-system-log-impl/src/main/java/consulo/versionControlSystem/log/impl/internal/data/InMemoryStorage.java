/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.application.util.BiDirectionalEnumerator;
import consulo.util.collection.HashingStrategy;
import consulo.versionControlSystem.log.CommitId;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsLogStorage;
import consulo.versionControlSystem.log.VcsRef;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

public class InMemoryStorage implements VcsLogStorage {
    private final BiDirectionalEnumerator<CommitId> myCommitIdEnumerator = new BiDirectionalEnumerator<>(1, HashingStrategy.canonical());
    private final BiDirectionalEnumerator<VcsRef> myRefsEnumerator = new BiDirectionalEnumerator<>(1, HashingStrategy.canonical());

    @Override
    public int getCommitIndex(Hash hash, VirtualFile root) {
        return getOrPut(hash, root);
    }

    private int getOrPut(Hash hash, VirtualFile root) {
        return myCommitIdEnumerator.enumerate(new CommitId(hash, root));
    }

    
    @Override
    public CommitId getCommitId(int commitIndex) {
        return myCommitIdEnumerator.getValue(commitIndex);
    }

    @Override
    public @Nullable CommitId findCommitId(Predicate<CommitId> condition) {
        CommitId[] result = new CommitId[]{null};
        myCommitIdEnumerator.forEachValue(commitId -> {
            if (condition.test(commitId)) {
                result[0] = commitId;
            }
        });
        return result[0];
    }

    @Override
    public int getRefIndex(VcsRef ref) {
        return myRefsEnumerator.enumerate(ref);
    }

    @Override
    public @Nullable VcsRef getVcsRef(int refIndex) {
        return myRefsEnumerator.getValue(refIndex);
    }

    @Override
    public void flush() {
        // nothing to flush
    }
}
