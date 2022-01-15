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

package com.intellij.psi.impl.cache.impl.todo;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.cache.impl.id.PlatformIdTableBuilding;
import com.intellij.psi.search.IndexPatternProvider;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IntInlineKeyDescriptor;
import com.intellij.util.io.KeyDescriptor;
import consulo.lang.util.LanguageVersionUtil;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Eugene Zhuravlev
 *         Date: Jan 20, 2008
 */
public class TodoIndex extends FileBasedIndexExtension<TodoIndexEntry, Integer> {
  @NonNls
  public static final ID<TodoIndexEntry, Integer> NAME = ID.create("TodoIndex");

  private final FileTypeManager myFileTypeManager;

  @Inject
  public TodoIndex(Application application, FileTypeManager manager) {
    myFileTypeManager = manager;
    application.getMessageBus().connect().subscribe(IndexPatternProvider.INDEX_PATTERNS_CHANGED, evt -> FileBasedIndex.getInstance().requestRebuild(NAME));
  }

  private final KeyDescriptor<TodoIndexEntry> myKeyDescriptor = new KeyDescriptor<>() {
    @Override
    public void save(@Nonnull final DataOutput out, final TodoIndexEntry value) throws IOException {
      out.writeUTF(value.pattern);
      out.writeBoolean(value.caseSensitive);
    }

    @Override
    public TodoIndexEntry read(@Nonnull final DataInput in) throws IOException {
      final String pattern = in.readUTF();
      final boolean caseSensitive = in.readBoolean();
      return new TodoIndexEntry(pattern, caseSensitive);
    }
  };

  private final DataExternalizer<Integer> myValueExternalizer = new IntInlineKeyDescriptor() {
    @Override
    protected boolean isCompactFormat() {
      return true;
    }
  };

  private final DataIndexer<TodoIndexEntry, Integer, FileContent> myIndexer = inputData -> {
    final VirtualFile file = inputData.getFile();
    final DataIndexer<TodoIndexEntry, Integer, FileContent> indexer = PlatformIdTableBuilding.getTodoIndexer(inputData.getFileType(), inputData.getProject(), file);
    if (indexer != null) {
      return indexer.map(inputData);
    }
    return Collections.emptyMap();
  };

  private final FileBasedIndex.InputFilter myInputFilter = (project, file) -> {
    if (!file.isInLocalFileSystem()) {
      return false; // do not index TODOs in library sources
    }

    final FileType fileType = file.getFileType();

    if (fileType instanceof LanguageFileType) {
      final Language lang = ((LanguageFileType)fileType).getLanguage();
      final ParserDefinition parserDef = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
      final TokenSet commentTokens = parserDef != null ? parserDef.getCommentTokens(LanguageVersionUtil.findLanguageVersion(lang, project, file)) : null;
      return commentTokens != null;
    }

    return PlatformIdTableBuilding.isTodoIndexerRegistered(fileType) || fileType instanceof CustomSyntaxTableFileType;
  };

  @Override
  public int getVersion() {
    int version = 10;
    FileType[] types = myFileTypeManager.getRegisteredFileTypes();
    Arrays.sort(types, (o1, o2) -> Comparing.compare(o1.getId(), o2.getId()));

    for (FileType fileType : types) {
      DataIndexer<TodoIndexEntry, Integer, FileContent> indexer = TodoIndexers.INSTANCE.forFileType(fileType);
      if (indexer == null) continue;

      int versionFromIndexer = indexer instanceof VersionedTodoIndexer ? (((VersionedTodoIndexer)indexer).getVersion()) : 0xFF;
      version = version * 31 + (versionFromIndexer ^ indexer.getClass().getName().hashCode());
    }
    return version;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Nonnull
  @Override
  public ID<TodoIndexEntry, Integer> getName() {
    return NAME;
  }

  @Nonnull
  @Override
  public DataIndexer<TodoIndexEntry, Integer, FileContent> getIndexer() {
    return myIndexer;
  }

  @Nonnull
  @Override
  public KeyDescriptor<TodoIndexEntry> getKeyDescriptor() {
    return myKeyDescriptor;
  }

  @Nonnull
  @Override
  public DataExternalizer<Integer> getValueExternalizer() {
    return myValueExternalizer;
  }

  @Nonnull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return myInputFilter;
  }

  @Override
  public boolean hasSnapshotMapping() {
    return true;
  }
}
