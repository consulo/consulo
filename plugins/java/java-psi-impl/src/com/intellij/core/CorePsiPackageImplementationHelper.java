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
package com.intellij.core;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaPackage;
import com.intellij.psi.impl.file.PsiPackageImplementationHelper;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;

/**
 * @author yole
 */
public class CorePsiPackageImplementationHelper extends PsiPackageImplementationHelper {
  @Override
  public GlobalSearchScope adjustAllScope(PsiJavaPackage psiPackage, GlobalSearchScope globalSearchScope) {
    return globalSearchScope;
  }

  @Override
  public VirtualFile[] occursInPackagePrefixes(PsiJavaPackage psiPackage) {
    return VirtualFile.EMPTY_ARRAY;
  }

  @Override
  public void handleQualifiedNameChange(PsiJavaPackage psiPackage, String newQualifiedName) {
  }

  @Override
  public void navigate(PsiJavaPackage psiPackage, boolean requestFocus) {
  }

  @Override
  public boolean packagePrefixExists(PsiJavaPackage psiPackage) {
    return false;
  }

  @Override
  public Object[] getDirectoryCachedValueDependencies(PsiJavaPackage cachedValueProvider) {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }
}
