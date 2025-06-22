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

package consulo.language.editor.inspection;

import consulo.document.util.TextRange;
import consulo.language.editor.annotation.ProblemGroup;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiElement;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Maxim.Mossienko
 * @since 2009-09-16
 */
public class GlobalInspectionUtil {
  private static final String LOC_MARKER = " #loc";

  @Nonnull
  public static String createInspectionMessage(@Nonnull String message) {
    //TODO: FIXME!
    return message + LOC_MARKER;
  }

  public static void createProblem(@Nonnull PsiElement elt,
                                   @Nonnull HighlightInfo info,
                                   TextRange range,
                                   @Nullable ProblemGroup problemGroup,
                                   @Nonnull InspectionManager manager,
                                   @Nonnull ProblemDescriptionsProcessor problemDescriptionsProcessor,
                                   @Nonnull GlobalInspectionContext globalContext) {
    List<LocalQuickFix> fixes = new ArrayList<>();
    info.forEachQuickFix((action, textRange) -> {
      if (action instanceof LocalQuickFix) {
        fixes.add((LocalQuickFix)action);
      }
    });

    ProblemDescriptor descriptor = manager.createProblemDescriptor(elt, range, createInspectionMessage(StringUtil.notNullize(info.getDescription())), ProblemHighlightType.from(info.getType()), false,
                                                                   fixes.isEmpty() ? null : fixes.toArray(new LocalQuickFix[fixes.size()]));
    descriptor.setProblemGroup(problemGroup);
    problemDescriptionsProcessor.addProblemElement(GlobalInspectionContextUtil.retrieveRefElement(elt, globalContext), descriptor);
  }
}
