// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.codeInsight.daemon;

import consulo.annotation.component.ServiceImpl;
import consulo.language.editor.internal.intention.IntentionsUI;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.internal.intention.CachedIntentions;
import consulo.desktop.awt.codeInsight.intention.impl.IntentionHintComponent;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.awt.*;

@Singleton
@ServiceImpl
public class IntentionsUIImpl extends IntentionsUI {

  private volatile IntentionHintComponent myLastIntentionHint;

  @Inject
  public IntentionsUIImpl(Project project) {
    super(project);
  }

  @Override
  @RequiredUIAccess
  public void update(CachedIntentions cachedIntentions, boolean actionsChanged) {
    UIAccess.assertIsUIThread();
    Editor editor = cachedIntentions.getEditor();
    if (editor == null) return;
    if (!ApplicationManager.getApplication().isUnitTestMode() && !editor.getContentComponent().hasFocus()) return;
    if (!actionsChanged) return;

    LogicalPosition caretPos = editor.getCaretModel().getLogicalPosition();
    Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    Point xy = editor.logicalPositionToXY(caretPos);

    hide();
    if (!HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(false) &&
        visibleArea.contains(xy) &&
        editor.getSettings().isShowIntentionBulb() &&
        editor.getCaretModel().getCaretCount() == 1 &&
        cachedIntentions.showBulb()) {
      showHint(cachedIntentions.getFile(), editor, cachedIntentions);
    }
  }

  @Override
  @RequiredUIAccess
  public void showHint(PsiFile file, Editor editor, CachedIntentions cachedIntentions) {
    myLastIntentionHint = IntentionHintComponent.showIntentionHint(cachedIntentions.getProject(), file, editor, false, cachedIntentions);
  }

  /**
   * The hint is what owns the list here - it is built with the bulb and expanded at once, so what is shown without
   * a bulb having been pressed is still the same component.
   */
  @Override
  @RequiredUIAccess
  public void showPopup(PsiFile file, Editor editor, CachedIntentions cachedIntentions) {
    myLastIntentionHint = IntentionHintComponent.showIntentionHint(cachedIntentions.getProject(), file, editor, true, cachedIntentions);
  }

  @Override
  @RequiredUIAccess
  public void hide() {
    UIAccess.assertIsUIThread();
    IntentionHintComponent hint = myLastIntentionHint;
    if (hint != null && !hint.isDisposed() && hint.isVisible()) {
      hint.hide();
      myLastIntentionHint = null;
    }
  }
}
