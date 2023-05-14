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
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.annotation.DeprecationInfo;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.fileEditor.FileEditorWithProvider;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.logging.Logger;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

/**
 * Author: msk
 */
@Deprecated
@DeprecationInfo("Desktop only")
public class DesktopFileEditorWithProviderComposite extends DesktopEditorComposite implements FileEditorWithProviderComposite {
  private static final Logger LOG = Logger.getInstance(DesktopFileEditorWithProviderComposite.class);
  private FileEditorProvider[] myProviders;

  public DesktopFileEditorWithProviderComposite(@Nonnull VirtualFile file, @Nonnull FileEditor[] editors, @Nonnull FileEditorProvider[] providers, @Nonnull FileEditorManagerEx fileEditorManager) {
    super(file, editors, fileEditorManager);
    myProviders = providers;
  }

  @Override
  @Nonnull
  public FileEditorProvider[] getProviders() {
    return myProviders;
  }

  @Override
  @Nonnull
  public FileEditorWithProvider getSelectedEditorWithProvider() {
    LOG.assertTrue(myEditors.length > 0, myEditors.length);
    if (myEditors.length == 1) {
      LOG.assertTrue(myTabbedPaneWrapper == null);
      return new FileEditorWithProvider(myEditors[0], myProviders[0]);
    }
    else { // we have to get myEditor from tabbed pane
      LOG.assertTrue(myTabbedPaneWrapper != null);
      int index = myTabbedPaneWrapper.getSelectedIndex();
      if (index == -1) {
        index = 0;
      }
      LOG.assertTrue(index >= 0, index);
      LOG.assertTrue(index < myEditors.length, index);
      return new FileEditorWithProvider(myEditors[index], myProviders[index]);
    }
  }

  @Override
  public void addEditor(@Nonnull FileEditor editor, FileEditorProvider provider) {
    addEditor(editor);
    myProviders = ArrayUtil.append(myProviders, provider);
  }
}
