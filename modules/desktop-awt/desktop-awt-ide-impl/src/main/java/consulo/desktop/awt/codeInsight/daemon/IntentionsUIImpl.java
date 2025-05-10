// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.codeInsight.daemon;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.codeInsight.daemon.impl.IntentionsUI;
import consulo.language.editor.hint.HintManager;
import consulo.ide.impl.idea.codeInsight.intention.impl.CachedIntentions;
import consulo.ide.impl.idea.codeInsight.intention.impl.IntentionHintComponent;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

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
  public IntentionHintComponent getLastIntentionHint() {
    return myLastIntentionHint;
  }

  @Override
  @RequiredUIAccess
  public void update(@Nonnull CachedIntentions cachedIntentions, boolean actionsChanged) {
    UIAccess.assertIsUIThread();
    Editor editor = cachedIntentions.getEditor();
    if (editor == null) return;
    if (!ApplicationManager.getApplication().isUnitTestMode() && !editor.getContentComponent().hasFocus()) return;
    if (!actionsChanged) return;

    Project project = cachedIntentions.getProject();
    LogicalPosition caretPos = editor.getCaretModel().getLogicalPosition();
    Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    Point xy = editor.logicalPositionToXY(caretPos);

    hide();
    if (!HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(false) &&
        visibleArea.contains(xy) &&
        editor.getSettings().isShowIntentionBulb() &&
        editor.getCaretModel().getCaretCount() == 1 &&
        cachedIntentions.showBulb()) {
      myLastIntentionHint = IntentionHintComponent.showIntentionHint(project, cachedIntentions.getFile(), editor, false, cachedIntentions);
    }
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
