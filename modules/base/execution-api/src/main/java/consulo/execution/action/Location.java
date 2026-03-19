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
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;
import java.util.Iterator;

public abstract class Location<E extends PsiElement> {
  public static final Key<Location<?>> DATA_KEY = Key.create("Location");
  public static final Key<Location<?>[]> DATA_KEYS = Key.create("LocationArray");

  
  public abstract E getPsiElement();

  
  public abstract Project getProject();

  
  public abstract <T extends PsiElement> Iterator<Location<T>> getAncestors(Class<T> ancestorClass, boolean strict);

  @Nullable
  @RequiredReadAction
  public VirtualFile getVirtualFile() {
    E psiElement = getPsiElement();
    if (!psiElement.isValid()) return null;
    PsiFile psiFile = psiElement.getContainingFile();
    if (psiFile == null) return null;
    VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null || !virtualFile.isValid()) return null;
    return virtualFile;
  }

  @Nullable
  @RequiredReadAction
  public OpenFileDescriptor getOpenFileDescriptor() {
    VirtualFile virtualFile = getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    return OpenFileDescriptorFactory.getInstance(getProject()).newBuilder(virtualFile).offset(getPsiElement().getTextOffset()).build();
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public <Ancestor extends PsiElement> Location<Ancestor> getParent(Class<Ancestor> parentClass) {
    Iterator<Location<PsiElement>> ancestors = getAncestors(PsiElement.class, true);
    if (!ancestors.hasNext()) return null;
    Location<? extends PsiElement> parent = ancestors.next();
    if (parentClass.isInstance(parent.getPsiElement())) return (Location<Ancestor>)parent;
    return null;
  }

  public @Nullable <T extends PsiElement> Location<T> getAncestorOrSelf(Class<T> ancestorClass) {
    Iterator<Location<T>> ancestors = getAncestors(ancestorClass, false);
    if (!ancestors.hasNext()) return null;
    return ancestors.next();
  }

  public @Nullable <Ancestor extends PsiElement> Ancestor getParentElement(Class<Ancestor> parentClass) {
    return safeGetPsiElement(getParent(parentClass));
  }

  public static @Nullable <T extends PsiElement> T safeGetPsiElement(Location<T> location) {
    return location != null ? location.getPsiElement() : null;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public static <T> T safeCast(Object obj, Class<T> expectedClass) {
    if (expectedClass.isInstance(obj)) return (T)obj;
    return null;
  }

  
  @RequiredReadAction
  public PsiLocation<E> toPsiLocation() {
    return new PsiLocation<>(getProject(), getPsiElement());
  }

  public abstract @Nullable Module getModule();
}
