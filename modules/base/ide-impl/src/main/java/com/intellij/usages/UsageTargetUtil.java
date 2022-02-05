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
package com.intellij.usages;

import consulo.dataContext.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import consulo.editor.Editor;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UsageTargetUtil {
  private static final ExtensionPointName<UsageTargetProvider> EP_NAME = ExtensionPointName.create("consulo.usageTargetProvider");

  public static UsageTarget[] findUsageTargets(DataProvider dataProvider) {
    Editor editor = dataProvider.getDataUnchecked(PlatformDataKeys.EDITOR);
    PsiFile file = dataProvider.getDataUnchecked(LangDataKeys.PSI_FILE);

    List<UsageTarget> result = new ArrayList<>();
    if (file != null && editor != null) {
      UsageTarget[] targets = findUsageTargets(editor, file);
      if (targets != null) Collections.addAll(result, targets);
    }
    PsiElement psiElement = dataProvider.getDataUnchecked(LangDataKeys.PSI_ELEMENT);
    if (psiElement != null) {
      UsageTarget[] targets = findUsageTargets(psiElement);
      if (targets != null)Collections.addAll(result, targets);
    }

    return result.isEmpty() ? null : result.toArray(new UsageTarget[result.size()]);
  }

  public static UsageTarget[] findUsageTargets(Editor editor, PsiFile file) {
    List<UsageTarget> result = new ArrayList<>();
    for (UsageTargetProvider provider : EP_NAME.getExtensionList()) {
      UsageTarget[] targets = provider.getTargets(editor, file);
      if (targets != null) Collections.addAll(result, targets);
    }
    return result.isEmpty() ? null : result.toArray(new UsageTarget[result.size()]);
  }

  public static UsageTarget[] findUsageTargets(PsiElement psiElement) {
    List<UsageTarget> result = new ArrayList<>();
    for (UsageTargetProvider provider : EP_NAME.getExtensionList()) {
      UsageTarget[] targets = provider.getTargets(psiElement);
      if (targets != null) Collections.addAll(result, targets);
    }
    return result.isEmpty() ? null : result.toArray(new UsageTarget[result.size()]);
  }
}
