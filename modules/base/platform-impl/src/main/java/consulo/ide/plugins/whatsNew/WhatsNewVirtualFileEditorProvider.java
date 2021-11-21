/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.plugins.whatsNew;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ide.updateSettings.impl.UpdateHistory;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 15/11/2021
 */
public class WhatsNewVirtualFileEditorProvider implements FileEditorProvider, DumbAware {
  private final Provider<UpdateHistory> myUpdateHistoryProvider;

  @Inject
  public WhatsNewVirtualFileEditorProvider(Provider<UpdateHistory> updateHistoryProvider) {
    myUpdateHistoryProvider = updateHistoryProvider;
  }

  @Override
  public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
    return file instanceof WhatsNewVirtualFile;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
    return new WhatsNewVirtualFileEditor(project, myUpdateHistoryProvider.get(), file);
  }

  @Nonnull
  @Override
  public String getEditorTypeId() {
    return "whats_new";
  }
}
