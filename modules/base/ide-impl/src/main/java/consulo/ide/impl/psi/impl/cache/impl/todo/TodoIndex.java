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

package consulo.ide.impl.psi.impl.cache.impl.todo;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.psi.impl.cache.impl.id.PlatformIdTableBuilding;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.index.io.IntInlineKeyDescriptor;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataExternalizer;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.search.IndexPatternChangeListener;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.todo.TodoIndexEntry;
import consulo.language.psi.stub.todo.TodoIndexer;
import consulo.language.psi.stub.todo.VersionedTodoIndexer;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Eugene Zhuravlev
 * Date: Jan 20, 2008
 */
@ExtensionImpl
public class TodoIndex extends FileBasedIndexExtension<TodoIndexEntry, Integer> {
  @NonNls
  public static final ID<TodoIndexEntry, Integer> NAME = ID.create("TodoIndex");

  private final FileTypeManager myFileTypeManager;

  @Inject
  public TodoIndex(Application application, FileTypeManager manager) {
    myFileTypeManager = manager;
    application.getMessageBus().connect().subscribe(IndexPatternChangeListener.class, (o, n) -> FileBasedIndex.getInstance().requestRebuild(NAME));
  }

  private final KeyDescriptor<TodoIndexEntry> myKeyDescriptor = new KeyDescriptor<>() {
    @Override
    public void save(@Nonnull final DataOutput out, final TodoIndexEntry value) throws IOException {
      out.writeUTF(value.getPattern());
      out.writeBoolean(value.isCaseSensitive());
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
    if (!needsTodoIndex(project, file)) return false;
    FileType fileType = file.getFileType();

    DataIndexer<TodoIndexEntry, Integer, FileContent> indexer = PlatformIdTableBuilding.getTodoIndexer(fileType, project, file);
    return indexer != null;
  };

  @Override
  public int getVersion() {
    int version = 10;
    FileType[] types = myFileTypeManager.getRegisteredFileTypes();
    Arrays.sort(types, (o1, o2) -> Comparing.compare(o1.getId(), o2.getId()));

    for (FileType fileType : types) {
      DataIndexer<TodoIndexEntry, Integer, FileContent> indexer = TodoIndexer.forFileType(fileType);
      if (indexer == null) continue;

      int versionFromIndexer = indexer instanceof VersionedTodoIndexer ? (((VersionedTodoIndexer)indexer).getVersion()) : 0xFF;
      version = version * 31 + (versionFromIndexer ^ indexer.getClass().getName().hashCode());
    }
    return version;
  }

  public static boolean needsTodoIndex(@Nullable Project project, @Nonnull VirtualFile vFile) {
    if (!vFile.isInLocalFileSystem()) {
      return false;
    }

    if (project != null && ProjectFileIndex.getInstance(project).isInContent(vFile)) {
      return true;
    }

    //for (ExtraPlaceChecker checker : EP_NAME.getExtensionList()) {
    //  if (checker.accept(project, vFile)) {
    //    return true;
    //  }
    //}
    return false;
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
