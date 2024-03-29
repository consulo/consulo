// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.stubs;

import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.index.io.CompressionUtil;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.PersistentHashMapValueStorage;
import jakarta.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SerializedStubTreeDataExternalizer implements DataExternalizer<SerializedStubTree> {
  private final boolean myIncludeInputs;
  private final SerializationManagerEx mySerializationManager;

  public SerializedStubTreeDataExternalizer() {
    this(true, null);
  }

  public SerializedStubTreeDataExternalizer(boolean inputs, SerializationManagerEx manager) {
    myIncludeInputs = inputs;
    mySerializationManager = manager;
  }

  @Override
  public final void save(@Nonnull final DataOutput out, @Nonnull final SerializedStubTree tree) throws IOException {
    if (PersistentHashMapValueStorage.COMPRESSION_ENABLED) {
      DataInputOutputUtil.writeINT(out, tree.myTreeByteLength);
      out.write(tree.myTreeBytes, 0, tree.myTreeByteLength);
      if (myIncludeInputs) {
        DataInputOutputUtil.writeINT(out, tree.myIndexedStubByteLength);
        out.write(tree.myIndexedStubBytes, 0, tree.myIndexedStubByteLength);
      }
    }
    else {
      CompressionUtil.writeCompressed(out, tree.myTreeBytes, 0, tree.myTreeByteLength);
      if (myIncludeInputs) CompressionUtil.writeCompressed(out, tree.myIndexedStubBytes, 0, tree.myIndexedStubByteLength);
    }
  }

  @Nonnull
  @Override
  public final SerializedStubTree read(@Nonnull final DataInput in) throws IOException {
    if (PersistentHashMapValueStorage.COMPRESSION_ENABLED) {
      int serializedStubsLength = DataInputOutputUtil.readINT(in);
      byte[] bytes = new byte[serializedStubsLength];
      in.readFully(bytes);
      int indexedStubByteLength;
      byte[] indexedStubBytes;
      if (myIncludeInputs) {
        indexedStubByteLength = DataInputOutputUtil.readINT(in);
        indexedStubBytes = new byte[indexedStubByteLength];
        in.readFully(indexedStubBytes);
      }
      else {
        indexedStubByteLength = 0;
        indexedStubBytes = ArrayUtil.EMPTY_BYTE_ARRAY;
      }
      SerializedStubTree tree = new SerializedStubTree(bytes, bytes.length, null, indexedStubBytes, indexedStubByteLength, null);
      if (mySerializationManager != null) tree.setSerializationManager(mySerializationManager);
      return tree;
    }
    else {
      byte[] treeBytes = CompressionUtil.readCompressed(in);
      byte[] indexedStubBytes = myIncludeInputs ? CompressionUtil.readCompressed(in) : ArrayUtil.EMPTY_BYTE_ARRAY;
      return new SerializedStubTree(treeBytes, treeBytes.length, null, indexedStubBytes, indexedStubBytes.length, null);
    }
  }
}
