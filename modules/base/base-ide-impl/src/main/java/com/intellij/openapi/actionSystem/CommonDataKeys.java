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
package com.intellij.openapi.actionSystem;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public interface CommonDataKeys {
  Key<Project> PROJECT = Key.create("project");
  Key<Module> MODULE = Key.create("module");
  Key<Editor> EDITOR = Key.create("editor");
  /**
   * This key can be used to obtain reference to host editor instance, in case {@link #EDITOR} key is referring to an injected editor.
   */
  Key<Editor> HOST_EDITOR = Key.create("host.editor");
  /**
   * A key to retrieve caret instance (in host or injected editor, depending on context).
   */
  Key<Caret> CARET = Key.create("caret");
  /**
   * Returns com.intellij.openapi.editor.Editor even if focus currently is in find bar
   */
  Key<Editor> EDITOR_EVEN_IF_INACTIVE = Key.create("editor.even.if.inactive");
  Key<Navigatable> NAVIGATABLE = Key.create("Navigatable");
  Key<Navigatable[]> NAVIGATABLE_ARRAY = Key.create("NavigatableArray");
  Key<VirtualFile> VIRTUAL_FILE = Key.create("virtualFile");
  Key<VirtualFile[]> VIRTUAL_FILE_ARRAY = Key.create("virtualFileArray");

  Key<PsiElement> PSI_ELEMENT = Key.create("psi.Element");
  Key<PsiElement[]> PSI_ELEMENT_ARRAY = Key.create("psi.Element.array");
  Key<PsiFile> PSI_FILE = Key.create("psi.File");
  /**
   * This key can be used to check if the current context relates to a virtual space in editor.
   * @see com.intellij.openapi.editor.EditorSettings#setVirtualSpace(boolean)
   */
  Key<Boolean> EDITOR_VIRTUAL_SPACE = Key.create("editor.virtual.space");
}
