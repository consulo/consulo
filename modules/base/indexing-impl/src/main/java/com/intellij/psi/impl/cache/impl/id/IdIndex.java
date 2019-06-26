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

package com.intellij.psi.impl.cache.impl.id;

import com.intellij.lang.cacheBuilder.CacheBuilderRegistry;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.util.SystemProperties;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.InlineKeyDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 * Date: Jan 16, 2008
 */
public class IdIndex extends FileBasedIndexExtension<IdIndexEntry, Integer> {
  @NonNls
  public static final ID<IdIndexEntry, Integer> NAME = ID.create("IdIndex");

  private final FileBasedIndex.InputFilter myInputFilter;

  public static final boolean ourSnapshotMappingsEnabled = SystemProperties.getBooleanProperty("idea.index.snapshot.mappings.enabled", true);

  private final DataExternalizer<Integer> myValueExternalizer = new DataExternalizer<Integer>() {
    @Override
    public void save(@Nonnull final DataOutput out, final Integer value) throws IOException {
      out.write(value.intValue() & UsageSearchContext.ANY);
    }

    @Override
    public Integer read(@Nonnull final DataInput in) throws IOException {
      return Integer.valueOf(in.readByte() & UsageSearchContext.ANY);
    }
  };

  private final KeyDescriptor<IdIndexEntry> myKeyDescriptor = new InlineKeyDescriptor<IdIndexEntry>() {
    @Override
    public IdIndexEntry fromInt(int n) {
      return new IdIndexEntry(n);
    }

    @Override
    public int toInt(IdIndexEntry idIndexEntry) {
      return idIndexEntry.getWordHashCode();
    }
  };

  private final DataIndexer<IdIndexEntry, Integer, FileContent> myIndexer = new DataIndexer<IdIndexEntry, Integer, FileContent>() {
    @Override
    @Nonnull
    public Map<IdIndexEntry, Integer> map(@Nonnull final FileContent inputData) {
      final IdIndexer indexer = IdTableBuilding.getFileTypeIndexer(myRegistry, inputData.getFileType());
      if (indexer != null) {
        return indexer.map(inputData);
      }

      return Collections.emptyMap();
    }
  };

  protected final CacheBuilderRegistry myRegistry;

  @Inject
  IdIndex(CacheBuilderRegistry registry) {
    myRegistry = registry;
    myInputFilter = (project, file) -> isIndexable(file.getFileType(), registry);
  }

  @Override
  public int getVersion() {
    return 16 + (ourSnapshotMappingsEnabled ? 0xFF : 0); // TODO: version should enumerate all word scanner versions and build version upon that set
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Nonnull
  @Override
  public ID<IdIndexEntry, Integer> getName() {
    return NAME;
  }

  @Nonnull
  @Override
  public DataIndexer<IdIndexEntry, Integer, FileContent> getIndexer() {
    return myIndexer;
  }

  @Nonnull
  @Override
  public DataExternalizer<Integer> getValueExternalizer() {
    return myValueExternalizer;
  }

  @Nonnull
  @Override
  public KeyDescriptor<IdIndexEntry> getKeyDescriptor() {
    return myKeyDescriptor;
  }

  @Nonnull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return myInputFilter;
  }

  public static boolean isIndexable(FileType fileType, CacheBuilderRegistry registry) {
    return fileType instanceof LanguageFileType || fileType instanceof CustomSyntaxTableFileType || IdTableBuilding.isIdIndexerRegistered(fileType) || registry.getCacheBuilder(fileType) != null;
  }

  @Override
  public boolean hasSnapshotMapping() {
    return true;
  }
}
