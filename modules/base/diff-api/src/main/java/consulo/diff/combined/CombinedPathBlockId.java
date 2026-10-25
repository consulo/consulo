// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.combined;

import consulo.diff.DiffFilePath;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class CombinedPathBlockId implements CombinedBlockId {
    private final DiffFilePath myPath;
    private final @Nullable FileStatus myFileStatus;
    private final @Nullable Object myTag;

    public CombinedPathBlockId(DiffFilePath path, @Nullable FileStatus fileStatus) {
        this(path, fileStatus, null);
    }

    public CombinedPathBlockId(DiffFilePath path, @Nullable FileStatus fileStatus, @Nullable Object tag) {
        myPath = path;
        myFileStatus = fileStatus;
        myTag = tag;
    }

    public DiffFilePath getPath() {
        return myPath;
    }

    public @Nullable FileStatus getFileStatus() {
        return myFileStatus;
    }

    public @Nullable Object getTag() {
        return myTag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CombinedPathBlockId that)) {
            return false;
        }
        return myPath.equals(that.myPath) && Objects.equals(myFileStatus, that.myFileStatus) && Objects.equals(myTag, that.myTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myPath, myFileStatus, myTag);
    }

    @Override
    public String toString() {
        return "CombinedPathBlockId(path=" + myPath + ", fileStatus=" + myFileStatus + ", tag=" + myTag + ")";
    }
}
