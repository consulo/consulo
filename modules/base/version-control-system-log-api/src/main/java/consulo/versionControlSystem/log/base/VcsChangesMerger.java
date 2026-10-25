// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.log.base;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public abstract class VcsChangesMerger extends VcsStatusMerger<Change, FilePath> {
    @Override
    protected Change createStatus(Change.Type type, FilePath path, @Nullable FilePath secondPath) {
        return switch (type) {
            case NEW -> createChange(type, null, path);
            case DELETED -> createChange(type, path, null);
            case MOVED -> createChange(type, path, secondPath);
            case MODIFICATION -> createChange(type, path, path);
        };
    }

    protected abstract Change createChange(Change.Type type, @Nullable FilePath beforePath, @Nullable FilePath afterPath);

    public MergedChange mergedChange(FilePath path, List<Change> changesToParents) {
        return new SimpleMergedChange(merge(path, changesToParents), changesToParents);
    }

    @Override
    protected FilePath getFirstPath(Change info) {
        return switch (info.getType()) {
            case MODIFICATION, NEW -> Objects.requireNonNull(info.getAfterRevision()).getFile();
            case DELETED, MOVED -> Objects.requireNonNull(info.getBeforeRevision()).getFile();
        };
    }

    @Override
    protected @Nullable FilePath getSecondPath(Change info) {
        return info.getType() == Change.Type.MOVED ? Objects.requireNonNull(info.getAfterRevision()).getFile() : null;
    }

    @Override
    protected Change.Type getType(Change info) {
        return info.getType();
    }
}
