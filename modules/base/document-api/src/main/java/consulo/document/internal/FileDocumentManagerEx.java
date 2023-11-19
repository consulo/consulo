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
package consulo.document.internal;

import consulo.annotation.access.RequiredWriteAction;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23-Mar-22
 */
public interface FileDocumentManagerEx extends FileDocumentManager {
  void registerDocument(@Nonnull final Document document, @Nonnull VirtualFile virtualFile);

  @Override
  @RequiredWriteAction
  default void saveAllDocuments() {
    saveAllDocuments(true);
  }

  /**
   * @param isExplicit caused by user directly (Save action) or indirectly (e.g. Compile)
   */
  @RequiredWriteAction
   void saveAllDocuments(boolean isExplicit);
}
