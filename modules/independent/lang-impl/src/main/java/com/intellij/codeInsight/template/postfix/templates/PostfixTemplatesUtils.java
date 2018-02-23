/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.postfix.templates;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class PostfixTemplatesUtils {
  private PostfixTemplatesUtils() {
  }

  @Nullable
  public static TextRange surround(@Nonnull Surrounder surrounder,
                                   @Nonnull Editor editor,
                                   @Nonnull PsiElement expr) {
    Project project = expr.getProject();
    PsiElement[] elements = {expr};
    if (surrounder.isApplicable(elements)) {
      return surrounder.surroundElements(project, editor, elements);
    }
    else {
      showErrorHint(project, editor);
    }
    return null;
  }

  public static void showErrorHint(@Nonnull Project project, @Nonnull Editor editor) {
    CommonRefactoringUtil.showErrorHint(project, editor, "Can't expand postfix template", "Can't expand postfix template", "");
  }

  @Nonnull
  public static String getLangForProvider(@Nonnull PostfixTemplateProvider provider) {
    LanguageExtensionPoint[] extensions = new ExtensionPointName<LanguageExtensionPoint>(LanguagePostfixTemplate.EP_NAME).getExtensions();

    for (LanguageExtensionPoint extension : extensions) {
      if (provider.equals(extension.getInstance())) {
        return extension.getKey();
      }
    }

    return Language.ANY.getID();
  }
}
