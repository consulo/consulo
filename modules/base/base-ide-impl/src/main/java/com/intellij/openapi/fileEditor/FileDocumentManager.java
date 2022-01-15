// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileEditor;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.SavingRequestor;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Tracks the correspondence between {@link VirtualFile} instances and corresponding {@link Document} instances.
 * Manages the saving of changes to disk.
 */
public abstract class FileDocumentManager implements SavingRequestor {
  @Nonnull
  public static FileDocumentManager getInstance() {
    return ApplicationManager.getApplication().getComponent(FileDocumentManager.class);
  }

  /**
   * Returns the document for the specified virtual file.<p/>
   * <p>
   * Documents are cached on weak or strong references, depending on the nature of the virtual file. If the document for the given virtual file is not yet cached,
   * the file's contents are read from VFS and loaded into heap memory. An appropriate encoding is used. All line separators are converted to {@code \n}.<p/>
   * <p>
   * Should be invoked in a read action.
   *
   * @param file the file for which the document is requested.
   * @return the document, or null if the file represents a directory, or is binary without an associated decompiler,
   * or is too large.
   * @see VirtualFile#contentsToByteArray()
   * @see Application#runReadAction(Computable)
   */
  @Nullable
  public abstract Document getDocument(@Nonnull VirtualFile file);

  /**
   * Returns the document for the specified file which has already been loaded into memory.<p/>
   * <p>
   * Client code shouldn't normally use this method, because it's unpredictable and any garbage collection can result in it returning null.
   *
   * @param file the file for which the document is requested.
   * @return the document, or null if the specified virtual file hasn't been loaded into memory.
   */
  @Nullable
  public abstract Document getCachedDocument(@Nonnull VirtualFile file);

  /**
   * Returns the virtual file corresponding to the specified document.
   *
   * @param document the document for which the virtual file is requested.
   * @return the file, or null if the document wasn't created from a virtual file.
   */
  @Nullable
  public abstract VirtualFile getFile(@Nonnull Document document);

  /**
   * Saves all unsaved documents to disk. This operation can modify documents that will be saved
   * (due to 'Strip trailing spaces on Save' functionality). When saving, {@code \n} line separators are converted into
   * the ones used normally on the system, or the ones explicitly specified by the user. Encoding settings are honored.<p/>
   * <p>
   * Should be invoked on the event dispatch thread.
   */
  public abstract void saveAllDocuments();

  /**
   * Saves the specified document to disk. This operation can modify the document (due to 'Strip
   * trailing spaces on Save' functionality). When saving, {@code \n} line separators are converted into
   * the ones used normally on the system, or the ones explicitly specified by the user. Encoding settings are honored.<p/>
   * <p>
   * Should be invoked on the event dispatch thread.
   *
   * @param document the document to save.
   */
  public abstract void saveDocument(@Nonnull Document document);

  /**
   * Saves the document without stripping the trailing spaces or adding a blank line in the end of the file.<p/>
   * <p>
   * Should be invoked on the event dispatch thread.
   *
   * @param document the document to save.
   */
  public abstract void saveDocumentAsIs(@Nonnull Document document);

  /**
   * Returns all documents that have unsaved changes.
   *
   * @return the documents that have unsaved changes.
   */
  @Nonnull
  public abstract Document[] getUnsavedDocuments();

  /**
   * Checks if the document has unsaved changes.
   *
   * @param document the document to check.
   * @return true if the document has unsaved changes, false otherwise.
   */
  public abstract boolean isDocumentUnsaved(@Nonnull Document document);

  /**
   * Checks if the document corresponding to the specified file has unsaved changes.
   *
   * @param file the file to check.
   * @return true if the file has unsaved changes, false otherwise.
   */
  public abstract boolean isFileModified(@Nonnull VirtualFile file);

  /**
   * Check if only beginning of the file was loaded for Document.
   *
   * @see FileUtilRt#isTooLarge
   */
  public abstract boolean isPartialPreviewOfALargeFile(@Nonnull Document document);

  /**
   * Discards unsaved changes for the specified document and reloads it from disk.
   *
   * @param document the document to reload.
   */
  public abstract void reloadFromDisk(@Nonnull Document document);

  @Nonnull
  public abstract String getLineSeparator(@Nullable VirtualFile file, @Nullable Project project);

  /**
   * Requests writing access on the given document, possibly involving interaction with user.
   *
   * @param document document
   * @param project  project
   * @return true if writing access allowed
   * @see com.intellij.openapi.vfs.ReadonlyStatusHandler#ensureFilesWritable(Project, VirtualFile...)
   */
  public abstract boolean requestWriting(@Nonnull Document document, @Nullable Project project);

  /**
   * Requests writing access info on the given document. Can involve interaction with user.
   */
  @Nonnull
  public WriteAccessStatus requestWritingStatus(@Nonnull Document document, @Nullable Project project) {
    return requestWriting(document, project) ? WriteAccessStatus.WRITABLE : WriteAccessStatus.NON_WRITABLE;
  }

  public static boolean fileForDocumentCheckedOutSuccessfully(@Nonnull Document document, @Nonnull Project project) {
    return getInstance().requestWriting(document, project);
  }

  /**
   * Discards unsaved changes for the specified files.
   *
   * @param files the files to discard the changes for.
   */
  public abstract void reloadFiles(@Nonnull VirtualFile... files);

  /**
   * Stores the write access status (true if the document has the write access; false otherwise)
   * and a message about the reason for the read-only status.
   */
  public static class WriteAccessStatus {
    public static final WriteAccessStatus NON_WRITABLE = new WriteAccessStatus(false);
    public static final WriteAccessStatus WRITABLE = new WriteAccessStatus(true);

    private final boolean myWithWriteAccess;
    @Nonnull
    private final String myReadOnlyMessage;

    private WriteAccessStatus(boolean withWriteAccess) {
      myWithWriteAccess = withWriteAccess;
      myReadOnlyMessage = withWriteAccess ? "" : EditorBundle.message("editing.read.only.file.hint");
    }

    public WriteAccessStatus(@Nonnull String readOnlyMessage) {
      myWithWriteAccess = false;
      myReadOnlyMessage = readOnlyMessage;
    }

    public boolean hasWriteAccess() {
      return myWithWriteAccess;
    }

    @Nonnull
    public String getReadOnlyMessage() {
      return myReadOnlyMessage;
    }
  }
}
