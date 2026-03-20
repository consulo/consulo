/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.editor.inject;

import consulo.codeEditor.Editor;
import consulo.codeEditor.InjectedEditor;
import consulo.codeEditor.LogicalPosition;
import consulo.document.DocumentWindow;
import consulo.language.psi.PsiFile;
import consulo.util.dataholder.UserDataHolderEx;

public interface EditorWindow extends UserDataHolderEx, InjectedEditor {
  
  static Editor getTopLevelEditor(Editor editor) {
    return editor instanceof EditorWindow ? ((EditorWindow)editor).getDelegate() : editor;
  }

  boolean isValid();

  
  PsiFile getInjectedFile();

  
  LogicalPosition hostToInjected(LogicalPosition hPos);

  
  LogicalPosition injectedToHost(LogicalPosition pos);

  
  Editor getDelegate();

  
  @Override
  DocumentWindow getDocument();
}
