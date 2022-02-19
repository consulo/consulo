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
package com.intellij.openapi.fileEditor.impl;

import consulo.component.extension.ExtensionPointName;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import consulo.project.Project;
import com.intellij.openapi.util.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.fileEditor.impl.EditorWithProviderComposite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class EditorFileSwapper {
  public static final ExtensionPointName<EditorFileSwapper> EP_NAME = ExtensionPointName.create("consulo.editorFileSwapper");

  @Nullable
  public abstract Pair<VirtualFile, Integer> getFileToSwapTo(Project project, EditorWithProviderComposite editorWithProviderComposite);

  @Nullable
  public static TextEditor findSinglePsiAwareEditor(@Nonnull FileEditor[] fileEditors) {
    TextEditor res = null;

    for (FileEditor fileEditor : fileEditors) {
      if (fileEditor instanceof TextEditorImpl) {
        if (res == null) {
          res = (TextEditor)fileEditor;
        }
        else {
          return null;
        }
      }
    }

    return res;
  }
}
