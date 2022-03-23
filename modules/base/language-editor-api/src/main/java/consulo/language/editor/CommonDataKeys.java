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
package consulo.language.editor;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.codeEditor.EditorSettings;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

public interface CommonDataKeys {
  Key<Project> PROJECT = Project.KEY;
  Key<Module> MODULE = Module.KEY;
  Key<Editor> EDITOR = Editor.KEY;
  /**
   * This key can be used to obtain reference to host editor instance, in case {@link #EDITOR} key is referring to an injected editor.
   */
  Key<Editor> HOST_EDITOR = EditorKeys.HOST_EDITOR;
  /**
   * A key to retrieve caret instance (in host or injected editor, depending on context).
   */
  Key<Caret> CARET = Caret.KEY;
  /**
   * Returns Editor even if focus currently is in find bar
   */
  Key<Editor> EDITOR_EVEN_IF_INACTIVE = EditorKeys.EDITOR_EVEN_IF_INACTIVE;

  Key<Navigatable> NAVIGATABLE = Navigatable.KEY;
  Key<Navigatable[]> NAVIGATABLE_ARRAY = Navigatable.KEY_OF_ARRAY;
  Key<VirtualFile> VIRTUAL_FILE = VirtualFile.KEY;
  Key<VirtualFile[]> VIRTUAL_FILE_ARRAY = VirtualFile.KEY_OF_ARRAY;

  Key<PsiElement> PSI_ELEMENT = PsiElement.KEY;
  Key<PsiElement[]> PSI_ELEMENT_ARRAY = PsiElement.KEY_OF_ARRAY;
  Key<PsiFile> PSI_FILE = PsiFile.KEY;
  /**
   * This key can be used to check if the current context relates to a virtual space in editor.
   *
   * @see EditorSettings#setVirtualSpace(boolean)
   */
  Key<Boolean> EDITOR_VIRTUAL_SPACE = EditorKeys.EDITOR_VIRTUAL_SPACE;
}
