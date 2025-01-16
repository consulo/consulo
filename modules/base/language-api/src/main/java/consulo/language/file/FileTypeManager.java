// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.file;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.util.PerApplicationInstance;
import consulo.component.ComponentManager;
import consulo.language.Language;
import consulo.virtualFileSystem.fileType.FileTypeListener;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.*;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

/**
 * Manages the relationship between filenames and {@link FileType} instances.
 */
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
public abstract class FileTypeManager extends FileTypeRegistry {
  private static Supplier<FileTypeManager> ourInstance = PerApplicationInstance.of(FileTypeManager.class);

  /**
   * Returns the singleton instance of the FileTypeManager component.
   *
   * @return the instance of FileTypeManager
   */
  @Nonnull
  public static FileTypeManager getInstance() {
    return ourInstance.get();
  }

  @Nullable
  public LanguageFileType findFileTypeByLanguage(@Nonnull Language language) {
    return language.findMyFileType(getRegisteredFileTypes());
  }

  /**
   * Checks if the specified file is ignored by the IDE. Ignored files are not visible in
   * different project views and cannot be opened in the editor. They will neither be parsed nor compiled.
   *
   * @param name The name of the file to check.
   * @return {@code true} if the file is ignored, {@code false} otherwise.
   */
  public abstract boolean isFileIgnored(@Nonnull String name);

  /**
   * Returns the list of extensions associated with the specified file type.
   *
   * @param type The file type for which the extensions are requested.
   * @return The array of extensions associated with the file type.
   * @deprecated since more generic way of associations using wildcards exist, not every association matches extension paradigm
   */
  @Deprecated
  @Nonnull
  public abstract String[] getAssociatedExtensions(@Nonnull FileType type);


  @Nonnull
  public abstract List<FileNameMatcher> getAssociations(@Nonnull FileType type);

  /**
   * Adds a listener for receiving notifications about changes in the list of
   * registered file types.
   *
   * @param listener The listener instance.
   * @deprecated Subscribe to {@link #TOPIC} on any message bus level instead.
   */
  @Deprecated
  public abstract void addFileTypeListener(@Nonnull FileTypeListener listener);

  /**
   * Removes a listener for receiving notifications about changes in the list of
   * registered file types.
   *
   * @param listener The listener instance.
   * @deprecated Subscribe to {@link #TOPIC} on any message bus level instead.
   */
  @Deprecated
  public abstract void removeFileTypeListener(@Nonnull FileTypeListener listener);

  /**
   * If file is already associated with any known file type returns it.
   * Otherwise asks user to select file type and associates it with fileName extension if any selected.
   *
   * @param file file to ask for file type association
   * @return Known file type or {@code null}. Never returns {@link FileTypes#UNKNOWN}.
   * @deprecated Use {@link #getKnownFileTypeOrAssociate(VirtualFile, Project)} instead
   */
  @Nullable
  @Deprecated
  public FileType getKnownFileTypeOrAssociate(@Nonnull VirtualFile file) {
    return file.getFileType();
  }

  @Nullable
  public abstract FileType getKnownFileTypeOrAssociate(@Nonnull VirtualFile file, @Nonnull Project project);

  @Nullable
  @Override
  public FileType getKnownFileTypeOrAssociate(@Nonnull VirtualFile file, @Nonnull ComponentManager project) {
    return getKnownFileTypeOrAssociate(file, (Project)project);
  }

  /**
   * Returns the semicolon-delimited list of patterns for files and folders
   * which are excluded from the project structure though they may be present
   * physically on disk.
   *
   * @return Semicolon-delimited list of patterns.
   */
  @Nonnull
  @Deprecated
  public String getIgnoredFilesList() {
    Set<String> masks = getIgnoredFiles();
    return masks.isEmpty() ? "" : String.join(";", masks) + ";";
  }

  /**
   * Returns the list of patterns for files and folders
   * which are excluded from the project structure though they may be present
   * physically on disk.
   */
  @Nonnull
  public Set<String> getIgnoredFiles() {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets new list of semicolon-delimited patterns for files and folders which
   * are excluded from the project structure.
   *
   * @param list List of semicolon-delimited patterns.
   */
  public void setIgnoredFilesList(@Nonnull String list) {
    Set<String> files = new LinkedHashSet<>();
    StringTokenizer tokenizer = new StringTokenizer(list, ";");
    while (tokenizer.hasMoreTokens()) {
      String ignoredFile = tokenizer.nextToken();
      if (ignoredFile != null) {
        files.add(ignoredFile);
      }
    }

    setIgnoredFiles(files);
  }

  /**
   * Sets new list of patterns for files and folders which
   * are excluded from the project structure.
   *
   */
  public void setIgnoredFiles(@Nonnull Set<String> list) {
    throw new UnsupportedOperationException();
  }

  /**
   * Adds an extension to the list of extensions associated with a file type.
   *
   * @param type      the file type to associate the extension with.
   * @param extension the extension to associate.
   */
  public final void associateExtension(@Nonnull FileType type, @Nonnull @NonNls String extension) {
    associate(type, FileNameMatcherFactory.getInstance().createExtensionFileNameMatcher(extension));
  }

  public final void associatePattern(@Nonnull FileType type, @Nonnull @NonNls String pattern) {
    associate(type, parseFromString(pattern));
  }

  public abstract void associate(@Nonnull FileType type, @Nonnull FileNameMatcher matcher);

  /**
   * Removes an extension from the list of extensions associated with a file type.
   *
   * @param type      the file type to remove the extension from.
   * @param extension the extension to remove.
   */
  public final void removeAssociatedExtension(@Nonnull FileType type, @Nonnull @NonNls String extension) {
    removeAssociation(type, FileNameMatcherFactory.getInstance().createExtensionFileNameMatcher(extension));
  }

  public abstract void removeAssociation(@Nonnull FileType type, @Nonnull FileNameMatcher matcher);

  @Nonnull
  public static FileNameMatcher parseFromString(@Nonnull String pattern) {
    return FileNameMatcherFactory.getInstance().createMatcher(pattern);
  }

  @Nonnull
  public abstract FileType getStdFileType(@Nonnull @NonNls String fileTypeName);
}
