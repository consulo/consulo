// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.editor.impl.view;

import consulo.application.util.function.Processor;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.*;
import consulo.codeEditor.impl.ClipDetector;
import consulo.codeEditor.impl.FontInfo;
import consulo.codeEditor.impl.IterationState;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.*;
import consulo.colorScheme.TextAttributesEffectsBuilder.EffectDescriptor;
import consulo.desktop.awt.editor.impl.DesktopEditorImpl;
import consulo.desktop.awt.editor.impl.EditorComponentImpl;
import consulo.desktop.awt.editor.impl.FocusModeModel;
import consulo.desktop.awt.editor.impl.SoftWrapModelImpl;
import consulo.document.Document;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.ui.paint.EffectPainter;
import consulo.ide.impl.idea.util.containers.PeekableIteratorWrapper;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.paint.LinePainter2D;
import consulo.ui.ex.awt.paint.PaintUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.StandardColors;
import consulo.ui.util.ColorValueUtil;
import consulo.ui.util.LightDarkColorValue;
import consulo.util.collection.PeekableIterator;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.lang.Couple;
import consulo.util.lang.ObjectUtil;
import gnu.trove.TFloatArrayList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

import static consulo.colorScheme.TextAttributesEffectsBuilder.EffectSlot.FRAME_SLOT;

/**
 * Renders editor contents.
 */
