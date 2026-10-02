// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressManager;
import consulo.endpoint.internal.EndpointStringUtil;
import consulo.endpoint.url.reference.AuthorityReference;
import consulo.endpoint.url.reference.SchemeReference;
import consulo.endpoint.url.reference.UrlPathReference;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceUtil;
import consulo.language.psi.SyntaxTraverser;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.util.collection.SmartList;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@ExtensionImpl(id = "urlPathReferenceContributor", order = "before legacy")
public final class UrlPathReferenceCompletionContributor extends CompletionContributor {
    private static LookupElementBuilder mkLookup(Object element, String lookupString) {
        return LookupElementBuilder.create(element, lookupString)
            .withIcon(PlatformIconGroup.generalWeb());
    }

    @Override
    @RequiredReadAction
    public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
        PsiFile containingFile = parameters.getPosition().getContainingFile();
        if (containingFile == null) {
            return;
        }
        @Nullable PsiReference multiReference = containingFile.findReferenceAt(parameters.getOffset());
        if (multiReference == null) {
            return;
        }

        @Nullable SchemeReference schemeReference = PsiReferenceUtil.findReferenceOfClass(multiReference, SchemeReference.class);
        @Nullable AuthorityReference authorityReference = PsiReferenceUtil.findReferenceOfClass(multiReference, AuthorityReference.class);

        if (authorityReference != null) {
            @Nullable String referencePrefix = CompletionUtilCore.findReferencePrefix(parameters);
            String prefix = referencePrefix != null ? referencePrefix : "";
            CompletionResultSet authCompletion = result.withPrefixMatcher(result.getPrefixMatcher().cloneWithPrefix(prefix));
            @Nullable String schemaHint = null;
            if (schemeReference != null) {
                @Nullable String givenValue = schemeReference.getGivenValue();
                if (givenValue != null && schemeReference.getSupportedSchemes().contains(givenValue)) {
                    schemaHint = givenValue;
                }
            }
            Iterator<AuthorityPomTarget> authorities = getAvailableAuthoritiesForFile(containingFile, schemaHint).iterator();
            while (authorities.hasNext()) {
                AuthorityPomTarget authority = authorities.next();
                authCompletion.addElement(mkLookup(authority, authority.getName()));
            }
            if (schemeReference == null || !Objects.equals(schemeReference.getRangeInElement(), authorityReference.getRangeInElement())) {
                return;
            }
        }

        @Nullable UrlPathReference urlPathReference = PsiReferenceUtil.findReferenceOfClass(multiReference, UrlPathReference.class);
        if (urlPathReference != null) {
            Iterator<LookupElement> variants = UrlTargetInfoFakeElementUtil.getVariantsIterator(urlPathReference);
            while (variants.hasNext()) {
                LookupElement lookupElement = variants.next();
                ProgressManager.checkCanceled();
                result.addElement(lookupElement);
            }
        }

        if (schemeReference != null) {
            List<String> supportedSchemes = schemeReference.getSupportedSchemes();
            for (String scheme : supportedSchemes) {
                result.addElement(mkLookup(scheme, scheme));
            }
            Iterator<AuthorityPomTarget> authorities = getAvailableAuthoritiesForFile(containingFile, null).iterator();
            while (authorities.hasNext()) {
                AuthorityPomTarget authority = authorities.next();
                for (String scheme : supportedSchemes) {
                    String schemeAndHost = scheme + authority.getName();
                    result.addElement(PrioritizedLookupElement.withPriority(mkLookup(schemeAndHost, schemeAndHost), -20.0));
                }
            }
        }
    }

    @RequiredReadAction
    private Stream<AuthorityPomTarget> getAvailableAuthoritiesForFile(PsiFile containingFile, @Nullable String schema) {
        return Stream.concat(
            UrlTargetInfoFakeElementUtil.getAvailableAuthorities(containingFile.getProject(), schema).stream(),
            collectAuthorityReferences(containingFile, schema)
        );
    }

    @RequiredReadAction
    private Stream<AuthorityPomTarget> collectAuthorityReferences(PsiFile file, @Nullable String schema) {
        Iterable<PsiLanguageInjectionHost> hosts = SyntaxTraverser.psiTraverser(file).filter(PsiLanguageInjectionHost.class);
        return StreamSupport.stream(hosts.spliterator(), false)
            .flatMap(host -> collectAuthRefsForGivenSchema(host, schema).stream())
            .map(AuthorityReference::getGivenValue)
            .filter(value -> value != null && !EndpointStringUtil.isBlank(value))
            .map(AuthorityPomTarget::new);
    }

    @RequiredReadAction
    private List<AuthorityReference> collectAuthRefsForGivenSchema(PsiLanguageInjectionHost host, @Nullable String schema) {
        List<SchemeReference> schemeRefs = new SmartList<>();
        List<AuthorityReference> authRefs = new SmartList<>();
        for (PsiReference reference : host.getReferences()) {
            for (PsiReference ref : PsiReferenceUtil.unwrapMultiReference(reference)) {
                if (ref instanceof AuthorityReference authorityReference) {
                    authRefs.add(authorityReference);
                }
                else if (ref instanceof SchemeReference schemeReference) {
                    schemeRefs.add(schemeReference);
                }
            }
        }
        if (schema == null) {
            return authRefs;
        }

        List<AuthorityReference> result = new ArrayList<>();
        for (AuthorityReference ar : authRefs) {
            for (SchemeReference it : schemeRefs) {
                if (it.getRangeInElement().contains(ar.getRangeInElement().getStartOffset() - 1)
                    && Objects.equals(it.getGivenValue(), schema)) {
                    result.add(ar);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public Language getLanguage() {
        return Language.ANY;
    }
}
