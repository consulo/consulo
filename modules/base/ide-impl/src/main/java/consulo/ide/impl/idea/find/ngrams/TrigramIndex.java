/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.find.ngrams;

import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.language.psi.stub.DocumentChangeDependentIndex;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.ScalarIndexExtension;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.util.lang.ThreadLocalCachedIntArray;
import consulo.ide.impl.idea.openapi.util.text.TrigramBuilder;
import consulo.language.internal.psi.stub.IdIndex;
import consulo.util.lang.SystemProperties;
import consulo.ide.impl.idea.util.indexing.*;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.EnumeratorIntegerDescriptor;
import consulo.index.io.KeyDescriptor;

import jakarta.annotation.Nonnull;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

/**
 * @author max
 */
@ExtensionImpl
public class TrigramIndex extends ScalarIndexExtension<Integer> implements CustomInputsIndexFileBasedIndexExtension<Integer>, DocumentChangeDependentIndex {
  public static final boolean ENABLED = SystemProperties.getBooleanProperty("idea.internal.trigramindex.enabled", true);

  public static final ID<Integer, Void> INDEX_ID = ID.create("Trigram.Index");

  private static final FileBasedIndex.InputFilter INPUT_FILTER = (project, file) -> isIndexable(file.getFileType());

  public static boolean isIndexable(FileType fileType) {
    return ENABLED && !fileType.isBinary();
  }

  @Nonnull
  @Override
  public ID<Integer, Void> getName() {
    return INDEX_ID;
  }

  @Nonnull
  @Override
  public DataIndexer<Integer, Void, FileContent> getIndexer() {
    return new DataIndexer<Integer, Void, FileContent>() {
      @Override
      @Nonnull
      public Map<Integer, Void> map(@Nonnull FileContent inputData) {
        MyTrigramProcessor trigramProcessor = new MyTrigramProcessor();
        TrigramBuilder.processTrigrams(inputData.getContentAsText(), trigramProcessor);

        return trigramProcessor.map;
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
  public FileBasedIndex.InputFilter getInputFilter() {
    return INPUT_FILTER;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return ENABLED ? 3 + (IdIndex.ourSnapshotMappingsEnabled ? 0xFF : 0) : 1;
  }

  @Override
  public boolean hasSnapshotMapping() {
    return true;
  }

  private static final ThreadLocalCachedIntArray spareBufferLocal = new ThreadLocalCachedIntArray();

  @Nonnull
  @Override
  public DataExternalizer<Collection<Integer>> createExternalizer() {
    return new DataExternalizer<Collection<Integer>>() {
      @Override
      public void save(@Nonnull DataOutput out, @Nonnull Collection<Integer> value) throws IOException {
        final int numberOfValues = value.size();

        int[] buffer = spareBufferLocal.getBuffer(numberOfValues);
        int ptr = 0;
        for (Integer i : value) {
          buffer[ptr++] = i;
        }
        Arrays.sort(buffer, 0, numberOfValues);

        DataInputOutputUtil.writeINT(out, numberOfValues);
        int prev = 0;
        for (ptr = 0; ptr < numberOfValues; ++ptr) {
          DataInputOutputUtil.writeLONG(out, (long)buffer[ptr] - prev);
          prev = buffer[ptr];
        }
      }

      @Nonnull
      @Override
      public Collection<Integer> read(@Nonnull DataInput in) throws IOException {
        int size = DataInputOutputUtil.readINT(in);
        ArrayList<Integer> result = new ArrayList<>(size);
        int prev = 0;
        while (size-- > 0) {
          int l = (int)(DataInputOutputUtil.readLONG(in) + prev);
          result.add(l);
          prev = l;
        }
        return result;
      }
    };
  }

  private static class MyTrigramProcessor extends TrigramBuilder.TrigramProcessor {
    Map<Integer, Void> map;

    @Override
    public boolean consumeTrigramsCount(int count) {
      map = new HashMap<>(count);
      return true;
    }

    @Override
    public boolean test(int value) {
      map.put(value, null);
      return true;
    }
  }
}
