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
package consulo.ide.impl.execution.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
@ExtensionImpl
public class RunConfigurationFileEditorProvider implements FileEditorProvider, DumbAware {
  @Override
  public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
    return file instanceof RunConfigurationVirtualFile && !project.isDefault();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
    return new RunConfigurationFileEditor(project, file);
  }

  @Nonnull
  @Override
  public String getEditorTypeId() {
    return "run_configuration";
  }
}
