// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.document.util.TextRange;
import consulo.endpoint.url.UrlPath;
import consulo.endpoint.url.reference.UrlPksParser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PartiallyKnownString;
import consulo.util.collection.SmartList;

import java.util.Collections;
import java.util.List;

public final class PathVariableDeclarationUtil {
    private PathVariableDeclarationUtil() {
    }

    public static PsiReference[] createPathVariableReferencesForPks(PsiElement host,
                                                                   PartiallyKnownString fullPks,
                                                                   UrlPksParser parser,
                                                                   PathVariableUsagesProvider pathVariableUsagesProvider) {
        String expressionValue = fullPks.getValueIfKnown();
        if (expressionValue == null) {
            return PsiReference.EMPTY_ARRAY;
        }

        List<PsiReference> result = new SmartList<>();
        for (PartiallyKnownString segmentPks : parser.splitUrlPath(fullPks)) {
            for (TextRange textRange : getVariablesTextRangesInPks(segmentPks, parser.pksPathSegment(segmentPks))) {
                TextRange rangeInHost = segmentPks.mapRangeToHostRange(host, textRange);
                if (rangeInHost == null) {
                    continue;
                }
                result.add(createPathVariableReference(host, rangeInHost, pathVariableUsagesProvider));
            }
        }

        return result.toArray(PsiReference.EMPTY_ARRAY);
    }

    private static List<TextRange> getVariablesTextRangesInPks(PartiallyKnownString pks, UrlPath.PathSegment maybeComplexVariable) {
        if (maybeComplexVariable instanceof UrlPath.PathSegment.Variable variable) {
            String variableName = variable.getVariableName();
            if (variableName == null) {
                return Collections.emptyList();
            }

            int start = pks.findIndexOfInKnown(variableName);
            if (start == -1) {
                return Collections.emptyList();
            }
            return Collections.singletonList(TextRange.from(start, variableName.length()));
        }

        if (!(maybeComplexVariable instanceof UrlPath.PathSegment.Composite composite)) {
            return Collections.emptyList();
        }
        List<TextRange> result = new SmartList<>();
        int position = 0;
        for (UrlPath.PathSegment segment : composite.getSegments()) {
            if (!(segment instanceof UrlPath.PathSegment.Variable variable)) {
                continue;
            }
            String variableName = variable.getVariableName();
            if (variableName == null) {
                continue;
            }
            int start = pks.findIndexOfInKnown(variableName, position);
            if (start == -1) {
                continue;
            }

            TextRange textRange = TextRange.from(start, variableName.length());
            result.add(textRange);
            position = textRange.getEndOffset();
        }

        return result;
    }

    private static PsiReference createPathVariableReference(PsiElement host,
                                                            TextRange variableRange,
                                                            PathVariableUsagesProvider pathVariableUsagesProvider) {
        return new PathVariableDeclaringReference(host, variableRange, pathVariableUsagesProvider);
    }
}
