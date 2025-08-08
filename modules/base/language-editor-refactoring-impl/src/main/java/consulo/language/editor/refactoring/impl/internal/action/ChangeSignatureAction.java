/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.impl.internal.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.dataContext.DataContext;
import consulo.language.Language;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.editor.refactoring.action.BasePlatformRefactoringAction;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.changeSignature.ChangeSignatureHandler;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiReference;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ActionImpl(id = "ChangeSignature")
public class ChangeSignatureAction extends BasePlatformRefactoringAction {
    public ChangeSignatureAction() {
        super(ActionLocalize.actionChangesignatureText(), ActionLocalize.actionChangesignatureDescription());
        setInjectedContext(true);
    }

    @Override
    public boolean isAvailableInEditorOnly() {
        return false;
    }

    @Override
    @RequiredReadAction
    public boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
        return elements.length == 1 && findTargetMember(elements[0]) != null;
    }

    @Override
    @RequiredReadAction
    protected boolean isAvailableOnElementInEditorAndFile(
        @Nonnull PsiElement element,
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        @Nonnull DataContext context
    ) {
        PsiElement targetMember = findTargetMember(element);
        if (targetMember == null) {
            ChangeSignatureHandler targetHandler = getChangeSignatureHandler(file.getLanguage());
            return targetHandler != null;
        }
        ChangeSignatureHandler targetHandler = getChangeSignatureHandler(targetMember.getLanguage());
        return targetHandler != null;
    }

    @Nullable
    @RequiredReadAction
    private static PsiElement findTargetMember(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }
        ChangeSignatureHandler fileHandler = getChangeSignatureHandler(element.getLanguage());
        if (fileHandler != null) {
            PsiElement targetMember = fileHandler.findTargetMember(element);
            if (targetMember != null) {
                return targetMember;
            }
        }
        PsiReference reference = element.getReference();
        if (reference == null && element instanceof PsiNameIdentifierOwner) {
            return element;
        }
        if (reference != null) {
            return reference.resolve();
        }
        return null;
    }

    @Nullable
    @Override
    protected RefactoringActionHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider) {
        return provider.getChangeSignatureHandler();
    }

    @Nullable
    @Override
    protected RefactoringActionHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider, final PsiElement element) {
        return new RefactoringActionHandler() {
            @Override
            @RequiredUIAccess
            public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
                editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
                PsiElement targetMember = findTargetMember(element);
                if (targetMember == null) {
                    ChangeSignatureHandler handler = getChangeSignatureHandler(file.getLanguage());
                    if (handler != null) {
                        String notFoundMessage = handler.getTargetNotFoundMessage();
                        if (notFoundMessage != null) {
                            CommonRefactoringUtil.showErrorHint(
                                project,
                                editor,
                                notFoundMessage,
                                ChangeSignatureHandler.REFACTORING_NAME.get(),
                                null
                            );
                        }
                    }
                    return;
                }
                ChangeSignatureHandler handler = getChangeSignatureHandler(targetMember.getLanguage());
                if (handler == null) {
                    return;
                }
                handler.invoke(project, new PsiElement[]{targetMember}, dataContext);
            }

            @Override
            @RequiredUIAccess
            public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
                if (elements.length != 1) {
                    return;
                }
                PsiElement targetMember = findTargetMember(elements[0]);
                if (targetMember == null) {
                    return;
                }
                ChangeSignatureHandler handler = getChangeSignatureHandler(targetMember.getLanguage());
                if (handler == null) {
                    return;
                }
                handler.invoke(project, new PsiElement[]{targetMember}, dataContext);
            }
        };
    }

    @Nullable
    private static ChangeSignatureHandler getChangeSignatureHandler(Language language) {
        return RefactoringSupportProvider.forLanguage(language).getChangeSignatureHandler();
    }
}
