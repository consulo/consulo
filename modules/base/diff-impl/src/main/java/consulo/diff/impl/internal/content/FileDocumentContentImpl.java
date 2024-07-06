/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.content;

import consulo.application.SaveAndSyncHandler;
import consulo.diff.content.FileContent;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.util.LineCol;
import consulo.document.Document;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.platform.LineSeparator;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class FileDocumentContentImpl extends DocumentContentImpl implements FileContent {
  @Nonnull
  private final VirtualFile myFile;

  public FileDocumentContentImpl(@Nullable Project project,
                                 @Nonnull Document document,
                                 @Nonnull VirtualFile file) {
    super(project, document, file.getFileType(), file, getSeparator(file), file.getCharset(), file.getBOM() != null);
    myFile = file;
  }

  @Nullable
  @Override
  public Navigatable getNavigatable(@Nonnull LineCol position) {
    Project project = getProject();
    if (project == null || project.isDefault() || !myFile.isValid()) return null;
    return OpenFileDescriptorFactory.getInstance(project)
                                    .newBuilder(myFile)
                                    .line(position.line)
                                    .column(position.column)
                                    .build();
  }

  @Nullable
  private static LineSeparator getSeparator(@Nonnull VirtualFile file) {
    String s = LoadTextUtil.detectLineSeparator(file, true);
    if (s == null) return null;
    return LineSeparator.fromString(s);
  }

  @Nonnull
  @Override
  public VirtualFile getFile() {
    return myFile;
  }

  @Override
  public void onAssigned(boolean isAssigned) {
    if (isAssigned && SaveAndSyncHandler.getInstance().isSyncOnFrameActivation()) DiffImplUtil.markDirtyAndRefresh(true, false, false, myFile);
  }
}
