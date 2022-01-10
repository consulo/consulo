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
package com.intellij.execution;

import consulo.logging.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import consulo.annotation.access.RequiredReadAction;
import javax.annotation.Nonnull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class PsiLocation<E extends PsiElement> extends Location<E> {
  private static final Logger LOG = Logger.getInstance(PsiLocation.class);
  private final E myPsiElement;
  private final Project myProject;
  private final Module myModule;

  @RequiredReadAction
  public PsiLocation(E psiElement) {
    this(psiElement.getProject(), psiElement);
  }

  @RequiredReadAction
  public PsiLocation(@Nonnull final Project project, @Nonnull final E psiElement) {
    myPsiElement = psiElement;
    myProject = project;
    myModule = ModuleUtilCore.findModuleForPsiElement(psiElement);
  }

  public PsiLocation(@Nonnull Project project, Module module, @Nonnull E psiElement) {
    myPsiElement = psiElement;
    myProject = project;
    myModule = module;
  }

  @Override
  @Nonnull
  public E getPsiElement() {
    return myPsiElement;
  }

  @Override
  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Override
  public Module getModule() {
    return myModule;
  }

  @Override
  @Nonnull
  public <T extends PsiElement> Iterator<Location<T>> getAncestors(@Nonnull final Class<T> ancestorClass, final boolean strict) {
    final T first = strict || !ancestorClass.isInstance(myPsiElement) ? findNext(myPsiElement, ancestorClass) : (T)myPsiElement;
    return new Iterator<Location<T>>() {
      private T myCurrent = first;
      @Override
      public boolean hasNext() {
        return myCurrent != null;
      }

      @Override
      public Location<T> next() {
        if (myCurrent == null) throw new NoSuchElementException();
        final PsiLocation<T> psiLocation = new PsiLocation<>(myProject, myCurrent);
        myCurrent = findNext(myCurrent, ancestorClass);
        return psiLocation;
      }

      @Override
      public void remove() {
        LOG.assertTrue(false);
      }
    };
  }

  @Override
  @Nonnull
  public PsiLocation<E> toPsiLocation() {
    return this;
  }

  private static <ElementClass extends PsiElement> ElementClass findNext(final PsiElement psiElement, final Class<ElementClass> ancestorClass) {
    PsiElement element = psiElement;
    while ((element = element.getParent()) != null && !(element instanceof PsiFile)) {
      final ElementClass ancestor = Location.safeCast(element, ancestorClass);
      if (ancestor != null) return ancestor;
    }
    return null;
  }

  public static <T extends PsiElement> Location<T> fromPsiElement(@Nonnull Project project, final T element) {
    if (element == null) return null;
    return new PsiLocation<>(project, element);
  }

  public static <T extends PsiElement> Location<T> fromPsiElement(final T element) {
    return fromPsiElement(element, null);
  }

  public static <T extends PsiElement> Location<T> fromPsiElement(T element, Module module) {
    if (element == null || !element.isValid()) return null;
    return module != null ? new PsiLocation<>(element.getProject(), module, element) : new PsiLocation<>(element.getProject(), element);
  }
}
