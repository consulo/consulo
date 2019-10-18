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

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.WeakFactoryMap;
import consulo.ui.RequiredUIAccess;
import consulo.annotations.RequiredReadAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.Reference;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightFileDocumentManager extends FileDocumentManager {
  private static final Key<VirtualFile> MOCK_VIRTUAL_FILE_KEY = Key.create("MockVirtualFile");
  private final Function<CharSequence, Document> myFactory;
  @Nullable
  private final Key<Reference<Document>> myCachedDocumentKey;

  public LightFileDocumentManager() {
    myFactory = DocumentImpl::new;
    myCachedDocumentKey = null;
  }

  private final WeakFactoryMap<VirtualFile, Document> myDocuments = new WeakFactoryMap<VirtualFile, Document>() {
    @Override
    protected Document create(final VirtualFile key) {
      if (key.isDirectory() || isBinaryWithoutDecompiler(key)) return null;

      CharSequence text = LoadTextUtil.loadText(key);
      final Document document = myFactory.apply(text);
      document.putUserData(MOCK_VIRTUAL_FILE_KEY, key);
      return document;
    }

    private boolean isBinaryWithoutDecompiler(VirtualFile file) {
      final FileType ft = file.getFileType();
      return ft.isBinary() && BinaryFileTypeDecompilers.INSTANCE.forFileType(ft) == null;
    }
  };

  @RequiredReadAction
  @Override
  public Document getDocument(@Nonnull VirtualFile file) {
    return myDocuments.get(file);
  }

  @Override
  public Document getCachedDocument(@Nonnull VirtualFile file) {
    if (myCachedDocumentKey != null) {
      Reference<Document> reference = file.getUserData(myCachedDocumentKey);
      return reference != null ? reference.get() : null;
    }
    return null;
  }

  @Override
  public VirtualFile getFile(@Nonnull Document document) {
    return document.getUserData(MOCK_VIRTUAL_FILE_KEY);
  }

  @RequiredUIAccess
  @Override
  public void saveAllDocuments() {
  }

  @RequiredUIAccess
  @Override
  public void saveDocument(@Nonnull Document document) {
  }

  @RequiredUIAccess
  @Override
  public void saveDocumentAsIs(@Nonnull Document document) {
  }

  @Override
  @Nonnull
  public Document[] getUnsavedDocuments() {
    return new Document[0];
  }

  @Override
  public boolean isDocumentUnsaved(@Nonnull Document document) {
    return false;
  }

  @Override
  public boolean isFileModified(@Nonnull VirtualFile file) {
    return false;
  }

  @RequiredUIAccess
  @Override
  public void reloadFromDisk(@Nonnull Document document) {
  }

  @RequiredUIAccess
  @Override
  public void reloadFiles(final VirtualFile... files) {
  }

  @Override
  @Nonnull
  public String getLineSeparator(VirtualFile file, Project project) {
    return "";
  }

  @Override
  public boolean requestWriting(@Nonnull Document document, @Nullable Project project) {
    return true;
  }
}
