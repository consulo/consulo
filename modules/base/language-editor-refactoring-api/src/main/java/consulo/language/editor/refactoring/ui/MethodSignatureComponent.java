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
package consulo.language.editor.refactoring.ui;

import consulo.document.Document;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.codeEditor.EditorEx;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nullable;

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
    EditorEx editor = (EditorEx)getEditor();
    if (editor != null) {
      editor.getScrollingModel().scrollVertically(0);
      editor.getScrollingModel().scrollHorizontally(0);
    }
  }

  @Override
  protected EditorEx createEditor() {
    EditorEx editor = super.createEditor();
    String fileName = getFileName();
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