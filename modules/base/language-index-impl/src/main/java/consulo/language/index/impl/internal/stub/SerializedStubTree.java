/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.index.impl.internal.stub;

import consulo.language.index.impl.internal.moduleAware.VariantDescriptor;

import consulo.language.impl.DebugUtil;
import consulo.language.internal.SerializationManagerEx;
import consulo.language.psi.stub.*;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.BufferExposingByteArrayOutputStream;
import consulo.util.io.DigestUtil;
import consulo.util.io.UnsyncByteArrayInputStream;
import consulo.util.lang.ThreadLocalCachedValue;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author max
 */
public class SerializedStubTree {
  private static final Logger LOG = Logger.getInstance(SerializedStubTree.class);
  private static final ThreadLocalCachedValue<MessageDigest> HASHER = new ThreadLocalCachedValue<MessageDigest>() {
    
    @Override
    protected MessageDigest create() {
      return DigestUtil.sha256();
    }
  };

  // serialized tree
  final byte[] myTreeBytes;
  final int myTreeByteLength;
  private Stub myStubElement;

  // stub forward indexes
  final byte[] myIndexedStubBytes;
  final int myIndexedStubByteLength;
  private Map<StubIndexKey, Map<Object, StubIdList>> myIndexedStubs;

  private volatile SerializationManagerEx mySerializationManager;

  private final @Nullable VariantDescriptor myDescriptor;
  private final List<StubVariant> myVariants;
  private volatile Map<StubIndexKey, Map<Object, StubIdList>> myUnionIndexedStubs;

  public record StubVariant(VariantDescriptor descriptor, SerializedStubTree tree) {
  }

  public void setSerializationManager(SerializationManagerEx serializationManager) {
    mySerializationManager = serializationManager;
  }

  public SerializedStubTree(byte[] treeBytes, int treeByteLength, @Nullable Stub stubElement,
                            byte[] indexedStubBytes, int indexedStubByteLength, @Nullable Map<StubIndexKey, Map<Object, StubIdList>> indexedStubs) {
    this(treeBytes, treeByteLength, stubElement, indexedStubBytes, indexedStubByteLength, indexedStubs, null, List.of());
  }

  public SerializedStubTree(byte[] treeBytes, int treeByteLength, @Nullable Stub stubElement,
                            byte[] indexedStubBytes, int indexedStubByteLength, @Nullable Map<StubIndexKey, Map<Object, StubIdList>> indexedStubs,
                            @Nullable VariantDescriptor descriptor, List<StubVariant> variants) {
    myTreeBytes = treeBytes;
    myTreeByteLength = treeByteLength;
    myStubElement = stubElement;
    myIndexedStubBytes = indexedStubBytes;
    myIndexedStubByteLength = indexedStubByteLength;
    myIndexedStubs = indexedStubs;
    myDescriptor = descriptor;
    myVariants = List.copyOf(variants);
  }

  /**
   * This tree as the primary variant of a file, described by {@code descriptor}, together with its secondary variants.
   */
  public SerializedStubTree withVariants(VariantDescriptor descriptor, List<StubVariant> variants) {
    return new SerializedStubTree(myTreeBytes, myTreeByteLength, myStubElement, myIndexedStubBytes, myIndexedStubByteLength, myIndexedStubs, descriptor, variants);
  }

  public int getVariantCount() {
    return 1 + myVariants.size();
  }

  public SerializedStubTree getVariantTree(int index) {
    return index == 0 ? this : myVariants.get(index - 1).tree();
  }

  public @Nullable VariantDescriptor getDescriptor(int index) {
    return index == 0 ? myDescriptor : myVariants.get(index - 1).descriptor();
  }

  public List<StubVariant> getVariants() {
    return myVariants;
  }

  /**
   * @return the index of the variant whose providers carry exactly the given payloads, or -1
   */
  public int findVariant(Map<String, byte[]> payloads) {
    for (int i = 0; i < getVariantCount(); i++) {
      VariantDescriptor descriptor = getDescriptor(i);
      if (descriptor != null && descriptor.matches(payloads)) {
        return i;
      }
    }
    return -1;
  }

