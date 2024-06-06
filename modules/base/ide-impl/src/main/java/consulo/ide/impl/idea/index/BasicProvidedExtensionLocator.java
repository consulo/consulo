// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.index;

import consulo.annotation.component.ExtensionImpl;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.psi.stubs.StubUpdatingIndex;
import consulo.ide.impl.psi.stubs.provided.StubProvidedIndexExtension;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.index.io.ID;
import consulo.ide.impl.idea.util.indexing.provided.ProvidedIndexExtension;
import consulo.ide.impl.idea.util.indexing.provided.ProvidedIndexExtensionLocator;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.KeyDescriptor;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import java.io.File;

@ExtensionImpl
public class BasicProvidedExtensionLocator implements ProvidedIndexExtensionLocator {
  private static final String PREBUILT_INDEX_PATH_PROP = "prebuilt.hash.index.dir";

  @Nullable
  @Override
  public <K, V> ProvidedIndexExtension<K, V> findProvidedIndexExtension(@Nonnull FileBasedIndexExtension<K, V> originalExtension) {
    File root = getPrebuiltIndexPath();
    if (root == null || !new File(root, StringUtil.toLowerCase(originalExtension.getName().getName())).exists()) return null;

    return originalExtension.getName().equals(StubUpdatingIndex.INDEX_ID)
           ? (ProvidedIndexExtension<K, V>)new StubProvidedIndexExtension(root)
           : new ProvidedIndexExtensionImpl<>(root, originalExtension);
  }

  @Nullable
  private static File getPrebuiltIndexPath() {
    String path = System.getProperty(PREBUILT_INDEX_PATH_PROP);
    if (path == null) return null;
    File file = new File(path);
    return file.exists() ? file : null;
  }

  private static class ProvidedIndexExtensionImpl<K, V> implements ProvidedIndexExtension<K, V> {
    @Nonnull
    private final File myIndexFile;
    @Nonnull
    private final ID<K, V> myIndexId;
    @Nonnull
    private final KeyDescriptor<K> myKeyDescriptor;
    @Nonnull
    private final DataExternalizer<V> myValueExternalizer;

    private ProvidedIndexExtensionImpl(@Nonnull File file, @Nonnull FileBasedIndexExtension<K, V> originalExtension) {
      myIndexFile = file;
      myIndexId = originalExtension.getName();
      myKeyDescriptor = originalExtension.getKeyDescriptor();
      myValueExternalizer = originalExtension.getValueExternalizer();
    }

    @Nonnull
    @Override
    public File getIndexPath() {
      return myIndexFile;
    }

    @Nonnull
    @Override
    public ID<K, V> getIndexId() {
      return myIndexId;
    }

    @Nonnull
    @Override
    public KeyDescriptor<K> createKeyDescriptor() {
      return myKeyDescriptor;
    }

    @Nonnull
    @Override
    public DataExternalizer<V> createValueExternalizer() {
      return myValueExternalizer;
    }
  }
}
