// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.hash;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.index.io.*;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.language.impl.internal.psi.stub.FileContentImpl;
import consulo.language.index.impl.internal.CustomImplementationFileBasedIndexExtension;
import consulo.language.index.impl.internal.CustomInputsIndexFileBasedIndexExtension;
import consulo.language.index.impl.internal.UpdatableIndex;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.logging.Logger;
import consulo.util.lang.ShutDownTracker;
import jakarta.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public class FileContentHashIndexExtension extends FileBasedIndexExtension<Integer, Void>
        implements CustomImplementationFileBasedIndexExtension<Integer, Void>, CustomInputsIndexFileBasedIndexExtension<Integer>, Disposable {
  private static final Logger LOG = Logger.getInstance(FileContentHashIndexExtension.class);
  public static final ID<Integer, Void> HASH_INDEX_ID = ID.create("file.content.hash.index");

  @Nonnull
  private final ContentHashesUtil.HashEnumerator myEnumerator;
  private final int myDirHash;

  @Nonnull
  public static FileContentHashIndexExtension create(@Nonnull File enumeratorDir, @Nonnull Disposable parent) throws IOException {
    FileContentHashIndexExtension extension = new FileContentHashIndexExtension(enumeratorDir);
    Disposer.register(parent, extension);
    return extension;
  }

  private FileContentHashIndexExtension(@Nonnull File enumeratorDir) throws IOException {
    myEnumerator = new ContentHashesUtil.HashEnumerator(enumeratorDir);
    myDirHash = enumeratorDir.getAbsolutePath().hashCode();
    ShutDownTracker.getInstance().registerShutdownTask(() -> closeEnumerator());
  }

  @Nonnull
  @Override
  public ID<Integer, Void> getName() {
    return HASH_INDEX_ID;
  }

  @Nonnull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Nonnull
  @Override
  public DataIndexer<Integer, Void, FileContent> getIndexer() {
    return fc -> {
      byte[] hash = ((FileContentImpl)fc).getHash();
      LOG.assertTrue(hash != null);
      try {
        int id;
        synchronized (myEnumerator) {
          id = myEnumerator.tryEnumerate(hash);
        }
        return id == 0 ? Collections.emptyMap() : Collections.singletonMap(id, null);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  @Nonnull
  @Override
  public KeyDescriptor<Integer> getKeyDescriptor() {
    return EnumeratorIntegerDescriptor.INSTANCE;
  }

  @Nonnull
  @Override
  public DataExternalizer<Void> getValueExternalizer() {
    return VoidDataExternalizer.INSTANCE;
  }

  @Override
  public int getVersion() {
    return myDirHash;
  }

  @Override
  public void dispose() {
    closeEnumerator();
  }

  @Nonnull
  @Override
  public DataExternalizer<Collection<Integer>> createExternalizer() {
    return new DataExternalizer<Collection<Integer>>() {
      @Override
      public void save(@Nonnull DataOutput out, Collection<Integer> value) throws IOException {
        assert value.isEmpty() || value.size() == 1;
        DataInputOutputUtil.writeINT(out, value.isEmpty() ? 0 : value.iterator().next());
      }

      @Override
      public Collection<Integer> read(@Nonnull DataInput in) throws IOException {
        int id = DataInputOutputUtil.readINT(in);
        return id == 0 ? Collections.emptyList() : Collections.singleton(id);
      }
    };
  }

  private void closeEnumerator() {
    synchronized (myEnumerator) {
      if (myEnumerator.isClosed()) return;
      try {
        myEnumerator.close();
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  @Nonnull
  @Override
  public UpdatableIndex<Integer, Void, FileContent> createIndexImplementation(@Nonnull FileBasedIndexExtension<Integer, Void> extension, @Nonnull IndexStorage<Integer, Void> storage)
          throws IOException {
    return new FileContentHashIndex(((FileContentHashIndexExtension)extension), storage);
  }
}
