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
package consulo.language.editor.refactoring.rename.inplace;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.editor.refactoring.rename.RenamePsiElementProcessor;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.TemplateState;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.undoRedo.internal.StartMarkAction;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * User: anna
 * Date: 11/9/11
 */
@ExtensionImpl(id = "member", order = "after variable")
public class MemberInplaceRenameHandler extends VariableInplaceRenameHandler {
  @Override
  protected boolean isAvailable(PsiElement element, Editor editor, PsiFile file) {
    final PsiElement nameSuggestionContext = file.findElementAt(editor.getCaretModel().getOffset());
    if (element == null && LookupManager.getActiveLookup(editor) != null) {
      element = PsiTreeUtil.getParentOfType(nameSuggestionContext, PsiNamedElement.class);
    }
    final RefactoringSupportProvider
      supportProvider = element != null ? RefactoringSupportProvider.forLanguage(element.getLanguage()) : null;
    return editor.getSettings().isVariableInplaceRenameEnabled()
           && supportProvider != null
           && supportProvider.isMemberInplaceRenameAvailable(element, nameSuggestionContext);
  }

  @Override
  public InplaceRefactoring doRename(@Nonnull final PsiElement elementToRename, final Editor editor, final DataContext dataContext) {
    if (elementToRename instanceof PsiNameIdentifierOwner) {
      final RenamePsiElementProcessor processor = RenamePsiElementProcessor.forElement(elementToRename);
      if (processor.isInplaceRenameSupported()) {
        final StartMarkAction startMarkAction = StartMarkAction.canStart(elementToRename.getProject());
        if (startMarkAction == null || processor.substituteElementToRename(elementToRename, editor) == elementToRename) {
          processor.substituteElementToRename(elementToRename, editor, new Consumer<PsiElement>() {
            @Override
            public void accept(PsiElement element) {
              final MemberInplaceRenamer renamer = createMemberRenamer(element, (PsiNameIdentifierOwner)elementToRename, editor);
              boolean startedRename = renamer.performInplaceRename();
              if (!startedRename) {
                performDialogRename(elementToRename, editor, dataContext);
              }
            }
          });
          return null;
        }
        else {
          final InplaceRefactoring inplaceRefactoring = editor.getUserData(InplaceRefactoring.INPLACE_RENAMER);
          if (inplaceRefactoring != null && inplaceRefactoring.getClass() == MemberInplaceRenamer.class) {
            final TemplateState templateState = TemplateManager.getInstance(editor.getProject()).getTemplateState(EditorWindow.getTopLevelEditor(editor));
            if (templateState != null) {
              templateState.gotoEnd(true);
            }
          }
        }
      }
    }
    performDialogRename(elementToRename, editor, dataContext);
    return null;
  }

  @Nonnull
  protected MemberInplaceRenamer createMemberRenamer(@Nonnull PsiElement element, PsiNameIdentifierOwner elementToRename, Editor editor) {
    return new MemberInplaceRenamer(elementToRename, element, editor);
  }
}
