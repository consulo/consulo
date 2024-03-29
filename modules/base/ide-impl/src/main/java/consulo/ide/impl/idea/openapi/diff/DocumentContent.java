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

import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.LineSeparator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class DocumentContent extends DiffContent {
  private final Document myDocument;
  private final VirtualFile myFile;
  private final FileType myOverridenType;
  private Project myProject;
  private FileDocumentManager myDocumentManager;

  public DocumentContent(Project project, Document document) {
    this(project, document, null);
  }

  public DocumentContent(Project project, @Nonnull Document document, FileType type) {
    myProject = project;
    myDocument = document;
    myDocumentManager = FileDocumentManager.getInstance();
    myFile = myDocumentManager.getFile(document);
    myOverridenType = type;
  }

  public DocumentContent(Document document) {
    this(null, document, null);
  }

  public DocumentContent(Document document, FileType type) {
    this(null, document, type);
  }

  public Document getDocument() {
    return myDocument;
  }

  public OpenFileDescriptorImpl getOpenFileDescriptor(int offset) {
    VirtualFile file = getFile();
    if (file == null) return null;
    if (myProject == null) return null;
    return new OpenFileDescriptorImpl(myProject, file, offset);
  }

  public VirtualFile getFile() {
    return myFile;
  }

  @Nullable
  public FileType getContentType() {
    return myOverridenType == null ? DiffContentUtil.getContentType(getFile()) : myOverridenType;
  }

  public byte[] getBytes() {
    return myDocument.getText().getBytes();
  }

  @Nonnull
  @Override
  public LineSeparator getLineSeparator() {
    return LineSeparator.fromString(myDocumentManager.getLineSeparator(myFile, myProject));
  }
}
