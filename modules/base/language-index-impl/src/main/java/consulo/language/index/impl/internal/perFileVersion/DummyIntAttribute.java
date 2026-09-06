// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.perFileVersion;

final class DummyIntAttribute implements IntFileAttribute {
    static final DummyIntAttribute INSTANCE = new DummyIntAttribute();

    private DummyIntAttribute() {
    }

    @Override
    public int readInt(int fileId) {
        return 0;
    }

    @Override
    public void writeInt(int fileId, int value) {
    }

    @Override
    public void close() {
    }

    @Override
    public void closeAndUnsafelyUnmap() {
    }
}
