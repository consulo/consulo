package com.intellij.execution.console;

import com.intellij.openapi.editor.Editor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;

public abstract class GutterContentProvider {
  protected static final int MAX_LINE_END_GUTTER_WIDTH_IN_CHAR = 2;

  public void beforeUiComponentUpdate(@Nonnull Editor editor) {
  }

  public void documentCleared(@Nonnull Editor editor) {
  }

  public void beforeEvaluate(@Nonnull Editor editor) {
  }

  public abstract boolean hasText();

  @Nullable
  public abstract String getText(int line, @Nonnull Editor editor);

  @Nullable
  public abstract String getToolTip(int line, @Nonnull Editor editor);

  public abstract void doAction(int line, @Nonnull Editor editor);

  public abstract boolean drawIcon(int line, @Nonnull Graphics g, int y, @Nonnull Editor editor);

  public boolean isShowSeparatorLine(int line, @Nonnull Editor editor) {
    return true;
  }

  public int getLineStartGutterOverlap(@Nonnull Editor editor) {
    return 0;
  }
}