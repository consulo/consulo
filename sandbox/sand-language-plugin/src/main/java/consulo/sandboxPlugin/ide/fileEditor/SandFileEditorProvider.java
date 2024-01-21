/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.ide.fileEditor;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorPolicy;
import consulo.fileEditor.FileEditorProvider;
import consulo.fileEditor.FileEditorState;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Element;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 30.05.14
 */
@ExtensionImpl
public class SandFileEditorProvider implements FileEditorProvider {
  @Override
  public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
    return file.getFileType() == SandFileType.INSTANCE;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
    return new SandFileEditor(this);
  }

  @Override
  public void disposeEditor(@Nonnull FileEditor editor) {

  }

  @Nonnull
  @Override
  public FileEditorState readState(@Nonnull Element sourceElement, @Nonnull Project project, @Nonnull VirtualFile file) {
    return FileEditorState.INSTANCE;
  }

  @Override
  public void writeState(@Nonnull FileEditorState state, @Nonnull Project project, @Nonnull Element targetElement) {

  }

  @Nonnull
  @Override
  public String getEditorTypeId() {
    return "sand-editor";
  }

  @Nonnull
  @Override
  public FileEditorPolicy getPolicy() {
    return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
  }
}
