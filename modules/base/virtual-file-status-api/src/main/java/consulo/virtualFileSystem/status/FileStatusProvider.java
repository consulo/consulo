/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.virtualFileSystem.status;

import consulo.component.extension.ExtensionPointName;
import consulo.document.Document;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
public interface FileStatusProvider {

  ExtensionPointName<FileStatusProvider> EP_NAME = ExtensionPointName.create("consulo.vcs.fileStatusProvider");

  FileStatus getFileStatus(@Nonnull VirtualFile virtualFile);

  void refreshFileStatusFromDocument(@Nonnull VirtualFile virtualFile, @Nonnull Document doc);

  @Nonnull
  ThreeState getNotChangedDirectoryParentingStatus(@Nonnull VirtualFile virtualFile);
}