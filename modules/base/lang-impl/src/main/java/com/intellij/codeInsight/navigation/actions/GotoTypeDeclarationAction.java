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

package com.intellij.codeInsight.navigation.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.codeInsight.TargetElementUtil;
import consulo.codeInsight.TargetElementUtilEx;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
  public void update(final AnActionEvent event) {
    if (Extensions.getExtensions(TypeDeclarationProvider.EP_NAME).length == 0) {
      event.getPresentation().setVisible(false);
    }
    else {
      super.update(event);
    }
  }

  @Override
  @RequiredUIAccess
  public void invoke(@Nonnull final Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    int offset = editor.getCaretModel().getOffset();
    PsiElement[] symbolTypes = findSymbolTypes(editor, offset);
    if (symbolTypes == null || symbolTypes.length == 0) return;
    if (symbolTypes.length == 1) {
      navigate(project, symbolTypes[0]);
    }
    else {
      NavigationUtil.getPsiElementPopup(symbolTypes, CodeInsightBundle.message("choose.type.popup.title")).showInBestPositionFor(editor);
    }
  }

  private static void navigate(@Nonnull Project project, @Nonnull PsiElement symbolType) {
    PsiElement element = symbolType.getNavigationElement();
    assert element != null : "SymbolType :" + symbolType + "; file: " + symbolType.getContainingFile();
    VirtualFile file = element.getContainingFile().getVirtualFile();
    if (file != null) {
      OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, element.getTextOffset());
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
    final PsiElement[] psiElements = findSymbolTypes(editor, offset);
    if (psiElements != null && psiElements.length > 0) return psiElements[0];
    return null;
  }

  @Nullable
  @RequiredReadAction
  public static PsiElement[] findSymbolTypes(Editor editor, int offset) {
    Set<String> flags = ContainerUtil
            .newHashSet(TargetElementUtilEx.REFERENCED_ELEMENT_ACCEPTED, TargetElementUtilEx.ELEMENT_NAME_ACCEPTED, TargetElementUtilEx.LOOKUP_ITEM_ACCEPTED);
    PsiElement targetElement = TargetElementUtil.findTargetElement(editor, flags, offset);

    if (targetElement != null) {
      final PsiElement[] symbolType = getSymbolTypeDeclarations(targetElement, editor, offset);
      return symbolType == null ? PsiElement.EMPTY_ARRAY : symbolType;
    }

    final PsiReference psiReference = TargetElementUtil.findReference(editor, offset);
    if (psiReference instanceof PsiPolyVariantReference) {
      final ResolveResult[] results = ((PsiPolyVariantReference)psiReference).multiResolve(false);
      Set<PsiElement> types = new HashSet<PsiElement>();

      for (ResolveResult r : results) {
        final PsiElement[] declarations = getSymbolTypeDeclarations(r.getElement(), editor, offset);
        if (declarations != null) {
          for (PsiElement declaration : declarations) {
            assert declaration != null;
            types.add(declaration);
          }
        }
      }

      if (!types.isEmpty()) return PsiUtilCore.toPsiElementArray(types);
    }

    return null;
  }

  @Nullable
  @RequiredReadAction
  private static PsiElement[] getSymbolTypeDeclarations(final PsiElement targetElement, Editor editor, int offset) {
    for (TypeDeclarationProvider provider : Extensions.getExtensions(TypeDeclarationProvider.EP_NAME)) {
      PsiElement[] result = provider.getSymbolTypeDeclarations(targetElement, editor, offset);
      if (result != null) {
        return result;
      }
    }
    return null;
  }
}