  public SerializedStubTree(Stub rootStub, SerializationManagerEx serializationManager, StubForwardIndexExternalizer<?> forwardIndexExternalizer) throws IOException {
    BufferExposingByteArrayOutputStream bytes = new BufferExposingByteArrayOutputStream();
    serializationManager.serialize(rootStub, bytes);
    myTreeBytes = bytes.getInternalBuffer();
    myTreeByteLength = bytes.size();
    ObjectStubBase root = (ObjectStubBase)rootStub;
    myIndexedStubs = indexTree(root);
    BufferExposingByteArrayOutputStream indexBytes = new BufferExposingByteArrayOutputStream();
    forwardIndexExternalizer.save(new DataOutputStream(indexBytes), myIndexedStubs);
    myIndexedStubBytes = indexBytes.getInternalBuffer();
    myIndexedStubByteLength = indexBytes.size();
    myDescriptor = null;
    myVariants = List.of();
  }

  
  public SerializedStubTree reSerialize(SerializationManagerImpl currentSerializationManager,
                                        SerializationManagerImpl newSerializationManager,
                                        StubForwardIndexExternalizer currentForwardIndexSerializer,
                                        StubForwardIndexExternalizer newForwardIndexSerializer) throws IOException {
    BufferExposingByteArrayOutputStream outStub = new BufferExposingByteArrayOutputStream();
    currentSerializationManager.reSerialize(new ByteArrayInputStream(myTreeBytes, 0, myTreeByteLength), outStub, newSerializationManager);

    byte[] reSerializedIndexBytes;
    int reSerializedIndexByteLength;

    if (currentForwardIndexSerializer == newForwardIndexSerializer) {
      reSerializedIndexBytes = myIndexedStubBytes;
      reSerializedIndexByteLength = myIndexedStubByteLength;
    }
    else {
      BufferExposingByteArrayOutputStream reSerializedStubIndices = new BufferExposingByteArrayOutputStream();
      if (myIndexedStubs == null) {
        restoreIndexedStubs(currentForwardIndexSerializer);
      }
      assert myIndexedStubs != null;
      newForwardIndexSerializer.save(new DataOutputStream(reSerializedStubIndices), myIndexedStubs);
      reSerializedIndexBytes = reSerializedStubIndices.getInternalBuffer();
      reSerializedIndexByteLength = reSerializedStubIndices.size();
    }

    List<StubVariant> variants = new ArrayList<>(myVariants.size());
    for (StubVariant variant : myVariants) {
      variants.add(new StubVariant(variant.descriptor(), variant.tree().reSerialize(currentSerializationManager, newSerializationManager, currentForwardIndexSerializer, newForwardIndexSerializer)));
    }
    return new SerializedStubTree(outStub.getInternalBuffer(), outStub.size(), null, reSerializedIndexBytes, reSerializedIndexByteLength, myIndexedStubs, myDescriptor, variants);
  }

  void restoreIndexedStubs(StubForwardIndexExternalizer<?> dataExternalizer) throws IOException {
    if (myIndexedStubs == null) {
      myIndexedStubs = dataExternalizer.read(new DataInputStream(new ByteArrayInputStream(myIndexedStubBytes, 0, myIndexedStubByteLength)));
    }
    for (StubVariant variant : myVariants) {
      variant.tree().restoreIndexedStubs(dataExternalizer);
    }
  }

  <K> StubIdList restoreIndexedStubs(StubForwardIndexExternalizer<?> dataExternalizer, StubIndexKey<K, ?> indexKey, K key, int variant) throws IOException {
    return getVariantTree(variant).restoreIndexedStubs(dataExternalizer, indexKey, key);
  }

  <K> StubIdList restoreIndexedStubs(StubForwardIndexExternalizer<?> dataExternalizer, StubIndexKey<K, ?> indexKey, K key) throws IOException {
    Map<StubIndexKey, Map<Object, StubIdList>> incompleteMap = dataExternalizer.doRead(new DataInputStream(new ByteArrayInputStream(myIndexedStubBytes, 0, myIndexedStubByteLength)), indexKey, key);
    if (incompleteMap == null) return null;
    Map<Object, StubIdList> map = incompleteMap.get(indexKey);
    return map == null ? null : map.get(key);
  }

  
  /**
   * The keys of every variant of the file: the inverted stub indexes point at a file whenever any of its variants
   * contains a key, and the variant-specific stub ids are resolved from the value at query time.
   */
  public Map<StubIndexKey, Map<Object, StubIdList>> getStubIndicesValueMap() {
    if (myVariants.isEmpty()) {
      return myIndexedStubs;
    }
    Map<StubIndexKey, Map<Object, StubIdList>> union = myUnionIndexedStubs;
    if (union != null) {
      return union;
    }
    try {
      restoreIndexedStubs(StubForwardIndexExternalizer.IdeStubForwardIndexesExternalizer.INSTANCE);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    union = new HashMap<>();
    for (int i = 0; i < getVariantCount(); i++) {
      Map<StubIndexKey, Map<Object, StubIdList>> variantMap = getVariantTree(i).myIndexedStubs;
      if (variantMap == null) {
        continue;
      }
      for (Map.Entry<StubIndexKey, Map<Object, StubIdList>> entry : variantMap.entrySet()) {
        Map<Object, StubIdList> merged = union.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
        for (Map.Entry<Object, StubIdList> keyEntry : entry.getValue().entrySet()) {
          merged.putIfAbsent(keyEntry.getKey(), keyEntry.getValue());
        }
      }
    }
    myUnionIndexedStubs = union;
    return union;
  }

  @TestOnly
  public Map<StubIndexKey, Map<Object, StubIdList>> readStubIndicesValueMap() throws IOException {
    restoreIndexedStubs(StubForwardIndexExternalizer.IdeStubForwardIndexesExternalizer.INSTANCE);
    return myIndexedStubs;
  }

  // willIndexStub is one time optimization hint, once can safely pass false
  
  public Stub getStub(boolean willIndexStub) throws SerializerNotFoundException {
    SerializationManagerEx manager = mySerializationManager;
    if (manager == null) {
      manager = SerializationManagerEx.getInstanceEx();
    }
    return getStub(willIndexStub, manager);
  }

  
  public Stub getStub(boolean willIndexStub, SerializationManagerEx serializationManager) throws SerializerNotFoundException {
    Stub stubElement = myStubElement;
    if (stubElement != null) {
      // not null myStubElement means we just built SerializedStubTree for indexing,
      // if we request stub for indexing we can safely use it
      myStubElement = null;
      if (willIndexStub) return stubElement;
    }
    return retrieveStubFromBytes(serializationManager);
  }

  
  Stub retrieveStubFromBytes(SerializationManagerEx serializationManager) throws SerializerNotFoundException {
    return serializationManager.deserialize(new UnsyncByteArrayInputStream(myTreeBytes, 0, myTreeByteLength));
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }
    if (!(that instanceof SerializedStubTree)) {
      return false;
    }
    SerializedStubTree thatTree = (SerializedStubTree)that;
    if (!sameBytes(thatTree) || myVariants.size() != thatTree.myVariants.size() || !Objects.equals(myDescriptor, thatTree.myDescriptor)) {
      return false;
    }
    for (int i = 0; i < myVariants.size(); i++) {
      StubVariant mine = myVariants.get(i);
      StubVariant theirs = thatTree.myVariants.get(i);
      if (!mine.descriptor().equals(theirs.descriptor()) || !mine.tree().sameBytes(theirs.tree())) {
        return false;
      }
    }
    return true;
  }

