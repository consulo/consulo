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
package consulo.ide.impl.idea.diff.requests;

import consulo.ide.impl.idea.diff.DiffContext;
import consulo.ide.impl.idea.diff.DiffContextEx;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.file.FileTypeManager;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.ide.impl.idea.openapi.fileTypes.ex.FileTypeChooser;
import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.LinkMouseListenerBase;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public class UnknownFileTypeDiffRequest extends ComponentDiffRequest {
  @jakarta.annotation.Nullable
  private final String myFileName;
  @jakarta.annotation.Nullable
  private final String myTitle;

  public UnknownFileTypeDiffRequest(@Nonnull VirtualFile file, @jakarta.annotation.Nullable String title) {
    this(file.getName(), title);
  }

  public UnknownFileTypeDiffRequest(@Nonnull String fileName, @Nullable String title) {
    boolean knownFileType = FileTypeManager.getInstance().getFileTypeByFileName(fileName) != UnknownFileType.INSTANCE;
    myFileName = knownFileType ? null : fileName;
    myTitle = title;
  }

  @Nonnull
  @Override
  public JComponent getComponent(@Nonnull final DiffContext context) {
    final SimpleColoredComponent label = new SimpleColoredComponent();
    label.setTextAlign(SwingConstants.CENTER);
    label.append("Can't show diff for unknown file type. ",
                 new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UIUtil.getInactiveTextColor()));
    if (myFileName != null) {
      label.append("Associate", SimpleTextAttributes.LINK_ATTRIBUTES, new Runnable() {
        @Override
        public void run() {
          FileType type = FileTypeChooser.associateFileType(myFileName);
          if (type != null) onSuccess(context);
        }
      });
      LinkMouseListenerBase.installSingleTagOn(label);
    }
    return JBUI.Panels.simplePanel(label).withBorder(JBUI.Borders.empty(5));
  }

  @jakarta.annotation.Nullable
  public String getFileName() {
    return myFileName;
  }

  @jakarta.annotation.Nullable
  @Override
  public String getTitle() {
    return myTitle;
  }

  protected void onSuccess(@Nonnull DiffContext context) {
    if (context instanceof DiffContextEx) ((DiffContextEx)context).reloadDiffRequest();
  }
}
