package com.intellij.codeInsight.editorActions.enter;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 5/30/11 2:23 PM
 */
public class EnterHandlerDelegateAdapter implements EnterHandlerDelegate {

  @Override
  public Result preprocessEnter(@Nonnull PsiFile file,
                                @Nonnull Editor editor,
                                @Nonnull Ref<Integer> caretOffset,
                                @Nonnull Ref<Integer> caretAdvance,
                                @Nonnull DataContext dataContext,
                                EditorActionHandler originalHandler)
  {
    return Result.Continue;
  }

  @Override
  public Result postProcessEnter(@Nonnull PsiFile file, @Nonnull Editor editor, @Nonnull DataContext dataContext) {
    return Result.Continue;
  }
}
