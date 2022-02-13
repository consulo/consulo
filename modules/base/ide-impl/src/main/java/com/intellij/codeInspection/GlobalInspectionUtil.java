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

package com.intellij.codeInspection;

import consulo.language.editor.highlight.impl.HighlightInfoImpl;
import consulo.language.editor.inspection.GlobalInspectionContext;
import consulo.language.editor.inspection.ProblemDescriptionsProcessor;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.intention.IntentionAction;
import com.intellij.codeInspection.ex.GlobalInspectionContextUtil;
import consulo.language.editor.annotation.ProblemGroup;
import com.intellij.openapi.util.Pair;
import consulo.document.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiElement;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Maxim.Mossienko
 * Date: 16.09.2009
 * Time: 20:35:06
 */
public class GlobalInspectionUtil {
  private static final String LOC_MARKER = " #loc";

  @Nonnull
  public static String createInspectionMessage(@Nonnull String message) {
    //TODO: FIXME!
    return message + LOC_MARKER;
  }

  public static void createProblem(@Nonnull PsiElement elt,
                                   @Nonnull HighlightInfoImpl info,
                                   TextRange range,
                                   @javax.annotation.Nullable ProblemGroup problemGroup,
                                   @Nonnull InspectionManager manager,
                                   @Nonnull ProblemDescriptionsProcessor problemDescriptionsProcessor,
                                   @Nonnull GlobalInspectionContext globalContext) {
    List<LocalQuickFix> fixes = new ArrayList<LocalQuickFix>();
    if (info.quickFixActionRanges != null) {
      for (Pair<HighlightInfoImpl.IntentionActionDescriptor, TextRange> actionRange : info.quickFixActionRanges) {
        final IntentionAction action = actionRange.getFirst().getAction();
        if (action instanceof LocalQuickFix) {
          fixes.add((LocalQuickFix)action);
        }
      }
    }
    ProblemDescriptor descriptor = manager.createProblemDescriptor(elt, range, createInspectionMessage(StringUtil.notNullize(info.getDescription())),
                                                                   HighlightInfoImpl.convertType(info.type), false,
                                                                   fixes.isEmpty() ? null : fixes.toArray(new LocalQuickFix[fixes.size()]));
    descriptor.setProblemGroup(problemGroup);
    problemDescriptionsProcessor.addProblemElement(
            GlobalInspectionContextUtil.retrieveRefElement(elt, globalContext),
            descriptor
    );
  }
}
