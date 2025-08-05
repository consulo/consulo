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

package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataProvider;
import consulo.language.editor.PlatformDataKeys;
import consulo.dataContext.GetDataRule;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author mike
 */
@ExtensionImpl
public class FileTextRule implements GetDataRule<String> {
  @Nonnull
  @Override
  public Key<String> getKey() {
    return PlatformDataKeys.FILE_TEXT;
  }

  @Override
  public String getData(@Nonnull DataProvider dataProvider) {
    VirtualFile virtualFile = dataProvider.getDataUnchecked(VirtualFile.KEY);
    if (virtualFile == null) {
      return null;
    }

    FileType fileType = virtualFile.getFileType();
    if (fileType.isBinary() || fileType.isReadOnly()) {
      return null;
    }

    Project project = dataProvider.getDataUnchecked(Project.KEY);
    if (project == null) {
      return null;
    }

    Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
    if (document == null) {
      return null;
    }

    return document.getText();
  }
}
