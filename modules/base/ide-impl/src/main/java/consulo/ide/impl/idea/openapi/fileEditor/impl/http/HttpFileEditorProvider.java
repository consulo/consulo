/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileEditor.impl.http;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorPolicy;
import consulo.fileEditor.FileEditorProvider;
import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.text.TextEditorState;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import consulo.ui.annotation.RequiredUIAccess;
import org.jdom.Element;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@ExtensionImpl
public class HttpFileEditorProvider implements FileEditorProvider, DumbAware {
  public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
    return file instanceof HttpVirtualFile && !file.isDirectory();
  }

  @RequiredUIAccess
  @Nonnull
  public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
    return new HttpFileEditor(project, (HttpVirtualFile)file); 
  }

  @Nonnull
  public FileEditorState readState(@Nonnull Element sourceElement, @Nonnull Project project, @Nonnull VirtualFile file) {
    return new TextEditorState();
  }

  public void writeState(@Nonnull FileEditorState state, @Nonnull Project project, @Nonnull Element targetElement) {
  }

  @Nonnull
  public String getEditorTypeId() {
    return "httpFileEditor";
  }

  @Nonnull
  public FileEditorPolicy getPolicy() {
    return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
  }
}
