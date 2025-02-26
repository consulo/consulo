// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hint;

import consulo.application.Application;
import consulo.codeEditor.*;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUIUtil;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.reference.SoftReference;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.language.editor.hint.HintManager;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;

public class EditorFragmentComponent extends JPanel {
  private static final Logger LOG = Logger.getInstance(EditorFragmentComponent.class);
  private static final Key<WeakReference<LightweightHintImpl>> CURRENT_HINT = Key.create("EditorFragmentComponent.currentHint");
  private static final int LINE_BORDER_THICKNESS = 1;
  private static final int EMPTY_BORDER_THICKNESS = 2;

  private EditorFragmentComponent(Component component, EditorEx editor, int startLine, int endLine, boolean showFolding, boolean showGutter) {
    editor.setPurePaintingMode(true);
    try {
      doInit(component, editor, startLine, endLine, showFolding, showGutter);
    }
    finally {
      editor.setPurePaintingMode(false);
    }
  }

  private void doInit(Component anchorComponent, EditorEx editor, int startLine, int endLine, boolean showFolding, boolean showGutter) {
    boolean newRendering = editor instanceof RealEditor;
    int savedScrollOffset = newRendering ? 0 : editor.getScrollingModel().getHorizontalScrollOffset();

    FoldingModelEx foldingModel = editor.getFoldingModel();
    boolean isFoldingEnabled = foldingModel.isFoldingEnabled();
    if (!showFolding) {
      foldingModel.setFoldingEnabled(false);
    }
    int textImageWidth;
    int markersImageWidth;
    int textImageHeight;
    BufferedImage textImage;
    BufferedImage markersImage;
    JComponent rowHeader;
    try {
      Document doc = editor.getDocument();
      int endOffset = endLine < doc.getLineCount() ? doc.getLineEndOffset(Math.max(0, endLine - 1)) : doc.getTextLength();
      int widthAdjustment = newRendering ? EditorUtil.getSpaceWidth(Font.PLAIN, editor) : 0;
      textImageWidth = Math.min(editor.getMaxWidthInRange(doc.getLineStartOffset(startLine), endOffset) + widthAdjustment, getWidthLimit(editor));

      Point p1 = editor.logicalPositionToXY(new LogicalPosition(startLine, 0));
      Point p2 = editor.logicalPositionToXY(new LogicalPosition(Math.max(endLine, startLine + 1), 0));
      int y1 = p1.y;
      int y2 = p2.y;
      textImageHeight = y2 - y1 == 0 ? editor.getLineHeight() : y2 - y1;
      LOG.assertTrue(textImageHeight > 0, "Height: " + textImageHeight + "; startLine:" + startLine + "; endLine:" + endLine + "; p1:" + p1 + "; p2:" + p2);

      if (savedScrollOffset > 0) {
        editor.getScrollingModel().scrollHorizontally(0);
      }

      textImage = UIUtil.createImage(anchorComponent == null ? editor.getContentComponent() : anchorComponent, textImageWidth, textImageHeight, BufferedImage.TYPE_INT_RGB);
      Graphics textGraphics = textImage.getGraphics();
      EditorUIUtil.setupAntialiasing(textGraphics);

      if (showGutter) {
        rowHeader = editor.getGutterComponentEx().getComponent();
        markersImageWidth = Math.max(1, rowHeader.getWidth());

        markersImage = UIUtil.createImage(editor.getComponent(), markersImageWidth, textImageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics markerGraphics = markersImage.getGraphics();
        EditorUIUtil.setupAntialiasing(markerGraphics);

        markerGraphics.translate(0, -y1);
        markerGraphics.setClip(0, y1, rowHeader.getWidth(), textImageHeight);
        markerGraphics.setColor(TargetAWT.to(getBackgroundColor(editor)));
        markerGraphics.fillRect(0, y1, rowHeader.getWidth(), textImageHeight);
        rowHeader.paint(markerGraphics);
      }
      else {
        markersImageWidth = 0;
        rowHeader = null;
        markersImage = null;
      }

      textGraphics.translate(0, -y1);
      textGraphics.setClip(0, y1, textImageWidth, textImageHeight);
      boolean wasVisible = editor.setCaretVisible(false);
      editor.getContentComponent().paint(textGraphics);
      if (wasVisible) {
        editor.setCaretVisible(true);
      }
    }
    finally {
      if (!showFolding) {
        foldingModel.setFoldingEnabled(isFoldingEnabled);
      }
    }

    if (savedScrollOffset > 0) {
      editor.getScrollingModel().scrollHorizontally(savedScrollOffset);
    }

    JComponent component = new JComponent() {
      @Override
      public Dimension getPreferredSize() {
        return new Dimension(textImageWidth + markersImageWidth, textImageHeight);
      }

      @Override
      protected void paintComponent(Graphics graphics) {
        if (markersImage != null) {
          UIUtil.drawImage(graphics, markersImage, 0, 0, null);
          UIUtil.drawImage(graphics, textImage, rowHeader.getWidth(), 0, null);
        }
        else {
          UIUtil.drawImage(graphics, textImage, 0, 0, null);
        }
      }
    };

    setLayout(new BorderLayout());
    add(component);

    setBorder(createEditorFragmentBorder(editor));
  }

  private static int getWidthLimit(@Nonnull Editor editor) {
    Component component = editor.getComponent();
    int screenWidth = ScreenUtil.getScreenRectangle(component).width;
    if (screenWidth > 0) return screenWidth;
    Window window = SwingUtilities.getWindowAncestor(component);
    return window == null ? Integer.MAX_VALUE : window.getWidth();
  }

  /**
   * @param y {@code y} coordinate in layered pane coordinate system.
   */
  @Nullable
  static LightweightHintImpl showEditorFragmentHintAt(
    Editor editor,
    TextRange range,
    int y,
    boolean showUpward,
    boolean showFolding,
    boolean hideByAnyKey,
    boolean hideByScrolling,
    boolean useCaretRowBackground
  ) {
    if (Application.get().isUnitTestMode()) return null;
    Document document = editor.getDocument();

    int startOffset = range.getStartOffset();
    int startLine = document.getLineNumber(startOffset);
    CharSequence text = document.getCharsSequence();
    // There is a possible case that we have a situation like below:
    //    line 1
    //    line 2 <fragment start>
    //    line 3<fragment end>
    // We don't want to include 'line 2' to the target fragment then.
    boolean incrementLine = false;
    for (int offset = startOffset, max = Math.min(range.getEndOffset(), text.length()); offset < max; offset++) {
      char c = text.charAt(offset);
      incrementLine = StringUtil.isWhiteSpace(c);
      if (!incrementLine || c == '\n') {
        break;
      }
    }
    if (incrementLine) {
      startLine++;
    }

    int endLine = Math.min(document.getLineNumber(range.getEndOffset()) + 1, document.getLineCount() - 1);

    if (startLine >= endLine) return null;

    EditorFragmentComponent fragmentComponent = createEditorFragmentComponent(editor, startLine, endLine, showFolding, true, useCaretRowBackground);

    if (showUpward) {
      y -= fragmentComponent.getPreferredSize().height;
      y = Math.max(0, y);
    }

    final JComponent c = editor.getComponent();
    int x = SwingUtilities.convertPoint(c, new Point(JBUIScale.scale(-3), 0), UIUtil.getRootPane(c)).x; //IDEA-68016

    LightweightHintImpl currentHint = SoftReference.dereference(editor.getUserData(CURRENT_HINT));
    if (currentHint != null) currentHint.hide();

    Point p = new Point(x, y);
    LightweightHintImpl hint = new MyComponentHint(fragmentComponent);
    HintManagerImpl.getInstanceImpl().showEditorHint(
      hint,
      editor,
      p,
      (hideByAnyKey ? HintManager.HIDE_BY_ANY_KEY : 0) |
        (hideByScrolling ? HintManager.HIDE_BY_SCROLLING : 0) |
        HintManager.HIDE_BY_TEXT_CHANGE |
        HintManager.HIDE_BY_MOUSEOVER,
      0,
      false,
      new HintHint(editor.getContentComponent(), p)
    );
    editor.putUserData(CURRENT_HINT, new WeakReference<>(hint));
    return hint;
  }

  public static EditorFragmentComponent createEditorFragmentComponent(
    Editor editor,
    int startLine,
    int endLine,
    boolean showFolding,
    boolean showGutter
  ) {
    return createEditorFragmentComponent(editor, startLine, endLine, showFolding, showGutter, true);
  }

  private static EditorFragmentComponent createEditorFragmentComponent(
    Editor editor,
    int startLine,
    int endLine,
    boolean showFolding,
    boolean showGutter,
    boolean useCaretRowBackground
  ) {
    return createEditorFragmentComponent(null, editor, startLine, endLine, showFolding, showGutter, useCaretRowBackground);
  }

  /**
   * @param component Should be provided if editor is not currently displayable.
   *                  Makes for correct rendering on multi-monitor configurations.
   */
  private static EditorFragmentComponent createEditorFragmentComponent(
    Component component,
    Editor editor,
    int startLine,
    int endLine,
    boolean showFolding,
    boolean showGutter,
    boolean useCaretRowBackground
  ) {
    final EditorEx editorEx = (EditorEx)editor;
    final ColorValue old = editorEx.getBackgroundColor();
    ColorValue backColor = getBackgroundColor(editor, useCaretRowBackground);
    editorEx.setBackgroundColor(backColor);
    EditorFragmentComponent fragmentComponent = new EditorFragmentComponent(component, editorEx, startLine, endLine, showFolding, showGutter);
    fragmentComponent.setBackground(TargetAWT.to(backColor));

    editorEx.setBackgroundColor(old);
    return fragmentComponent;
  }

  @Nullable
  public static LightweightHintImpl showEditorFragmentHint(Editor editor, TextRange range, boolean showFolding, boolean hideByAnyKey) {
    if (!(editor instanceof EditorEx)) return null;
    JRootPane rootPane = editor.getComponent().getRootPane();
    if (rootPane == null) return null;
    JLayeredPane layeredPane = rootPane.getLayeredPane();
    int lineHeight = editor.getLineHeight();
    int overhang = editor.getScrollingModel().getVisibleArea().y - editor.logicalPositionToXY(editor.offsetToLogicalPosition(range.getEndOffset())).y;
    int yRelative = overhang > 0 && overhang < lineHeight ? lineHeight - overhang + JBUIScale.scale(LINE_BORDER_THICKNESS + EMPTY_BORDER_THICKNESS) : 0;
    Point point = SwingUtilities.convertPoint(((EditorEx)editor).getScrollPane().getViewport(), -2, yRelative, layeredPane);
    return showEditorFragmentHintAt(editor, range, point.y, true, showFolding, hideByAnyKey, true, false);
  }

  public static ColorValue getBackgroundColor(Editor editor) {
    return getBackgroundColor(editor, true);
  }

  public static ColorValue getBackgroundColor(Editor editor, boolean useCaretRowBackground) {
    EditorColorsScheme colorsScheme = editor.getColorsScheme();
    ColorValue color = colorsScheme.getColor(EditorColors.CARET_ROW_COLOR);
    if (!useCaretRowBackground || color == null) {
      color = colorsScheme.getDefaultBackground();
    }
    return color;
  }

  @Nonnull
  public static CompoundBorder createEditorFragmentBorder(@Nonnull Editor editor) {
    ColorValue borderColor = editor.getColorsScheme().getColor(EditorColors.SELECTED_TEARLINE_COLOR);
    Border outsideBorder = JBUI.Borders.customLine(TargetAWT.to(borderColor), LINE_BORDER_THICKNESS);
    Border insideBorder = JBUI.Borders.empty(EMPTY_BORDER_THICKNESS, EMPTY_BORDER_THICKNESS);
    return BorderFactory.createCompoundBorder(outsideBorder, insideBorder);
  }

  public static int getAvailableVisualLinesAboveEditor(@Nonnull Editor editor) {
    int availableVisualLines = 2;
    JComponent editorComponent = editor.getComponent();
    Container editorComponentParent = editorComponent.getParent();
    if (editorComponentParent != null) {
      JRootPane rootPane = editorComponent.getRootPane();
      if (rootPane != null) {
        Container contentPane = rootPane.getContentPane();
        if (contentPane != null) {
          int y = SwingUtilities.convertPoint(editorComponentParent, editorComponent.getLocation(), contentPane).y;
          int visualLines = y / editor.getLineHeight();
          availableVisualLines = Math.max(availableVisualLines, visualLines);
        }
      }
    }
    return availableVisualLines;
  }

  private static class MyComponentHint extends LightweightHintImpl {
    MyComponentHint(JComponent component) {
      super(component);
      setForceLightweightPopup(true);
    }

    @Override
    public void hide() {
      // needed for Alt-Q multiple times
      // Q: not good?
      SwingUtilities.invokeLater(() -> super.hide());
    }
  }
}
