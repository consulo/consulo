/*
 * Copyright 2013 Consulo.org
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
package org.consulo.java.platform.psi.impl;

import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.PsiPackageBase;
import org.consulo.psi.PsiPackageManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 8:45/20.05.13
 */
public class JavaPsiPackageImpl extends PsiPackageBase {
  public JavaPsiPackageImpl(PsiManager manager, PsiPackageManager packageManager, String qualifiedName) {
    super(manager, packageManager, qualifiedName);
  }

  @Override
  protected Collection<PsiDirectory> getAllDirectories() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Language getLanguage() {
    return JavaLanguage.INSTANCE;
  }
}
