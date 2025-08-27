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
package consulo.execution.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

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
  public PsiLocation(@Nonnull Project project, @Nonnull E psiElement) {
    myPsiElement = psiElement;
    myProject = project;
    myModule = psiElement.getModule();
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
  public <T extends PsiElement> Iterator<Location<T>> getAncestors(@Nonnull final Class<T> ancestorClass, boolean strict) {
    final T first = strict || !ancestorClass.isInstance(myPsiElement) ? findNext(myPsiElement, ancestorClass) : (T)myPsiElement;
    return new Iterator<>() {
      private T myCurrent = first;
      @Override
      public boolean hasNext() {
        return myCurrent != null;
      }

      @Override
      public Location<T> next() {
        if (myCurrent == null) throw new NoSuchElementException();
        PsiLocation<T> psiLocation = new PsiLocation<>(myProject, myCurrent);
        myCurrent = findNext(myCurrent, ancestorClass);
        return psiLocation;
      }

      @Override
      public void remove() {
        LOG.assertTrue(false);
      }
    };
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public PsiLocation<E> toPsiLocation() {
    return this;
  }

  private static <ElementClass extends PsiElement> ElementClass findNext(PsiElement psiElement, Class<ElementClass> ancestorClass) {
    PsiElement element = psiElement;
    while ((element = element.getParent()) != null && !(element instanceof PsiFile)) {
      ElementClass ancestor = Location.safeCast(element, ancestorClass);
      if (ancestor != null) return ancestor;
    }
    return null;
  }

  @RequiredReadAction
  public static <T extends PsiElement> Location<T> fromPsiElement(@Nonnull Project project, T element) {
    if (element == null) return null;
    return new PsiLocation<>(project, element);
  }

  @RequiredReadAction
  public static <T extends PsiElement> Location<T> fromPsiElement(T element) {
    return fromPsiElement(element, null);
  }

  @RequiredReadAction
  public static <T extends PsiElement> Location<T> fromPsiElement(T element, Module module) {
    if (element == null || !element.isValid()) return null;
    return module != null ? new PsiLocation<>(element.getProject(), module, element) : new PsiLocation<>(element.getProject(), element);
  }
}
