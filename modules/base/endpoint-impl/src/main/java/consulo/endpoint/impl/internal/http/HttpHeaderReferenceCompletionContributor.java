// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.http;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressManager;
import consulo.endpoint.http.HttpHeaderDictionary;
import consulo.endpoint.http.HttpHeaderDocumentation;
import consulo.endpoint.http.HttpHeaderReference;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceUtil;
import consulo.platform.base.icon.PlatformIconGroup;

import java.util.Map;

@ExtensionImpl(id = "httpHeaderReferenceContributor", order = "before legacy")
public class HttpHeaderReferenceCompletionContributor extends CompletionContributor {
    @Override
    @RequiredReadAction
    public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
        PsiFile containingFile = parameters.getPosition().getContainingFile();
        if (containingFile == null) {
            return;
        }
        PsiReference multiReference = containingFile.findReferenceAt(parameters.getOffset());
        if (multiReference == null) {
            return;
        }

        if (PsiReferenceUtil.findReferenceOfClass(multiReference, HttpHeaderReference.class) != null) {
            for (Map.Entry<String, HttpHeaderDocumentation> headerEntry : HttpHeaderDictionary.getHeaders().entrySet()) {
                ProgressManager.checkCanceled();

                result.accept(LookupElementBuilder.create(headerEntry.getValue(), headerEntry.getKey())
                    .withIcon(PlatformIconGroup.nodesConstant()));
            }
        }
    }

    @Override
    public Language getLanguage() {
        return Language.ANY;
    }
}
