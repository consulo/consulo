// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.stub;

import consulo.index.io.CompressionUtil;
import consulo.index.io.PersistentHashMapValueStorage;
import consulo.index.io.data.DataExternalizer;
import consulo.language.index.impl.internal.moduleAware.VariantDescriptor;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.language.internal.SerializationManagerEx;
import consulo.util.collection.ArrayUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
  public final void save(DataOutput out, SerializedStubTree tree) throws IOException {
    writePayload(out, tree);
    VariantDescriptor descriptor = tree.getDescriptor(0);
    DataInputOutputUtil.writeINT(out, descriptor == null ? 0 : 1);
    if (descriptor != null) {
      VariantDescriptor.write(out, descriptor);
    }
    List<SerializedStubTree.StubVariant> variants = tree.getVariants();
    DataInputOutputUtil.writeINT(out, variants.size());
    for (SerializedStubTree.StubVariant variant : variants) {
      VariantDescriptor.write(out, variant.descriptor());
      writePayload(out, variant.tree());
    }
  }

  private void writePayload(DataOutput out, SerializedStubTree tree) throws IOException {
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

  @Override
  public final SerializedStubTree read(DataInput in) throws IOException {
    SerializedStubTree primary = readPayload(in);
    VariantDescriptor descriptor = DataInputOutputUtil.readINT(in) == 0 ? null : VariantDescriptor.read(in);
    int variantCount = DataInputOutputUtil.readINT(in);
    if (descriptor == null && variantCount == 0) {
      return primary;
    }
    List<SerializedStubTree.StubVariant> variants = new ArrayList<>(variantCount);
    for (int i = 0; i < variantCount; i++) {
      VariantDescriptor variantDescriptor = VariantDescriptor.read(in);
      variants.add(new SerializedStubTree.StubVariant(variantDescriptor, readPayload(in)));
    }
    return primary.withVariants(descriptor, variants);
  }

  private SerializedStubTree readPayload(DataInput in) throws IOException {
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
      SerializedStubTree tree = new SerializedStubTree(treeBytes, treeBytes.length, null, indexedStubBytes, indexedStubBytes.length, null);
      if (mySerializationManager != null) tree.setSerializationManager(mySerializationManager);
      return tree;
    }
  }
}
