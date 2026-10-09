/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.dataContext.DataContext;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.documentation.ExternalDocumentationHandler;
import consulo.language.editor.documentation.ExternalDocumentationProvider;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.lang.StringUtil;
import consulo.webBrowser.BrowserUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public final class ExternalDocumentationOpener {
    private record Target(
        @Nullable PsiElement element,
        @Nullable PsiElement originalElement,
        @Nullable DocumentationProvider provider,
        List<String> urls
    ) {
    }

    private ExternalDocumentationOpener() {
    }

    @RequiredUIAccess
    public static void open(Project project, PsiElement element, @Nullable PsiElement originalElement, @Nullable String url, DataContext dataContext) {
        SmartPointerManager pointerManager = SmartPointerManager.getInstance(project);
        open(
            project,
            pointerManager.createSmartPsiElementPointer(element),
            originalElement == null ? null : pointerManager.createSmartPsiElementPointer(originalElement),
            url,
            dataContext
        );
    }

    @RequiredUIAccess
    public static void open(
        Project project,
        SmartPsiElementPointer<? extends PsiElement> element,
        @Nullable SmartPsiElementPointer<? extends PsiElement> originalElement,
        @Nullable String url,
        DataContext dataContext
    ) {
        CoroutineScope.launchAsync(
            project.coroutineContext(),
            () -> Coroutine
                .first(ReadLock.<Object, Target>apply((ignored, continuation) -> resolve(element, originalElement, url, continuation)))
                .then(CodeExecution.<Target, Target>apply(target -> chooseFetchable(project, target)))
                .then(UIAction.<Target, Target>apply(target -> {
                    show(target, dataContext);
                    return target;
                }))
        );
    }

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    private static @Nullable Target resolve(
        SmartPsiElementPointer<? extends PsiElement> pointer,
        @Nullable SmartPsiElementPointer<? extends PsiElement> originalPointer,
        @Nullable String url,
        Continuation<?> continuation
    ) {
        PsiElement element = pointer.getElement();
        if (element == null) {
            ((Continuation<Target>) continuation).finishEarly(null);
            return null;
        }

        PsiElement originalElement = originalPointer == null ? DocumentationManagerHelper.getOriginalElement(element) : originalPointer.getElement();
        DocumentationProvider provider = DocumentationManagerHelper.getProviderFromElement(element);
        if (provider instanceof ExternalDocumentationHandler handler && handler.handleExternal(element, originalElement)) {
            ((Continuation<Target>) continuation).finishEarly(null);
            return null;
        }

        List<String> urls = StringUtil.isEmptyOrSpaces(url) ? provider.getUrlFor(element, originalElement) : List.of(url);
        return new Target(element, originalElement, provider, urls == null ? List.of() : List.copyOf(urls));
    }

    private static Target chooseFetchable(Project project, Target target) {
        PsiElement element = target.element();
        if (element == null || !(target.provider() instanceof ExternalDocumentationProvider provider) || target.urls().size() <= 1) {
            return target;
        }

        for (String url : target.urls()) {
            List<String> single = List.of(url);
            if (provider.fetchExternalDocumentation(project, element, single) != null) {
                return new Target(element, target.originalElement(), target.provider(), single);
            }
        }
        return target;
    }

    @RequiredUIAccess
    private static void show(Target target, DataContext dataContext) {
        List<String> urls = target.urls();
        if (urls.isEmpty()) {
            PsiElement element = target.element();
            if (element != null
                && target.provider() instanceof ExternalDocumentationProvider provider
                && provider.canPromptToConfigureDocumentation(element)) {
                provider.promptToConfigureDocumentation(element);
            }
            return;
        }

        if (urls.size() == 1) {
            BrowserUtil.browse(urls.get(0));
            return;
        }

        JBPopupFactory.getInstance()
            .createListPopup(new BaseListPopupStep<>(CodeInsightLocalize.javadocExternalRootChooseTitle().get(), urls) {
                @Override
                public @Nullable PopupStep onChosen(String selectedValue, boolean finalChoice) {
                    BrowserUtil.browse(selectedValue);
                    return FINAL_CHOICE;
                }
            })
            .showInBestPositionFor(dataContext);
    }
}
