// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import com.sun.jna.Pointer;

import java.awt.image.DataBuffer;

final class DirectDataBufferInt extends DataBuffer {
    Pointer myMemory;
    private final int myOffset;

    DirectDataBufferInt(Pointer memory, int memLength, int offset) {
        super(TYPE_INT, memLength);
        this.myMemory = memory;
        this.myOffset = offset;
    }

    @Override
    public int getElem(int bank, int i) {
        return myMemory.getInt(myOffset + i * 4L); // same as: *((jint *)((char *)Pointer + offset))
    }

    @Override
    public void setElem(int bank, int i, int val) {
        myMemory.setInt(myOffset + i * 4L, val); // same as: *((jint *)((char *)Pointer + offset)) = value
    }
}

