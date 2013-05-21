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
package com.intellij.psi.impl.file;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaPackage;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * @author yole
 */
public abstract class PsiPackageImplementationHelper {
  public abstract GlobalSearchScope adjustAllScope(PsiJavaPackage psiPackage, GlobalSearchScope globalSearchScope);

  public abstract VirtualFile[] occursInPackagePrefixes(PsiJavaPackage psiPackage);

  public abstract void handleQualifiedNameChange(PsiJavaPackage psiPackage, String newQualifiedName);

  public abstract void navigate(PsiJavaPackage psiPackage, boolean requestFocus);

  public abstract boolean packagePrefixExists(PsiJavaPackage psiPackage);

  public abstract Object[] getDirectoryCachedValueDependencies(PsiJavaPackage cachedValueProvider);

  public static PsiPackageImplementationHelper getInstance() {
    return ServiceManager.getService(PsiPackageImplementationHelper.class);
  }
}
