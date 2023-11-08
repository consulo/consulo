// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.impl.cache.impl.id;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.util.indexing.CustomInputsIndexFileBasedIndexExtension;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.language.cacheBuilder.CacheBuilderRegistry;
import consulo.language.internal.psi.stub.IdIndex;
import consulo.language.psi.stub.IdIndexEntry;
import consulo.language.psi.stub.IdIndexer;
import consulo.language.psi.stub.IdTableBuilding;
import consulo.util.lang.Comparing;
import consulo.util.lang.ThreadLocalCachedIntArray;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

@ExtensionImpl
public class IdIndexImpl extends IdIndex implements CustomInputsIndexFileBasedIndexExtension<IdIndexEntry> {
  private static final ThreadLocalCachedIntArray spareBufferLocal = new ThreadLocalCachedIntArray();

  private final FileTypeRegistry myFileTypeRegistry;

  @Inject
  public IdIndexImpl(CacheBuilderRegistry cacheBuilderRegistry, FileTypeRegistry fileTypeRegistry) {
    super(cacheBuilderRegistry);
    myFileTypeRegistry = fileTypeRegistry;
  }

  @Override
  public int getVersion() {
    FileType[] types = myFileTypeRegistry.getRegisteredFileTypes();
    Arrays.sort(types, (o1, o2) -> Comparing.compare(o1.getId(), o2.getId()));

    int version = super.getVersion();
    for (FileType fileType : types) {
      if (!isIndexable(myCacheBuilderRegistry, fileType)) continue;
      IdIndexer indexer = IdTableBuilding.getFileTypeIndexer(fileType);
      if (indexer == null) continue;
      version = version * 31 + (indexer.getVersion() ^ indexer.getClass().getName().hashCode());
    }
    return version;
  }

  @Nonnull
  @Override
  public DataExternalizer<Collection<IdIndexEntry>> createExternalizer() {
    return new DataExternalizer<>() {
      @Override
      public void save(@Nonnull DataOutput out, @Nonnull Collection<IdIndexEntry> value) throws IOException {
        int size = value.size();
        final int[] values = spareBufferLocal.getBuffer(size);
        int ptr = 0;
        for (IdIndexEntry ie : value) {
          values[ptr++] = ie.getWordHashCode();
        }
        Arrays.sort(values, 0, size);
        DataInputOutputUtil.writeINT(out, size);
        int prev = 0;
        for (int i = 0; i < size; ++i) {
          DataInputOutputUtil.writeLONG(out, (long)values[i] - prev);
          prev = values[i];
        }
      }

      @Override
      public Collection<IdIndexEntry> read(@Nonnull DataInput in) throws IOException {
        int length = DataInputOutputUtil.readINT(in);
        ArrayList<IdIndexEntry> entries = new ArrayList<>(length);
        int prev = 0;
        while (length-- > 0) {
          final int l = (int)(DataInputOutputUtil.readLONG(in) + prev);
          entries.add(new IdIndexEntry(l));
          prev = l;
        }
        return entries;
      }
    };
  }
}
