// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.indexing.impl.forward;

import consulo.util.lang.Comparing;
import consulo.application.util.VolatileNotNullLazyValue;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.indexing.*;
import consulo.ide.impl.idea.util.indexing.impl.InputData;
import consulo.ide.impl.idea.util.indexing.impl.InputDataDiffBuilder;
import consulo.ide.impl.idea.util.indexing.impl.KeyValueUpdateProcessor;
import consulo.ide.impl.idea.util.indexing.impl.RemovedKeyProcessor;
import consulo.application.progress.ProgressManager;
import consulo.index.io.ID;
import consulo.index.io.StorageException;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.index.io.IndexExtension;
import consulo.language.psi.stub.SingleEntryFileBasedIndexExtension;
import consulo.logging.Logger;
import consulo.util.io.ByteArraySequence;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

public class SingleEntryIndexForwardIndexAccessor<V> implements ForwardIndexAccessor<Integer, V> {
  private static final Logger LOG = Logger.getInstance(SingleEntryIndexForwardIndexAccessor.class);
  private final ID<Integer, V> myIndexId;
  private final VolatileNotNullLazyValue<UpdatableIndex<Integer, V, ?>> myIndex = new VolatileNotNullLazyValue<UpdatableIndex<Integer, V, ?>>() {
    @Nonnull
    @Override
    protected UpdatableIndex<Integer, V, ?> compute() {
      return ((FileBasedIndexImpl)FileBasedIndex.getInstance()).getIndex(myIndexId);
    }
  };

  @SuppressWarnings("unchecked")
  public SingleEntryIndexForwardIndexAccessor(IndexExtension<?, ?, V> extension) {
    LOG.assertTrue(extension instanceof SingleEntryFileBasedIndexExtension);
    myIndexId = (ID<Integer, V>)extension.getName();
  }

  @Nonnull
  @Override
  public InputDataDiffBuilder<Integer, V> getDiffBuilder(int inputId, @Nullable ByteArraySequence sequence) throws IOException {
    Ref<Map<Integer, V>> dataRef = Ref.create();
    StorageException[] ex = {null};
    ProgressManager.getInstance().executeNonCancelableSection(() -> {
      try {
        dataRef.set(myIndex.getValue().getIndexedFileData(inputId));
      }
      catch (StorageException e) {
        ex[0] = e;
      }
    });
    if (ex[0] != null) {
      throw new IOException(ex[0]);
    }
    Map<Integer, V> currentData = dataRef.get();

    return new SingleValueDiffBuilder<>(inputId, currentData);
  }

  @Nullable
  @Override
  public ByteArraySequence serializeIndexedData(@Nonnull InputData<Integer, V> data) {
    return null;
  }

  public static class SingleValueDiffBuilder<V> extends InputDataDiffBuilder<Integer, V> {
    private final int myInputId;
    private final boolean myContainsValue;
    @Nullable
    private final V myCurrentValue;

    public SingleValueDiffBuilder(int inputId, @Nonnull Map<Integer, V> currentData) {
      this(inputId, !currentData.isEmpty(), ContainerUtil.getFirstItem(currentData.values()));
    }

    private SingleValueDiffBuilder(int inputId, boolean containsValue, @Nullable V currentValue) {
      super(inputId);
      myInputId = inputId;
      myContainsValue = containsValue;
      myCurrentValue = currentValue;
    }

    @Override
    public boolean differentiate(@Nonnull Map<Integer, V> newData,
                                 @Nonnull KeyValueUpdateProcessor<? super Integer, ? super V> addProcessor,
                                 @Nonnull KeyValueUpdateProcessor<? super Integer, ? super V> updateProcessor,
                                 @Nonnull RemovedKeyProcessor<? super Integer> removeProcessor) throws StorageException {
      boolean newValueExists = !newData.isEmpty();
      V newValue = ContainerUtil.getFirstItem(newData.values());
      if (myContainsValue) {
        if (!newValueExists) {
          removeProcessor.process(myInputId, myInputId);
          return true;
        }
        else if (Comparing.equal(myCurrentValue, newValue)) {
          return false;
        }
        else {
          updateProcessor.process(myInputId, newValue, myInputId);
          return true;
        }
      }
      else {
        if (newValueExists) {
          addProcessor.process(myInputId, newValue, myInputId);
          return true;
        }
        else {
          return false;
        }
      }
    }
  }
}
