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

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Iterator;

public abstract class Location<E extends PsiElement> {
  public static final Key<Location<?>> DATA_KEY = Key.create("Location");
  public static final Key<Location<?>[]> DATA_KEYS = Key.create("LocationArray");

  @Nonnull
  public abstract E getPsiElement();

  @Nonnull
  public abstract Project getProject();

  @Nonnull
  public abstract <T extends PsiElement> Iterator<Location<T>> getAncestors(Class<T> ancestorClass, boolean strict);

  @Nullable
  public VirtualFile getVirtualFile() {
    final E psiElement = getPsiElement();
    if (!psiElement.isValid()) return null;
    final PsiFile psiFile = psiElement.getContainingFile();
    if (psiFile == null) return null;
    final VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null || !virtualFile.isValid()) return null;
    return virtualFile;
  }

  @Nullable
  public OpenFileDescriptor getOpenFileDescriptor() {
    VirtualFile virtualFile = getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    return new OpenFileDescriptor(getProject(), virtualFile, getPsiElement().getTextOffset());
  }

  @Nullable
  public <Ancestor extends PsiElement> Location<Ancestor> getParent(final Class<Ancestor> parentClass) {
    final Iterator<Location<PsiElement>> ancestors = getAncestors(PsiElement.class, true);
    if (!ancestors.hasNext()) return null;
    final Location<? extends PsiElement> parent = ancestors.next();
    if (parentClass.isInstance(parent.getPsiElement())) return (Location<Ancestor>)parent;
    return null;
  }

  @javax.annotation.Nullable
  public <T extends PsiElement> Location<T> getAncestorOrSelf(final Class<T> ancestorClass) {
    final Iterator<Location<T>> ancestors = getAncestors(ancestorClass, false);
    if (!ancestors.hasNext()) return null;
    return ancestors.next();
  }

  @Nullable
  public <Ancestor extends PsiElement> Ancestor getParentElement(final Class<Ancestor> parentClass) {
    return safeGetPsiElement(getParent(parentClass));
  }

  @Nullable
  public static <T extends PsiElement> T safeGetPsiElement(final Location<T> location) {
    return location != null ? location.getPsiElement() : null;
  }

  @Nullable
  public static <T> T safeCast(final Object obj, final Class<T> expectedClass) {
    if (expectedClass.isInstance(obj)) return (T)obj;
    return null;
  }

  @Nonnull
  public PsiLocation<E> toPsiLocation() {
    return new PsiLocation<>(getProject(), getPsiElement());
  }

  @Nullable
  public abstract Module getModule();
}
