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

package com.intellij.ide.impl.dataRules;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import javax.annotation.Nonnull;

public class PsiFileRule implements GetDataRule<PsiFile> {
  @Nonnull
  @Override
  public Key<PsiFile> getKey() {
    return CommonDataKeys.PSI_FILE;
  }

  @Override
  public PsiFile getData(@Nonnull DataProvider dataProvider) {
    final PsiElement element = dataProvider.getDataUnchecked(LangDataKeys.PSI_ELEMENT);
    if (element != null) {
      return element.getContainingFile();
    }
    Project project = dataProvider.getDataUnchecked(CommonDataKeys.PROJECT);
    if (project != null) {
      VirtualFile vFile = dataProvider.getDataUnchecked(PlatformDataKeys.VIRTUAL_FILE);
      if (vFile != null) {
        return PsiManager.getInstance(project).findFile(vFile);
      }
    }
    return null;
  }
}
