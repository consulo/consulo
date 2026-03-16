/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.component.ComponentManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.impl.DocumentImpl;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import consulo.util.dataholder.Key;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.BinaryFileDecompiler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.LIGHT_TEST)
public class LightFileDocumentManager implements FileDocumentManager {
  private static final Key<VirtualFile> MOCK_VIRTUAL_FILE_KEY = Key.create("MockVirtualFile");
  private final Function<? super CharSequence, ? extends Document> myFactory;
  @Nullable
  private final Key<Document> myCachedDocumentKey;

  public LightFileDocumentManager() {
    myFactory = DocumentImpl::new;
    myCachedDocumentKey = null;
  }

  private static final Key<Document> MOCK_DOC_KEY = Key.create("MOCK_DOC_KEY");

  private static boolean isBinaryWithoutDecompiler(VirtualFile file) {
    FileType ft = file.getFileType();
    return ft.isBinary() && BinaryFileDecompiler.forFileType(ft) == null;
  }

  @Override
  public Document getDocument(VirtualFile file) {
    Document document = file.getUserData(MOCK_DOC_KEY);
    if (document == null) {
      if (file.isDirectory() || isBinaryWithoutDecompiler(file)) return null;

      CharSequence text = LoadTextUtil.loadText(file);
      document = myFactory.apply(text);
      document.putUserData(MOCK_VIRTUAL_FILE_KEY, file);
      document = file.putUserDataIfAbsent(MOCK_DOC_KEY, document);
    }
    return document;
  }

  @Override
  public Document getCachedDocument(VirtualFile file) {
    if (myCachedDocumentKey != null) {
      return file.getUserData(myCachedDocumentKey);
    }
    return null;
  }

  @Override
  public VirtualFile getFile(Document document) {
    return document.getUserData(MOCK_VIRTUAL_FILE_KEY);
  }

  @Override
  public void saveAllDocuments(UIAccess uiAccess) {
  }

  @Override
  public void saveDocument(Document document) {
  }

  @Override
  public void saveDocument(Document document, boolean isExplicit) {
  }

  @Override
  public void saveDocumentAsIs(Document document) {
  }

  @Override
  
  public Document[] getUnsavedDocuments() {
    return Document.EMPTY_ARRAY;
  }

  @Override
  public boolean isDocumentUnsaved(Document document) {
    return false;
  }

  @Override
  public boolean isFileModified(VirtualFile file) {
    return false;
  }

  @Override
  public boolean isPartialPreviewOfALargeFile(Document document) {
    return false;
  }

  @Override
  public void reloadFromDisk(Document document) {
  }

  @Override
  public void reloadFiles(VirtualFile... files) {
  }

  @Override
  
  public String getLineSeparator(VirtualFile file, ComponentManager project) {
    return LoadTextUtil.getDetectedLineSeparator(file);
  }

  @Override
  public boolean requestWriting(Document document, @Nullable ComponentManager project) {
    return true;
  }
}
