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
package consulo.ide.impl.idea.ide.projectView.actions;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.ApplicationManager;
import consulo.module.Module;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleRootModel;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.content.ContentFolderTypeProvider;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public class MarkRootAction extends DumbAwareAction {
  @Nullable
  private final ContentFolderTypeProvider myContentFolderType;

  public MarkRootAction(
    @Nonnull LocalizeValue text,
    @Nonnull LocalizeValue description,
    @Nullable Image icon,
    @Nullable ContentFolderTypeProvider contentFolderTypeProvider
  ) {
    super(text, description, icon);
    myContentFolderType = contentFolderTypeProvider;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    Module module = e.getData(Module.KEY);
    VirtualFile[] vFiles = e.getData(VirtualFile.KEY_OF_ARRAY);
    assert vFiles != null;
    final ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
    for (VirtualFile vFile : vFiles) {
      ContentEntry entry = findContentEntry(model, vFile);
      if (entry != null) {
        final ContentFolder[] sourceFolders = entry.getFolders(LanguageContentFolderScopes.all());
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
    ApplicationManager.getApplication().runWriteAction(() -> model.commit());
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
    Module module = e.getData(Module.KEY);
    VirtualFile[] vFiles = e.getData(VirtualFile.KEY_OF_ARRAY);
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
        for (ContentFolder contentFolder : contentEntry.getFolders(LanguageContentFolderScopes.all())) {
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
