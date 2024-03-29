// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.stub;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.index.io.ID;
import consulo.index.io.IndexExtension;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * <a href="http://www.jetbrains.org/intellij/sdk/docs/basics/indexing_and_psi_stubs/file_based_indexes.html">SDK Docs</a>
 * <p>
 * V class MUST have equals / hashcode properly defined!!!
 *
 * @author Eugene Zhuravlev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class FileBasedIndexExtension<K, V> extends IndexExtension<K, V, FileContent> {

  public static final ExtensionPointName<FileBasedIndexExtension> EXTENSION_POINT_NAME = ExtensionPointName.create(FileBasedIndexExtension.class);

  private static final int DEFAULT_CACHE_SIZE = 1024;

  @Nonnull
  @Override
  public abstract ID<K, V> getName();

  /**
   * @return filter for file are supposed being indexed by the {@link IndexExtension#getIndexer()}.
   * <p>
   * Usually {@link DefaultFileTypeSpecificInputFilter} can be used here to index only files with given file-type.
   * Note that check only file's extension is usually error-prone way and prefer to check {@link VirtualFile#getFileType()}:
   * for example user can enforce language file as plain text one.
   */
  @Nonnull
  public abstract FileBasedIndex.InputFilter getInputFilter();

  public abstract boolean dependsOnFileContent();

  public boolean indexDirectories() {
    return false;
  }

  /**
   * @see FileBasedIndexExtension#DEFAULT_CACHE_SIZE
   */
  public int getCacheSize() {
    return DEFAULT_CACHE_SIZE;
  }

  /**
   * For most indices the method should return an empty collection.
   *
   * @return collection of file types to which file size limit will not be applied when indexing.
   * This is the way to allow indexing of files whose limit exceeds {@link PersistentFSConstants#getMaxIntellisenseFileSize()}.
   * <p>
   * Use carefully, because indexing large files may influence index update speed dramatically.
   */
  @Nonnull
  public Collection<FileType> getFileTypesWithSizeLimitNotApplicable() {
    return List.of();
  }

  public boolean keyIsUniqueForIndexedFile() {
    return false;
  }

  public boolean traceKeyHashToVirtualFileMapping() {
    return false;
  }

  public boolean hasSnapshotMapping() {
    return false;
  }
}
