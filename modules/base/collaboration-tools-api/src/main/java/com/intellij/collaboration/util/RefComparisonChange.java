// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import consulo.util.dataholder.Key;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.history.ShortVcsRevisionNumber;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Descriptor of a file change between two revisions
 */
@ApiStatus.Experimental
public final class RefComparisonChange {
    public static final Key<RefComparisonChange> KEY = Key.create("RefComparisonChange");

    private final @Nonnull ShortVcsRevisionNumber revisionNumberBefore;
    private final @Nullable FilePath filePathBefore;
    private final @Nonnull ShortVcsRevisionNumber revisionNumberAfter;
    private final @Nullable FilePath filePathAfter;

    public RefComparisonChange(
        @Nonnull ShortVcsRevisionNumber revisionNumberBefore,
        @Nullable FilePath filePathBefore,
        @Nonnull ShortVcsRevisionNumber revisionNumberAfter,
        @Nullable FilePath filePathAfter
    ) {
        this.revisionNumberBefore = revisionNumberBefore;
        this.filePathBefore = filePathBefore;
        this.revisionNumberAfter = revisionNumberAfter;
        this.filePathAfter = filePathAfter;
    }

    public @Nonnull ShortVcsRevisionNumber getRevisionNumberBefore() {
        return revisionNumberBefore;
    }

    public @Nullable FilePath getFilePathBefore() {
        return filePathBefore;
    }

    public @Nonnull ShortVcsRevisionNumber getRevisionNumberAfter() {
        return revisionNumberAfter;
    }

    public @Nullable FilePath getFilePathAfter() {
        return filePathAfter;
    }

    public static @Nonnull FileStatus fileStatus(@Nonnull RefComparisonChange change) {
        if (change.filePathBefore == null) {
            return FileStatus.ADDED;
        }
        if (change.filePathAfter == null) {
            return FileStatus.DELETED;
        }
        return FileStatus.MODIFIED;
    }

    public static @Nonnull FilePath filePath(@Nonnull RefComparisonChange change) {
        FilePath result = change.filePathAfter != null ? change.filePathAfter : change.filePathBefore;
        assert result != null;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RefComparisonChange that)) {
            return false;
        }
        return revisionNumberBefore.equals(that.revisionNumberBefore) &&
            Objects.equals(filePathBefore, that.filePathBefore) &&
            revisionNumberAfter.equals(that.revisionNumberAfter) &&
            Objects.equals(filePathAfter, that.filePathAfter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revisionNumberBefore, filePathBefore, revisionNumberAfter, filePathAfter);
    }

    @Override
    public String toString() {
        return "RefComparisonChange(" +
            "revisionNumberBefore=" + revisionNumberBefore +
            ", filePathBefore=" + filePathBefore +
            ", revisionNumberAfter=" + revisionNumberAfter +
            ", filePathAfter=" + filePathAfter +
            ")";
    }
}
