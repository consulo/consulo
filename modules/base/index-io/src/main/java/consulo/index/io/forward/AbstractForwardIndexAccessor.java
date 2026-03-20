// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.index.io.forward;

import consulo.index.io.InputData;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataOutputStream;
import consulo.util.io.BufferExposingByteArrayOutputStream;
import consulo.util.io.ByteArraySequence;
import consulo.util.io.ThreadLocalCachedByteArray;
import consulo.util.io.UnsyncByteArrayInputStream;
import org.jspecify.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;

public abstract class AbstractForwardIndexAccessor<Key, Value, DataType> implements ForwardIndexAccessor<Key, Value> {
  
  private final DataExternalizer<DataType> myDataTypeExternalizer;

  public AbstractForwardIndexAccessor(DataExternalizer<DataType> externalizer) {
    myDataTypeExternalizer = externalizer;
  }

  protected abstract InputDataDiffBuilder<Key, Value> createDiffBuilder(int inputId, @Nullable DataType inputData) throws IOException;

  public @Nullable DataType deserializeData(@Nullable ByteArraySequence sequence) throws IOException {
    if (sequence == null) return null;
    return deserializeFromByteSeq(sequence, myDataTypeExternalizer);
  }

  
  @Override
  public InputDataDiffBuilder<Key, Value> getDiffBuilder(int inputId, @Nullable ByteArraySequence sequence) throws IOException {
    return createDiffBuilder(inputId, deserializeData(sequence));
  }

  public abstract @Nullable DataType convertToDataType(InputData<Key, Value> data);

  @Override
  public @Nullable ByteArraySequence serializeIndexedData(InputData<Key, Value> data) throws IOException {
    return serializeIndexedData(convertToDataType(data));
  }

  public @Nullable ByteArraySequence serializeIndexedData(@Nullable DataType data) throws IOException {
    if (data == null) return null;
    return serializeToByteSeq(data, myDataTypeExternalizer, getBufferInitialSize(data));
  }

  protected int getBufferInitialSize(DataType dataType) {
    return 4;
  }

  private static final ThreadLocalCachedByteArray ourSpareByteArray = new ThreadLocalCachedByteArray();

  public static <Data> ByteArraySequence serializeToByteSeq(Data data, DataExternalizer<Data> externalizer, int bufferInitialSize) throws IOException {
    BufferExposingByteArrayOutputStream out = new BufferExposingByteArrayOutputStream(ourSpareByteArray.getBuffer(bufferInitialSize));
    DataOutputStream stream = new DataOutputStream(out);
    externalizer.save(stream, data);
    return out.toByteArraySequence();
  }

  public static <Data> Data deserializeFromByteSeq(ByteArraySequence bytes, DataExternalizer<Data> externalizer) throws IOException {
    DataInputStream stream = new DataInputStream(new UnsyncByteArrayInputStream(bytes.getBytes(), bytes.getOffset(), bytes.getLength()));
    return externalizer.read(stream);
  }
}
