// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.fileType;

import consulo.annotation.DeprecationInfo;
import consulo.component.internal.RootComponentHolder;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A service for retrieving file types for files.
 *
 * <p><b>Performance notice.</b> There are different rules of file type matching for a file: matching by file name, by extension,
 * by file content, by custom logic providers and so on. They are all executed by the general methods {@code getFileTypeByFile},
 * thus implying that execution of
 * such methods is as long as the sum of all possible matching checks in the worst case. That includes reading file contents to
 * feed to all {@link FileTypeDetector} instances, checking {@link FileTypeIdentifiableByVirtualFile} and so on. Such actions
 * may lead to considerable slowdowns if used on large {@code VirtualFile} collections, e.g. in
 * {@link consulo.ide.impl.idea.openapi.vfs.newvfs.BulkFileListener} implementations.
 *
 * <p> If it is possible and correct to restrict file type matching by particular means (e.g. match only by file name),
 * it is advised to do so, in order to improve the performance of the check, e.g. use
 * <pre><code>
 * FileTypeRegistry.getInstance().getFileTypeByFileName(file.getNameSequence())
 * </code></pre>
 * instead of
 * <pre><code>
 * file.getFileType()
 * </code></pre>
 * <p>
 * Also, if you are interested not in getting file type, but rather comparing file type with a known one, prefer using
 * {@link #isFileOfType(VirtualFile, FileType)}, as it is faster than {@link #getFileTypeByFile(VirtualFile)} as well.
 *
 * @author yole
 */
public abstract class FileTypeRegistry {
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use constructor injection")
  public static FileTypeRegistry getInstance() {
    return RootComponentHolder.getRootComponent().getInstance(FileTypeRegistry.class);
  }

  public abstract boolean isFileIgnored(@Nonnull VirtualFile file);

  /**
   * Checks if the given file has the given file type. This is faster than getting the file type
   * and comparing it, because for file types that are identified by virtual file, it will only
   * check if the given file type matches, and will not run other detectors. However, this can
   * lead to inconsistent results if two file types report the same file as matching (which should
   * generally be avoided).
   */
  public abstract boolean isFileOfType(@Nonnull VirtualFile file, @Nonnull FileType type);

  /**
   * Returns the list of all registered file types.
   *
   * @return The list of file types.
   */
  @Nonnull
  public abstract FileType[] getRegisteredFileTypes();

  /**
   * Returns the file type for the specified file.
   *
   * @param file The file for which the type is requested.
   * @return The file type instance.
   */
  @Nonnull
  public abstract FileType getFileTypeByFile(@Nonnull VirtualFile file);

  /**
   * Returns the file type for the specified file.
   *
   * @param file    The file for which the type is requested.
   * @param content Content of the file (if already available, to avoid reading from disk again)
   * @return The file type instance.
   */
  @Nonnull
  public FileType getFileTypeByFile(@Nonnull VirtualFile file, @Nullable byte[] content) {
    return getFileTypeByFile(file);
  }

  /**
   * Returns the file type for the specified file name.
   *
   * @param fileNameSeq The file name for which the type is requested.
   * @return The file type instance, or {@link FileTypes#UNKNOWN} if not found.
   */
  @Nonnull
  public FileType getFileTypeByFileName(@Nonnull CharSequence fileNameSeq) {
    return getFileTypeByFileName(fileNameSeq.toString());
  }

  /**
   * Same as {@linkplain FileTypeRegistry#getFileTypeByFileName(CharSequence)} but receives String parameter.
   * <p>
   * Consider to use the method above in case when you want to get VirtualFile's file type by file name.
   */
  @Nonnull
  public abstract FileType getFileTypeByFileName(@Nonnull String fileTypeId);

  /**
   * Returns the file type for the specified extension.
   * Note that a more general way of obtaining file type is with {@link #getFileTypeByFile(VirtualFile)}
   *
   * @param extension The extension for which the file type is requested, not including the leading '.'.
   * @return The file type instance, or {@link UnknownFileType#INSTANCE} if corresponding file type not found
   */
  @Nonnull
  public abstract FileType getFileTypeByExtension(@Nonnull String extension);

  /**
   * Finds a file type with the specified name.
   */
  @Nullable
  public abstract FileType findFileTypeByName(@Nonnull String fileTypeName);
}
