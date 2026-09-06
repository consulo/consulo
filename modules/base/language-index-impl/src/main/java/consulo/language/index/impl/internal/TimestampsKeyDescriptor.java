// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.index.io.KeyDescriptor;
import consulo.index.io.PersistentEnumerator;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/** Descriptor for {@link PersistentEnumerator} */
public class TimestampsKeyDescriptor implements KeyDescriptor<TimestampsImmutable> {
    @Override
    public boolean equals(TimestampsImmutable val1, TimestampsImmutable val2) {
        return val1.equals(val2);
    }

    @Override
    public int hashCode(TimestampsImmutable value) {
        return value.hashCode();
    }

    @Override
    public void save(DataOutput out, TimestampsImmutable value) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(64);
        try (DataOutputStream stream = new DataOutputStream(outStream)) {
            value.writeToStream(stream);
        }
        out.writeInt(outStream.size());
        out.write(outStream.toByteArray());
    }

    @Override
    public TimestampsImmutable read(DataInput dataIn) throws IOException {
        int size = dataIn.readInt();
        byte[] data = new byte[size];
        dataIn.readFully(data);
        return TimestampsImmutable.readTimestamps(ByteBuffer.wrap(data));
    }
}
