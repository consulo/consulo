/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.diff;

import consulo.diff.request.DiffRequest;
import consulo.document.Document;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.LineSeparator;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Represents some data that probably can be compared with some other.
 *
 * @see DiffRequest
 * @deprecated use {@link consulo.diff.content.DiffContent} instead
 */
@Deprecated
public abstract class DiffContent {
  private final List<Listener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private boolean myIsEmpty;

  public void addListener(Listener listener) {
    myListeners.add(listener);
  }

  public void removeListener(Listener listener) {
    myListeners.remove(listener);
  }

  /**
   * This content becomes invalid for some reason. Diff tool should stop show it.
   */
  protected void fireContentInvalid() {
    for (Listener listener : myListeners) {
      listener.contentInvalid();
    }
  }

  /**
   * Means this content represents binary data. It should be used only for byte by byte comparison.
   * E.g. directories aren't binary (in spite of they aren't text)
   *
   * @return true if this content represents binary data
   */
  public boolean isBinary() {
    return false;
  }

  public void setIsEmpty(boolean isEmpty) {
    myIsEmpty = isEmpty;
  }

  public boolean isEmpty() {
    return myIsEmpty;
  }

  /**
   * Called by {@link consulo.ide.impl.idea.openapi.diff.DiffTool}
   * when document returned by {@link #getDocument()} is opened in editor. Implementors may use this notification to
   * add listeners when document is editing and remove when editing done to avoid memory leaks.
   *
   * @param isAssigned true means editing started, false means editing stopped.
   *                   Total number of calls with true should be same as for false
   */
  public void onAssigned(boolean isAssigned) {
  }

  /**
   * Represents this content as Document
   * null means content has no text representation
   *
   * @return document associated with this content
   */
  public abstract Document getDocument();

  /**
   * Provides a way to open given text place in editor
   * null means given offset can't be opened in editor
   *
   * @param offset in document returned by {@link #getDocument()}
   * @return {@link OpenFileDescriptorImpl} to open this content in editor
   */
  public abstract OpenFileDescriptorImpl getOpenFileDescriptor(int offset);

  /**
   * @return VirtualFile from which this content gets data.
   *         null means this content has no file associated
   */
  @Nullable
  public abstract VirtualFile getFile();

  /**
   * @return FileType of content.
   *         null means use other content's type for this one
   */
  @Nullable
  public abstract FileType getContentType();

  /**
   * @return Binary represntation of content.
   *         Should not be null if {@link #getFile()} returns existing not directory file
   * @throws java.io.IOException
   */
  public abstract byte[] getBytes() throws IOException;

  /**
   * Creates DiffContent associated with given file.
   *
   * @param project
   * @param file
   * @return content associated with file
   */
  public static FileContent fromFile(Project project, VirtualFile file) {
    return file != null ? new FileContent(project, file) : null;
  }

  /**
   * Creates DiffContent associated with given document
   *
   * @param project
   * @param document
   * @return content associated with document
   */
  public static DocumentContent fromDocument(Project project, Document document) {
    return new DocumentContent(project, document);
  }

  /**
   * @return line separator used in this content, or null if it is unknown.
   */
  @Nullable
  public LineSeparator getLineSeparator() {
    return null;
  }

  public interface Listener {
    void contentInvalid();
  }
}
