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
package com.intellij.semantic;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author peter
 */
public abstract class SemService {

  public static SemService getSemService(Project p) {
    return ServiceManager.getService(p, SemService.class);
  }

  @javax.annotation.Nullable
  public <T extends SemElement> T getSemElement(SemKey<T> key, @Nonnull PsiElement psi) {
    final List<T> list = getSemElements(key, psi);
    if (list.isEmpty()) return null;
    return list.get(0);
  }

  public abstract <T extends SemElement> List<T> getSemElements(SemKey<T> key, @Nonnull PsiElement psi);

  @javax.annotation.Nullable
  public abstract <T extends SemElement> List<T> getCachedSemElements(SemKey<T> key, @Nonnull PsiElement psi);

  public abstract <T extends SemElement> void setCachedSemElement(SemKey<T> key, @Nonnull PsiElement psi, @javax.annotation.Nullable T semElement);

  public abstract void clearCachedSemElements(@Nonnull PsiElement psi);

  public abstract void clearCache();

  /**
   * Caches won't be cleared on PSI changes inside this action
   * @param change the action
   */
  public abstract void performAtomicChange(@Nonnull Runnable change);

  public abstract boolean isInsideAtomicChange();
}
