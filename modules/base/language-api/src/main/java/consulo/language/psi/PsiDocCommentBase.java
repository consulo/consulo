/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.psi;

import jakarta.annotation.Nullable;

/**
 * Implementers of this interface may provide their own way of finding an associated doc comment owner element
 * which may be essential for languages other than Java.
 *
 * @author Rustam Vishnyakov
 */
public interface PsiDocCommentBase extends PsiComment {
  /**
   * @return The element which this doc comment refers to or null if there is no such element.
   */
  @Nullable
  PsiElement getOwner();
}
