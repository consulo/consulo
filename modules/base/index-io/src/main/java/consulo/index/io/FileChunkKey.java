// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io;

import org.jspecify.annotations.Nullable;

final class FileChunkKey<OwnerType> implements Comparable<FileChunkKey<OwnerType>> {
    private final OwnerType myOwner;
    private final long myOffset;

    FileChunkKey(OwnerType owner, long offset) {
        myOwner = owner;
        myOffset = offset;
    }

    @Override
    public int hashCode() {
        return (int) (myOwner.hashCode() * 31 + myOffset);
    }

    public OwnerType getOwner() {
        return myOwner;
    }

    public long getOffset() {
        return myOffset;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(@Nullable Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FileChunkKey<OwnerType> k = (FileChunkKey<OwnerType>) obj;
        return k.myOwner == myOwner && k.myOffset == myOffset;
    }

    @Override
    public int compareTo(FileChunkKey<OwnerType> o) {
        if (myOwner != o.myOwner) {
            return myOwner.hashCode() - o.myOwner.hashCode();
        }
        return myOffset == o.myOffset ? 0 : myOffset - o.myOffset < 0 ? -1 : 1;
    }
}
