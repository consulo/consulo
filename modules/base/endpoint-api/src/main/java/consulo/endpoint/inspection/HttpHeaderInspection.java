// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.inspection;

import consulo.endpoint.http.HttpHeaderDictionary;
import consulo.endpoint.internal.EndpointStringUtil;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

public interface HttpHeaderInspection {
    boolean isCustomHeader(String header, PsiElement psiElement);

    default @Nullable LocalQuickFix getCustomHttpHeaderFix(PsiElement psiElement, String header) {
        return HttpHeaderInspectionBase.getCustomHeadersEnabled(psiElement) && !EndpointStringUtil.isBlank(header)
            ? new AddCustomHttpHeaderIntentionAction(header)
            : null;
    }

    default void checkHeader(ProblemsHolder holder, String header, PsiElement psiElement) {
        boolean customHeader = isCustomHeader(header, psiElement);

        boolean existingHeader = customHeader
            || HttpHeaderDictionary.getHeaders().keySet().stream().anyMatch(it -> StringUtil.equalsIgnoreCase(header, it));

        if (!existingHeader) {
            holder.newProblem(EndpointLocalize.inspectionIncorrectHttpHeaderUnknownHeader())
                .range(psiElement)
                .highlightType(ProblemHighlightType.WEAK_WARNING)
                .withOptionalFix(getCustomHttpHeaderFix(psiElement, header))
                .create();
        }
    }
}
