/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.navigation.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.TargetElementUtilExtender;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.action.TypeDeclarationProvider;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.ui.PopupNavigationUtil;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

public class GotoTypeDeclarationAction extends BaseCodeInsightAction implements CodeInsightActionHandler, DumbAware {

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return this;
    }

    @Override
    protected boolean isValidForLookup() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent event) {
        if (!TypeDeclarationProvider.EP_NAME.hasAnyExtensions()) {
            event.getPresentation().setVisible(false);
        }
        else {
            super.update(event);
        }
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        int offset = editor.getCaretModel().getOffset();
        PsiElement[] symbolTypes = findSymbolTypes(editor, offset);
        if (symbolTypes == null || symbolTypes.length == 0) {
            return;
        }
        if (symbolTypes.length == 1) {
            navigate(project, symbolTypes[0]);
        }
        else {
            editor.showPopupInBestPositionFor(PopupNavigationUtil.getPsiElementPopup(
                symbolTypes,
                CodeInsightLocalize.chooseTypePopupTitle().get()
            ));
        }
    }

    private static void navigate(@Nonnull Project project, @Nonnull PsiElement symbolType) {
        PsiElement element = symbolType.getNavigationElement();
        assert element != null : "SymbolType :" + symbolType + "; file: " + symbolType.getContainingFile();
        VirtualFile file = element.getContainingFile().getVirtualFile();
        if (file != null) {
            OpenFileDescriptorImpl descriptor = new OpenFileDescriptorImpl(project, file, element.getTextOffset());
            FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement findSymbolType(Editor editor, int offset) {
        PsiElement[] psiElements = findSymbolTypes(editor, offset);
        if (psiElements != null && psiElements.length > 0) {
            return psiElements[0];
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement[] findSymbolTypes(Editor editor, int offset) {
        Set<String> flags = Set.of(
            TargetElementUtilExtender.REFERENCED_ELEMENT_ACCEPTED,
            TargetElementUtilExtender.ELEMENT_NAME_ACCEPTED,
            TargetElementUtilExtender.LOOKUP_ITEM_ACCEPTED
        );
        PsiElement targetElement = TargetElementUtil.findTargetElement(editor, flags, offset);

        if (targetElement != null) {
            PsiElement[] symbolType = getSymbolTypeDeclarations(targetElement, editor, offset);
            return symbolType == null ? PsiElement.EMPTY_ARRAY : symbolType;
        }

        PsiReference psiReference = TargetElementUtil.findReference(editor, offset);
        if (psiReference instanceof PsiPolyVariantReference polyVariantReference) {
            ResolveResult[] results = polyVariantReference.multiResolve(false);
            Set<PsiElement> types = new HashSet<>();

            for (ResolveResult r : results) {
                PsiElement[] declarations = getSymbolTypeDeclarations(r.getElement(), editor, offset);
                if (declarations != null) {
                    for (PsiElement declaration : declarations) {
                        assert declaration != null;
                        types.add(declaration);
                    }
                }
            }

            if (!types.isEmpty()) {
                return PsiUtilCore.toPsiElementArray(types);
            }
        }

        return null;
    }

    @Nullable
    @RequiredReadAction
    private static PsiElement[] getSymbolTypeDeclarations(PsiElement targetElement, Editor editor, int offset) {
        for (TypeDeclarationProvider provider : TypeDeclarationProvider.EP_NAME.getExtensionList()) {
            PsiElement[] result = provider.getSymbolTypeDeclarations(targetElement, editor, offset);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
