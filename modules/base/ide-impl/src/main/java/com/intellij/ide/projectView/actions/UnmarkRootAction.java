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

import consulo.application.AllIcons;
import consulo.ui.ex.action.ActionsBundle;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.LangDataKeys;
import consulo.module.Module;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.ModuleRootManager;
import com.intellij.openapi.util.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.content.LanguageContentFolderScopes;

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
        for (ContentFolder contentFolder : contentEntry.getFolders(LanguageContentFolderScopes.all())) {
          if (Comparing.equal(contentFolder.getFile(), vFile)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
