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

package consulo.language.editor.refactoring.ui;

import consulo.application.Application;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.util.RefactoringDescriptionLocation;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.psi.ElementDescriptionUtil;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.usage.UsageViewUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;

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

  public static void processIncorrectOperation(Project project, IncorrectOperationException e) {
    @NonNls String message = e.getMessage();
    int index = message != null ? message.indexOf("java.io.IOException") : -1;
    if (index > 0) {
      message = message.substring(index + "java.io.IOException".length());
    }

    String s = message;
    Application.get()
      .invokeLater(() -> Messages.showMessageDialog(project, s, RefactoringLocalize.errorTitle().get(), UIUtil.getErrorIcon()));
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
