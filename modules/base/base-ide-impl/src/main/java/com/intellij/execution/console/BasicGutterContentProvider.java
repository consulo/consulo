package com.intellij.execution.console;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

  @Nullable
  @Override
  public String getText(int line, @Nonnull Editor editor) {
    return null;
  }

  @Nullable
  @Override
  public String getToolTip(int line, @Nonnull Editor editor) {
    return null;
  }

  @Override
  public void doAction(int line, @Nonnull Editor editor) {
  }

  @Override
  public void beforeEvaluate(@Nonnull Editor editor) {
    Document document = editor.getDocument();
    document.insertString(document.getTextLength(), EVAL_IN_MARKER);
  }

  @Override
  public boolean drawIcon(int line, @Nonnull Graphics g, int y, @Nonnull Editor editor) {
    int markerCount = getMarkerCount(line, editor.getDocument());
    if (markerCount == EVAL_OUT_MARKER.length() || markerCount == EVAL_IN_MARKER.length()) {
      g.setColor(JBColor.GRAY);
      g.drawString(markerCount == EVAL_IN_MARKER.length() ? ">" : "<", 3, y);
      return true;
    }
    return false;
  }

  @Override
  public final boolean isShowSeparatorLine(int line, @Nonnull Editor editor) {
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

  protected boolean doIsShowSeparatorLine(int line, @Nonnull Editor editor, @Nonnull Document document) {
    return isLineRelationshipComputable;
  }

  private static int getMarkerCount(int line, @Nonnull Document document) {
    return StringUtil.countChars(document.getImmutableCharSequence(), EVAL_IN_MARKER.charAt(0), document.getLineStartOffset(line), true);
  }
}