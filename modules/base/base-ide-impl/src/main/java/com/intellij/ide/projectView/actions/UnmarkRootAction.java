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

import com.intellij.icons.AllIcons;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.roots.ContentFolderScopes;

/**
 * @author yole
 */
public class UnmarkRootAction extends MarkRootAction {
  public UnmarkRootAction() {
    super(ActionsBundle.message("action.UnmarkRoot.text"), null, AllIcons.Actions.Cancel, null);
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(canUnmark(e));
  }

  public boolean canUnmark(AnActionEvent e) {
    Module module = e.getData(LangDataKeys.MODULE);
    VirtualFile[] vFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (module == null || vFiles == null) {
      return false;
    }
    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    final ContentEntry[] contentEntries = moduleRootManager.getContentEntries();

    for (VirtualFile vFile : vFiles) {
      if (!vFile.isDirectory()) {
        continue;
      }

      for (ContentEntry contentEntry : contentEntries) {
        for (ContentFolder contentFolder : contentEntry.getFolders(ContentFolderScopes.all())) {
          if (Comparing.equal(contentFolder.getFile(), vFile)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
