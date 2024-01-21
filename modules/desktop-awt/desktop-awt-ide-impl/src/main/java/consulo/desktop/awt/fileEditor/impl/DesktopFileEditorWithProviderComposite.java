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
package consulo.desktop.awt.fileEditor.impl;

import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.logging.Logger;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * Author: msk
 */
public class DesktopFileEditorWithProviderComposite extends DesktopEditorComposite implements FileEditorWithProviderComposite {
  private static final Logger LOG = Logger.getInstance(DesktopFileEditorWithProviderComposite.class);

  public DesktopFileEditorWithProviderComposite(@Nonnull VirtualFile file,
                                                @Nonnull FileEditor[] editors,
                                                @Nonnull FileEditorManagerEx fileEditorManager) {
    super(file, editors, fileEditorManager);
  }

  @Override
  @Nonnull
  public FileEditor getSelectedEditor() {
    LOG.assertTrue(myEditors.length > 0, myEditors.length);
    if (myEditors.length == 1) {
      LOG.assertTrue(myTabbedPaneWrapper == null);
      return myEditors[0];
    }
    else { // we have to get myEditor from tabbed pane
      LOG.assertTrue(myTabbedPaneWrapper != null);
      int index = myTabbedPaneWrapper.getSelectedIndex();
      if (index == -1) {
        index = 0;
      }
      LOG.assertTrue(index >= 0, index);
      LOG.assertTrue(index < myEditors.length, index);
      return myEditors[index];
    }
  }
}
