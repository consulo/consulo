// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.stub;

import consulo.index.io.StorageException;
import consulo.index.io.internal.DebugAssertions;
import consulo.index.io.forward.KeyValueUpdateProcessor;
import consulo.index.io.forward.RemovedKeyProcessor;
import consulo.index.io.forward.InputDataDiffBuilder;
import consulo.language.psi.stub.StubIndex;
import consulo.language.psi.stub.StubIndexKey;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

class StubCumulativeInputDiffBuilder extends InputDataDiffBuilder<Integer, SerializedStubTree> {
  private final int myInputId;
  @Nullable
  private final SerializedStubTree myCurrentTree;

  StubCumulativeInputDiffBuilder(int inputId, @Nullable SerializedStubTree currentTree) {
    super(inputId);
    myInputId = inputId;
    myCurrentTree = currentTree;
  }

  @Override
  public boolean differentiate(@Nonnull Map<Integer, SerializedStubTree> newData,
                               @Nonnull KeyValueUpdateProcessor<? super Integer, ? super SerializedStubTree> addProcessor,
                               @Nonnull KeyValueUpdateProcessor<? super Integer, ? super SerializedStubTree> updateProcessor,
                               @Nonnull RemovedKeyProcessor<? super Integer> removeProcessor) throws StorageException {
    if (!newData.isEmpty()) {
      SerializedStubTree newSerializedStubTree = newData.values().iterator().next();
      if (myCurrentTree != null) {
        if (treesAreEqual(newSerializedStubTree, myCurrentTree)) return false;
        removeProcessor.process(myInputId, myInputId);
      }
      addProcessor.process(myInputId, newSerializedStubTree, myInputId);
      updateStubIndices(newSerializedStubTree);
    }
    else {
      removeProcessor.process(myInputId, myInputId);
      updateStubIndices(null);
    }
    return true;
  }

  private static boolean treesAreEqual(@Nonnull SerializedStubTree newSerializedStubTree, @Nonnull SerializedStubTree currentTree) {
    return Arrays.equals(currentTree.getTreeHash(), newSerializedStubTree.getTreeHash()) && treesAreReallyEqual(newSerializedStubTree, currentTree);
  }

  private static boolean treesAreReallyEqual(@Nonnull SerializedStubTree newSerializedStubTree, @Nonnull SerializedStubTree currentTree) {
    if (newSerializedStubTree.equals(currentTree)) {
      return true;
    }
    if (DebugAssertions.DEBUG) {
      SerializedStubTree.reportStubTreeHashCollision(newSerializedStubTree, currentTree);
    }
    return false;
  }

  private void updateStubIndices(@Nullable SerializedStubTree newSerializedStubTree) {
    Map<StubIndexKey, Map<Object, StubIdList>> previousStubIndicesValueMap = myCurrentTree == null ? Collections.emptyMap() : myCurrentTree.getStubIndicesValueMap();
    Map<StubIndexKey, Map<Object, StubIdList>> newStubIndicesValueMap = newSerializedStubTree == null ? Collections.emptyMap() : newSerializedStubTree.getStubIndicesValueMap();
    updateStubIndices(getAffectedIndices(previousStubIndicesValueMap, newStubIndicesValueMap), myInputId, previousStubIndicesValueMap, newStubIndicesValueMap);
  }

  private static void updateStubIndices(@Nonnull final Collection<StubIndexKey> indexKeys,
                                        final int inputId,
                                        @Nonnull final Map<StubIndexKey, Map<Object, StubIdList>> oldStubTree,
                                        @Nonnull final Map<StubIndexKey, Map<Object, StubIdList>> newStubTree) {
    final StubIndexImpl stubIndex = (StubIndexImpl)StubIndex.getInstance();
    for (StubIndexKey key : indexKeys) {
      final Map<Object, StubIdList> oldMap = oldStubTree.get(key);
      final Map<Object, StubIdList> newMap = newStubTree.get(key);

      final Map<Object, StubIdList> _oldMap = oldMap != null ? oldMap : Collections.emptyMap();
      final Map<Object, StubIdList> _newMap = newMap != null ? newMap : Collections.emptyMap();

      stubIndex.updateIndex(key, inputId, _oldMap, _newMap);
    }
  }

  @Nonnull
  private static Collection<StubIndexKey> getAffectedIndices(@Nonnull final Map<StubIndexKey, Map<Object, StubIdList>> oldStubTree,
                                                             @Nonnull final Map<StubIndexKey, Map<Object, StubIdList>> newStubTree) {
    Set<StubIndexKey> allIndices = new HashSet<>();
    allIndices.addAll(oldStubTree.keySet());
    allIndices.addAll(newStubTree.keySet());
    return allIndices;
  }
}
