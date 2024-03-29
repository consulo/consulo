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
package consulo.language.editor.inspection.reference;

import consulo.language.psi.PsiFile;

/**
 * A node in the reference graph corresponding to a file.
 *
 * @author anna
 */
public interface RefFile extends RefElement {
  /**
   * Returns the file to which the node corresponds.
   *
   * @return the file for the node.
   */
  @Override
  default PsiFile getPsiElement() {
    return getElement();
  }

  @Deprecated
  @Override
  default PsiFile getElement() {
    throw new UnsupportedOperationException();
  }
}
