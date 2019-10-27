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
package com.intellij.openapi.fileEditor.impl;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.fileEditor.impl.EditorWindow;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;

/**
 * @author yole
 */
public interface EditorTabTitleProvider {
  ExtensionPointName<EditorTabTitleProvider> EP_NAME = ExtensionPointName.create("com.intellij.editorTabTitleProvider");

  @Nullable
  String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file);

  @Nullable
  default String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file, @Nullable EditorWindow editorWindow) {
    return getEditorTabTitle(project, file);
  }

  @Nullable
  default String getEditorTabTooltipText(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
    return null;
  }
}
