// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs.provided;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.stubs.*;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.provided.ProvidedIndexExtension;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.VoidDataExternalizer;
import consulo.disposer.Disposer;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.io.File;

public class StubProvidedIndexExtension implements ProvidedIndexExtension<Integer, SerializedStubTree> {
  @Nonnull
  private final File myIndexFile;

  public StubProvidedIndexExtension(@Nonnull File file) {
    myIndexFile = file;
  }

  @Nonnull
  @Override
  public File getIndexPath() {
    return myIndexFile;
  }

  @Nonnull
  @Override
  public ID<Integer, SerializedStubTree> getIndexId() {
    return StubUpdatingIndex.INDEX_ID;
  }

  @Nonnull
  @Override
  public KeyDescriptor<Integer> createKeyDescriptor() {
    return EnumeratorIntegerDescriptor.INSTANCE;
  }

  @Nonnull
  @Override
  public DataExternalizer<SerializedStubTree> createValueExternalizer() {
    File path = getIndexPath();
    SerializationManagerImpl manager = new SerializationManagerImpl(new File(new File(path, StringUtil.toLowerCase(StubUpdatingIndex.INDEX_ID.getName())), "rep.names"), true);
    Disposer.register(ApplicationManager.getApplication(), manager);
    return new SerializedStubTreeDataExternalizer(false, manager);
  }

  @Nullable
  public <K> ProvidedIndexExtension<K, Void> findProvidedStubIndex(@Nonnull StubIndexExtension<K, ?> extension) {
    String name = extension.getKey().getName();
    File path = getIndexPath();

    File indexPath = new File(path, StringUtil.toLowerCase(name));
    if (!indexPath.exists()) return null;

    return new ProvidedIndexExtension<K, Void>() {
      @Nonnull
      @Override
      public File getIndexPath() {
        return myIndexFile;
      }

      @Nonnull
      @Override
      public ID<K, Void> getIndexId() {
        return (ID)extension.getKey();
      }

      @Nonnull
      @Override
      public KeyDescriptor<K> createKeyDescriptor() {
        return extension.getKeyDescriptor();
      }

      @Nonnull
      @Override
      public DataExternalizer<Void> createValueExternalizer() {
        return VoidDataExternalizer.INSTANCE;
      }
    };
  }
}
