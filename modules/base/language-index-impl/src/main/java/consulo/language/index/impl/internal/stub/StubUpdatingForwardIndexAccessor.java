// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.stub;

import consulo.application.progress.ProgressManager;
import consulo.index.io.StorageException;
import consulo.index.io.InputData;
import consulo.language.index.impl.internal.UpdatableIndex;
import consulo.index.io.forward.ForwardIndexAccessor;
import consulo.index.io.forward.InputDataDiffBuilder;
import consulo.language.psi.stub.FileContent;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.ByteArraySequence;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.Map;

class StubUpdatingForwardIndexAccessor implements ForwardIndexAccessor<Integer, SerializedStubTree> {
  private volatile UpdatableIndex<Integer, SerializedStubTree, FileContent> myIndex;

  @Nonnull
  @Override
  public InputDataDiffBuilder<Integer, SerializedStubTree> getDiffBuilder(int inputId, @Nullable ByteArraySequence sequence) throws IOException {
    Ref<Map<Integer, SerializedStubTree>> dataRef = Ref.create();
    StorageException[] ex = {null};
    ProgressManager.getInstance().executeNonCancelableSection(() -> {
      try {
        dataRef.set(myIndex.getIndexedFileData(inputId));
      }
      catch (StorageException e) {
        ex[0] = e;
      }
    });
    if (ex[0] != null) {
      throw new IOException(ex[0]);
    }
    Map<Integer, SerializedStubTree> data = dataRef.get();
    SerializedStubTree tree = ContainerUtil.isEmpty(data) ? null : ContainerUtil.getFirstItem(data.values());
    if (tree != null) {
      tree.restoreIndexedStubs(StubForwardIndexExternalizer.IdeStubForwardIndexesExternalizer.INSTANCE);
    }
    return new StubCumulativeInputDiffBuilder(inputId, tree);
  }

  @Nullable
  @Override
  public ByteArraySequence serializeIndexedData(@Nonnull InputData<Integer, SerializedStubTree> data) {
    return null;
  }

  void setIndex(@Nonnull UpdatableIndex<Integer, SerializedStubTree, FileContent> index) {
    myIndex = index;
  }
}
