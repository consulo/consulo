// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.stub;

import consulo.application.ApplicationManager;
import consulo.disposer.Disposer;
import consulo.index.io.EnumeratorIntegerDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.VoidDataExternalizer;
import consulo.index.io.data.DataExternalizer;
import consulo.language.index.impl.internal.provided.ProvidedIndexExtension;
import consulo.language.psi.stub.StubIndexExtension;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;

public class StubProvidedIndexExtension implements ProvidedIndexExtension<Integer, SerializedStubTree> {
  
  private final File myIndexFile;

  public StubProvidedIndexExtension(File file) {
    myIndexFile = file;
  }

  
  @Override
  public File getIndexPath() {
    return myIndexFile;
  }

  
  @Override
  public ID<Integer, SerializedStubTree> getIndexId() {
    return StubUpdatingIndex.INDEX_ID;
  }

  
  @Override
  public KeyDescriptor<Integer> createKeyDescriptor() {
    return EnumeratorIntegerDescriptor.INSTANCE;
  }

  
  @Override
  public DataExternalizer<SerializedStubTree> createValueExternalizer() {
    File path = getIndexPath();
    SerializationManagerImpl manager = new SerializationManagerImpl(new File(new File(path, StringUtil.toLowerCase(StubUpdatingIndex.INDEX_ID.getName())), "rep.names"), true);
    Disposer.register(ApplicationManager.getApplication(), manager);
    return new SerializedStubTreeDataExternalizer(false, manager);
  }

  public @Nullable <K> ProvidedIndexExtension<K, Void> findProvidedStubIndex(StubIndexExtension<K, ?> extension) {
    String name = extension.getKey().getName();
    File path = getIndexPath();

    File indexPath = new File(path, StringUtil.toLowerCase(name));
    if (!indexPath.exists()) return null;

    return new ProvidedIndexExtension<K, Void>() {
      
      @Override
      public File getIndexPath() {
        return myIndexFile;
      }

      
      @Override
      public ID<K, Void> getIndexId() {
        return (ID)extension.getKey();
      }

      
      @Override
      public KeyDescriptor<K> createKeyDescriptor() {
        return extension.getKeyDescriptor();
      }

      
      @Override
      public DataExternalizer<Void> createValueExternalizer() {
        return VoidDataExternalizer.INSTANCE;
      }
    };
  }
}
