package consulo.execution.ui.console.language;

import consulo.codeEditor.Editor;
import org.jspecify.annotations.Nullable;

import java.awt.*;

public abstract class GutterContentProvider {
  public static final int MAX_LINE_END_GUTTER_WIDTH_IN_CHAR = 2;

  public void beforeUiComponentUpdate(Editor editor) {
  }

  public void documentCleared(Editor editor) {
  }

  public void beforeEvaluate(Editor editor) {
  }

  public abstract boolean hasText();

  public abstract @Nullable String getText(int line, Editor editor);

  public abstract @Nullable String getToolTip(int line, Editor editor);

  public abstract void doAction(int line, Editor editor);

  public abstract boolean drawIcon(int line, Graphics g, int y, Editor editor);

  public boolean isShowSeparatorLine(int line, Editor editor) {
    return true;
  }

  public int getLineStartGutterOverlap(Editor editor) {
    return 0;
  }
}