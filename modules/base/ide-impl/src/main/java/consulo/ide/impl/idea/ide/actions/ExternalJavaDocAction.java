/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.util.function.ThrowableComputable;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.documentation.ExternalDocumentationHandler;
import consulo.language.editor.documentation.ExternalDocumentationProvider;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.util.lang.StringUtil;
import consulo.webBrowser.BrowserUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class ExternalJavaDocAction extends AnAction {
    public ExternalJavaDocAction() {
        setInjectedContext(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        Editor editor = e.getData(Editor.KEY);
        PsiElement element = getElement(e.getDataContext(), editor);
        if (element == null) {
            Messages.showMessageDialog(
                project,
                IdeLocalize.messagePleaseSelectElementForJavadoc().get(),
                IdeLocalize.titleNoElementSelected().get(),
                Messages.getErrorIcon()
            );
            return;
        }

        PsiFile context = e.getDataContext().getData(PsiFile.KEY);

        PsiElement originalElement = getOriginalElement(context, editor);
        DocumentationManagerHelper.storeOriginalElement(project, originalElement, element);

        showExternalJavadoc(element, originalElement, null, e.getDataContext());
    }

    public static void showExternalJavadoc(
        PsiElement element,
        PsiElement originalElement,
        String docUrl,
        DataContext dataContext
    ) {
        DocumentationProvider provider = DocumentationManagerHelper.getProviderFromElement(element);
        if (provider instanceof ExternalDocumentationHandler externalDocumentationHandler
            && externalDocumentationHandler.handleExternal(element, originalElement)) {
            return;
        }
        Project project = dataContext.getData(Project.KEY);
        Component contextComponent = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        Application.get().executeOnPooledThread(() -> {
            List<String> urls;
            if (StringUtil.isEmptyOrSpaces(docUrl)) {
                ThrowableComputable<List<String>, RuntimeException> action = () -> provider.getUrlFor(element, originalElement);
                urls = AccessRule.read(action);
            }
            else {
                urls = Collections.singletonList(docUrl);
            }
            if (provider instanceof ExternalDocumentationProvider externalDocumentationProvider && urls != null && urls.size() > 1) {
                for (String url : urls) {
                    List<String> thisUrlList = Collections.singletonList(url);
                    String doc = externalDocumentationProvider.fetchExternalDocumentation(project, element, thisUrlList);
                    if (doc != null) {
                        urls = thisUrlList;
                        break;
                    }
                }
            }
            List<String> finalUrls = urls;
            Application.get().invokeLater(
                () -> {
                    if (ContainerUtil.isEmpty(finalUrls)) {
                        if (element != null && provider instanceof ExternalDocumentationProvider externalDocumentationProvider
                            && externalDocumentationProvider.canPromptToConfigureDocumentation(element)) {
                            externalDocumentationProvider.promptToConfigureDocumentation(element);
                        }
                    }
                    else if (finalUrls.size() == 1) {
                        BrowserUtil.browse(finalUrls.get(0));
                    }
                    else {
                        JBPopupFactory.getInstance().createListPopup(
                            new BaseListPopupStep<String>("Choose external documentation root", ArrayUtil.toStringArray(finalUrls)) {
                                @Override
                                public PopupStep onChosen(String selectedValue, boolean finalChoice) {
                                    BrowserUtil.browse(selectedValue);
                                    return FINAL_CHOICE;
                                }
                            }
                        ).showInBestPositionFor(DataManager.getInstance().getDataContext(contextComponent));
                    }
                },
                IdeaModalityState.nonModal()
            );
        });

    }

    @Nullable
    @RequiredReadAction
    private static PsiElement getOriginalElement(PsiFile context, Editor editor) {
        return (context != null && editor != null) ? context.findElementAt(editor.getCaretModel().getOffset()) : null;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Editor editor = e.getData(Editor.KEY);
        PsiElement element = getElement(e.getDataContext(), editor);
        PsiElement originalElement = getOriginalElement(e.getData(PsiFile.KEY), editor);
        DocumentationManagerHelper.storeOriginalElement(e.getData(Project.KEY), originalElement, element);
        DocumentationProvider provider = DocumentationManagerHelper.getProviderFromElement(element);
        boolean enabled;
        if (provider instanceof ExternalDocumentationProvider edProvider) {
            enabled = edProvider.hasDocumentationFor(element, originalElement) || edProvider.canPromptToConfigureDocumentation(element);
        }
        else {
            List<String> urls = provider.getUrlFor(element, originalElement);
            enabled = urls != null && !urls.isEmpty();
        }
        if (editor != null) {
            presentation.setEnabled(enabled);
            if (ActionPlaces.isMainMenuOrActionSearch(e.getPlace())) {
                presentation.setVisible(true);
            }
            else {
                presentation.setVisible(enabled);
            }
        }
        else {
            presentation.setEnabled(enabled);
            presentation.setVisible(true);
        }
    }

    @RequiredReadAction
    private static PsiElement getElement(DataContext dataContext, Editor editor) {
        PsiElement element = dataContext.getData(PsiElement.KEY);
        if (element == null && editor != null) {
            PsiReference reference = TargetElementUtil.findReference(editor, editor.getCaretModel().getOffset());
            if (reference != null) {
                element = reference.getElement();
            }
        }
        return element;
    }
}