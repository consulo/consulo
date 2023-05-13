// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor;

import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.GutterMark;
import consulo.ui.ex.action.ActionGroup;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntUnaryOperator;

public interface EditorGutterComponentEx extends EditorGutter {
  /**
   * The key to retrieve a logical editor line position of a latest actionable click inside the gutter.
   * Available to gutter popup actions (see {@link #setGutterPopupGroup(ActionGroup)},
   * {@link GutterIconRenderer#getPopupMenuActions()}, {@link TextAnnotationGutterProvider#getPopupActions(int, Editor)})
   */
  public static final Key<Integer> LOGICAL_LINE_AT_CURSOR = Key.create("EditorGutter.LOGICAL_LINE_AT_CURSOR");

  /**
   * The key to retrieve a editor gutter icon center position of a latest actionable click inside the gutter.
   * Available to gutter popup actions (see {@link #setGutterPopupGroup(ActionGroup)},
   * {@link GutterIconRenderer#getPopupMenuActions()}, {@link TextAnnotationGutterProvider#getPopupActions(int, Editor)})
   */
  public static final Key<Point> ICON_CENTER_POSITION = Key.create("EditorGutter.ICON_CENTER_POSITION");

  @Nullable
  public abstract FoldRegion findFoldingAnchorAt(int x, int y);

  @Nonnull
  public abstract List<GutterMark> getGutterRenderers(int line);

  public abstract int getWhitespaceSeparatorOffset();

  public abstract void revalidateMarkup();

  public abstract int getLineMarkerAreaOffset();

  public abstract int getIconAreaOffset();

  public abstract int getLineMarkerFreePaintersAreaOffset();

  public abstract int getIconsAreaWidth();

  public abstract int getAnnotationsAreaOffset();

  public abstract int getAnnotationsAreaWidth();

  @Nullable
  public abstract Point getCenterPoint(GutterIconRenderer renderer);

  @Deprecated
  default void setLineNumberConvertor(@Nullable IntUnaryOperator lineNumberConvertor) {
    setLineNumberConvertor(lineNumberConvertor, null);
  }

  @Deprecated
  default void setLineNumberConvertor(@Nullable IntUnaryOperator lineNumberConvertor1, @Nullable IntUnaryOperator lineNumberConvertor2) {
    setLineNumberConverter(convertFromOperator(lineNumberConvertor1),
                           lineNumberConvertor2 == null ? null : convertFromOperator(lineNumberConvertor2));
  }

  private static LineNumberConverter convertFromOperator(IntUnaryOperator operator) {
    if (operator == null) {
      return LineNumberConverter.DEFAULT;
    }

    return new LineNumberConverter() {
      @Nullable
      @Override
      public Integer convert(@Nonnull Editor editor, int lineNumber) {
        return operator.applyAsInt(lineNumber);
      }

      @Nullable
      @Override
      public Integer getMaxLineNumber(@Nonnull Editor editor) {
        return editor.getDocument().getLineCount();
      }
    };
  }

  /**
   * Changes how line numbers are displayed in the gutter. Disables showing additional line numbers.
   *
   * @see #setLineNumberConverter(LineNumberConverter, LineNumberConverter)
   */
  default void setLineNumberConverter(@Nonnull LineNumberConverter converter) {
    setLineNumberConverter(converter, null);
  }

  /**
   * Changes how line numbers are displayed in the gutter.
   *
   * @param primaryConverter    converter for primary line number shown in gutter
   * @param additionalConverter if not {@code null}, defines an additional column of numbers to be displayed in the gutter
   */
  void setLineNumberConverter(@Nonnull LineNumberConverter primaryConverter, @Nullable LineNumberConverter additionalConverter);

  public abstract void setShowDefaultGutterPopup(boolean show);

  /**
   * When set to false, makes {@link #closeAllAnnotations()} a no-op and hides the corresponding context menu action.
   */
  public abstract void setCanCloseAnnotations(boolean canCloseAnnotations);

  public abstract void setGutterPopupGroup(@Nullable ActionGroup group);

  public abstract void setPaintBackground(boolean value);

  public abstract void setForceShowLeftFreePaintersArea(boolean value);

  public abstract void setForceShowRightFreePaintersArea(boolean value);

  public abstract void setInitialIconAreaWidth(int width);

  default boolean isInsideMarkerArea(@Nonnull MouseEvent e) {
    throw new AbstractMethodError("unsupported platform");
  }

  default int getHoveredFreeMarkersLine() {
    return -1;
  }

  @Nullable
  default GutterMark getGutterRenderer(final Point p) {
    return null;
  }

  default void repaint() {
  }

  default JComponent getComponent() {
    throw new AbstractMethodError("unsupported platform");
  }
}
