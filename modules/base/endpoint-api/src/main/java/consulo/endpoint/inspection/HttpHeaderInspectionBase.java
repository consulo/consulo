// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.inspection;

import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;

public abstract class HttpHeaderInspectionBase<S> extends LocalInspectionTool {
    public static final String HTTP_HEADER_INSPECTION = "IncorrectHttpHeaderInspection";

    public abstract boolean getCustomHeadersEnabled(S state);

    public abstract Set<String> getCustomHeaders(S state);

    public abstract void addCustomHeader(S state, String header);

    public static boolean getCustomHeadersEnabled(PsiElement context) {
        Boolean enabled = computeWithHttpHeaderInspection(context, (tool, state) -> tool.getCustomHeadersEnabled(state));
        return enabled != null ? enabled : false;
    }

    public static Set<String> getCustomHeaders(PsiElement context) {
        Set<String> headers = computeWithHttpHeaderInspection(context, (tool, state) -> tool.getCustomHeaders(state));
        return headers != null ? headers : Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    private static <S, R> @Nullable R computeWithHttpHeaderInspection(
        PsiElement context,
        BiFunction<HttpHeaderInspectionBase<S>, S, R> function
    ) {
        PsiFile file = context.getContainingFile();
        if (file == null) {
            return null;
        }
        PsiFile containingFile = file.getOriginalFile();
        InspectionProfile profile = InspectionProjectProfileManager.getInstance(context.getProject()).getCurrentProfile();

        InspectionTool tool = profile.getUnwrappedTool(HTTP_HEADER_INSPECTION, containingFile);
        if (!(tool instanceof HttpHeaderInspectionBase)) {
            return null;
        }
        S state = profile.getToolState(HTTP_HEADER_INSPECTION, containingFile);
        if (state == null) {
            return null;
        }
        return function.apply((HttpHeaderInspectionBase<S>) tool, state);
    }
}
