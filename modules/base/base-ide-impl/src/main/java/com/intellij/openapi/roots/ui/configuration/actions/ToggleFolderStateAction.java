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
package com.intellij.openapi.roots.ui.configuration.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ui.configuration.ContentEntryEditor;
import com.intellij.openapi.roots.ui.configuration.ContentEntryTreeEditor;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.roots.ContentFolderTypeProvider;

import javax.swing.*;

/**
 * @author Eugene Zhuravlev
 * @since Oct 14, 2003
 */
public class ToggleFolderStateAction extends ContentEntryEditingAction {
  private final ContentEntryTreeEditor myEntryTreeEditor;
  private final ContentFolderTypeProvider myContentFolderType;

  public ToggleFolderStateAction(JTree tree, ContentEntryTreeEditor entryEditor, ContentFolderTypeProvider contentFolderType) {
    super(tree, contentFolderType.getName(), contentFolderType.getIcon());
    myEntryTreeEditor = entryEditor;
    myContentFolderType = contentFolderType;
  }

  @Override
  public boolean displayTextInToolbar() {
    return true;
  }

  @Override
  public boolean isSelected(final AnActionEvent e) {
    final VirtualFile[] selectedFiles = getSelectedFiles();
    if (selectedFiles.length == 0) return false;

    final ContentEntryEditor editor = myEntryTreeEditor.getContentEntryEditor();
    final ContentFolder folder = editor.getFolder(selectedFiles[0]);
    return folder != null && folder.getType() == myContentFolderType;
  }

  @Override
  public void setSelected(final AnActionEvent e, final boolean isSelected) {
    final VirtualFile[] selectedFiles = getSelectedFiles();
    assert selectedFiles.length != 0;

    final ContentEntryEditor contentEntryEditor = myEntryTreeEditor.getContentEntryEditor();
    for (VirtualFile selectedFile : selectedFiles) {
      final ContentFolder contentFolder = contentEntryEditor.getFolder(selectedFile);
      if (isSelected) {
        if (contentFolder == null) { // not marked yet
          contentEntryEditor.addFolder(selectedFile, myContentFolderType);
        }
        else {
          if (myContentFolderType.equals(contentFolder.getType())) {

            contentEntryEditor.removeFolder(contentFolder);
            contentEntryEditor.addFolder(selectedFile, myContentFolderType);
          }
        }
      }
      else {
        if (contentFolder != null) { // already marked
          contentEntryEditor.removeFolder(contentFolder);
        }
      }
    }
  }
}
