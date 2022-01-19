/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.refactoring.ui;

import consulo.document.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import consulo.virtualFileSystem.fileType.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import consulo.awt.TargetAWT;

import javax.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class MethodSignatureComponent extends EditorTextField {
  public MethodSignatureComponent(String signature, Project project, FileType filetype) {
    this(EditorFactory.getInstance().createDocument(signature), project, filetype);
  }

  public MethodSignatureComponent(Document document, Project project, FileType filetype) {
    super(document, project, filetype, true, false);
    setFontInheritedFromLAF(false);
    setBackground(TargetAWT.to(EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.CARET_ROW_COLOR)));
  }

  public void setSignature(String signature) {
    setText(signature);
    final EditorEx editor = (EditorEx)getEditor();
    if (editor != null) {
      editor.getScrollingModel().scrollVertically(0);
      editor.getScrollingModel().scrollHorizontally(0);
    }
  }

  @Override
  protected EditorEx createEditor() {
    EditorEx editor = super.createEditor();
    final String fileName = getFileName();
    if (fileName != null) {
      editor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(getProject(), fileName));
    }
    editor.getSettings().setWhitespacesShown(false);
    editor.setHorizontalScrollbarVisible(true);
    editor.setVerticalScrollbarVisible(true);
    return editor;
  }

  @Nullable
  protected String getFileName() {
    return null;
  }
}