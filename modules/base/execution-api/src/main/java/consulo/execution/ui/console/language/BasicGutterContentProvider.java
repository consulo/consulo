package consulo.execution.ui.console.language;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.ui.ex.JBColor;
import consulo.util.lang.StringUtil;

import org.jspecify.annotations.Nullable;
import java.awt.*;

public class BasicGutterContentProvider extends GutterContentProvider {
  public static final String EVAL_OUT_MARKER = "\u200C";
  public static final String EVAL_IN_MARKER = EVAL_OUT_MARKER + EVAL_OUT_MARKER;

  protected final boolean isLineRelationshipComputable;

  public BasicGutterContentProvider() {
    this(true);
  }

  /**
   * @param isLineRelationshipComputable true if you can compute line relationship for all lines (override {@link #doIsShowSeparatorLine})
   */
  public BasicGutterContentProvider(boolean isLineRelationshipComputable) {
    this.isLineRelationshipComputable = isLineRelationshipComputable;
  }

  @Override
  public boolean hasText() {
    return false;
  }

  @Override
  public @Nullable String getText(int line, Editor editor) {
    return null;
  }

  @Override
  public @Nullable String getToolTip(int line, Editor editor) {
    return null;
  }

  @Override
  public void doAction(int line, Editor editor) {
  }

  @Override
  public void beforeEvaluate(Editor editor) {
    Document document = editor.getDocument();
    document.insertString(document.getTextLength(), EVAL_IN_MARKER);
  }

  @Override
  public boolean drawIcon(int line, Graphics g, int y, Editor editor) {
    int markerCount = getMarkerCount(line, editor.getDocument());
    if (markerCount == EVAL_OUT_MARKER.length() || markerCount == EVAL_IN_MARKER.length()) {
      g.setColor(JBColor.GRAY);
      g.drawString(markerCount == EVAL_IN_MARKER.length() ? ">" : "<", 3, y);
      return true;
    }
    return false;
  }

  @Override
  public final boolean isShowSeparatorLine(int line, Editor editor) {
    Document document = editor.getDocument();
    if ((line + 1) >= document.getLineCount()) {
      return true;
    }

    int markerCount = getMarkerCount(line, document);
    if (markerCount == EVAL_IN_MARKER.length()) {
      return getMarkerCount(line + 1, document) != EVAL_OUT_MARKER.length();
    }
    else if (!isLineRelationshipComputable && markerCount == EVAL_OUT_MARKER.length()) {
      return getMarkerCount(line + 1, document) == EVAL_IN_MARKER.length();
    }
    else if (!isLineRelationshipComputable && markerCount == 0) {
      return getMarkerCount(line + 1, document) != 0 || (line != 0 && getMarkerCount(line - 1, document) != 0);
    }
    else {
      return doIsShowSeparatorLine(line, editor, document);
    }
  }

  protected boolean doIsShowSeparatorLine(int line, Editor editor, Document document) {
    return isLineRelationshipComputable;
  }

  private static int getMarkerCount(int line, Document document) {
    return StringUtil.countChars(document.getImmutableCharSequence(), EVAL_IN_MARKER.charAt(0), document.getLineStartOffset(line), true);
  }
}