// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.language.editor.completion.CompletionType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Predicate;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class AutoPopupController {
  /**
   * Settings this user data key to the editor with a completion provider
   * makes the autopopup scheduling ignore the state of the corresponding setting.
   * <p/>
   * This doesn't affect other conditions when autopopup is not possible (e.g. power save mode).
   */
  public static final Key<Boolean> ALWAYS_AUTO_POPUP = Key.create("Always Show Completion Auto-Popup");
  /**
   * If editor has Boolean.TRUE by this key completion popup would be shown without advertising text at the bottom.
   */
  public static final Key<Boolean> NO_ADS = Key.create("Show Completion Auto-Popup without Ads");

  /**
   * If editor has Boolean.TRUE by this key completion popup would be shown every time when editor gets focus.
   * For example this key can be used for TextFieldWithAutoCompletion.
   * (TextFieldWithAutoCompletion looks like standard JTextField and completion shortcut is not obvious to be active)
   */
  public static final Key<Boolean> AUTO_POPUP_ON_FOCUS_GAINED = Key.create("Show Completion Auto-Popup On Focus Gained");


  public static AutoPopupController getInstance(@Nonnull Project project) {
    return project.getInstance(AutoPopupController.class);
  }

  public abstract void autoPopupMemberLookup(Editor editor, @Nullable Predicate<? super PsiFile> condition);

  public abstract void autoPopupMemberLookup(Editor editor, CompletionType completionType, @Nullable Predicate<? super PsiFile> condition);

  public abstract void scheduleAutoPopup(@Nonnull Editor editor, @Nonnull CompletionType completionType, @Nullable Predicate<? super PsiFile> condition);

  public abstract void scheduleAutoPopup(Editor editor);

  public abstract void showParameterInfo(Project project, final Editor editor, PsiFile file, int lbraceOffset, PsiElement highlightedElement, boolean requestFocus);

  public abstract void autoPopupParameterInfo(@Nonnull Editor editor, @Nullable Object highlightedMethod);

  public abstract void showCompletionPopup(@Nonnull Editor editor, @Nonnull CompletionType completionType, boolean invokedExplicitly, boolean autopopup, boolean synchronous);
}