  private boolean sameBytes(SerializedStubTree thatTree) {
    int length = myTreeByteLength;
    if (length != thatTree.myTreeByteLength) {
      return false;
    }
    byte[] thisBytes = myTreeBytes;
    byte[] thatBytes = thatTree.myTreeBytes;
    for (int i = 0; i < length; i++) {
      if (thisBytes[i] != thatBytes[i]) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    if (myTreeBytes == null) {
      return 0;
    }

    int result = 1;
    for (int i = 0; i < myTreeByteLength; i++) {
      result = 31 * result + myTreeBytes[i];
    }

    return result;
  }

  
  private String dumpStub() {
    String deserialized;
    try {
      deserialized = "stub: " + DebugUtil.stubTreeToString(getStub(true));
    }
    catch (SerializerNotFoundException e) {
      LOG.error(e);
      deserialized = "error while stub deserialization: " + e.getMessage();
    }
    return deserialized + "\n bytes: " + toHexString(myTreeBytes, myTreeByteLength);
  }

  
  static Map<StubIndexKey, Map<Object, StubIdList>> indexTree(Stub root) {
    ObjectStubTree objectStubTree = root instanceof PsiFileStub ? new StubTree((PsiFileStub)root, false) : new ObjectStubTree((ObjectStubBase)root, false);
    StubIndexImpl indexImpl = (StubIndexImpl)StubIndex.getInstance();
    Map<StubIndexKey, Map<Object, int[]>> map = objectStubTree.indexStubTree(k -> indexImpl.getKeyHashingStrategy((StubIndexKey<Object, ?>)k));

    // xxx:fix refs inplace
    for (StubIndexKey key : map.keySet()) {
      Map<Object, int[]> value = map.get(key);
      for (Object k : value.keySet()) {
        int[] ints = value.get(k);
        StubIdList stubList = ints.length == 1 ? new StubIdList(ints[0]) : new StubIdList(ints, ints.length);
        ((Map<Object, StubIdList>)(Map)value).put(k, stubList);
      }
    }
    return (Map<StubIndexKey, Map<Object, StubIdList>>)(Map)map;
  }

  private byte[] myTreeHash;

  
  synchronized byte[] getTreeHash() {
    if (myTreeHash == null) {
      MessageDigest digest = HASHER.getValue();
      digest.update(myTreeBytes, 0, myTreeByteLength);
      myTreeHash = digest.digest();
    }
    return myTreeHash;
  }

  static void reportStubTreeHashCollision(SerializedStubTree newTree, SerializedStubTree existingTree) {
    String oldTreeDump = "\nexisting tree " + existingTree.dumpStub();
    String newTreeDump = "\nnew tree " + newTree.dumpStub();
    byte[] hash = newTree.getTreeHash();
    LOG.info("Stub tree hashing collision. Different trees have the same hash = " + toHexString(hash, hash.length) +
             ". Hashing algorithm = " + HASHER.getValue().getAlgorithm() + "." + oldTreeDump + newTreeDump, new Exception());
  }

  private static String toHexString(byte[] hash, int length) {
    return Arrays.stream(ArrayUtil.toIntArray(hash)).limit(length).mapToObj(b -> String.format("%02x", b & 0xFF)).collect(Collectors.joining(", "));
  }
}