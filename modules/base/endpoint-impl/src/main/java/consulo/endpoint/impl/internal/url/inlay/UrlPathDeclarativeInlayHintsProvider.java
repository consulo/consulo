// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.inlay;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.url.inlay.UrlPathInlayHint;
import consulo.endpoint.url.inlay.UrlPathInlayHintsProviderSemElement;
import consulo.endpoint.url.inlay.UrlPathInlayLanguagesProvider;
import consulo.language.Language;
import consulo.language.editor.inlay.DeclarativeInlayHintsCollector;
import consulo.language.editor.inlay.DeclarativeInlayHintsProvider;
import consulo.language.editor.inlay.DeclarativeInlayHintsProviderFactory;
import consulo.language.editor.inlay.DeclarativeInlayHintsSettings;
import consulo.language.editor.inlay.InlayGroup;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.sem.SemService;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class UrlPathDeclarativeInlayHintsProvider implements DeclarativeInlayHintsProvider {
    public static final String PROVIDER_ID_PREFIX = "microservices.url.path.inlay.hints.";

    private final Language myLanguage;
    private final List<UrlPathInlayLanguagesProvider> myLanguagesProviders;

    public UrlPathDeclarativeInlayHintsProvider(Language language, List<UrlPathInlayLanguagesProvider> languagesProviders) {
        myLanguage = language;
        myLanguagesProviders = List.copyOf(languagesProviders);
    }

    @Override
    public @Nullable DeclarativeInlayHintsCollector createCollector(PsiFile file, Editor editor) {
        Project project = file.getProject();
        if (DumbService.isDumb(project) || project.isDefault()) {
            return null;
        }

        return new UrlPathInlayHintsCollector(this, file, SemService.getSemService(project));
    }

    @RequiredReadAction
    public List<UrlPathInlayHint> collectHints(PsiElement element, SemService semService) {
        List<UrlPathInlayHint> hints = new ArrayList<>();
        for (UrlPathInlayLanguagesProvider languagesProvider : myLanguagesProviders) {
            List<UrlPathInlayHintsProviderSemElement> hintSemProviders =
                UrlPathInlayHintsPresentationUtil.selectProvidersFromGroups(inlaysInElement(languagesProvider, element, semService));
            for (UrlPathInlayHintsProviderSemElement hintSemProvider : hintSemProviders) {
                hints.addAll(hintSemProvider.getInlayHints());
            }
        }
        return hints;
    }

    @Override
    public Language getLanguage() {
        return myLanguage;
    }

    @Override
    public String getId() {
        return getProviderId(myLanguage);
    }

    @Override
    public LocalizeValue getName() {
        return EndpointLocalize.microservicesInlayProviderName();
    }

    @Override
    public LocalizeValue getDescription() {
        return EndpointLocalize.inlayMicroservicesUrlPathInlayHintsDescription();
    }

    @Override
    public LocalizeValue getPreviewFileText() {
        return LocalizeValue.empty();
    }

    @Override
    public InlayGroup getGroup() {
        return InlayGroup.URL_PATH_GROUP;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    public static String getProviderId(Language language) {
        return PROVIDER_ID_PREFIX + language.getID();
    }

    @RequiredReadAction
    public static List<UrlPathInlayHintsProviderSemElement> inlaysInElement(
        UrlPathInlayLanguagesProvider urlPathInlayLanguagesProvider,
        PsiElement element,
        SemService semService
    ) {
        List<UrlPathInlayHintsProviderSemElement> result = new ArrayList<>();
        for (PsiElement potentialElement : urlPathInlayLanguagesProvider.getPotentialElementsWithHintsProviders(element)) {
            result.addAll(semService.getSemElementsNoCache(UrlPathInlayHintsProviderSemElement.INLAY_HINT_SEM_KEY, potentialElement));
        }
        return result;
    }

    public static void setUrlPathInlaysEnabledForLanguage(Language language, boolean enabled) {
        DeclarativeInlayHintsSettings settings = DeclarativeInlayHintsSettings.getInstance();
        List<UrlPathDeclarativeInlayHintsProvider> providers = getUrlPathProvidersForLanguage(language);
        if (providers.isEmpty()) {
            settings.setProviderEnabled(getProviderId(language), enabled);
            return;
        }
        for (UrlPathDeclarativeInlayHintsProvider provider : providers) {
            settings.setProviderEnabled(provider.getId(), enabled);
        }
    }

    @RequiredReadAction
    public static boolean isUrlPathInlaysEnabledForLanguage(Language language) {
        DeclarativeInlayHintsSettings settings = DeclarativeInlayHintsSettings.getInstance();
        List<UrlPathDeclarativeInlayHintsProvider> providers = getUrlPathProvidersForLanguage(language);
        if (providers.isEmpty()) {
            return settings.isProviderEnabled(getProviderId(language), true);
        }
        for (UrlPathDeclarativeInlayHintsProvider provider : providers) {
            if (settings.isProviderEnabled(provider.getId(), provider.isEnabledByDefault())) {
                return true;
            }
        }
        return false;
    }

    private static List<UrlPathDeclarativeInlayHintsProvider> getUrlPathProvidersForLanguage(Language language) {
        UrlPathDeclarativeInlayHintsProviderFactory factory = Application.get()
            .getExtensionPoint(DeclarativeInlayHintsProviderFactory.class)
            .findExtension(UrlPathDeclarativeInlayHintsProviderFactory.class);
        return factory == null ? List.of() : factory.getUrlPathProvidersForLanguage(language);
    }

    @RequiredReadAction
    public static boolean shouldHaveUrlPathInlayAroundOffset(PsiElement element, int offset) {
        return shouldHaveUrlPathInlayAroundOffset(element, offset, Integer.MAX_VALUE);
    }

    @RequiredReadAction
    public static boolean shouldHaveUrlPathInlayAroundOffset(PsiElement element, int offset, int searchLimit) {
        UrlPathInlayLanguagesProvider languagesProvider =
            UrlPathInlayHintsProviderFactoryUtil.getLanguagesProviderByLanguage(element.getLanguage());
        if (languagesProvider == null) {
            return false;
        }

        SemService semService = SemService.getSemService(element.getProject());
        PsiElement current = element;
        int taken = 0;
        while (current != null && taken < searchLimit) {
            for (UrlPathInlayHintsProviderSemElement hintSemProvider : inlaysInElement(languagesProvider, current, semService)) {
                for (UrlPathInlayHint hint : hintSemProvider.getInlayHints()) {
                    SmartPsiElementPointer<PsiElement> attachedTo = hint.getAttachedTo();
                    PsiElement attachedElement = attachedTo != null ? attachedTo.getElement() : null;
                    if (attachedElement != null && attachedElement.getTextRange().grown(1).contains(offset)) {
                        return true;
                    }
                }
            }

            taken++;
            PsiElement parent = current.getParent();
            current = parent instanceof PsiFile ? null : parent;
        }
        return false;
    }
}
