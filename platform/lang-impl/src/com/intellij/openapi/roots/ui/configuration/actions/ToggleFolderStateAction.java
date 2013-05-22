/*
 * Copyright 2013 Consulo.org
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
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ui.configuration.ContentEntryEditor;
import com.intellij.openapi.roots.ui.configuration.ContentEntryTreeEditor;
import com.intellij.openapi.roots.ui.configuration.ContentFolderIconUtil;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;

/**
 * @author Eugene Zhuravlev
 * @since Oct 14, 2003
 */
public class ToggleFolderStateAction extends ContentEntryEditingAction {
  private final ContentEntryTreeEditor myEntryTreeEditor;
  private final ContentFolder.ContentFolderType myContentFolderType;

  public ToggleFolderStateAction(JTree tree, ContentEntryTreeEditor entryEditor, ContentFolder.ContentFolderType contentFolderType) {
    super(tree);
    myEntryTreeEditor = entryEditor;
    myContentFolderType = contentFolderType;

    final Presentation templatePresentation = getTemplatePresentation();
    switch (contentFolderType) {

      case SOURCE:
        templatePresentation.setText(ProjectBundle.message("module.toggle.sources.action"));
        templatePresentation.setDescription(ProjectBundle.message("module.toggle.sources.action.description"));
        break;
      case TEST:
        templatePresentation.setText(ProjectBundle.message("module.toggle.test.sources.action"));
        templatePresentation.setDescription(ProjectBundle.message("module.toggle.test.sources.action.description"));
        break;
      case RESOURCE:
        templatePresentation.setText(ProjectBundle.message("module.toggle.resources.action"));
        templatePresentation.setDescription(ProjectBundle.message("module.toggle.resources.action.description"));
        break;
      case EXCLUDED:
      case EXCLUDED_OUTPUT:
        templatePresentation.setText(ProjectBundle.message("module.toggle.excluded.action"));
        templatePresentation.setDescription(ProjectBundle.message("module.toggle.excluded.action.description"));
        break;
    }
    templatePresentation.setIcon(ContentFolderIconUtil.getRootIcon(contentFolderType));
  }

  @Override
  public boolean isSelected(final AnActionEvent e) {
    final VirtualFile[] selectedFiles = getSelectedFiles();
    if (selectedFiles.length == 0) return false;

    final ContentEntryEditor editor = myEntryTreeEditor.getContentEntryEditor();
    return editor.getFolder(selectedFiles[0], myContentFolderType) != null;
  }

  @Override
  public void setSelected(final AnActionEvent e, final boolean isSelected) {
    final VirtualFile[] selectedFiles = getSelectedFiles();
    assert selectedFiles.length != 0;

    final ContentEntryEditor contentEntryEditor = myEntryTreeEditor.getContentEntryEditor();
    for (VirtualFile selectedFile : selectedFiles) {
      final ContentFolder contentFolder = contentEntryEditor.getFolder(selectedFile, myContentFolderType);
      if (isSelected) {
        if (contentFolder == null) { // not marked yet
          contentEntryEditor.addFolder(selectedFile, myContentFolderType);
        }
        else {
          if (myContentFolderType != contentFolder.getType()) {

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
