// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.CodeInsightColors;
import consulo.document.util.TextRange;
import consulo.document.util.TextRangeUtil;
import consulo.endpoint.url.reference.UrlSegmentReference;
import consulo.language.Language;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.ContributedReferencesAnnotator;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.ContributedReferenceHost;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.OuterLanguageElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class EndpointReferenceAnnotator implements ContributedReferencesAnnotator {
    @Override
    @RequiredReadAction
    public void annotate(PsiElement element, List<PsiReference> references, AnnotationHolder holder) {
        if (holder.isBatchMode() || !(element instanceof PsiLanguageInjectionHost || element instanceof ContributedReferenceHost)) {
            return;
        }

        if (references.stream().anyMatch(it -> it instanceof UrlSegmentReference)) {
            TextRange rangeInElement = ElementManipulators.getValueTextRange(element);
            String elementText = element.getText();
            List<TextRange> lineRanges = TextRangeUtil.splitToTextRanges(rangeInElement.substring(elementText), "\n", false)
                .map(subRange -> {
                    String text = subRange.substring(elementText);
                    int lastIndex = text.length() - 1;
                    int leftStrip = text.length();
                    for (int i = 0; i <= lastIndex; i++) {
                        if (!isWhitespace(text.charAt(i))) {
                            leftStrip = i;
                            break;
                        }
                    }
                    int lastNonWhitespace = -1;
                    for (int i = lastIndex; i >= 0; i--) {
                        if (!isWhitespace(text.charAt(i))) {
                            lastNonWhitespace = i;
                            break;
                        }
                    }
                    int rightStrip = lastIndex - lastNonWhitespace;
                    boolean onlySpaces = leftStrip == text.length();

                    if (!onlySpaces) {
                        return TextRange.create(subRange.getStartOffset() + leftStrip, subRange.getEndOffset() - rightStrip);
                    }
                    else {
                        return subRange;
                    }
                })
                .map(it -> it.shiftRight(rangeInElement.getStartOffset()))
                .toList();
            for (TextRange range : lineRanges) {
                highlightRange(range, element, holder);
            }
        }
    }

    @RequiredReadAction
    private void highlightRange(TextRange rangeInElement, PsiElement element, AnnotationHolder holder) {
        if (rangeInElement.isEmpty()) {
            return;
        }

        int elementStartOffset = element.getTextRange().getStartOffset();
        List<TextRange> injectedRanges = new ArrayList<>();
        InjectedLanguageManager.getInstance(element.getProject()).enumerate(element, (injectedPsi, places) -> {
            for (PsiLanguageInjectionHost.Shred place : places) {
                injectedRanges.add(place.getRangeInsideHost().shiftRight(elementStartOffset));
            }
        });
        List<TextRange> excludedRanges = new ArrayList<>();
        for (OuterLanguageElement templateElement : PsiTreeUtil.getChildrenOfTypeAsList(element, OuterLanguageElement.class)) {
            excludedRanges.add(templateElement.getTextRange());
        }
        excludedRanges.addAll(injectedRanges);
        Iterable<TextRange> elementOwnRanges = TextRangeUtil.excludeRanges(rangeInElement.shiftRight(elementStartOffset), excludedRanges);
        for (TextRange range : elementOwnRanges) {
            holder.newSilentAnnotation(HighlightInfoType.HIGHLIGHTED_REFERENCE_SEVERITY)
                .range(range)
                .textAttributes(CodeInsightColors.INACTIVE_HYPERLINK_ATTRIBUTES)
                .create();
        }
    }

    @Override
    public Language getLanguage() {
        return Language.ANY;
    }

    private static boolean isWhitespace(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }
}
