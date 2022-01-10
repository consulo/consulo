/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.ide.projectView.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ContentFolderTypeProvider;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
 */
public class MarkRootAction extends DumbAwareAction {
  @Nullable
  private final ContentFolderTypeProvider myContentFolderType;

  public MarkRootAction(@Nullable String text,
                           @Nullable String description,
                           @Nullable Image icon,
                           @Nullable ContentFolderTypeProvider contentFolderTypeProvider) {
    super(text, description, icon);
    myContentFolderType = contentFolderTypeProvider;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    Module module = e.getData(LangDataKeys.MODULE);
    VirtualFile[] vFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    assert vFiles != null;
    final ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
    for (VirtualFile vFile : vFiles) {
      ContentEntry entry = findContentEntry(model, vFile);
      if (entry != null) {
        final ContentFolder[] sourceFolders = entry.getFolders(ContentFolderScopes.all());
        for (ContentFolder sourceFolder : sourceFolders) {
          if (Comparing.equal(sourceFolder.getFile(), vFile)) {
            entry.removeFolder(sourceFolder);
            break;
          }
        }
        if (myContentFolderType != null) {
          entry.addFolder(vFile, myContentFolderType);
        }
      }
    }
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        model.commit();
      }
    });
  }

  @Nullable
  public static ContentEntry findContentEntry(@Nonnull ModuleRootModel model, @Nonnull VirtualFile vFile) {
    final ContentEntry[] contentEntries = model.getContentEntries();
    for (ContentEntry contentEntry : contentEntries) {
      final VirtualFile contentEntryFile = contentEntry.getFile();
      if (contentEntryFile != null && VfsUtilCore.isAncestor(contentEntryFile, vFile, false)) {
        return contentEntry;
      }
    }
    return null;
  }

  @Override
  public void update(AnActionEvent e) {
    boolean enabled = canMark(e);
    e.getPresentation().setVisible(enabled);
    e.getPresentation().setEnabled(enabled);
  }

  public boolean canMark(AnActionEvent e) {
    Module module = e.getData(LangDataKeys.MODULE);
    VirtualFile[] vFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (module == null || vFiles == null) {
      return false;
    }
    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    final ContentEntry[] contentEntries = moduleRootManager.getContentEntries();

    for (VirtualFile vFile : vFiles) {
      if (!vFile.isDirectory()) {
        return false;
      }

      for (ContentEntry contentEntry : contentEntries) {
        for (ContentFolder contentFolder : contentEntry.getFolders(ContentFolderScopes.all())) {
          if (Comparing.equal(contentFolder.getFile(), vFile)) {
            if (contentFolder.getType() == myContentFolderType) {
              return false;
            }
          }
        }
      }
    }
    return true;
  }
}
