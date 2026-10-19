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
package consulo.diff.request;

import consulo.diff.DiffContext;
import consulo.diff.DiffContextEx;
import consulo.diff.localize.DiffLocalize;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.style.ComponentColors;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import org.jspecify.annotations.Nullable;


public class UnknownFileTypeDiffRequest extends ComponentDiffRequest {
  private final @Nullable String myFileName;
  private final @Nullable String myTitle;

  public UnknownFileTypeDiffRequest(VirtualFile file, @Nullable String title) {
    this(file.getName(), title);
  }

  public UnknownFileTypeDiffRequest(String fileName, @Nullable String title) {
    boolean knownFileType = FileTypeRegistry.getInstance().getFileTypeByFileName(fileName) != UnknownFileType.INSTANCE;
    myFileName = knownFileType ? null : fileName;
    myTitle = title;
  }

  
  @Override
  @RequiredUIAccess
  public Component getComponent(DiffContext context) {
    HorizontalLayout row = HorizontalLayout.create();
    Label label = Label.create(DiffLocalize.diffCanNotShowUnknown());
    label.setForegroundColor(ComponentColors.DISABLED_TEXT);
    row.add(label);
    if (myFileName != null) {
      row.add(Hyperlink.create(DiffLocalize.diffAssociateFileType(), event -> {
        FileType type = FileTypeRegistry.getInstance().getKnownFileTypeOrAssociate(myFileName);
        if (type != null) {
          onSuccess(context);
        }
      }));
    }
    return DockLayout.create().center(row);
  }

  public @Nullable String getFileName() {
    return myFileName;
  }

  @Override
  public @Nullable String getTitle() {
    return myTitle;
  }

  protected void onSuccess(DiffContext context) {
    if (context instanceof DiffContextEx) ((DiffContextEx)context).reloadDiffRequest();
  }
}
