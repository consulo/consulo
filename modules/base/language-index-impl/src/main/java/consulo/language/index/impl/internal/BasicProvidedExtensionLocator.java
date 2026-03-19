// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataExternalizer;
import consulo.language.index.impl.internal.provided.ProvidedIndexExtension;
import consulo.language.index.impl.internal.stub.StubProvidedIndexExtension;
import consulo.language.index.impl.internal.stub.StubUpdatingIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;

@ExtensionImpl
public class BasicProvidedExtensionLocator implements ProvidedIndexExtensionLocator {
  private static final String PREBUILT_INDEX_PATH_PROP = "prebuilt.hash.index.dir";

  @Nullable
  @Override
  public <K, V> ProvidedIndexExtension<K, V> findProvidedIndexExtension(FileBasedIndexExtension<K, V> originalExtension) {
    File root = getPrebuiltIndexPath();
    if (root == null || !new File(root, StringUtil.toLowerCase(originalExtension.getName().getName())).exists()) return null;

    return originalExtension.getName().equals(StubUpdatingIndex.INDEX_ID)
           ? (ProvidedIndexExtension<K, V>)new StubProvidedIndexExtension(root)
           : new ProvidedIndexExtensionImpl<>(root, originalExtension);
  }

  private static @Nullable File getPrebuiltIndexPath() {
    String path = System.getProperty(PREBUILT_INDEX_PATH_PROP);
    if (path == null) return null;
    File file = new File(path);
    return file.exists() ? file : null;
  }

  private static class ProvidedIndexExtensionImpl<K, V> implements ProvidedIndexExtension<K, V> {
    
    private final File myIndexFile;
    
    private final ID<K, V> myIndexId;
    
    private final KeyDescriptor<K> myKeyDescriptor;
    
    private final DataExternalizer<V> myValueExternalizer;

    private ProvidedIndexExtensionImpl(File file, FileBasedIndexExtension<K, V> originalExtension) {
      myIndexFile = file;
      myIndexId = originalExtension.getName();
      myKeyDescriptor = originalExtension.getKeyDescriptor();
      myValueExternalizer = originalExtension.getValueExternalizer();
    }

    
    @Override
    public File getIndexPath() {
      return myIndexFile;
    }

    
    @Override
    public ID<K, V> getIndexId() {
      return myIndexId;
    }

    
    @Override
    public KeyDescriptor<K> createKeyDescriptor() {
      return myKeyDescriptor;
    }

    
    @Override
    public DataExternalizer<V> createValueExternalizer() {
      return myValueExternalizer;
    }
  }
}
