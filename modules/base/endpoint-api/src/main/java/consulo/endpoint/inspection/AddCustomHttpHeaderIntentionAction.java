// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.inspection;

import consulo.endpoint.localize.EndpointLocalize;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

public final class AddCustomHttpHeaderIntentionAction implements LocalQuickFix {
    private final String myHeader;

    public AddCustomHttpHeaderIntentionAction(String header) {
        myHeader = header;
    }

    @Override
    public LocalizeValue getName() {
        return EndpointLocalize.inspectionIncorrectHttpHeaderAddCustom();
    }

    @Override
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        PsiElement psiElement = descriptor.getPsiElement();
        if (psiElement == null) {
            return;
        }
        InspectionProfile currentProfile = InspectionProjectProfileManager.getInstance(project).getCurrentProfile();

        currentProfile.<HttpHeaderInspectionBase<Object>, Object>modifyToolSettings(
            HttpHeaderInspectionBase.HTTP_HEADER_INSPECTION,
            psiElement,
            (tool, state) -> tool.addCustomHeader(state, myHeader)
        );
    }
}
