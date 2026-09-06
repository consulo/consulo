// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.projectFilter;

import consulo.util.collection.ConcurrentBitSet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ConcurrentFileIds {
    private final ConcurrentBitSet myFileIds;

    public ConcurrentFileIds() {
        this(new ConcurrentBitSet());
    }

    public ConcurrentFileIds(ConcurrentBitSet fileIds) {
        myFileIds = fileIds;
    }

    public int getCardinality() {
        return myFileIds.cardinality();
    }

    public int getSize() {
        return myFileIds.size();
    }

    public boolean isEmpty() {
        return myFileIds.cardinality() == 0;
    }

    public boolean get(int fileId) {
        return myFileIds.get(fileId);
    }

    public void set(int fileId, boolean v) {
        myFileIds.set(fileId, v);
    }

    public void clear() {
        myFileIds.clear();
    }

    public void writeTo(DataOutput outputStream) throws IOException {
        myFileIds.writeTo(outputStream);
    }

    public static ConcurrentFileIds readFrom(DataInput stream) throws IOException {
        return new ConcurrentFileIds(ConcurrentBitSet.readFrom(stream));
    }
}
