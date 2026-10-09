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
import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.dataContext.DataContext;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.documentation.ExternalDocumentationProvider;
import consulo.language.editor.impl.internal.documentation.ExternalDocumentationOpener;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;

import java.util.List;

@ActionImpl(id = "ExternalJavaDoc")
public class ExternalJavaDocAction extends DumbAwareAction implements AnActionWithAsyncUpdate {
    public ExternalJavaDocAction() {
        super(ActionLocalize.actionExternaljavadocText(), ActionLocalize.actionExternaljavadocDescription());
        setInjectedContext(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
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
                UIUtil.getErrorIcon()
            );
            return;
        }

        PsiFile context = e.getDataContext().getData(PsiFile.KEY);

        PsiElement originalElement = getOriginalElement(context, editor);
        DocumentationManagerHelper.storeOriginalElement(project, originalElement, element);

        showExternalJavadoc(element, originalElement, null, e.getDataContext());
    }

    @RequiredUIAccess
    public static void showExternalJavadoc(
        PsiElement element,
        @Nullable PsiElement originalElement,
        @Nullable String docUrl,
        DataContext dataContext
    ) {
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            project = element.getProject();
        }
        ExternalDocumentationOpener.open(project, element, originalElement, docUrl, dataContext);
    }

    @RequiredReadAction
    private static @Nullable PsiElement getOriginalElement(PsiFile context, Editor editor) {
        return (context != null && editor != null) ? context.findElementAt(editor.getCaretModel().getOffset()) : null;
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return ActionSafeReadLock.run(e, presentation -> {
            Editor editor = e.getData(EditorKeys.EDITOR_SNAPSHOT);
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
        }).toCoroutine();
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