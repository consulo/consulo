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

package consulo.language.psi;

import jakarta.annotation.Nullable;

/**
 * <tt>PsiQualifiedNamedElement</tt> interface marks psi elements that can have
 * fully qualified name and defines parent-child like relationship between such
 * elements.
 * <p>
 * Implementations of <tt>PsiClass</tt>, <tt>PsiPackage</tt> and <tt>PsiAnnotation</tt>
 * for Java all implement <tt>PsiQualifiedNamedElement</tt> interface
 *
 * @author Konstantin Bulenkov
 * @since 9.0
 */
public interface PsiQualifiedNamedElement {
  /**
   * Returns the fully qualified name of the element.
   *
   * @return the qualified name of the element, or null
   */
  @Nullable
  String getQualifiedName();

  /**
   * Returns the name of the element.
   *
   * @return the element name
   */
  @Nullable
  String getName();
}
