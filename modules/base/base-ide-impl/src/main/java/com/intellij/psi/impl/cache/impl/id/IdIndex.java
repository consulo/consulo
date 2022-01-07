// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.cache.impl.id;

import com.intellij.lang.cacheBuilder.CacheBuilderRegistry;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.SystemProperties;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.InlineKeyDescriptor;
import com.intellij.util.io.KeyDescriptor;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 */
public class IdIndex extends FileBasedIndexExtension<IdIndexEntry, Integer> implements DocumentChangeDependentIndex {
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
      final IdIndexer indexer = IdTableBuilding.getFileTypeIndexer(inputData.getFileType());
      if (indexer != null) {
        return indexer.map(inputData);
      }

      return Collections.emptyMap();
    }
  };

  protected final CacheBuilderRegistry myCacheBuilderRegistry;

  @Inject
  public IdIndex(CacheBuilderRegistry cacheBuilderRegistry) {
    myCacheBuilderRegistry = cacheBuilderRegistry;

    myInputFilter = (project, file) -> isIndexable(myCacheBuilderRegistry, file.getFileType());
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

  public static boolean isIndexable(@Nonnull CacheBuilderRegistry cacheBuilderRegistry, FileType fileType) {
    return fileType instanceof LanguageFileType ||
           fileType instanceof CustomSyntaxTableFileType ||
           IdTableBuilding.isIdIndexerRegistered(fileType) ||
           cacheBuilderRegistry.getCacheBuilder(fileType) != null;
  }

  @Override
  public boolean hasSnapshotMapping() {
    return true;
  }

  public static boolean hasIdentifierInFile(@Nonnull PsiFile file, @Nonnull String name) {
    PsiUtilCore.ensureValid(file);
    if (file.getVirtualFile() == null || DumbService.isDumb(file.getProject())) {
      return StringUtil.contains(file.getViewProvider().getContents(), name);
    }

    GlobalSearchScope scope = GlobalSearchScope.fileScope(file);
    return !FileBasedIndex.getInstance().getContainingFiles(NAME, new IdIndexEntry(name, true), scope).isEmpty();
  }
}
