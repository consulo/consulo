// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.mime;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.document.util.TextRange;
import consulo.endpoint.internal.HttpReferenceService;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.EmptyResolveMessageProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class MimeTypeReference extends PsiReferenceBase<PsiElement> implements EmptyResolveMessageProvider {
    private final boolean myIsInjected;

    public MimeTypeReference(PsiElement element, TextRange range) {
        this(element, range, false);
    }

    public MimeTypeReference(PsiElement element, TextRange range, boolean isInjected) {
        super(element, range);
        myIsInjected = isInjected;
    }

    public boolean isInjected() {
        return myIsInjected;
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        String value = getValue();
        if (!MimeTypeConstants.MIME_PATTERN.matcher(value).matches()) {
            return null;
        }

        return Application.get().getInstance(HttpReferenceService.class).resolveMimeReference(getElement(), value);
    }

    @Override
    public LocalizeValue buildUnresolvedMessage(String referenceText) {
        return EndpointLocalize.mimeTypeElementError();
    }

    @Override
    @RequiredReadAction
    public boolean isReferenceTo(PsiElement element) {
        return Application.get().getInstance(HttpReferenceService.class).isReferenceToMimeElement(element, getValue());
    }

    @Override
    @RequiredReadAction
    public Object[] getVariants() {
        return Arrays.stream(MimeTypeConstants.PREDEFINED_MIME_VARIANTS)
            .map(it -> LookupElementBuilder.create(it).withIcon(PlatformIconGroup.nodesType()))
            .toArray();
    }

    public static PsiReference[] forElement(PsiElement injectionHost) {
        return forElement(injectionHost, ElementManipulators.getValueTextRange(injectionHost));
    }

    public static PsiReference[] forElement(PsiElement injectionHost, TextRange range) {
        return forElement(injectionHost, range, false);
    }

    public static PsiReference[] forElement(PsiElement injectionHost, TextRange range, boolean isInjected) {
        String valueText = ElementManipulators.getValueText(injectionHost);
        List<String> charSequences = StringUtil.split(valueText, ";");
        if (charSequences.size() > 0) {
            String mimeName = charSequences.get(0);
            TextRange subRange = new TextRange(range.getStartOffset(), range.getStartOffset() + mimeName.length());
            return new PsiReference[]{new MimeTypeReference(injectionHost, subRange, isInjected)};
        }
        return new PsiReference[]{new MimeTypeReference(injectionHost, range, isInjected)};
    }
}
