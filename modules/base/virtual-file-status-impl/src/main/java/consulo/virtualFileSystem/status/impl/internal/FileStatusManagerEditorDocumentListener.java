/*
 * Copyright 2013-2022 consulo.io
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
package consulo.virtualFileSystem.status.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.event.EditorDocumentListener;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 21-Jun-22
 */
@ExtensionImpl
public class FileStatusManagerEditorDocumentListener implements EditorDocumentListener {
  private final Provider<FileStatusManager> myFileStatusManagerProvider;

  @Inject
  public FileStatusManagerEditorDocumentListener(Provider<FileStatusManager> fileStatusManagerProvider) {
    myFileStatusManagerProvider = fileStatusManagerProvider;
  }

  @Override
  public void documentChanged(DocumentEvent event) {
    FileStatusManagerImpl fileStatusManager = (FileStatusManagerImpl)myFileStatusManagerProvider.get();

    if (event.getOldLength() == 0 && event.getNewLength() == 0) return;
    VirtualFile file = FileDocumentManager.getInstance().getFile(event.getDocument());
    if (file != null) {
      fileStatusManager.refreshFileStatusFromDocument(file, event.getDocument());
    }
  }
}