public class EditorPainter implements TextDrawingCallback {
    private static final RGBColor CARET_LIGHT = new RGBColor(255, 255, 255);
    private static final RGBColor CARET_DARK = new RGBColor(0, 0, 0);
    private static final Stroke IME_COMPOSED_TEXT_UNDERLINE_STROKE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{0, 2, 0, 2}, 0);
    private static final int CARET_DIRECTION_MARK_SIZE = 5;
    private static final char IDEOGRAPHIC_SPACE = '\u3000'; // http://www.marathon-studios.com/unicode/U3000/Ideographic_Space
    private static final String WHITESPACE_CHARS = " \t" + IDEOGRAPHIC_SPACE;
    private static final Object ourCachedDot = ObjectUtil.sentinel("space symbol");

    private final EditorView myView;

    EditorPainter(EditorView view) {
        myView = view;
    }

    void paint(Graphics2D g) {
        new Session(myView, g).paint();
    }

    void repaintCarets() {
        DesktopEditorImpl editor = myView.getEditor();
        DesktopEditorImpl.CaretRectangle[] locations = editor.getCaretLocations(false);
        if (locations == null) {
            return;
        }
        int nominalLineHeight = myView.getNominalLineHeight();
        int topOverhang = myView.getTopOverhang();
        for (DesktopEditorImpl.CaretRectangle location : locations) {
            float x = (float) location.myPoint.getX();
            int y = (int) location.myPoint.getY() - topOverhang;
            float width = Math.max(location.myWidth, CARET_DIRECTION_MARK_SIZE);
            int xStart = (int) Math.floor(x - width);
            int xEnd = (int) Math.ceil(x + width);
            editor.getContentComponent().repaintEditorComponentExact(xStart, y, xEnd - xStart, nominalLineHeight);
        }
    }

    @Override
    public void drawChars(@Nonnull Graphics g, @Nonnull char[] data, int start, int end, int x, int y, Color color, Object f) {
        FontInfo fontInfo = (FontInfo) f;
        g.setFont(fontInfo.getFont());
        g.setColor(color);
        g.drawChars(data, start, end - start, x, y);
    }

    public static boolean isMarginShown(@Nonnull Editor editor) {
        return editor.getSettings().isRightMarginShown() &&
            editor.getColorsScheme().getColor(EditorColors.RIGHT_MARGIN_COLOR) != null &&
            (Registry.is("editor.show.right.margin.in.read.only.files") || editor.getDocument().isWritable());
    }

    public static int getIndentGuideShift(@Nonnull Editor editor) {
        return -Session.getTabGap(Session.getWhiteSpaceScale(editor)) / 2;
    }

    private static class Session {
        private final EditorView myView;
        private final DesktopEditorImpl myEditor;
        private final Document myDocument;
        private final CharSequence myText;
        private final MarkupModelEx myDocMarkup;
        private final MarkupModelEx myEditorMarkup;
        private final XCorrector myCorrector;
        private final Graphics2D myGraphics;
        private final Rectangle myClip;
        private final int myYShift;
        private final int myStartVisualLine;
        private final int myEndVisualLine;
        private final int myStartOffset;
        private final int myEndOffset;
        private final ClipDetector myClipDetector;
        private final IterationState.CaretData myCaretData;
        private final Map<Integer, Couple<Integer>> myVirtualSelectionMap;
        private final IntObjectMap<List<LineExtensionData>> myExtensionData = IntMaps.newIntObjectHashMap(); // key is visual line
        private final IntObjectMap<TextAttributes> myBetweenLinesAttributes = IntMaps.newIntObjectHashMap(); // key is bottom visual line
        private final int myLineHeight;
        private final int myAscent;
        private final int myDescent;
        private final ColorValue myDefaultBackgroundColor;
        private final ColorValue myBackgroundColor;
        private final int myMarginColumns;
        private final List<Consumer<Graphics2D>> myTextDrawingTasks = new ArrayList<>();
        private final JBUI.ScaleContext myScaleContext;
        private MarginPositions myMarginPositions;

        private Session(EditorView view, Graphics2D g) {
            myView = view;
            myEditor = myView.getEditor();
            myDocument = myEditor.getDocument();
            myText = myDocument.getImmutableCharSequence();
            myDocMarkup = myEditor.getFilteredDocumentMarkupModel();
            myEditorMarkup = myEditor.getMarkupModel();
            myCorrector = XCorrector.create(myView);
            myGraphics = g;
            myClip = myGraphics.getClipBounds();
            myYShift = -myClip.y;
            myStartVisualLine = myView.yToVisualLine(myClip.y);
            myEndVisualLine = myView.yToVisualLine(myClip.y + myClip.height - 1);
            myStartOffset = myView.visualLineToOffset(myStartVisualLine);
            myEndOffset = myView.visualLineToOffset(myEndVisualLine + 1);
            myClipDetector = new ClipDetector(myEditor, myClip);
            myCaretData = myEditor.isPaintSelection() ? IterationState.createCaretData(myEditor) : null;
            myVirtualSelectionMap = createVirtualSelectionMap(myEditor, myStartVisualLine, myEndVisualLine);
            myLineHeight = myView.getLineHeight();
            myAscent = myView.getAscent();
            myDescent = myView.getDescent();
            myDefaultBackgroundColor = myEditor.getColorsScheme().getDefaultBackground();
            myBackgroundColor = myEditor.getBackgroundColor();
            myMarginColumns = myEditor.getSettings().getRightMargin(myEditor.getProject());
            myScaleContext = JBUI.ScaleContext.create(myGraphics);
        }

        private void paint() {
            if (myEditor.getContentComponent().isOpaque()) {
                myGraphics.setColor(TargetAWT.to(myBackgroundColor));
                myGraphics.fillRect(myClip.x, myClip.y, myClip.width, myClip.height);
            }

            myGraphics.translate(0, -myYShift);

            if (paintPlaceholderText()) {
                paintCaret();
                return;
            }

            paintBackground();
            paintRightMargin();
            paintCustomRenderers();
            paintLineMarkersSeparators(myDocMarkup);
            paintLineMarkersSeparators(myEditorMarkup);
            paintTextWithEffects();
            paintHighlightersAfterEndOfLine(myDocMarkup);
            paintHighlightersAfterEndOfLine(myEditorMarkup);
            paintBorderEffect(myEditor.getHighlighter());
            paintBorderEffect(myDocMarkup);
            paintBorderEffect(myEditorMarkup);
            paintBlockInlays();
            paintCaret();
            paintComposedTextDecoration();

            myGraphics.translate(0, myYShift);
        }

        private boolean paintPlaceholderText() {
            CharSequence hintText = myEditor.getPlaceholder();
            EditorComponentImpl editorComponent = myEditor.getContentComponent();
            if (myDocument.getTextLength() > 0 ||
                hintText == null ||
                hintText.length() == 0 ||
                KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == editorComponent && !myEditor.getShowPlaceholderWhenFocused()) {
                return false;
            }

            hintText = SwingUtilities.layoutCompoundLabel(myGraphics.getFontMetrics(), hintText.toString(), null, 0, 0, 0, 0, SwingUtilities.calculateInnerArea(editorComponent, null), // account for insets
                new Rectangle(), new Rectangle(), 0);
            EditorFontType fontType = EditorFontType.PLAIN;
            ColorValue color = myEditor.getFoldingModel().getPlaceholderAttributes().getForegroundColor();
            TextAttributes attributes = myEditor.getPlaceholderAttributes();
            if (attributes != null) {
                int type = attributes.getFontType();
                if (type == Font.ITALIC) {
                    fontType = EditorFontType.ITALIC;
                }
                else if (type == Font.BOLD) {
                    fontType = EditorFontType.BOLD;
                }
                else if (type == (Font.ITALIC | Font.BOLD)) {
                    fontType = EditorFontType.BOLD_ITALIC;
                }

                ColorValue attColor = attributes.getForegroundColor();
                if (attColor != null) {
                    color = attColor;
                }
            }
            myGraphics.setColor(TargetAWT.to(color));
            myGraphics.setFont(myEditor.getColorsScheme().getFont(fontType));
            myGraphics.drawString(hintText.toString(), myView.getInsets().left, myView.getInsets().top + myAscent + myYShift);
            return true;
        }

        private void paintRightMargin() {
            if (!isMarginShown()) {
                return;
            }

            ColorValue visualGuidesColor = myEditor.getColorsScheme().getColor(EditorColors.VISUAL_INDENT_GUIDE_COLOR);
            if (visualGuidesColor != null) {
                myGraphics.setColor(TargetAWT.to(visualGuidesColor));
                for (Integer marginX : myCorrector.softMarginsX()) {
                    LinePainter2D.paint(myGraphics, marginX, 0, marginX, myClip.height);
                }
            }

            myGraphics.setColor(TargetAWT.to(myEditor.getColorsScheme().getColor(EditorColors.RIGHT_MARGIN_COLOR)));
            float baseMarginWidth = getBaseMarginWidth(myView);
            int baseMarginX = myCorrector.marginX(baseMarginWidth);
            if (myMarginPositions == null) {
                LinePainter2D.paint(myGraphics, baseMarginX, 0, baseMarginX, myClip.height);
            }
            else {
                int displayedLinesCount = myMarginPositions.x.length - 1;
                for (int i = 0; i <= displayedLinesCount; i++) {
                    int y = myMarginPositions.y[i];
                    int yStart = i == 0 ? 0 : y;
                    int yEnd = i == displayedLinesCount ? myClip.y + myClip.height : y + myLineHeight;
                    float width = myMarginPositions.x[i];
                    int x = width == 0 ? baseMarginX : (int) width;
                    myGraphics.fillRect(x, yStart, 1, yEnd - yStart);
                    if (i < displayedLinesCount) {
                        float nextWidth = myMarginPositions.x[i + 1];
                        int nextX = nextWidth == 0 ? baseMarginX : (int) nextWidth;
                        if (nextX != x) {
                            myGraphics.fillRect(Math.min(x, nextX), y + myLineHeight - 1, Math.abs(x - nextX) + 1, 1);
                        }
                    }
                }
            }
        }

        private static float getBaseMarginWidth(EditorView view) {
            Editor editor = view.getEditor();
            return editor.getSettings().getRightMargin(editor.getProject()) * view.getPlainSpaceWidth();
        }

        private boolean isMarginShown() {
            return EditorPainter.isMarginShown(myEditor);
        }

        private void paintBackground() {
            int lineCount = myEditor.getVisibleLineCount();
            boolean calculateMarginWidths = Registry.is("editor.adjust.right.margin") && isMarginShown() && myStartVisualLine < lineCount;
            myMarginPositions = calculateMarginWidths ? new MarginPositions(Math.min(myEndVisualLine, lineCount - 1) - myStartVisualLine + 2) : null;
            final LineWhitespacePaintingStrategy whitespacePaintingStrategy = new LineWhitespacePaintingStrategy(myEditor.getSettings());
            boolean paintAllSoftWraps = myEditor.getSettings().isAllSoftWrapsShown();
            float whiteSpaceScale = getWhiteSpaceScale(myEditor);
            final BasicStroke whiteSpaceStroke = new BasicStroke(calcFeatureSize(1, whiteSpaceScale));

            PeekableIterator<Caret> caretIterator = null;
            if (myEditor.getInlayModel().hasBlockElements()) {
                Iterator<Caret> carets = myEditor.getCaretModel().getAllCarets().stream().filter(Caret::hasSelection).sorted(Comparator.comparingInt(Caret::getSelectionStart)).iterator();
                caretIterator = new PeekableIteratorWrapper<>(carets);
            }

            final VisualPosition primarySelectionStart = myEditor.getSelectionModel().getSelectionStartPosition();
            final VisualPosition primarySelectionEnd = myEditor.getSelectionModel().getSelectionEndPosition();

            LineLayout prefixLayout = myView.getPrefixLayout();
            if (myStartVisualLine == 0 && prefixLayout != null) {
                float width = prefixLayout.getWidth();
                TextAttributes attributes = myView.getPrefixAttributes();
                paintBackground(attributes, myCorrector.startX(myStartVisualLine), myYShift + myView.visualLineToY(0), width);
                myTextDrawingTasks.add(g -> {
                    g.setColor(TargetAWT.to(attributes.getForegroundColor()));
                    paintLineLayoutWithEffect(prefixLayout, myCorrector.startX(myStartVisualLine), myAscent + myYShift + myView.visualLineToY(0), attributes.getEffectColor(), attributes.getEffectType());
                });
            }

            int startX = myView.getInsets().left;
            int endX = myClip.x + myClip.width;
            int prevY = Math.max(myView.getInsets().top, myClip.y) + myYShift;
            VisualLinesIterator visLinesIterator = new VisualLinesIterator(myEditor, myStartVisualLine);
            while (!visLinesIterator.atEnd()) {
                int visualLine = visLinesIterator.getVisualLine();
                if (visualLine > myEndVisualLine + 1) {
                    break;
                }
                int y = visLinesIterator.getY() + myYShift;
                if (calculateMarginWidths) {
                    myMarginPositions.y[visualLine - myStartVisualLine] = y;
                }
                if (y > prevY) {
                    TextAttributes attributes = getBetweenLinesAttributes(visualLine, visLinesIterator.getVisualLineStartOffset(), Objects.requireNonNull(caretIterator));
                    myBetweenLinesAttributes.put(visualLine, attributes);
                    paintBackground(attributes.getBackgroundColor(), startX, prevY, endX - startX, y - prevY);
                }
                boolean dryRun = visualLine > myEndVisualLine;
                if (dryRun && !calculateMarginWidths) {
                    break;
                }
                boolean paintSoftWraps = paintAllSoftWraps || myEditor.getCaretModel().getLogicalPosition().line == visLinesIterator.getStartLogicalLine();
                int[] currentLogicalLine = new int[]{-1};
                paintLineFragments(visLinesIterator, y, new LineFragmentPainter() {
                    @Override
                    public void paintBeforeLineStart(TextAttributes attributes, boolean hasSoftWrap, int columnEnd, float xEnd, int y) {
                        if (dryRun) {
                            return;
                        }
                        paintBackground(attributes, startX, y, xEnd);
                        if (!hasSoftWrap) {
                            return;
                        }
                        paintSelectionOnSecondSoftWrapLineIfNecessary(visualLine, columnEnd, xEnd, y, primarySelectionStart, primarySelectionEnd);
                        if (paintSoftWraps) {
                            myTextDrawingTasks.add(g -> {
                                SoftWrapModelImpl softWrapModel = myEditor.getSoftWrapModel();
                                int symbolWidth = softWrapModel.getMinDrawingWidthInPixels(SoftWrapDrawingType.AFTER_SOFT_WRAP);
                                softWrapModel.doPaint(g, SoftWrapDrawingType.AFTER_SOFT_WRAP, (int) xEnd - symbolWidth, y, myLineHeight);
                            });
                        }
                    }

                    @Override
                    public void paint(VisualLineFragmentsIterator.Fragment fragment, int start, int end, TextAttributes attributes, float xStart, float xEnd, int y) {
                        if (dryRun) {
                            return;
                        }
                        FoldRegion foldRegion = fragment.getCurrentFoldRegion();
                        TextAttributes foldRegionInnerAttributes = foldRegion == null || !Registry.is("editor.highlight.foldings") ? null : getInnerHighlighterAttributes(foldRegion);
                        if (foldRegionInnerAttributes == null || !paintFoldingBackground(foldRegionInnerAttributes, xStart, y, xEnd - xStart, foldRegion)) {
                            paintBackground(attributes, xStart, y, xEnd - xStart);
                        }
                        Inlay inlay = fragment.getCurrentInlay();
                        if (inlay != null) {
                            TextAttributes attrs = attributes.clone();
                            myTextDrawingTasks.add(g -> {
                                inlay.getRenderer().paint(inlay, g, new Rectangle((int) xStart, y, inlay.getWidthInPixels(), myLineHeight), attrs);
                            });
                        }
                        else {
                            if (foldRegionInnerAttributes != null) {
                                attributes = TextAttributes.merge(attributes, foldRegionInnerAttributes);
                            }
                            if (attributes != null) {
                                attributes.forEachEffect((type, color) -> myTextDrawingTasks.add(g -> paintTextEffect(xStart, xEnd, y + myAscent, color, type, foldRegion != null)));
                            }
                            if (attributes != null) {
                                ColorValue color = attributes.getForegroundColor();
                                if (color != null) {
                                    myTextDrawingTasks.add(g -> g.setColor(TargetAWT.to(color)));
                                    myTextDrawingTasks.add(fragment.draw(xStart, y + myAscent, start, end));
                                }
                            }
                        }
                        if (foldRegion == null) {
                            int logicalLine = fragment.getStartLogicalLine();
                            if (logicalLine != currentLogicalLine[0]) {
                                whitespacePaintingStrategy.update(myText, myDocument.getLineStartOffset(logicalLine), myDocument.getLineEndOffset(logicalLine));
                                currentLogicalLine[0] = logicalLine;
                            }
                            paintWhitespace(xStart, y + myAscent, start, end, whitespacePaintingStrategy, fragment, whiteSpaceStroke, whiteSpaceScale);
                        }
                    }

                    @Override
                    public void paintAfterLineEnd(IterationState it, int columnStart, float x, int y) {
                        if (dryRun) {
                            return;
                        }
                        TextAttributes backgroundAttributes = it.getPastLineEndBackgroundAttributes().clone();
                        paintBackground(backgroundAttributes, x, y, endX - x);
                        int offset = it.getEndOffset();
                        SoftWrap softWrap = myEditor.getSoftWrapModel().getSoftWrap(offset);
                        if (softWrap == null) {
                            collectExtensions(visualLine, offset);
                            paintLineExtensionsBackground(visualLine, x, y);
                            paintVirtualSelectionIfNecessary(visualLine, columnStart, x, y);
                            myTextDrawingTasks.add(g -> {
                                int logicalLine = myDocument.getLineNumber(offset);
                                List<Inlay> inlays = myEditor.getInlayModel().getAfterLineEndElementsForLogicalLine(logicalLine);
                                if (!inlays.isEmpty()) {
                                    float curX = x + myView.getPlainSpaceWidth();
                                    for (Inlay inlay : inlays) {
                                        int width = inlay.getWidthInPixels();
                                        inlay.getRenderer().paint(inlay, g, new Rectangle((int) curX, y, width, myLineHeight), backgroundAttributes);
                                        curX += width;
                                    }
                                }
                                paintLineExtensions(visualLine, logicalLine, x, y + myAscent);
                            });
                        }
                        else {
                            paintSelectionOnFirstSoftWrapLineIfNecessary(visualLine, columnStart, x, y, primarySelectionStart, primarySelectionEnd);
                            if (paintSoftWraps) {
                                myTextDrawingTasks.add(g -> {
                                    myEditor.getSoftWrapModel().doPaint(g, SoftWrapDrawingType.BEFORE_SOFT_WRAP_LINE_FEED, (int) x, y, myLineHeight);
                                });
                            }
                        }
                    }
                }, calculateMarginWidths && !visLinesIterator.endsWithSoftWrap() && !visLinesIterator.startsWithSoftWrap() ? width -> myMarginPositions.x[visualLine - myStartVisualLine] = width : null);
                prevY = y + myLineHeight;
                visLinesIterator.advance();
            }
            if (calculateMarginWidths && myEndVisualLine >= lineCount - 1) {
                myMarginPositions.y[myMarginPositions.y.length - 1] = myMarginPositions.y[myMarginPositions.y.length - 2] + myLineHeight;
            }
        }

        private boolean paintFoldingBackground(TextAttributes innerAttributes, float x, int y, float width, @Nonnull FoldRegion foldRegion) {
            if (innerAttributes.getBackgroundColor() != null && !isSelected(foldRegion)) {
                paintBackground(innerAttributes, x, y, width);
                ColorValue borderColor = myEditor.getColorsScheme().getColor(EditorColors.FOLDED_TEXT_BORDER_COLOR);
                if (borderColor != null) {
                    Shape border = getBorderShape(x, y, width, myLineHeight, 2, false);
                    if (border != null) {
                        myGraphics.setColor(TargetAWT.to(borderColor));
                        myGraphics.fill(border);
                    }
                }
                return true;
            }
            else {
                return false;
            }
        }

        private static Map<Integer, Couple<Integer>> createVirtualSelectionMap(Editor editor, int startVisualLine, int endVisualLine) {
            HashMap<Integer, Couple<Integer>> map = new HashMap<>();
            for (Caret caret : editor.getCaretModel().getAllCarets()) {
                if (caret.hasSelection()) {
                    VisualPosition selectionStart = caret.getSelectionStartPosition();
                    VisualPosition selectionEnd = caret.getSelectionEndPosition();
                    if (selectionStart.line == selectionEnd.line) {
                        int line = selectionStart.line;
                        if (line >= startVisualLine && line <= endVisualLine) {
                            map.put(line, Couple.of(selectionStart.column, selectionEnd.column));
                        }
                    }
                }
            }
            return map;
        }

        private void paintVirtualSelectionIfNecessary(int visualLine, int columnStart, float xStart, int y) {
            Couple<Integer> selectionRange = myVirtualSelectionMap.get(visualLine);
            if (selectionRange == null || selectionRange.second <= columnStart) {
                return;
            }
            float startX = selectionRange.first <= columnStart ? xStart : (float) myView.visualPositionToXY(new VisualPosition(visualLine, selectionRange.first)).getX();
            float endX = (float) Math.min(myClip.x + myClip.width, myView.visualPositionToXY(new VisualPosition(visualLine, selectionRange.second)).getX());
            paintBackground(myEditor.getColorsScheme().getColor(EditorColors.SELECTION_BACKGROUND_COLOR), startX, y, endX - startX);
        }

        private void paintSelectionOnSecondSoftWrapLineIfNecessary(int visualLine, int columnEnd, float xEnd, int y, VisualPosition selectionStartPosition, VisualPosition selectionEndPosition) {
            if (selectionStartPosition.equals(selectionEndPosition) ||
                visualLine < selectionStartPosition.line ||
                visualLine > selectionEndPosition.line ||
                visualLine == selectionStartPosition.line && selectionStartPosition.column >= columnEnd) {
                return;
            }

            float startX =
                (selectionStartPosition.line == visualLine && selectionStartPosition.column > 0) ? (float) myView.visualPositionToXY(selectionStartPosition).getX() : myCorrector.startX(visualLine);
            float endX = (selectionEndPosition.line == visualLine && selectionEndPosition.column < columnEnd) ? (float) myView.visualPositionToXY(selectionEndPosition).getX() : xEnd;

            paintBackground(myEditor.getColorsScheme().getColor(EditorColors.SELECTION_BACKGROUND_COLOR), startX, y, endX - startX);
        }

        private void paintSelectionOnFirstSoftWrapLineIfNecessary(int visualLine, int columnStart, float xStart, int y, VisualPosition selectionStartPosition, VisualPosition selectionEndPosition) {
            if (selectionStartPosition.equals(selectionEndPosition) ||
                visualLine < selectionStartPosition.line ||
                visualLine > selectionEndPosition.line ||
                visualLine == selectionEndPosition.line && selectionEndPosition.column <= columnStart) {
                return;
            }

            float startX = selectionStartPosition.line == visualLine && selectionStartPosition.column > columnStart ? (float) myView.visualPositionToXY(selectionStartPosition).getX() : xStart;
            float endX = selectionEndPosition.line == visualLine ? (float) myView.visualPositionToXY(selectionEndPosition).getX() : myClip.x + myClip.width;

            paintBackground(myEditor.getColorsScheme().getColor(EditorColors.SELECTION_BACKGROUND_COLOR), startX, y, endX - startX);
        }

        private void paintBackground(TextAttributes attributes, float x, int y, float width) {
            if (attributes == null) {
                return;
            }
            paintBackground(attributes.getBackgroundColor(), x, y, width);
        }

        private void paintBackground(ColorValue color, float x, int y, float width) {
            paintBackground(color, x, y, width, myLineHeight);
        }

        private void paintBackground(ColorValue color, float x, int y, float width, int height) {
            if (width <= 0 || color == null || color.equals(myDefaultBackgroundColor) || color.equals(myBackgroundColor)) {
                return;
            }
            myGraphics.setColor(TargetAWT.to(color));
            myGraphics.fill(new Rectangle2D.Float(x, y, width, height));
        }

        private void paintCustomRenderers() {
            myGraphics.translate(0, myYShift);
            myEditorMarkup.processRangeHighlightersOverlappingWith(myStartOffset, myEndOffset, highlighter -> {
                CustomHighlighterRenderer customRenderer = highlighter.getCustomRenderer();
                if (customRenderer != null) {
                    int highlighterStart = highlighter.getStartOffset();
                    int highlighterEnd = highlighter.getEndOffset();
                    if (highlighterStart <= myEndOffset && highlighterEnd >= myStartOffset && myClipDetector.rangeCanBeVisible(highlighterStart, highlighterEnd)) {
                        customRenderer.paint(myEditor, highlighter, myGraphics);
                    }
                }
                return true;
            });
            myGraphics.translate(0, -myYShift);
        }

        private void paintLineMarkersSeparators(MarkupModelEx markupModel) {
            // we decrement startOffset to capture also line-range highlighters on the previous line,
            // cause they can render a separator visible on current line
            markupModel.processRangeHighlightersOverlappingWith(myStartOffset - 1, myEndOffset, highlighter -> {
                paintLineMarkerSeparator(highlighter);
                return true;
            });
        }

        private void paintLineMarkerSeparator(RangeHighlighter marker) {
            Color separatorColor = marker.getLineSeparatorColor();
            LineSeparatorRenderer lineSeparatorRenderer = marker.getLineSeparatorRenderer();
            if (separatorColor == null && lineSeparatorRenderer == null) {
                return;
            }
            boolean isTop = marker.getLineSeparatorPlacement() == SeparatorPlacement.TOP;
            int edgeOffset = isTop ? myDocument.getLineStartOffset(myDocument.getLineNumber(marker.getStartOffset())) : myDocument.getLineEndOffset(myDocument.getLineNumber(marker.getEndOffset()));
            int visualLine = myView.offsetToVisualLine(edgeOffset, !isTop);
            int y = myView.visualLineToY(visualLine) + (isTop ? 0 : myLineHeight) - 1 + myYShift;
            int startX = myCorrector.lineSeparatorStart(myClip.x);
            int endX = myCorrector.lineSeparatorEnd(myClip.x + myClip.width);
            myGraphics.setColor(separatorColor);
            if (lineSeparatorRenderer != null) {
                lineSeparatorRenderer.drawLine(myGraphics, startX, endX, y);
            }
            else {
                LinePainter2D.paint(myGraphics, startX, y, endX, y);
            }
        }

        private void paintTextWithEffects() {
            myTextDrawingTasks.forEach(t -> t.accept(myGraphics));
            ComplexTextFragment.flushDrawingCache(myGraphics);
        }

        @Nullable
        private TextAttributes getInnerHighlighterAttributes(@Nonnull FoldRegion region) {
            if (region.areInnerHighlightersMuted()) {
                return null;
            }
            List<RangeHighlighterEx> innerHighlighters = new ArrayList<>();
            collectVisibleInnerHighlighters(region, myEditorMarkup, innerHighlighters);
            collectVisibleInnerHighlighters(region, myDocMarkup, innerHighlighters);
            if (innerHighlighters.isEmpty()) {
                return null;
            }
            EditorColorsScheme colorsScheme = myEditor.getColorsScheme();
            innerHighlighters.sort(IterationState.createByLayerThenByAttributesComparator(colorsScheme));
            ColorValue fgColor = null;
            ColorValue bgColor = null;
            ColorValue effectColor = null;
            EffectType effectType = null;

            for (RangeHighlighter h : innerHighlighters) {
                TextAttributes attrs = h.getTextAttributes(colorsScheme);
                if (attrs == null) {
                    continue;
                }
                if (fgColor == null && attrs.getForegroundColor() != null) {
                    fgColor = attrs.getForegroundColor();
                }
                if (bgColor == null && attrs.getBackgroundColor() != null) {
                    bgColor = attrs.getBackgroundColor();
                }
                if (effectColor == null && attrs.getEffectColor() != null) {
                    EffectType type = attrs.getEffectType();
                    if (type != null && type != EffectType.BOXED && type != EffectType.ROUNDED_BOX && type != EffectType.STRIKEOUT) {
                        effectColor = attrs.getEffectColor();
                        effectType = type;
                    }
                }
            }
            return new TextAttributes(fgColor, bgColor, effectColor, effectType, Font.PLAIN);
        }

        private static void collectVisibleInnerHighlighters(@Nonnull FoldRegion region, @Nonnull MarkupModelEx markupModel, @Nonnull List<? super RangeHighlighterEx> highlighters) {
            int startOffset = region.getStartOffset();
            int endOffset = region.getEndOffset();
            markupModel.processRangeHighlightersOverlappingWith(startOffset, endOffset, h -> {
                if (h.isVisibleIfFolded() && h.getAffectedAreaStartOffset() >= startOffset && h.getAffectedAreaEndOffset() <= endOffset) {
                    highlighters.add(h);
                }
                return true;
            });
        }

        private float paintLineLayoutWithEffect(LineLayout layout, float x, float y, @Nullable ColorValue effectColor, @Nullable EffectType effectType) {
            paintTextEffect(x, x + layout.getWidth(), (int) y, effectColor, effectType, false);
            for (LineLayout.VisualFragment fragment : layout.getFragmentsInVisualOrder(x)) {
                fragment.draw(myGraphics, fragment.getStartX(), y);
                x = fragment.getEndX();
            }
            return x;
        }

        private void paintTextEffect(float xFrom, float xTo, int y, @Nullable ColorValue effectColor, @Nullable EffectType effectType, boolean allowBorder) {
            if (effectColor == null) {
                return;
            }
            myGraphics.setColor(TargetAWT.to(effectColor));
            int xStart = (int) xFrom;
            int xEnd = (int) xTo;
            if (effectType == EffectType.LINE_UNDERSCORE) {
                EffectPainter.LINE_UNDERSCORE.paint(myGraphics, xStart, y, xEnd - xStart, myDescent, myEditor.getColorsScheme().getFont(EditorFontType.PLAIN));
            }
            else if (effectType == EffectType.BOLD_LINE_UNDERSCORE) {
                EffectPainter.BOLD_LINE_UNDERSCORE.paint(myGraphics, xStart, y, xEnd - xStart, myDescent, myEditor.getColorsScheme().getFont(EditorFontType.PLAIN));
            }
            else if (effectType == EffectType.STRIKEOUT) {
                EffectPainter.STRIKE_THROUGH.paint(myGraphics, xStart, y, xEnd - xStart, myView.getCharHeight(), myEditor.getColorsScheme().getFont(EditorFontType.PLAIN));
            }
            else if (effectType == EffectType.WAVE_UNDERSCORE) {
                EffectPainter.WAVE_UNDERSCORE.paint(myGraphics, xStart, y, xEnd - xStart, myDescent, myEditor.getColorsScheme().getFont(EditorFontType.PLAIN));
            }
            else if (effectType == EffectType.BOLD_DOTTED_LINE) {
                EffectPainter.BOLD_DOTTED_UNDERSCORE.paint(myGraphics, xStart, y, xEnd - xStart, myDescent, myEditor.getColorsScheme().getFont(EditorFontType.PLAIN));
            }
            else if (allowBorder && (effectType == EffectType.BOXED || effectType == EffectType.ROUNDED_BOX)) {
                drawSimpleBorder(xFrom, xTo, y - myAscent, effectType == EffectType.ROUNDED_BOX);
            }
        }

        private static int calcFeatureSize(int unscaledSize, float scale) {
            return Math.max(1, Math.round(scale * unscaledSize));
        }

        private void paintWhitespace(float x,
                                     int y,
                                     int start,
                                     int end,
                                     LineWhitespacePaintingStrategy whitespacePaintingStrategy,
                                     VisualLineFragmentsIterator.Fragment fragment,
                                     BasicStroke stroke,
                                     float scale) {
            if (!whitespacePaintingStrategy.showAnyWhitespace()) {
                return;
            }

            boolean restoreStroke = false;
            Stroke defaultStroke = myGraphics.getStroke();
            Color color = TargetAWT.to(myEditor.getColorsScheme().getColor(EditorColors.WHITESPACES_COLOR));

            boolean isRtl = fragment.isRtl();
            int baseStartOffset = fragment.getStartOffset();
            int startOffset = isRtl ? baseStartOffset - start : baseStartOffset + start;
            int yToUse = y - 1;

            for (int i = start; i < end; i++) {
                int charOffset = isRtl ? baseStartOffset - i - 1 : baseStartOffset + i;
                char c = myText.charAt(charOffset);
                if (" \t\u3000".indexOf(c) >= 0 && whitespacePaintingStrategy.showWhitespaceAtOffset(charOffset)) {
                    int startX = (int) fragment.offsetToX(x, startOffset, isRtl ? baseStartOffset - i : baseStartOffset + i);
                    int endX = (int) fragment.offsetToX(x, startOffset, isRtl ? baseStartOffset - i - 1 : baseStartOffset + i + 1);

                    if (c == ' ') {
                        // making center point lie at the center of device pixel
                        float dotX = roundToPixelCenter((startX + endX) / 2.) - scale / 2;
                        float dotY = roundToPixelCenter(yToUse + 1 - myAscent + myLineHeight / 2.) - scale / 2;
                        myTextDrawingTasks.add(g -> {
                            CachingPainter.paint(g, dotX, dotY, scale, scale, _g -> {
                                _g.setColor(color);
                                _g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                _g.fill(new Ellipse2D.Float(0, 0, scale, scale));
                            }, ourCachedDot, color);
                        });
                    }
                    else if (c == '\t') {
                        double strokeWidth = Math.max(scale, PaintUtil.devPixel(myGraphics));
                        int yMid = yToUse - myView.getCharHeight() / 2;
                        int tabEndX = Math.max(startX + 1, endX - getTabGap(scale));
                        myTextDrawingTasks.add(g -> {
                            g.setColor(color);
                            LinePainter2D.paint(g, startX, yMid, tabEndX, yMid, LinePainter2D.StrokeType.INSIDE, strokeWidth);
                        });
                    }
                    else if (c == '\u3000') { // ideographic space
                        int charHeight = myView.getCharHeight();
                        int strokeWidth = Math.round(stroke.getLineWidth());
                        myTextDrawingTasks.add(g -> {
                            g.setColor(color);
                            g.setStroke(stroke);
                            g.drawRect(startX + JBUIScale.scale(2) + strokeWidth / 2, yToUse - charHeight + strokeWidth / 2, endX - startX - JBUIScale.scale(4) - (strokeWidth - 1), charHeight - (strokeWidth - 1));
                        });
                        restoreStroke = true;
                    }
                }
            }
            if (restoreStroke) {
                myTextDrawingTasks.add((g) -> {
                    g.setStroke(defaultStroke);
                });
            }
        }

        private static int getTabGap(float scale) {
            return calcFeatureSize(5, scale);
        }

        private static float getWhiteSpaceScale(@Nonnull Editor editor) {
            return ((float) editor.getColorsScheme().getEditorFontSize()) / FontPreferences.DEFAULT_FONT_SIZE;
        }

        private float roundToPixelCenter(double value) {
            double devPixel = 1 / PaintUtil.devValue(1, myScaleContext);
            return (float) (PaintUtil.alignToInt(value, myScaleContext, PaintUtil.RoundingMode.FLOOR, null) + devPixel / 2);
        }

        private void collectExtensions(int visualLine, int offset) {
            myEditor.processLineExtensions(myDocument.getLineNumber(offset), (info) -> {
                List<LineExtensionData> list = myExtensionData.get(visualLine);
                if (list == null) {
                    myExtensionData.put(visualLine, list = new ArrayList<>());
                }
                list.add(new LineExtensionData(info, LineLayout.create(myView, info.getText(), info.getFontType())));
                return true;
            });
        }

        private void paintLineExtensionsBackground(int visualLine, float x, int y) {
            List<LineExtensionData> data = myExtensionData.get(visualLine);
            if (data == null) {
                return;
            }
            for (LineExtensionData datum : data) {
                float width = datum.layout.getWidth();
                paintBackground(datum.info.getBgColor(), x, y, width);
                x += width;
            }
        }

        private void paintLineExtensions(int visualLine, int logicalLine, float x, int y) {
            List<LineExtensionData> data = myExtensionData.get(visualLine);
            if (data == null) {
                return;
            }
            for (LineExtensionData datum : data) {
                myGraphics.setColor(TargetAWT.to(datum.info.getColor()));
                x = paintLineLayoutWithEffect(datum.layout, x, y, datum.info.getEffectColor(), datum.info.getEffectType());
            }
            int currentLineWidth = myCorrector.lineWidth(visualLine, x);
            EditorSizeManager sizeManager = myView.getSizeManager();
            if (currentLineWidth > sizeManager.getMaxLineWithExtensionWidth()) {
                sizeManager.setMaxLineWithExtensionWidth(logicalLine, currentLineWidth);
                myEditor.getContentComponent().revalidate();
            }
        }

        private void paintHighlightersAfterEndOfLine(MarkupModelEx markupModel) {
            markupModel.processRangeHighlightersOverlappingWith(myStartOffset, myEndOffset, highlighter -> {
                if (highlighter.getStartOffset() >= myStartOffset) {
                    paintHighlighterAfterEndOfLine(highlighter);
                }
                return true;
            });
        }

        private void paintHighlighterAfterEndOfLine(RangeHighlighterEx highlighter) {
            if (!highlighter.isAfterEndOfLine()) {
                return;
            }
            int startOffset = highlighter.getStartOffset();
            int lineEndOffset = myDocument.getLineEndOffset(myDocument.getLineNumber(startOffset));
            if (myEditor.getFoldingModel().isOffsetCollapsed(lineEndOffset)) {
                return;
            }
            Point2D lineEnd = myView.offsetToXY(lineEndOffset, true, false);
            float x = (float) lineEnd.getX();
            int y = (int) lineEnd.getY() + myYShift;
            TextAttributes attributes = highlighter.getTextAttributes(myEditor.getColorsScheme());
            paintBackground(attributes, x, y, myView.getPlainSpaceWidth());
            if (attributes != null) {
                attributes.forEachEffect((type, color) -> paintTextEffect(x, x + myView.getPlainSpaceWidth() - 1, y + myAscent, color, type, false));
            }
        }

        private void paintBorderEffect(EditorHighlighter highlighter) {
            HighlighterIterator it = highlighter.createIterator(myStartOffset);
            while (!it.atEnd() && it.getStart() < myEndOffset) {
                TextAttributes attributes = it.getTextAttributes();
                EffectDescriptor borderDescriptor = getBorderDescriptor(attributes);
                if (borderDescriptor != null) {
                    paintBorderEffect(it.getStart(), it.getEnd(), borderDescriptor);
                }
                it.advance();
            }
        }

        private void paintBorderEffect(MarkupModelEx markupModel) {
            EditorColorsScheme colorsScheme = myEditor.getColorsScheme();

            markupModel.processRangeHighlightersOverlappingWith(myStartOffset, myEndOffset, rangeHighlighter -> {
                TextAttributes attributes = rangeHighlighter.getTextAttributes(colorsScheme);
                EffectDescriptor borderDescriptor = getBorderDescriptor(attributes);
                if (borderDescriptor != null) {
                    paintBorderEffect(rangeHighlighter.getAffectedAreaStartOffset(), rangeHighlighter.getAffectedAreaEndOffset(), borderDescriptor);
                }
                return true;
            });
        }

        /**
         * @return {@link EffectDescriptor descriptor} of border effect if attributes contains a border effect with not null color and
         * null otherwise
         */
        @Contract("null -> null")
        @Nullable
        private static EffectDescriptor getBorderDescriptor(@Nullable TextAttributes attributes) {
            return attributes == null || !attributes.hasEffects() ? null : TextAttributesEffectsBuilder.create(attributes).getEffectDescriptor(FRAME_SLOT);
        }

        private void paintBorderEffect(int startOffset, int endOffset, EffectDescriptor borderDescriptor) {
            startOffset = DocumentUtil.alignToCodePointBoundary(myDocument, startOffset);
            endOffset = DocumentUtil.alignToCodePointBoundary(myDocument, endOffset);
            if (!myClipDetector.rangeCanBeVisible(startOffset, endOffset)) {
                return;
            }
            int startLine = myDocument.getLineNumber(startOffset);
            int endLine = myDocument.getLineNumber(endOffset);
            if (startLine + 1 == endLine && startOffset == myDocument.getLineStartOffset(startLine) && endOffset == myDocument.getLineStartOffset(endLine)) {
                // special case of line highlighters
                endLine--;
                endOffset = myDocument.getLineEndOffset(endLine);
            }

            boolean rounded = borderDescriptor.effectType == EffectType.ROUNDED_BOX;
            myGraphics.setColor(TargetAWT.to(borderDescriptor.effectColor));
            VisualPosition startPosition = myView.offsetToVisualPosition(startOffset, true, false);
            VisualPosition endPosition = myView.offsetToVisualPosition(endOffset, false, true);
            if (startPosition.line == endPosition.line) {
                int y = myView.visualLineToY(startPosition.line) + myYShift;
                TFloatArrayList ranges = adjustedLogicalRangeToVisualRanges(startOffset, endOffset);
                for (int i = 0; i < ranges.size() - 1; i += 2) {
                    float startX = myCorrector.singleLineBorderStart(ranges.get(i));
                    float endX = myCorrector.singleLineBorderEnd(ranges.get(i + 1));
                    drawSimpleBorder(startX, endX, y, rounded);
                }
            }
            else {
                TFloatArrayList leadingRanges = adjustedLogicalRangeToVisualRanges(startOffset, myView.visualPositionToOffset(new VisualPosition(startPosition.line, Integer.MAX_VALUE, true)));
                TFloatArrayList trailingRanges = adjustedLogicalRangeToVisualRanges(myView.visualPositionToOffset(new VisualPosition(endPosition.line, 0)), endOffset);
                if (!leadingRanges.isEmpty() && !trailingRanges.isEmpty()) {
                    int minX = Math.min(myCorrector.minX(startPosition.line, endPosition.line), (int) leadingRanges.get(0));
                    int maxX = Math.max(myCorrector.maxX(startPosition.line, endPosition.line), (int) trailingRanges.get(trailingRanges.size() - 1));
                    boolean containsInnerLines = endPosition.line > startPosition.line + 1;
                    int lineHeight = myLineHeight - 1;
                    int leadingTopY = myView.visualLineToY(startPosition.line) + myYShift;
                    int leadingBottomY = leadingTopY + lineHeight;
                    int trailingTopY = myView.visualLineToY(endPosition.line) + myYShift;
                    int trailingBottomY = trailingTopY + lineHeight;
                    float start = 0;
                    float end = 0;
                    float leftGap = leadingRanges.get(0) - (containsInnerLines ? minX : trailingRanges.get(0));
                    int adjustY = leftGap == 0 ? 2 : leftGap > 0 ? 1 : 0; // avoiding 1-pixel gap between aligned lines
                    for (int i = 0; i < leadingRanges.size() - 1; i += 2) {
                        start = leadingRanges.get(i);
                        end = leadingRanges.get(i + 1);
                        if (i > 0) {
                            drawLine(leadingRanges.get(i - 1), leadingBottomY, start, leadingBottomY, rounded);
                        }
                        drawLine(start, leadingBottomY + (i == 0 ? adjustY : 0), start, leadingTopY, rounded);
                        if ((i + 2) < leadingRanges.size()) {
                            drawLine(start, leadingTopY, end, leadingTopY, rounded);
                            drawLine(end, leadingTopY, end, leadingBottomY, rounded);
                        }
                    }
                    end = Math.max(end, maxX);
                    drawLine(start, leadingTopY, end, leadingTopY, rounded);
                    drawLine(end, leadingTopY, end, trailingTopY - 1, rounded);
                    float targetX = trailingRanges.get(trailingRanges.size() - 1);
                    drawLine(end, trailingTopY - 1, targetX, trailingTopY - 1, rounded);
                    adjustY = end == targetX ? -2 : -1; // for lastX == targetX we need to avoid a gap when rounding is used
                    for (int i = trailingRanges.size() - 2; i >= 0; i -= 2) {
                        start = trailingRanges.get(i);
                        end = trailingRanges.get(i + 1);

                        drawLine(end, trailingTopY + (i == 0 ? adjustY : 0), end, trailingBottomY, rounded);
                        drawLine(end, trailingBottomY, start, trailingBottomY, rounded);
                        drawLine(start, trailingBottomY, start, trailingTopY, rounded);
                        if (i > 0) {
                            drawLine(start, trailingTopY, trailingRanges.get(i - 1), trailingTopY, rounded);
                        }
                    }
                    float lastX = start;
                    if (containsInnerLines) {
                        if (start != minX) {
                            drawLine(start, trailingTopY, start, trailingTopY - 1, rounded);
                            drawLine(start, trailingTopY - 1, minX, trailingTopY - 1, rounded);
                            drawLine(minX, trailingTopY - 1, minX, leadingBottomY + 1, rounded);
                        }
                        else {
                            drawLine(minX, trailingTopY, minX, leadingBottomY + 1, rounded);
                        }
                        lastX = minX;
                    }
                    targetX = leadingRanges.get(0);
                    if (lastX < targetX) {
                        drawLine(lastX, leadingBottomY + 1, targetX, leadingBottomY + 1, rounded);
                    }
                    else {
                        drawLine(lastX, leadingBottomY + 1, lastX, leadingBottomY, rounded);
                        drawLine(lastX, leadingBottomY, targetX, leadingBottomY, rounded);
                    }
                }
            }
        }

        private void drawSimpleBorder(float xStart, float xEnd, float y, boolean rounded) {
            Shape border = getBorderShape(xStart, y, xEnd - xStart, myLineHeight, 1, rounded);
            if (border != null) {
                Object old = myGraphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                myGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                myGraphics.fill(border);
                myGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
            }
        }

        private static Shape getBorderShape(float x, float y, float width, int height, int thickness, boolean rounded) {
            if (width <= 0 || height <= 0) {
                return null;
            }
            Shape outer = rounded ? new RoundRectangle2D.Float(x, y, width, height, 2, 2) : new Rectangle2D.Float(x, y, width, height);
            int doubleThickness = 2 * thickness;
            if (width <= doubleThickness || height <= doubleThickness) {
                return outer;
            }
            Shape inner = new Rectangle2D.Float(x + thickness, y + thickness, width - doubleThickness, height - doubleThickness);

            Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
            path.append(outer, false);
            path.append(inner, false);
            return path;
        }

        private void drawLine(float x1, int y1, float x2, int y2, boolean rounded) {
            if (rounded) {
                UIUtil.drawLinePickedOut(myGraphics, (int) x1, y1, (int) x2, y2);
            }
            else {
                LinePainter2D.paint(myGraphics, (int) x1, y1, (int) x2, y2);
            }
        }

        /**
         * Returns ranges obtained from {@link #logicalRangeToVisualRanges(int, int)}, adjusted for painting range border - lines should
         * line inside target ranges (except for empty range). Target offsets are supposed to be located on the same visual line.
         */
        private TFloatArrayList adjustedLogicalRangeToVisualRanges(int startOffset, int endOffset) {
            TFloatArrayList ranges = logicalRangeToVisualRanges(startOffset, endOffset);
            for (int i = 0; i < ranges.size() - 1; i += 2) {
                float startX = ranges.get(i);
                float endX = ranges.get(i + 1);
                if (startX == endX) {
                    if (startX > 0) {
                        startX--;
                    }
                    else {
                        endX++;
                    }
                }
                else {
                    endX--;
                }
                ranges.set(i, startX);
                ranges.set(i + 1, endX);
            }
            return ranges;
        }


        /**
         * Returns a list of pairs of x coordinates for visual ranges representing given logical range. If
         * {@code startOffset == endOffset}, a pair of equal numbers is returned, corresponding to target position. Target offsets are
         * supposed to be located on the same visual line.
         */
        private TFloatArrayList logicalRangeToVisualRanges(int startOffset, int endOffset) {
            assert startOffset <= endOffset;
            TFloatArrayList result = new TFloatArrayList();
            if (myDocument.getTextLength() == 0) {
                int minX = myCorrector.emptyTextX();
                result.add(minX);
                result.add(minX);
            }
            else {
                float lastX = -1;
                for (VisualLineFragmentsIterator.Fragment fragment : VisualLineFragmentsIterator.create(myView, startOffset, false, true)) {
                    int minOffset = fragment.getMinOffset();
                    int maxOffset = fragment.getMaxOffset();
                    if (startOffset == endOffset) {
                        lastX = fragment.getEndX();
                        Inlay inlay = fragment.getCurrentInlay();
                        if (inlay != null && !inlay.isRelatedToPrecedingText()) {
                            continue;
                        }
                        if (startOffset >= minOffset && startOffset < maxOffset) {
                            float x = fragment.offsetToX(startOffset);
                            result.add(x);
                            result.add(x);
                            break;
                        }
                    }
                    else if (startOffset < maxOffset && endOffset > minOffset) {
                        float x1 = minOffset == maxOffset ? fragment.getStartX() : fragment.offsetToX(Math.max(minOffset, startOffset));
                        float x2 = minOffset == maxOffset ? fragment.getEndX() : fragment.offsetToX(Math.min(maxOffset, endOffset));
                        if (x1 > x2) {
                            float tmp = x1;
                            x1 = x2;
                            x2 = tmp;
                        }
                        if (result.isEmpty() || x1 > result.get(result.size() - 1)) {
                            result.add(x1);
                            result.add(x2);
                        }
                        else {
                            result.set(result.size() - 1, x2);
                        }
                    }
                }
                if (startOffset == endOffset && result.isEmpty() && lastX >= 0) {
                    result.add(lastX);
                    result.add(lastX);
                }
            }
            return result;
        }

        private void paintComposedTextDecoration() {
            TextRange composedTextRange = myEditor.getComposedTextRange();
            if (composedTextRange != null) {
                Point2D p1 = myView.offsetToXY(Math.min(composedTextRange.getStartOffset(), myDocument.getTextLength()), true, false);
                Point2D p2 = myView.offsetToXY(Math.min(composedTextRange.getEndOffset(), myDocument.getTextLength()), false, true);

                int y = (int) p1.getY() + myAscent + 1 + myYShift;

                myGraphics.setStroke(IME_COMPOSED_TEXT_UNDERLINE_STROKE);
                myGraphics.setColor(TargetAWT.to(myEditor.getColorsScheme().getDefaultForeground()));
                LinePainter2D.paint(myGraphics, (int) p1.getX(), y, (int) p2.getX(), y);
            }
        }

        private void paintBlockInlays() {
            if (!myEditor.getInlayModel().hasBlockElements()) {
                return;
            }
            int startX = myView.getInsets().left;
            int lineCount = myEditor.getVisibleLineCount();
            VisualLinesIterator visLinesIterator = new VisualLinesIterator(myEditor, myStartVisualLine);
            while (!visLinesIterator.atEnd()) {
                int visualLine = visLinesIterator.getVisualLine();
                if (visualLine > myEndVisualLine || visualLine >= lineCount) {
                    break;
                }
                int y = visLinesIterator.getY() + myYShift;

                int curY = y;
                List<Inlay> inlaysAbove = visLinesIterator.getBlockInlaysAbove();
                if (!inlaysAbove.isEmpty()) {
                    TextAttributes attributes = getInlayAttributes(visualLine);
                    for (Inlay inlay : inlaysAbove) {
                        if (curY <= myClip.y + myYShift) {
                            break;
                        }
                        int height = inlay.getHeightInPixels();
                        if (height > 0) {
                            int newY = curY - height;
                            inlay.getRenderer().paint(inlay, myGraphics, new Rectangle(startX, newY, inlay.getWidthInPixels(), height), attributes);
                            curY = newY;
                        }
                    }
                }
                curY = y + myLineHeight;
                List<Inlay> inlaysBelow = visLinesIterator.getBlockInlaysBelow();
                if (!inlaysBelow.isEmpty()) {
                    TextAttributes attributes = getInlayAttributes(visualLine + 1);
                    for (Inlay inlay : inlaysBelow) {
                        if (curY >= myClip.y + myClip.height + myYShift) {
                            break;
                        }
                        int height = inlay.getHeightInPixels();
                        if (height > 0) {
                            inlay.getRenderer().paint(inlay, myGraphics, new Rectangle(startX, curY, inlay.getWidthInPixels(), height), attributes);
                            curY += height;
                        }
                    }
                }
                visLinesIterator.advance();
            }
        }

        private TextAttributes getInlayAttributes(int visualLine) {
            TextAttributes attributes = myBetweenLinesAttributes.get(visualLine);
            if (attributes != null) {
                return attributes;
            }
            // inlay shown below last document line
            return new TextAttributes();
        }

        @Nonnull
        private TextAttributes getBetweenLinesAttributes(int bottomVisualLine, int bottomVisualLineStartOffset, PeekableIterator<Caret> caretIterator) {
            boolean selection = false;
            while (caretIterator.hasNext() && caretIterator.peek().getSelectionEnd() < bottomVisualLineStartOffset) caretIterator.next();
            if (caretIterator.hasNext()) {
                Caret caret = caretIterator.peek();
                selection = caret.getSelectionStart() <= bottomVisualLineStartOffset && caret.getSelectionStartPosition().line < bottomVisualLine && bottomVisualLine <= caret.getSelectionEndPosition().line;
            }

            class MyProcessor implements Processor<RangeHighlighterEx> {
                private int layer;
                private ColorValue backgroundColor;

                private MyProcessor(boolean selection) {
                    backgroundColor = selection ? myEditor.getSelectionModel().getTextAttributes().getBackgroundColor() : null;
                    layer = backgroundColor == null ? Integer.MIN_VALUE : HighlighterLayer.SELECTION;
                }

                @Override
                public boolean process(RangeHighlighterEx highlighterEx) {
                    int layer = highlighterEx.getLayer();
                    if (layer > this.layer && highlighterEx.getAffectedAreaStartOffset() < bottomVisualLineStartOffset && highlighterEx.getAffectedAreaEndOffset() > bottomVisualLineStartOffset) {
                        TextAttributes attributes = highlighterEx.getTextAttributes(myEditor.getColorsScheme());
                        ColorValue backgroundColor = attributes == null ? null : attributes.getBackgroundColor();
                        if (backgroundColor != null) {
                            this.layer = layer;
                            this.backgroundColor = backgroundColor;
                        }
                    }
                    return true;
                }
            }
            MyProcessor processor = new MyProcessor(selection);
            myDocMarkup.processRangeHighlightersOverlappingWith(bottomVisualLineStartOffset, bottomVisualLineStartOffset, processor);
            myEditorMarkup.processRangeHighlightersOverlappingWith(bottomVisualLineStartOffset, bottomVisualLineStartOffset, processor);
            TextAttributes attributes = new TextAttributes();
            attributes.setBackgroundColor(processor.backgroundColor);
            return attributes;
        }

        private void paintCaret() {
            DesktopEditorImpl.CaretRectangle[] locations = myEditor.getCaretLocations(true);
            if (locations == null) {
                return;
            }

            Graphics2D g = myGraphics;
            int nominalLineHeight = myView.getNominalLineHeight();
            int topOverhang = myView.getTopOverhang();
            EditorSettings settings = myEditor.getSettings();
            ColorValue caretColor = myEditor.getColorsScheme().getColor(EditorColors.CARET_COLOR);
            if (caretColor == null) {
                caretColor = new LightDarkColorValue(CARET_DARK, CARET_LIGHT);
            }
            int minX = myView.getInsets().left;
            for (DesktopEditorImpl.CaretRectangle location : locations) {
                float x = (float) location.myPoint.getX();
                int y = (int) location.myPoint.getY() - topOverhang + myYShift;
                Caret caret = location.myCaret;
                CaretVisualAttributes attr = caret == null ? CaretVisualAttributes.DEFAULT : caret.getVisualAttributes();
                g.setColor(TargetAWT.to(attr.getColor() != null ? attr.getColor() : caretColor));
                boolean isRtl = location.myIsRtl;
                if (myEditor.isInsertMode() != settings.isBlockCursor()) {
                    int lineWidth = JBUIScale.scale(attr.getWidth(settings.getLineCursorWidth()));
                    // fully cover extra character's pixel which can appear due to antialiasing
                    // see IDEA-148843 for more details
                    if (x > minX && lineWidth > 1) {
                        x -= 1 / JBUIScale.sysScale(g);
                    }
                    g.fill(new Rectangle2D.Float(x, y, lineWidth, nominalLineHeight));
                    if (myDocument.getTextLength() > 0 && caret != null && !myView.getTextLayoutCache().getLineLayout(caret.getLogicalPosition().line).isLtr()) {
                        GeneralPath triangle = new GeneralPath(Path2D.WIND_NON_ZERO, 3);
                        triangle.moveTo(isRtl ? x + lineWidth : x, y);
                        triangle.lineTo(isRtl ? x + lineWidth - CARET_DIRECTION_MARK_SIZE : x + CARET_DIRECTION_MARK_SIZE, y);
                        triangle.lineTo(isRtl ? x + lineWidth : x, y + CARET_DIRECTION_MARK_SIZE);
                        triangle.closePath();
                        g.fill(triangle);
                    }
                }
                else {
                    float width = location.myWidth;
                    float startX = Math.max(minX, isRtl ? x - width : x);
                    g.fill(new Rectangle2D.Float(startX, y, width, nominalLineHeight));
                    if (myDocument.getTextLength() > 0 && caret != null) {
                        int targetVisualColumn = caret.getVisualPosition().column - (isRtl ? 1 : 0);
                        for (VisualLineFragmentsIterator.Fragment fragment : VisualLineFragmentsIterator.create(myView, caret.getVisualLineStart(), false)) {
                            if (fragment.getCurrentInlay() != null) {
                                continue;
                            }
                            int startVisualColumn = fragment.getStartVisualColumn();
                            int endVisualColumn = fragment.getEndVisualColumn();
                            if (startVisualColumn <= targetVisualColumn && targetVisualColumn < endVisualColumn) {
                                g.setColor(TargetAWT.to(ColorValueUtil.isDark(caretColor) ? CARET_LIGHT : CARET_DARK));
                                fragment.draw(startX, y + topOverhang + myAscent, fragment.visualColumnToOffset(targetVisualColumn - startVisualColumn),
                                    fragment.visualColumnToOffset(targetVisualColumn + 1 - startVisualColumn)).accept(g);
                                break;
                            }
                        }
                        ComplexTextFragment.flushDrawingCache(g);
                    }
                }
            }
        }

        private interface MarginWidthConsumer {
            void process(float width);
        }

        private void paintLineFragments(VisualLinesIterator visLineIterator, int y, LineFragmentPainter painter, MarginWidthConsumer marginWidthConsumer) {
            int visualLine = visLineIterator.getVisualLine();
            float x = myCorrector.startX(visualLine) + (visualLine == 0 ? myView.getPrefixTextWidthInPixels() : 0);
            int offset = visLineIterator.getVisualLineStartOffset();
            int visualLineEndOffset = visLineIterator.getVisualLineEndOffset();
            IterationState it = null;
            int prevEndOffset = -1;
            boolean firstFragment = true;
            int maxColumn = 0;
            int endLogicalLine = visLineIterator.getEndLogicalLine();
            boolean marginReached = false;
            for (VisualLineFragmentsIterator.Fragment fragment : VisualLineFragmentsIterator.create(myView, visLineIterator, null, true)) {
                int fragmentStartOffset = fragment.getStartOffset();
                int start = fragmentStartOffset;
                int end = fragment.getEndOffset();
                x = fragment.getStartX();
                if (firstFragment) {
                    firstFragment = false;
                    SoftWrap softWrap = myEditor.getSoftWrapModel().getSoftWrap(offset);
                    boolean hasSoftWrap = softWrap != null;
                    if (hasSoftWrap || myEditor.isRightAligned()) {
                        prevEndOffset = offset;
                        it = new IterationState(myEditor, offset == 0 ? 0 : DocumentUtil.getPreviousCodePointOffset(myDocument, offset), visualLineEndOffset, myCaretData, false, false, false, false);
                        if (it.getEndOffset() <= offset) {
                            it.advance();
                        }
                        if (x >= myClip.getMinX()) {
                            TextAttributes attributes = it.getStartOffset() == offset ? it.getBeforeLineStartBackgroundAttributes() : it.getMergedAttributes();
                            painter.paintBeforeLineStart(attributes, hasSoftWrap, fragment.getStartVisualColumn(), x, y);
                        }
                    }
                }
                FoldRegion foldRegion = fragment.getCurrentFoldRegion();
                if (foldRegion == null) {
                    if (start != prevEndOffset) {
                        it = new IterationState(myEditor, start, fragment.isRtl() ? offset : visualLineEndOffset, myCaretData, false, false, false, fragment.isRtl());
                    }
                    prevEndOffset = end;
                    assert it != null;
                    if (start == end) { // special case of inlays
                        if (start == it.getEndOffset() && !it.atEnd()) {
                            it.advance();
                        }
                        TextAttributes attributes = it.getStartOffset() == start ? it.getBreakAttributes() : it.getMergedAttributes();
                        float xNew = fragment.getEndX();
                        if (xNew >= myClip.getMinX()) {
                            painter.paint(fragment, 0, 0, attributes, x, xNew, y);
                        }
                        x = xNew;
                    }
                    else {
                        while (fragment.isRtl() ? start > end : start < end) {
                            if (fragment.isRtl() ? it.getEndOffset() >= start : it.getEndOffset() <= start) {
                                assert !it.atEnd();
                                it.advance();
                            }
                            TextAttributes attributes = it.getMergedAttributes();
                            int curEnd = fragment.isRtl() ? Math.max(it.getEndOffset(), end) : Math.min(it.getEndOffset(), end);
                            float xNew = fragment.offsetToX(x, start, curEnd);
                            if (xNew >= myClip.getMinX()) {
                                painter.paint(fragment, fragment.isRtl() ? fragmentStartOffset - start : start - fragmentStartOffset, fragment.isRtl() ? fragmentStartOffset - curEnd : curEnd - fragmentStartOffset,
                                    attributes, x, xNew, y);
                            }
                            x = xNew;
                            start = curEnd;
                        }
                        if (marginWidthConsumer != null &&
                            fragment.getEndLogicalLine() == endLogicalLine &&
                            fragment.getStartLogicalColumn() <= myMarginColumns &&
                            fragment.getEndLogicalColumn() > myMarginColumns) {
                            marginWidthConsumer.process(fragment.visualColumnToX(fragment.logicalToVisualColumn(myMarginColumns)));
                            marginReached = true;
                        }
                    }
                }
                else {
                    float xNew = fragment.getEndX();
                    if (xNew >= myClip.getMinX()) {
                        painter.paint(fragment, 0, fragment.getVisualLength(), getFoldRegionAttributes(foldRegion), x, xNew, y);
                    }
                    x = xNew;
                    prevEndOffset = -1;
                    it = null;
                }
                if (x > myClip.getMaxX()) {
                    return;
                }
                maxColumn = fragment.getEndVisualColumn();
            }
            if (firstFragment && myEditor.isRightAligned()) {
                it = new IterationState(myEditor, offset, visualLineEndOffset, myCaretData, false, false, false, false);
                if (it.getEndOffset() <= offset) {
                    it.advance();
                }
                painter.paintBeforeLineStart(it.getBeforeLineStartBackgroundAttributes(), false, maxColumn, x, y);
            }
            if (it == null || it.getEndOffset() != visualLineEndOffset) {
                it = new IterationState(myEditor, visualLineEndOffset == offset ? visualLineEndOffset : DocumentUtil.getPreviousCodePointOffset(myDocument, visualLineEndOffset), visualLineEndOffset,
                    myCaretData, false, false, false, false);
            }
            if (!it.atEnd()) {
                it.advance();
            }
            assert it.atEnd();
            painter.paintAfterLineEnd(it, maxColumn, x, y);
            if (marginWidthConsumer != null && !marginReached && (visualLine == myEditor.getCaretModel().getVisualPosition().line || x > myMarginColumns * myView.getPlainSpaceWidth())) {
                int endLogicalColumn = myView.offsetToLogicalPosition(visualLineEndOffset).column;
                if (endLogicalColumn <= myMarginColumns) {
                    marginWidthConsumer.process(x + (myMarginColumns - endLogicalColumn) * myView.getPlainSpaceWidth());
                }
            }
        }

        private TextAttributes getFoldRegionAttributes(FoldRegion foldRegion) {
            TextAttributes selectionAttributes = isSelected(foldRegion) ? myEditor.getSelectionModel().getTextAttributes() : null;
            TextAttributes defaultAttributes = getDefaultAttributes();
            if (myEditor.isInFocusMode(foldRegion)) {
                return ObjectUtil.notNull(myEditor.getUserData(FocusModeModel.FOCUS_MODE_ATTRIBUTES), getDefaultAttributes());
            }
            TextAttributes foldAttributes = myEditor.getFoldingModel().getPlaceholderAttributes();
            return mergeAttributes(mergeAttributes(selectionAttributes, foldAttributes), defaultAttributes);
        }

        @SuppressWarnings("UseJBColor")
        private TextAttributes getDefaultAttributes() {
            TextAttributes attributes = myEditor.getColorsScheme().getAttributes(HighlighterColors.TEXT);
            if (attributes.getForegroundColor() == null) {
                attributes.setForegroundColor(StandardColors.BLACK);
            }
            if (attributes.getBackgroundColor() == null) {
                attributes.setBackgroundColor(StandardColors.WHITE);
            }
            return attributes;
        }

        private static boolean isSelected(FoldRegion foldRegion) {
            int regionStart = foldRegion.getStartOffset();
            int regionEnd = foldRegion.getEndOffset();
            int[] selectionStarts = foldRegion.getEditor().getSelectionModel().getBlockSelectionStarts();
            int[] selectionEnds = foldRegion.getEditor().getSelectionModel().getBlockSelectionEnds();
            for (int i = 0; i < selectionStarts.length; i++) {
                int start = selectionStarts[i];
                int end = selectionEnds[i];
                if (regionStart >= start && regionEnd <= end) {
                    return true;
                }
            }
            return false;
        }

        private static TextAttributes mergeAttributes(TextAttributes primary, TextAttributes secondary) {
            if (primary == null) {
                return secondary;
            }
            if (secondary == null) {
                return primary;
            }
            TextAttributes result = new TextAttributes(primary.getForegroundColor() == null ? secondary.getForegroundColor() : primary.getForegroundColor(),
                primary.getBackgroundColor() == null ? secondary.getBackgroundColor() : primary.getBackgroundColor(), null, null,
                primary.getFontType() == Font.PLAIN ? secondary.getFontType() : primary.getFontType());

            return TextAttributesEffectsBuilder.create(secondary).coverWith(primary).applyTo(result);
        }
    }

    interface LineFragmentPainter {
        void paintBeforeLineStart(TextAttributes attributes, boolean hasSoftWrap, int columnEnd, float xEnd, int y);

        void paint(VisualLineFragmentsIterator.Fragment fragment, int start, int end, TextAttributes attributes, float xStart, float xEnd, int y);

        void paintAfterLineEnd(IterationState iterationState, int columnStart, float x, int y);
    }

    private static class LineWhitespacePaintingStrategy {
        private final boolean myWhitespaceShown;
        private final boolean myLeadingWhitespaceShown;
        private final boolean myInnerWhitespaceShown;
        private final boolean myTrailingWhitespaceShown;

        // Offsets on current line where leading whitespace ends and trailing whitespace starts correspondingly.
        private int currentLeadingEdge;
        private int currentTrailingEdge;

        LineWhitespacePaintingStrategy(EditorSettings settings) {
            myWhitespaceShown = settings.isWhitespacesShown();
            myLeadingWhitespaceShown = settings.isLeadingWhitespaceShown();
            myInnerWhitespaceShown = settings.isInnerWhitespaceShown();
            myTrailingWhitespaceShown = settings.isTrailingWhitespaceShown();
        }

        private boolean showAnyWhitespace() {
            return myWhitespaceShown && (myLeadingWhitespaceShown || myInnerWhitespaceShown || myTrailingWhitespaceShown);
        }

        private void update(CharSequence chars, int lineStart, int lineEnd) {
            if (showAnyWhitespace() && !(myLeadingWhitespaceShown && myInnerWhitespaceShown && myTrailingWhitespaceShown)) {
                currentTrailingEdge = CharArrayUtil.shiftBackward(chars, lineStart, lineEnd - 1, WHITESPACE_CHARS) + 1;
                currentLeadingEdge = CharArrayUtil.shiftForward(chars, lineStart, currentTrailingEdge, WHITESPACE_CHARS);
            }
        }

        private boolean showWhitespaceAtOffset(int offset) {
            return myWhitespaceShown && (offset < currentLeadingEdge ? myLeadingWhitespaceShown : offset >= currentTrailingEdge ? myTrailingWhitespaceShown : myInnerWhitespaceShown);
        }
    }

    private interface XCorrector {
        float startX(int line);

        int lineWidth(int line, float x);

        int emptyTextX();

        int minX(int startLine, int endLine);

        int maxX(int startLine, int endLine);

        int lineSeparatorStart(int minX);

        int lineSeparatorEnd(int maxX);

        float singleLineBorderStart(float x);

        float singleLineBorderEnd(float x);

        int marginX(float marginWidth);

        List<Integer> softMarginsX();

        @Nonnull
        static XCorrector create(@Nonnull EditorView view) {
            return view.getEditor().isRightAligned() ? new RightAligned(view) : new LeftAligned(view);
        }

        class LeftAligned implements XCorrector {
            private final EditorView myView;
            private final int myLeftInset;

            private LeftAligned(@Nonnull EditorView view) {
                myView = view;
                myLeftInset = myView.getInsets().left;
            }

            @Override
            public float startX(int line) {
                return myLeftInset;
            }

            @Override
            public int emptyTextX() {
                return myLeftInset;
            }

            @Override
            public int minX(int startLine, int endLine) {
                return myLeftInset;
            }

            @Override
            public int maxX(int startLine, int endLine) {
                return minX(startLine, endLine) + myView.getMaxTextWidthInLineRange(startLine, endLine - 1) - 1;
            }

            @Override
            public float singleLineBorderStart(float x) {
                return x;
            }

            @Override
            public float singleLineBorderEnd(float x) {
                return x + 1;
            }

            @Override
            public int lineWidth(int line, float x) {
                return (int) x - myLeftInset;
            }

            @Override
            public int lineSeparatorStart(int maxX) {
                return myLeftInset;
            }

            @Override
            public int lineSeparatorEnd(int maxX) {
                return isMarginShown(myView.getEditor()) ? Math.min(marginX(Session.getBaseMarginWidth(myView)), maxX) : maxX;
            }

            @Override
            public int marginX(float marginWidth) {
                return (int) (myLeftInset + marginWidth);
            }

            @Override
            public List<Integer> softMarginsX() {
                List<Integer> margins = myView.getEditor().getSettings().getSoftMargins();
                List<Integer> result = new ArrayList<>(margins.size());
                for (Integer margin : margins) {
                    result.add((int) (myLeftInset + margin * myView.getPlainSpaceWidth()));
                }
                return result;
            }
        }

        class RightAligned implements XCorrector {
            private final EditorView myView;

            private RightAligned(@Nonnull EditorView view) {
                myView = view;
            }

            @Override
            public float startX(int line) {
                return myView.getRightAlignmentLineStartX(line);
            }

            @Override
            public int lineWidth(int line, float x) {
                return (int) (x - myView.getRightAlignmentLineStartX(line));
            }

            @Override
            public int emptyTextX() {
                return myView.getRightAlignmentMarginX();
            }

            @Override
            public int minX(int startLine, int endLine) {
                return myView.getRightAlignmentMarginX() - myView.getMaxTextWidthInLineRange(startLine, endLine - 1) - 1;
            }

            @Override
            public int maxX(int startLine, int endLine) {
                return myView.getRightAlignmentMarginX() - 1;
            }

            @Override
            public float singleLineBorderStart(float x) {
                return x - 1;
            }

            @Override
            public float singleLineBorderEnd(float x) {
                return x;
            }

            @Override
            public int lineSeparatorStart(int minX) {
                return isMarginShown(myView.getEditor()) ? Math.max(marginX(Session.getBaseMarginWidth(myView)), minX) : minX;
            }

            @Override
            public int lineSeparatorEnd(int maxX) {
                return maxX;
            }

            @Override
            public int marginX(float marginWidth) {
                return (int) (myView.getRightAlignmentMarginX() - marginWidth);
            }

            @Override
            public List<Integer> softMarginsX() {
                List<Integer> margins = myView.getEditor().getSettings().getSoftMargins();
                List<Integer> result = new ArrayList<>(margins.size());
                for (Integer margin : margins) {
                    result.add((int) (myView.getRightAlignmentMarginX() - margin * myView.getPlainSpaceWidth()));
                }
                return result;
            }
        }
    }

    private static class LineExtensionData {
        private final LineExtensionInfo info;
        private final LineLayout layout;

        private LineExtensionData(LineExtensionInfo info, LineLayout layout) {
            this.info = info;
            this.layout = layout;
        }
    }

    private static class MarginPositions {
        private final float[] x;
        private final int[] y;

        private MarginPositions(int size) {
            x = new float[size];
            y = new int[size];
        }
    }
}
