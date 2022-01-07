package com.intellij.codeInsight.preview;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import consulo.logging.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.LightweightHint;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

import static com.intellij.codeInsight.hint.HintManagerImpl.getHintPosition;

public class ElementPreviewHintProvider implements ElementPreviewProvider {
  private static final Logger LOG = Logger.getInstance(ElementPreviewHintProvider.class);

  private static final int HINT_HIDE_FLAGS = HintManager.HIDE_BY_ANY_KEY |
                                             HintManager.HIDE_BY_OTHER_HINT |
                                             HintManager.HIDE_BY_SCROLLING |
                                             HintManager.HIDE_BY_TEXT_CHANGE |
                                             HintManager.HIDE_IF_OUT_OF_EDITOR;
  @Nullable
  private LightweightHint hint;

  @Override
  public boolean isSupportedFile(@Nonnull PsiFile psiFile) {
    for (PreviewHintProvider hintProvider : Extensions.getExtensions(PreviewHintProvider.EP_NAME)) {
      if (hintProvider.isSupportedFile(psiFile)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void show(@Nonnull PsiElement element, @Nonnull Editor editor, @Nonnull Point point, boolean keyTriggered) {
    LightweightHint newHint = getHint(element);
    hideCurrentHintIfAny();
    if (newHint == null) {
      return;
    }

    hint = newHint;
    HintManagerImpl.getInstanceImpl().showEditorHint(newHint, editor,
                                                     getHintPosition(newHint, editor, editor.xyToLogicalPosition(point), HintManager.RIGHT_UNDER),
                                                     HINT_HIDE_FLAGS, 0, false);
  }

  private void hideCurrentHintIfAny() {
    if (hint != null) {
      hint.hide();
      hint = null;
    }
  }

  @Override
  public void hide(@Nullable PsiElement element, @Nonnull Editor editor) {
    hideCurrentHintIfAny();
  }

  @Nullable
  private static LightweightHint getHint(@Nonnull PsiElement element) {
    for (PreviewHintProvider hintProvider : Extensions.getExtensions(PreviewHintProvider.EP_NAME)) {
      JComponent preview;
      try {
        preview = hintProvider.getPreviewComponent(element);
      }
      catch (Exception e) {
        LOG.error(e);
        continue;
      }
      if (preview != null) {
        return new LightweightHint(preview);
      }
    }
    return null;
  }
}