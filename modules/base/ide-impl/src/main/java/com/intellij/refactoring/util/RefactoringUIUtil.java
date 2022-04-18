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

package com.intellij.refactoring.util;

import consulo.language.findUsage.DescriptiveNameUtil;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;
import consulo.language.psi.PsiElement;
import consulo.language.psi.ElementDescriptionUtil;
import consulo.project.Project;
import consulo.application.ApplicationManager;
import consulo.ui.ex.awt.Messages;
import consulo.language.util.IncorrectOperationException;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.usage.UsageViewUtil;

/**
 * @author yole
 */
public class RefactoringUIUtil {
  private RefactoringUIUtil() {
  }

  public static String getDescription(@Nonnull PsiElement element, boolean includeParent) {
    return ElementDescriptionUtil.getElementDescription(element, includeParent
                                                                 ? RefactoringDescriptionLocation.WITH_PARENT
                                                                 : RefactoringDescriptionLocation.WITHOUT_PARENT);
  }

  public static void processIncorrectOperation(final Project project, IncorrectOperationException e) {
    @NonNls String message = e.getMessage();
    final int index = message != null ? message.indexOf("java.io.IOException") : -1;
    if (index > 0) {
      message = message.substring(index + "java.io.IOException".length());
    }

    final String s = message;
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        Messages.showMessageDialog(project, s, RefactoringBundle.message("error.title"), Messages.getErrorIcon());
      }
    });
  }

  public static String calculatePsiElementDescriptionList(PsiElement[] elements) {
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < elements.length; i++) {
      if (i > 0) buffer.append(", ");
      buffer.append(UsageViewUtil.getType(elements[i]));
      buffer.append(" ");
      buffer.append(DescriptiveNameUtil.getDescriptiveName(elements[i]));
    }

    return buffer.toString();
  }
}
