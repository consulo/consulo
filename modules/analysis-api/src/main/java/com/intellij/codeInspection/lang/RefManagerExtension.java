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

/*
 * User: anna
 * Date: 20-Dec-2007
 */
package com.intellij.codeInspection.lang;

import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefVisitor;
import com.intellij.lang.Language;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

public interface RefManagerExtension<T> {
  Key<T> getID();

  Language getLanguage();

  void iterate(RefVisitor visitor);

  void cleanup();

  void removeReference(RefElement refElement);

  @Nullable
  RefElement createRefElement(PsiElement psiElement);

  @Nullable
  RefEntity getReference(final String type, final String fqName);

  @Nullable
  String getType(RefEntity entity);

  RefEntity getRefinedElement(final RefEntity ref);

  void visitElement(final PsiElement element);

  @Nullable
  String getGroupName(final RefEntity entity);

  boolean belongsToScope(final PsiElement psiElement);

  void export(final RefEntity refEntity, final Element element);

  void onEntityInitialized(RefElement refEntity, PsiElement psiElement);
}
