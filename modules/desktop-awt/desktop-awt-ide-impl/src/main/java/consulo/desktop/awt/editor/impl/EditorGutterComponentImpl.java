// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.desktop.awt.editor.impl;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.*;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.impl.*;
import consulo.codeEditor.impl.util.EditorImplUtil;
import consulo.codeEditor.internal.FoldingUtil;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.EditorFontType;
import consulo.colorScheme.TextAttributes;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.codeEditor.impl.internal.VisualLinesIterator;
import consulo.desktop.awt.ui.ExperimentalUI;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.desktop.awt.ui.animation.AlphaAnimationContext;
import consulo.desktop.awt.ui.impl.image.DesktopAWTScalableImage;
import consulo.document.Document;
import consulo.document.MarkupIterator;
import consulo.document.internal.DocumentEx;
import consulo.document.util.Segment;
import consulo.execution.debug.internal.breakpoint.BreakpointEditorUtil;
import consulo.ide.impl.idea.codeInsight.daemon.NonHideableIconGutterMark;
import consulo.ide.impl.idea.codeInsight.hint.TooltipController;
import consulo.ide.impl.idea.ide.ui.customization.CustomActionsSchemaImpl;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUIUtil;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.editor.markup.LineMarkerRendererEx;
import consulo.ide.impl.idea.openapi.wm.impl.IdeGlassPaneImpl;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.impl.internal.hint.TooltipGroup;
import consulo.language.editor.impl.internal.hint.TooltipRenderer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.JBValue.JBValueGroup;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.awt.dnd.DnDDragStartBean;
import consulo.ui.ex.awt.dnd.DnDImage;
import consulo.ui.ex.awt.dnd.DnDNativeTarget;
import consulo.ui.ex.awt.dnd.DnDSupport;
import consulo.ui.ex.awt.event.HoverStateListener;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.paint.LinePainter2D;
import consulo.ui.ex.awt.paint.LinePainter2D.StrokeType;
import consulo.ui.ex.awt.paint.PaintUtil;
import consulo.ui.ex.awt.paint.PaintUtil.RoundingMode;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awt.util.JBSwingUtilities;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.image.ImageEffects;
import consulo.ui.style.StandardColors;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;
import consulo.util.collection.SmartList;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjPredicate;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gutter content (left to right):
 * <ul>
 * <li>GAP_BETWEEN_AREAS</li>
 * <li>Line numbers area
 * <ul>
 * <li>Line numbers</li>
 * <li>GAP_BETWEEN_AREAS</li>
 * <li>Additional line numbers (used in diff)</li>
 * </ul>
 * </li>
 * <li>GAP_BETWEEN_AREAS</li>
 * <li>Annotations area
 * <ul>
 * <li>Annotations</li>
 * <li>Annotations extra (used in distraction free mode)</li>
 * </ul>
 * </li>
 * <li>GAP_BETWEEN_AREAS</li>
 * <li>Line markers area
 * <ul>
 * <li>Left free painters</li>
 * <li>Icons</li>
 * <li>Gap (required by debugger to set breakpoints with mouse click - IDEA-137353) </li>
 * <li>Free painters</li>
 * </ul>
 * </li>
 * <li>Folding area</li>
 * </ul>
 */
public class EditorGutterComponentImpl extends JComponent implements EditorGutterComponentEx, MouseListener, MouseMotionListener, DataProvider, Accessible {
    private static final HoverStateListener HOVER_STATE_LISTENER = new HoverStateListener() {
        @Override
        protected void hoverChanged(@Nonnull Component component, boolean hovered) {
            if (component instanceof EditorGutterComponentImpl gutter && ExperimentalUI.isNewUI()) {
                gutter.myAlphaContext.setVisible(hovered);
            }
        }
    };

    private static final String EDITOR_GUTTER_CONTEXT_MENU_KEY = "editor.gutter.context.menu";

    private static final JBValueGroup JBVG = new JBValueGroup();
    private static final JBValue START_ICON_AREA_WIDTH = JBVG.value(17);
    private static final JBValue FREE_PAINTERS_LEFT_AREA_WIDTH = JBVG.value(8);
    private static final JBValue FREE_PAINTERS_RIGHT_AREA_WIDTH = JBVG.value(5);
    private static final JBValue GAP_BETWEEN_ICONS = JBVG.value(3);
    private static final JBValue GAP_BETWEEN_AREAS = JBVG.value(5);
    private static final JBValue GAP_BETWEEN_ANNOTATIONS = JBVG.value(5);
    private static final TooltipGroup GUTTER_TOOLTIP_GROUP = new TooltipGroup("GUTTER_TOOLTIP_GROUP", 0);

    private ClickInfo myLastActionableClick;
    private final DesktopEditorImpl myEditor;
    private final FoldingAnchorsOverlayStrategy myAnchorsDisplayStrategy;
    @Nullable
    private IntObjectMap<List<GutterMark>> myLineToGutterRenderers;

    private boolean myHasInlaysWithGutterIcons;
    private boolean myLineToGutterRenderersCacheForLogicalLines;
    private int myStartIconAreaWidth = START_ICON_AREA_WIDTH.get();
    private int myIconsAreaWidth;
    protected int myLineNumberAreaWidth;
    protected int myAdditionalLineNumberAreaWidth;
    @Nonnull
    private List<FoldRegion> myActiveFoldRegions = Collections.emptyList();
    protected int myTextAnnotationGuttersSize;
    protected int myTextAnnotationExtraSize;
    final it.unimi.dsi.fastutil.ints.IntList myTextAnnotationGutterSizes = new IntArrayList();
    final ArrayList<TextAnnotationGutterProvider> myTextAnnotationGutters = new ArrayList<>();
    private boolean myGapAfterAnnotations;
    private final Map<TextAnnotationGutterProvider, EditorGutterAction> myProviderToListener = new HashMap<>();
    private LocalizeValue myLastGutterToolTip;
    @Nonnull
    private LineNumberConverter myLineNumberConverter = LineNumberConverter.DEFAULT;
    @Nullable
    private LineNumberConverter myAdditionalLineNumberConverter;
    private boolean myShowDefaultGutterPopup = true;
    private boolean myCanCloseAnnotations = true;
    @Nullable
    private ActionGroup myCustomGutterPopupGroup;
    private final IntObjectMap<ColorValue> myTextFgColors = IntMaps.newIntObjectHashMap();
    private boolean myPaintBackground = true;
    private boolean myLeftFreePaintersAreaShown;
    private boolean myRightFreePaintersAreaShown;
    boolean myForceLeftFreePaintersAreaShown;
    boolean myForceRightFreePaintersAreaShown;
    private int myLastNonDumbModeIconAreaWidth;
    private final EditorGutterLayout myLayout = new EditorGutterLayout(this);
    private int myHoveredFreeMarkersLine = -1;
    boolean myDnDInProgress;
    @Nullable
    private AccessibleGutterLine myAccessibleGutterLine;

    private final AlphaAnimationContext myAlphaContext = new AlphaAnimationContext(composite -> {
        if (isShowing()) {
            repaint();
        }
    });

    EditorGutterComponentImpl(@Nonnull DesktopEditorImpl editor) {
        myEditor = editor;
        if (!Application.get().isHeadlessEnvironment()) {
            installDnD();
        }
        setOpaque(true);
        myAnchorsDisplayStrategy = new FoldingAnchorsOverlayStrategy(editor);

        Project project = myEditor.getProject();
        if (project != null) {
            project.getMessageBus().connect(myEditor.getDisposable()).subscribe(DumbModeListener.class, new DumbModeListener() {

                @Override
                public void exitDumbMode() {
                    updateSize();
                }
            });
        }
        if (ScreenReader.isActive()) {
            AccessibleGutterLine.installListeners(this);
        }
        else {
            ScreenReader.addPropertyChangeListener(ScreenReader.SCREEN_READER_ACTIVE_PROPERTY, editor.getDisposable(), e -> {
                if ((boolean) e.getNewValue()) {
                    AccessibleGutterLine.installListeners(this);
                }
            });
        }
        UISettingsUtil.setupEditorAntialiasing(this);
        HOVER_STATE_LISTENER.addTo(this);
    }

    @Nonnull
    DesktopEditorImpl getEditor() {
        return myEditor;
    }

    private void installDnD() {
        DnDSupport.createBuilder(this).setBeanProvider(info -> {
                final GutterMark renderer = getGutterRenderer(info.getPoint());
                if (renderer instanceof GutterIconRenderer gutterIconRenderer && gutterIconRenderer.getDraggableObject() != null
                    && (info.isCopy() || info.isMove())) {
                    myDnDInProgress = true;
                    return new DnDDragStartBean(renderer);
                }
                return null;
            }).setDropHandler(e -> {
                final Object attachedObject = e.getAttachedObject();
                if (attachedObject instanceof GutterIconRenderer && checkDumbAware(attachedObject)) {
                    final GutterDraggableObject draggableObject = ((GutterIconRenderer) attachedObject).getDraggableObject();
                    if (draggableObject != null) {
                        final int line = convertPointToLineNumber(e.getPoint());
                        if (line != -1) {
                            draggableObject.copy(line, myEditor.getVirtualFile(), e.getAction().getActionId());
                        }
                    }
                }
                else if (attachedObject instanceof DnDNativeTarget.EventInfo && myEditor.getSettings().isDndEnabled()) {
                    Transferable transferable = ((DnDNativeTarget.EventInfo) attachedObject).getTransferable();
                    if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        DesktopEditorImpl.handleDrop(myEditor, transferable, e.getAction().getActionId());
                    }
                }
                myDnDInProgress = false;
            }).setTargetChecker(e -> {
                final Object attachedObject = e.getAttachedObject();
                if (attachedObject instanceof GutterIconRenderer && checkDumbAware(attachedObject)) {
                    final GutterDraggableObject draggableObject = ((GutterIconRenderer) attachedObject).getDraggableObject();
                    if (draggableObject != null) {
                        final int line = convertPointToLineNumber(e.getPoint());
                        if (line != -1) {
                            e.setDropPossible(true);
                            e.setCursor(draggableObject.getCursor(line, e.getAction().getActionId()));
                        }
                    }
                }
                else if (attachedObject instanceof DnDNativeTarget.EventInfo && myEditor.getSettings().isDndEnabled()) {
                    Transferable transferable = ((DnDNativeTarget.EventInfo) attachedObject).getTransferable();
                    if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        final int line = convertPointToLineNumber(e.getPoint());
                        if (line != -1) {
                            e.setDropPossible(true);
                            myEditor.getCaretModel().moveToOffset(myEditor.getDocument().getLineStartOffset(line));
                        }
                    }
                }
                return true;
            }).setImageProvider(info -> {
                Image image = ImageUtil.toBufferedImage(getDragImage(getGutterRenderer(info.getPoint())));
                return new DnDImage(image, new Point(image.getWidth(null) / 2, image.getHeight(null) / 2));
            })
            .enableAsNativeTarget() // required to accept dragging from editor (as editor component doesn't use DnDSupport to implement drag'n'drop)
            .install();
    }

    Image getDragImage(GutterMark renderer) {
        return new DesktopAWTScalableImage(scaleImage(renderer.getIcon()));
    }

    private void fireResized() {
        processComponentEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_RESIZED));
    }

    @Override
    public Dimension getPreferredSize() {
        int w = myLayout.getWidth();
        Dimension size = new Dimension(w, myEditor.getPreferredHeight());
        JBInsets.addTo(size, getInsets());
        return size;
    }

    @Override
    protected void setUI(ComponentUI newUI) {
        super.setUI(newUI);
        reinitSettings(true);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        reinitSettings(true);
    }

    public void reinitSettings(boolean updateGutterSize) {
        updateSize(false, updateGutterSize);
        repaint();
    }

    @Override
    protected Graphics getComponentGraphics(Graphics graphics) {
        return JBSwingUtilities.runGlobalCGTransform(this, super.getComponentGraphics(graphics));
    }

    @Override
    public void paintComponent(Graphics g_) {
        Rectangle clip = g_.getClipBounds();
        if (clip.height < 0) {
            return;
        }

        Graphics2D g = (Graphics2D) getComponentGraphics(g_);
        AffineTransform old = setMirrorTransformIfNeeded(g, 0, getWidth());

        EditorUIUtil.setupAntialiasing(g);
        Color backgroundColor = getBackground();

        if (myEditor.isDisposed()) {
            g.setColor(myEditor.getDisposedBackground());
            g.fillRect(clip.x, clip.y, clip.width, clip.height);
            return;
        }

        int startVisualLine;
        int endVisualLine;

        int firstVisibleOffset;
        int lastVisibleOffset;

        Segment focusModeRange = myEditor.getFocusModeRange();
        if (focusModeRange == null) {
            startVisualLine = myEditor.yToVisualLine(clip.y);
            endVisualLine = myEditor.yToVisualLine(clip.y + clip.height);

            firstVisibleOffset = myEditor.visualLineStartOffset(startVisualLine);
            lastVisibleOffset = myEditor.visualLineStartOffset(endVisualLine + 1);
        }
        else {
            firstVisibleOffset = focusModeRange.getStartOffset();
            lastVisibleOffset = focusModeRange.getEndOffset();

            startVisualLine = myEditor.offsetToVisualLine(firstVisibleOffset);
            endVisualLine = myEditor.offsetToVisualLine(lastVisibleOffset);
        }

        // paint all backgrounds
        int gutterSeparatorX = getWhitespaceSeparatorOffset();
        paintBackground(g, clip, 0, gutterSeparatorX, TargetAWT.from(backgroundColor));
        paintBackground(g, clip, gutterSeparatorX, getWidth() - gutterSeparatorX, myEditor.getBackgroundColor());

        paintEditorBackgrounds(g, firstVisibleOffset, lastVisibleOffset);

        Object hint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (!UIUtil.isJreHiDPI(g)) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        try {
            paintAnnotations(g, startVisualLine, endVisualLine);

            if (focusModeRange != null) {
                int startY = Math.max(myEditor.visualLineToY(startVisualLine), clip.y);
                int endY = Math.min(myEditor.visualLineToY(endVisualLine), clip.y + clip.height);
                g.setClip(clip.x, startY, clip.width, endY - startY);
            }

            paintLineMarkers(g, firstVisibleOffset, lastVisibleOffset, startVisualLine, endVisualLine);

            g.setClip(clip);

            paintFoldingTree(g, clip, firstVisibleOffset, lastVisibleOffset);
            paintLineNumbers(g, startVisualLine, endVisualLine);
            paintCurrentAccessibleLine(g);

            if (ExperimentalUI.isNewUI() && myPaintBackground) {
                g.setColor(TargetAWT.to(getEditor().getColorsScheme().getColor(EditorColors.INDENT_GUIDE_COLOR)));
                LinePainter2D.paint(g, gutterSeparatorX, clip.y, gutterSeparatorX, clip.y + clip.height);
            }
        }
        finally {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
        }

        if (old != null) {
            g.setTransform(old);
        }
    }

    private void paintEditorBackgrounds(Graphics g, int firstVisibleOffset, int lastVisibleOffset) {
        myTextFgColors.clear();
        ColorValue defaultBackgroundColor = myEditor.getBackgroundColor();
        ColorValue defaultForegroundColor = myEditor.getColorsScheme().getDefaultForeground();
        int startX = myEditor.isInDistractionFreeMode()
            ? 0
            : ExperimentalUI.isNewUI()
            ? getWhitespaceSeparatorOffset() + 1
            : getWhitespaceSeparatorOffset();
        IterationState state = new IterationState(
            myEditor,
            firstVisibleOffset,
            lastVisibleOffset,
            null,
            true,
            false,
            true,
            false
        );
        while (!state.atEnd()) {
            drawEditorBackgroundForRange(
                g,
                state.getStartOffset(),
                state.getEndOffset(),
                state.getMergedAttributes(),
                defaultBackgroundColor,
                defaultForegroundColor,
                startX
            );
            state.advance();
        }
    }

    private void drawEditorBackgroundForRange(
        Graphics g,
        int startOffset,
        int endOffset,
        TextAttributes attributes,
        ColorValue defaultBackgroundColor,
        ColorValue defaultForegroundColor,
        int startX
    ) {
        ColorValue bgColor = myEditor.getBackgroundColor(attributes);
        if (Comparing.equal(bgColor, defaultBackgroundColor)) {
            return;
        }

        VisualPosition visualStart = myEditor.offsetToVisualPosition(startOffset, true, false);
        VisualPosition visualEnd = myEditor.offsetToVisualPosition(endOffset, false, false);
        int startVisualLine = visualStart.getLine() + (visualStart.getColumn() == 0 ? 0 : 1);
        int endVisualLine = visualEnd.getLine() - (visualEnd.getColumn() == 0 ? 1 : 0);
        if (startVisualLine <= endVisualLine) {
            int startY = myEditor.visualLineToY(startVisualLine);
            int endY = myEditor.visualLineToY(endVisualLine) + myEditor.getLineHeight();
            g.setColor(TargetAWT.to(bgColor));
            g.fillRect(startX, startY, getWidth() - startX, endY - startY);

            ColorValue fgColor = attributes.getForegroundColor();
            if (!Comparing.equal(fgColor, defaultForegroundColor)) {
                for (int line = startVisualLine; line <= endVisualLine; line++) {
                    myTextFgColors.put(line, fgColor);
                }
            }
        }
    }

    private void processClose(final MouseEvent e) {
        final IdeEventQueue queue = IdeEventQueue.getInstance();

        // See IDEA-59553 for rationale on why this feature is disabled
        //if (isLineNumbersShown()) {
        //  if (e.getX() >= getLineNumberAreaOffset() && getLineNumberAreaOffset() + getLineNumberAreaWidth() >= e.getX()) {
        //    queue.blockNextEvents(e);
        //    myEditor.getSettings().setLineNumbersShown(false);
        //    e.consume();
        //    return;
        //  }
        //}

        if (getGutterRenderer(e) != null) {
            return;
        }

        if (myEditor.getMouseEventArea(e) == EditorMouseEventArea.ANNOTATIONS_AREA) {
            queue.blockNextEvents(e);
            closeAllAnnotations();
            e.consume();
        }
    }

    private void paintAnnotations(Graphics2D g, int startVisualLine, int endVisualLine) {
        int x = getAnnotationsAreaOffset();
        int w = getAnnotationsAreaWidthEx();

        if (w == 0) {
            return;
        }

        int viewportStartY = myEditor.getScrollingModel().getVisibleArea().y;

        AffineTransform old = setMirrorTransformIfNeeded(g, x, w);
        try {
            ColorValue color = myEditor.getColorsScheme().getColor(EditorColors.ANNOTATIONS_COLOR);
            g.setColor(color != null ? TargetAWT.to(color) : JBColor.blue);
            g.setFont(myEditor.getColorsScheme().getFont(EditorFontType.PLAIN));

            for (int i = 0; i < myTextAnnotationGutters.size(); i++) {
                TextAnnotationGutterProvider gutterProvider = myTextAnnotationGutters.get(i);

                int lineHeight = myEditor.getLineHeight();
                int lastLine = myEditor.logicalToVisualPosition(new LogicalPosition(endLineNumber(), 0)).line;
                endVisualLine = Math.min(endVisualLine, lastLine);
                if (startVisualLine > endVisualLine) {
                    break;
                }

                int annotationSize = myTextAnnotationGutterSizes.getInt(i);

                int logicalLine = -1;
                ColorValue bg = null;
                VisualLinesIterator visLinesIterator = new VisualLinesIterator(myEditor, startVisualLine);
                while (!visLinesIterator.atEnd() && visLinesIterator.getVisualLine() <= endVisualLine) {
                    if (!visLinesIterator.isCustomFoldRegionLine()) {
                        int y = visLinesIterator.getY();
                        int bgLineHeight = lineHeight;
                        boolean paintText = !visLinesIterator.startsWithSoftWrap() || y <= viewportStartY;

                        if (y < viewportStartY && visLinesIterator.endsWithSoftWrap()) {  // "sticky" line annotation
                            y = viewportStartY;
                        }
                        else if (viewportStartY < y && y < viewportStartY + lineHeight && visLinesIterator.startsWithSoftWrap()) {
                            // avoid drawing bg over the "sticky" line above, or over a possible gap in the gutter below (e.g., code vision)
                            bgLineHeight = y - viewportStartY;
                            y = viewportStartY + lineHeight;
                        }

                        if (paintText || logicalLine == -1) {
                            logicalLine = visLinesIterator.getDisplayedLogicalLine();
                            bg = gutterProvider.getBgColor(logicalLine, myEditor);
                        }
                        if (bg != null) {
                            g.setColor(TargetAWT.to(bg));
                            g.fillRect(x, y, annotationSize, bgLineHeight);
                        }
                        if (paintText) {
                            paintAnnotationLine(g, gutterProvider, logicalLine, x, y);
                        }
                    }
                    visLinesIterator.advance();
                }

                x += annotationSize;
            }
        }
        finally {
            if (old != null) {
                g.setTransform(old);
            }
        }
    }

    private void paintAnnotationLine(Graphics g, TextAnnotationGutterProvider gutterProvider, int line, int x, int y) {
        String s = gutterProvider.getLineText(line, myEditor);
        if (!StringUtil.isEmpty(s)) {
            g.setColor(TargetAWT.to(myEditor.getColorsScheme().getColor(gutterProvider.getColor(line, myEditor))));
            EditorFontType style = gutterProvider.getStyle(line, myEditor);
            Font font = getFontForText(s, style);
            g.setFont(font);
            int offset = 0;
            if (gutterProvider.useMargin()) {
                if (gutterProvider.getLeftMargin() >= 0) {
                    offset = gutterProvider.getLeftMargin();
                }
                else {
                    offset = getGapBetweenAnnotations() / 2;
                }
            }
            g.drawString(s, offset + x, y + myEditor.getAscent());
        }
    }

    private Font getFontForText(String text, EditorFontType style) {
        Font font = ExperimentalUI.isNewUI()
            ? JBFont.regular().deriveFont((float) myEditor.getFontSize())
            : myEditor.getColorsScheme().getFont(style);
        return UIUtil.getFontWithFallbackIfNeeded(font, text);
    }

    private void paintFoldingTree(@Nonnull Graphics g, @Nonnull Rectangle clip, int firstVisibleOffset, int lastVisibleOffset) {
        if (isFoldingOutlineShown()) {
            doPaintFoldingTree((Graphics2D) g, clip, firstVisibleOffset, lastVisibleOffset);
        }
    }

    private void paintLineMarkers(Graphics2D g, int firstVisibleOffset, int lastVisibleOffset, int firstVisibleLine, int lastVisibleLine) {
        if (isLineMarkersShown()) {
            paintGutterRenderers(g, firstVisibleOffset, lastVisibleOffset, firstVisibleLine, lastVisibleLine);
        }
    }

    private void paintBackground(final Graphics g, final Rectangle clip, final int x, final int width, ColorValue background) {
        g.setColor(TargetAWT.to(background));
        g.fillRect(x, clip.y, width, clip.height);

        paintCaretRowBackground(g, x, width);
    }

    private void paintCaretRowBackground(final Graphics g, final int x, final int width) {
        if (!myEditor.getSettings().isCaretRowShown()) {
            return;
        }
        final VisualPosition visCaret = myEditor.getCaretModel().getVisualPosition();
        Color caretRowColor = TargetAWT.to(myEditor.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR));
        if (caretRowColor != null) {
            g.setColor(caretRowColor);
            final Point caretPoint = myEditor.visualPositionToXY(visCaret);
            g.fillRect(x, caretPoint.y, width, myEditor.getLineHeight());
        }
    }

    private void paintLineNumbers(Graphics2D g, int startVisualLine, int endVisualLine) {
        if (isLineNumbersShown()) {
            int offset = getLineNumberAreaOffset() + myLineNumberAreaWidth;
            doPaintLineNumbers(g, startVisualLine, endVisualLine, offset, myLineNumberConverter);
            if (myAdditionalLineNumberConverter != null) {
                doPaintLineNumbers(
                    g,
                    startVisualLine,
                    endVisualLine,
                    offset + getAreaWidthWithGap(myAdditionalLineNumberAreaWidth),
                    myAdditionalLineNumberConverter
                );
            }
        }
    }

    private void paintCurrentAccessibleLine(Graphics2D g) {
        if (myAccessibleGutterLine != null) {
            myAccessibleGutterLine.paint(g);
        }
    }

    @Override
    public Color getBackground() {
        return TargetAWT.to(getBackgroundColorValue());
    }

    private ColorValue getBackgroundColorValue() {
        if (myEditor.isInDistractionFreeMode() || !myPaintBackground) {
            return myEditor.getBackgroundColor();
        }

        if (ExperimentalUI.isNewUI()) {
            ColorValue bg = myEditor.getColorsScheme().getColor(EditorColors.EDITOR_GUTTER_BACKGROUND);
            return bg == null ? myEditor.getBackgroundColor() : bg;
        }

        ColorValue color = myEditor.getColorsScheme().getColor(EditorColors.GUTTER_BACKGROUND);
        return color != null ? color : EditorColors.GUTTER_BACKGROUND.getDefaultColorValue();
    }

    private Font getFontForLineNumbers() {
        Font editorFont = myEditor.getColorsScheme().getFont(EditorFontType.PLAIN);
        float editorFontSize = editorFont.getSize2D();
        return editorFont.deriveFont(Math.max(1f, editorFontSize - 1f));
    }

    private int calcLineNumbersAreaWidth(@Nonnull String maxLineNumberText) {
        return FontLayoutService.getInstance().stringWidth(getFontMetrics(getFontForLineNumbers()), maxLineNumberText);
    }

    private void doPaintLineNumbers(
        Graphics2D g,
        int startVisualLine,
        int endVisualLine,
        int offset,
        @Nonnull LineNumberConverter converter
    ) {
        int lastLine = myEditor.logicalToVisualPosition(new LogicalPosition(endLineNumber(), 0)).line;
        endVisualLine = Math.min(endVisualLine, lastLine);
        if (startVisualLine > endVisualLine) {
            return;
        }

        ColorValue color = myEditor.getColorsScheme().getColor(EditorColors.LINE_NUMBERS_COLOR);
        ColorValue colorUnderCaretRow = myEditor.getColorsScheme().getColor(EditorColors.LINE_NUMBER_ON_CARET_ROW_COLOR);
        Font font = getFontForLineNumbers();
        g.setFont(font);
        int viewportStartY = myEditor.getScrollingModel().getVisibleArea().y;

        AffineTransform old = setMirrorTransformIfNeeded(g, getLineNumberAreaOffset(), getLineNumberAreaWidth());
        try {
            int caretLogicalLine = myEditor.getCaretModel().getLogicalPosition().line;
            VisualLinesIterator visLinesIterator = new VisualLinesIterator(myEditor, startVisualLine);
            while (!visLinesIterator.atEnd() && visLinesIterator.getVisualLine() <= endVisualLine) {
                if (!visLinesIterator.isCustomFoldRegionLine() &&
                    (!visLinesIterator.startsWithSoftWrap() || visLinesIterator.getY() <= viewportStartY)) {
                    int logicalLine = visLinesIterator.getDisplayedLogicalLine();
                    String lineToDisplay = converter.convertLineNumberToString(myEditor, logicalLine + 1);
                    if (lineToDisplay != null) {
                        int y = visLinesIterator.getY();
                        if (y < viewportStartY && visLinesIterator.endsWithSoftWrap()) {  // "sticky" line number
                            y = viewportStartY;
                        }
                        if (myEditor.isInDistractionFreeMode()) {
                            ColorValue fgColor = myTextFgColors.get(visLinesIterator.getVisualLine());
                            g.setColor(TargetAWT.to(fgColor != null ? fgColor : color != null ? color : StandardColors.BLUE));
                        }
                        else {
                            g.setColor(TargetAWT.to(color));
                        }

                        if (colorUnderCaretRow != null && caretLogicalLine == logicalLine) {
                            g.setColor(TargetAWT.to(colorUnderCaretRow));
                        }

                        consulo.ui.image.Image iconOnTheLine = null;
                        consulo.ui.image.Image hoverIcon = null;
                        if (BreakpointEditorUtil.isBreakPointsOnLineNumbers()) {
                            VisualPosition visualPosition = myEditor.logicalToVisualPosition(new LogicalPosition(logicalLine, 0));
                            Optional<GutterMark> breakpoint = getGutterRenderers(visualPosition.line).stream()
                                .filter(r -> r instanceof GutterIconRenderer &&
                                    ((GutterIconRenderer) r).getAlignment() == GutterIconRenderer.Alignment.LINE_NUMBERS)
                                .findFirst();
                            if (breakpoint.isPresent()) {
                                iconOnTheLine = breakpoint.get().getIcon();
                            }
                            if ((myAlphaContext.isVisible() || isGutterContextMenuShown()) &&
                                Objects.equals(getClientProperty("active.line.number"), logicalLine)) {
                                Object activeIcon = getClientProperty("line.number.hover.icon");
                                if (activeIcon instanceof consulo.ui.image.Image activeIconImage) {
                                    hoverIcon = activeIconImage;
                                }
                            }
                        }

                        if (iconOnTheLine == null && hoverIcon == null) {
                            int textOffset = isMirrored() ?
                                offset - getLineNumberAreaWidth() - 1 :
                                offset - FontLayoutService.getInstance().stringWidth(g.getFontMetrics(), lineToDisplay);

                            g.drawString(lineToDisplay, textOffset, y + myEditor.getAscent());
                        }
                        else if (hoverIcon != null && iconOnTheLine == null) {
                            Icon icon = scaleIcon(hoverIcon);
                            int iconX = offset - icon.getIconWidth();
                            int iconY = y + (visLinesIterator.getLineHeight() - icon.getIconHeight()) / 2;
                            float alpha = JBUI.getFloat("Breakpoint.iconHoverAlpha", 0.5f);
                            alpha = alpha > 1f ? 1f : Math.max(alpha, 0f);
                            GraphicsUtil.paintWithAlpha(g, alpha, () -> icon.paintIcon(this, g, iconX, iconY));
                        }
                    }
                }
                visLinesIterator.advance();
            }
        }
        finally {
            if (old != null) {
                g.setTransform(old);
            }
        }
    }

    private int endLineNumber() {
        return Math.max(0, myEditor.getDocument().getLineCount() - 1);
    }

    @Nullable
    @Override
    public Object getData(@Nonnull Key dataId) {
        if (myEditor.isDisposed()) {
            return null;
        }

        if (EditorGutter.KEY == dataId) {
            return this;
        }
        if (Editor.KEY == dataId) {
            return myEditor;
        }
        if (EditorGutterComponentEx.LOGICAL_LINE_AT_CURSOR == dataId) {
            return myLastActionableClick == null ? null : myLastActionableClick.myLogicalLineAtCursor;
        }
        if (EditorGutterComponentEx.ICON_CENTER_POSITION == dataId) {
            return myLastActionableClick == null ? null : myLastActionableClick.myIconCenterPosition;
        }
        return null;
    }

    @FunctionalInterface
    interface RangeHighlighterProcessor {
        void process(@Nonnull RangeHighlighter highlighter);
    }

    void processRangeHighlighters(int startOffset, int endOffset, @Nonnull RangeHighlighterProcessor processor) {
        // we limit highlighters to process to between line starting at startOffset and line ending at endOffset
        MarkupIterator<RangeHighlighterEx> docHighlighters =
            myEditor.getFilteredDocumentMarkupModel().overlappingIterator(startOffset, endOffset, true, false);
        MarkupIterator<RangeHighlighterEx> editorHighlighters =
            myEditor.getMarkupModel().overlappingIterator(startOffset, endOffset, true, false);

        try {
            RangeHighlighterEx lastDocHighlighter = null;
            RangeHighlighterEx lastEditorHighlighter = null;
            while (true) {
                if (lastDocHighlighter == null && docHighlighters.hasNext()) {
                    lastDocHighlighter = docHighlighters.next();
                    if (lastDocHighlighter.getAffectedAreaStartOffset() > endOffset) {
                        lastDocHighlighter = null;
                        continue;
                    }
                    if (lastDocHighlighter.getAffectedAreaEndOffset() < startOffset) {
                        lastDocHighlighter = null;
                        continue;
                    }
                }

                if (lastEditorHighlighter == null && editorHighlighters.hasNext()) {
                    lastEditorHighlighter = editorHighlighters.next();
                    if (lastEditorHighlighter.getAffectedAreaStartOffset() > endOffset) {
                        lastEditorHighlighter = null;
                        continue;
                    }
                    if (lastEditorHighlighter.getAffectedAreaEndOffset() < startOffset) {
                        lastEditorHighlighter = null;
                        continue;
                    }
                }

                if (lastDocHighlighter == null && lastEditorHighlighter == null) {
                    return;
                }

                final RangeHighlighterEx lowerHighlighter;
                if (less(lastDocHighlighter, lastEditorHighlighter)) {
                    lowerHighlighter = lastDocHighlighter;
                    lastDocHighlighter = null;
                }
                else {
                    lowerHighlighter = lastEditorHighlighter;
                    lastEditorHighlighter = null;
                }

                processor.process(lowerHighlighter);
            }
        }
        finally {
            docHighlighters.dispose();
            editorHighlighters.dispose();
        }
    }

    private static boolean isValidLine(@Nonnull Document document, int line) {
        if (line < 0) {
            return false;
        }
        int lineCount = document.getLineCount();
        return lineCount == 0 ? line == 0 : line < lineCount;
    }

    private static boolean less(RangeHighlighter h1, RangeHighlighter h2) {
        return h1 != null && (h2 == null || h1.getStartOffset() < h2.getStartOffset());
    }

    @Override
    public void revalidateMarkup() {
        updateSize();
    }

    void updateSizeOnShowNotify() {
        updateSize(false, true);
    }

    public void updateSize() {
        updateSize(false, false);
    }

    void updateSize(boolean onLayout, boolean canShrink) {
        int prevHash = sizeHash();

        if (!onLayout) {
            clearLineToGutterRenderersCache();
            calcLineNumberAreaWidth();
            calcLineMarkerAreaWidth(canShrink);
            calcAnnotationsSize();
        }
        calcAnnotationExtraSize();

        if (prevHash != sizeHash()) {
            fireResized();
        }
        repaint();
    }

    private int sizeHash() {
        int result = getLineMarkerAreaWidth();
        result = 31 * result + myTextAnnotationGuttersSize;
        result = 31 * result + myTextAnnotationExtraSize;
        result = 31 * result + getLineNumberAreaWidth();
        return result;
    }

    private void calcAnnotationsSize() {
        myTextAnnotationGuttersSize = 0;
        myGapAfterAnnotations = false;
        final int lineCount = Math.max(myEditor.getDocument().getLineCount(), 1);
        final int guttersCount = myTextAnnotationGutters.size();
        for (int j = 0; j < guttersCount; j++) {
            TextAnnotationGutterProvider gutterProvider = myTextAnnotationGutters.get(j);
            int gutterSize = 0;
            for (int i = 0; i < lineCount; i++) {
                String lineText = gutterProvider.getLineText(i, myEditor);
                if (!StringUtil.isEmpty(lineText)) {
                    EditorFontType style = gutterProvider.getStyle(i, myEditor);
                    Font font = getFontForText(lineText, style);
                    FontMetrics fontMetrics = getFontMetrics(font);
                    gutterSize = Math.max(gutterSize, fontMetrics.stringWidth(lineText));
                }
            }
            if (gutterSize > 0) {
                boolean margin = gutterProvider.useMargin();
                myGapAfterAnnotations = margin;
                if (margin) {
                    gutterSize += getGapBetweenAnnotations();
                }
            }
            myTextAnnotationGutterSizes.set(j, gutterSize);
            myTextAnnotationGuttersSize += gutterSize;
        }
    }

    private void calcAnnotationExtraSize() {
        myTextAnnotationExtraSize = 0;
        if (!myEditor.isInDistractionFreeMode() || isMirrored()) {
            return;
        }

        Window frame = SwingUtilities.getWindowAncestor(myEditor.getComponent());
        if (frame == null) {
            return;
        }

        EditorSettings settings = myEditor.getSettings();
        int rightMargin = settings.getRightMargin(myEditor.getProject());
        if (rightMargin <= 0) {
            return;
        }

        JComponent editorComponent = myEditor.getComponent();
        RelativePoint point = new RelativePoint(editorComponent, new Point(0, 0));
        Point editorLocationInWindow = point.getPoint(frame);

        int editorLocationX = (int) editorLocationInWindow.getX();
        int rightMarginX = rightMargin * EditorUtil.getSpaceWidth(Font.PLAIN, myEditor) + editorLocationX;

        int width = editorLocationX + editorComponent.getWidth();
        if (rightMarginX < width && editorLocationX < width - rightMarginX) {
            int centeredSize =
                (width - rightMarginX - editorLocationX) / 2 - (getLineMarkerAreaWidth() + getLineNumberAreaWidth() + getFoldingAreaWidth() + 2 * getGapBetweenAreas());
            myTextAnnotationExtraSize = Math.max(0, centeredSize - myTextAnnotationGuttersSize);
        }
    }

    private boolean logicalLinesMatchVisualOnes() {
        return myEditor.getSoftWrapModel().getSoftWrapsIntroducedLinesNumber() == 0 && myEditor.getFoldingModel()
            .getTotalNumberOfFoldedLines() == 0;
    }

    void clearLineToGutterRenderersCache() {
        myLineToGutterRenderers = null;
    }

    private void buildGutterRenderersCache() {
        myLineToGutterRenderersCacheForLogicalLines = logicalLinesMatchVisualOnes();
        myLineToGutterRenderers = IntMaps.newIntObjectHashMap();
        processRangeHighlighters(0, myEditor.getDocument().getTextLength(), highlighter -> {
            GutterMark renderer = highlighter.getGutterIconRenderer();
            if (renderer == null) {
                return;
            }
            if (!areIconsShown() && !(renderer instanceof NonHideableIconGutterMark)) {
                return;
            }
            if (!isHighlighterVisible(highlighter)) {
                return;
            }
            int line = myEditor.offsetToVisualLine(highlighter.getStartOffset());
            List<GutterMark> renderers = myLineToGutterRenderers.get(line);
            if (renderers == null) {
                renderers = new SmartList<>();
                myLineToGutterRenderers.put(line, renderers);
            }

            renderers.add(renderer);
        });

        for (IntObjectMap.IntObjectEntry<List<GutterMark>> entry : new ArrayList<>(myLineToGutterRenderers.entrySet())) {
            int key = entry.getKey();
            List<GutterMark> value = entry.getValue();

            List<GutterMark> newValue = value;
            for (GutterMarkPreprocessor preprocessor : Application.get().getExtensionList(GutterMarkPreprocessor.class)) {
                newValue = preprocessor.processMarkers(value);
            }

            // Don't allow more than 5 icons per line
            newValue = ContainerUtil.getFirstItems(newValue, 4);

            myLineToGutterRenderers.put(key, newValue);
        }
    }

    private void calcLineMarkerAreaWidth(boolean canShrink) {
        myLeftFreePaintersAreaShown = myForceLeftFreePaintersAreaShown;
        myRightFreePaintersAreaShown = myForceRightFreePaintersAreaShown;

        processRangeHighlighters(0, myEditor.getDocument().getTextLength(), highlighter -> {
            LineMarkerRenderer lineMarkerRenderer = highlighter.getLineMarkerRenderer();
            if (lineMarkerRenderer != null) {
                LineMarkerRendererEx.Position position = getLineMarkerPosition(lineMarkerRenderer);
                if (position == LineMarkerRendererEx.Position.LEFT && isLineMarkerVisible(highlighter)) {
                    myLeftFreePaintersAreaShown = true;
                }
                if (position == LineMarkerRendererEx.Position.RIGHT && isLineMarkerVisible(highlighter)) {
                    myRightFreePaintersAreaShown = true;
                }
            }
        });

        int minWidth = areIconsShown() ? scaleWidth(myStartIconAreaWidth) : 0;
        myIconsAreaWidth = canShrink ? minWidth : Math.max(myIconsAreaWidth, minWidth);

        processGutterRenderers((line, renderers) -> {
            int width = 1;
            for (int i = 0; i < renderers.size(); i++) {
                GutterMark renderer = renderers.get(i);
                if (!checkDumbAware(renderer)) {
                    continue;
                }
                if (isMergedWithLineNumbers(renderer)) {
                    continue;
                }
                width += scaleIcon(renderer.getIcon()).getIconWidth();
                if (i > 0) {
                    width += getGapBetweenIcons();
                }
            }
            if (myIconsAreaWidth < width) {
                myIconsAreaWidth = width + 1;
            }
            return true;
        });

        myHasInlaysWithGutterIcons = false;
        myEditor.getInlayModel().getBlockElementsInRange(0, myEditor.getDocument().getTextLength()).forEach(inlay -> {
            GutterIconRenderer iconRenderer = inlay.getGutterIconRenderer();
            if (shouldBeShown(iconRenderer) && checkDumbAware(iconRenderer) && !EditorImplUtil.isInlayFolded(inlay)) {
                Icon icon = scaleIcon(iconRenderer.getIcon());
                if (icon.getIconHeight() <= inlay.getHeightInPixels()) {
                    myHasInlaysWithGutterIcons = true;
                    myIconsAreaWidth = Math.max(myIconsAreaWidth, icon.getIconWidth());
                }
            }
        });

        if (isDumbMode()) {
            myIconsAreaWidth = Math.max(myIconsAreaWidth, myLastNonDumbModeIconAreaWidth);
        }
        else {
            myLastNonDumbModeIconAreaWidth = myIconsAreaWidth;
        }
    }

    private boolean shouldBeShown(@Nullable GutterMark gutterIconRenderer) {
        return gutterIconRenderer != null && (areIconsShown() || gutterIconRenderer instanceof NonHideableIconGutterMark);
    }

    @Override
    @Nonnull
    public List<GutterMark> getGutterRenderers(int line) {
        if (myLineToGutterRenderers == null || myLineToGutterRenderersCacheForLogicalLines != logicalLinesMatchVisualOnes()) {
            buildGutterRenderersCache();
        }

        Segment focusModeRange = myEditor.getFocusModeRange();
        if (focusModeRange != null) {
            int start = myEditor.offsetToVisualLine(focusModeRange.getStartOffset());
            int end = myEditor.offsetToVisualLine(focusModeRange.getEndOffset());
            if (line < start || line > end) {
                return Collections.emptyList();
            }
        }

        List<GutterMark> marks = myLineToGutterRenderers.get(line);
        return marks != null ? marks : Collections.emptyList();
    }

    private void processGutterRenderers(@Nonnull IntObjPredicate<List<GutterMark>> processor) {
        if (myLineToGutterRenderers == null || myLineToGutterRenderersCacheForLogicalLines != logicalLinesMatchVisualOnes()) {
            buildGutterRenderersCache();
        }

        for (IntObjectMap.IntObjectEntry<List<GutterMark>> entry : myLineToGutterRenderers.entrySet()) {
            int key = entry.getKey();
            List<GutterMark> value = entry.getValue();

            if (!processor.test(key, value)) {
                return;
            }
        }
    }

    private boolean isHighlighterVisible(RangeHighlighter highlighter) {
        return !FoldingUtil.isHighlighterFolded(myEditor, highlighter);
    }

    private void paintGutterRenderers(
        final Graphics2D g,
        int firstVisibleOffset,
        int lastVisibleOffset,
        int firstVisibleLine,
        int lastVisibleLine
    ) {
        Object hint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            List<RangeHighlighter> highlighters = new ArrayList<>();
            processRangeHighlighters(firstVisibleOffset, lastVisibleOffset, highlighter -> {
                LineMarkerRenderer renderer = highlighter.getLineMarkerRenderer();
                if (renderer != null) {
                    highlighters.add(highlighter);
                }
            });

            ContainerUtil.sort(highlighters, Comparator.comparingInt(RangeHighlighter::getLayer));

            for (RangeHighlighter highlighter : highlighters) {
                paintLineMarkerRenderer(highlighter, g);
            }
        }
        finally {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
        }

        paintIcons(firstVisibleLine, lastVisibleLine, g);
    }

    private void paintIcons(final int firstVisibleLine, final int lastVisibleLine, final Graphics2D g) {
        VisualLinesIterator visLinesIterator = new VisualLinesIterator(myEditor, firstVisibleLine);
        while (!visLinesIterator.atEnd()) {
            int visualLine = visLinesIterator.getVisualLine();
            if (visualLine > lastVisibleLine) {
                break;
            }
            int y = visLinesIterator.getY();

            List<GutterMark> renderers = getGutterRenderers(visualLine);
            paintIconRow(visualLine, y, renderers, g);

            if (myHasInlaysWithGutterIcons) {
                Rectangle clip = g.getClipBounds();
                int curY = y;
                for (Inlay<?> inlay : visLinesIterator.getBlockInlaysAbove()) {
                    if (curY <= clip.y) {
                        break;
                    }
                    int height = inlay.getHeightInPixels();
                    if (height > 0) {
                        int newY = curY - height;
                        paintInlayIcon(inlay, g, newY);
                        curY = newY;
                    }
                }
                curY = y + visLinesIterator.getLineHeight();
                for (Inlay<?> inlay : visLinesIterator.getBlockInlaysBelow()) {
                    if (curY >= clip.y + clip.height) {
                        break;
                    }
                    int height = inlay.getHeightInPixels();
                    if (height > 0) {
                        paintInlayIcon(inlay, g, curY);
                        curY += height;
                    }
                }
            }

            visLinesIterator.advance();
        }
    }

    private void paintInlayIcon(Inlay<?> inlay, Graphics2D g, int y) {
        GutterIconRenderer iconRenderer = inlay.getGutterIconRenderer();
        if (shouldBeShown(iconRenderer) && checkDumbAware(iconRenderer)) {
            Icon icon = scaleIcon(iconRenderer.getIcon());
            if (icon.getIconHeight() <= inlay.getHeightInPixels()) {
                int iconWidth = icon.getIconWidth();
                int x = getIconAreaOffset() + myIconsAreaWidth - iconWidth;
                y += getTextAlignmentShiftForInlayIcon(icon, inlay);
                AffineTransform old = setMirrorTransformIfNeeded(g, x, iconWidth);
                icon.paintIcon(this, g, x, y);
                if (old != null) {
                    g.setTransform(old);
                }
            }
        }
    }

    private int getTextAlignmentShiftForInlayIcon(Icon icon, Inlay<?> inlay) {
        return Math.min(getTextAlignmentShift(icon), inlay.getHeightInPixels() - icon.getIconHeight());
    }

    private void paintIconRow(int visualLine, int lineY, List<? extends GutterMark> row, Graphics2D g) {
        processIconsRowForY(lineY, row, (x, y, renderer) -> {
            boolean isLoading = myLastActionableClick != null &&
                myLastActionableClick.myProgressVisualLine == visualLine &&
                myLastActionableClick.myProgressGutterMark == renderer;
            Icon icon = scaleIcon(renderer.getIcon());
            if (isLoading) {
                Icon loadingIcon = scaleIcon(AnimatedIcon.Default.INSTANCE);
                x -= (loadingIcon.getIconWidth() - icon.getIconWidth()) / 2;
                y -= (loadingIcon.getIconHeight() - icon.getIconHeight()) / 2;
                icon = loadingIcon;
            }

            AffineTransform old = setMirrorTransformIfNeeded(g, x, icon.getIconWidth());
            try {
                icon.paintIcon(this, g, x, y);
            }
            finally {
                if (old != null) {
                    g.setTransform(old);
                }
            }
        });
    }

    private void paintIconRow(int line, List<? extends GutterMark> row, final Graphics2D g) {
        processIconsRow(line, row, (x, y, renderer) -> {
            Icon icon = scaleIcon(renderer.getIcon());

            AffineTransform old = setMirrorTransformIfNeeded(g, x, icon.getIconWidth());
            try {
                icon.paintIcon(this, g, x, y);
            }
            finally {
                if (old != null) {
                    g.setTransform(old);
                }
            }
        });
    }

    private void paintLineMarkerRenderer(RangeHighlighter highlighter, Graphics g) {
        LineMarkerRenderer lineMarkerRenderer = highlighter.getLineMarkerRenderer();
        if (lineMarkerRenderer != null) {
            Rectangle rectangle = getLineRendererRectangle(highlighter);
            if (rectangle != null) {
                lineMarkerRenderer.paint(myEditor, g, rectangle);
            }
        }
    }

    private boolean isLineMarkerVisible(RangeHighlighter highlighter) {
        int startOffset = highlighter.getStartOffset();
        int endOffset = highlighter.getEndOffset();

        FoldRegion startFoldRegion = myEditor.getFoldingModel().getCollapsedRegionAtOffset(startOffset);
        FoldRegion endFoldRegion = myEditor.getFoldingModel().getCollapsedRegionAtOffset(endOffset);
        return startFoldRegion == null || !startFoldRegion.equals(endFoldRegion);
    }

    @Nullable
    Rectangle getLineRendererRectangle(RangeHighlighter highlighter) {
        if (!isLineMarkerVisible(highlighter)) {
            return null;
        }

        int startOffset = highlighter.getStartOffset();
        int endOffset = highlighter.getEndOffset();

        int startY = myEditor.visualLineToY(myEditor.offsetToVisualLine(startOffset));

        // top edge of the last line of the highlighted area
        int endY = myEditor.visualLineToY(myEditor.offsetToVisualLine(endOffset));
        // => add one line height to make height correct (bottom edge of the highlighted area)
        endY += myEditor.getLineHeight();

        LineMarkerRenderer renderer = ObjectUtil.assertNotNull(highlighter.getLineMarkerRenderer());
        LineMarkerRendererEx.Position position = getLineMarkerPosition(renderer);

        int w;
        int x;
        switch (position) {
            case LEFT:
                w = getLeftFreePaintersAreaWidth();
                x = getLeftFreePaintersAreaOffset();
                break;
            case RIGHT:
                w = getRightFreePaintersAreaWidth();
                x = getLineMarkerFreePaintersAreaOffset();
                break;
            case CUSTOM:
                w = getWidth();
                x = 0;
                break;
            default:
                throw new IllegalArgumentException(position.name());
        }

        int height = endY - startY;
        return new Rectangle(x, startY, w, height);
    }

    @FunctionalInterface
    interface LineGutterIconRendererProcessor {
        void process(int x, int y, @Nonnull GutterMark renderer);
    }

    private float getEditorScaleFactor() {
        if (Registry.is("editor.scale.gutter.icons")) {
            float scale = myEditor.getScale();
            if (Math.abs(1f - scale) > 0.10f) {
                return scale;
            }
        }
        return 1f;
    }

    Icon scaleIcon(consulo.ui.image.Image icon) {
        return TargetAWT.to(scaleImage(icon));
    }

    consulo.ui.image.Image scaleImage(consulo.ui.image.Image icon) {
        float scale = getEditorScaleFactor();
        return scale == 1f ? icon : ImageEffects.resize(icon, scale);
    }

    private int scaleWidth(int width) {
        return (int) (getEditorScaleFactor() * width);
    }

    void processIconsRow(int line, @Nonnull List<? extends GutterMark> row, @Nonnull LineGutterIconRendererProcessor processor) {
        processIconsRowForY(myEditor.visualLineToY(line), row, processor);
    }

    // y should be equal to visualLineToY(visualLine)
    private void processIconsRowForY(int y, @Nonnull List<? extends GutterMark> row, @Nonnull LineGutterIconRendererProcessor processor) {
        if (row.isEmpty()) {
            return;
        }
        int middleCount = 0;
        int middleSize = 0;
        int x = getIconAreaOffset() + 2;

        for (GutterMark r : row) {
            if (!checkDumbAware(r)) {
                continue;
            }
            final Icon icon = scaleIcon(r.getIcon());
            GutterIconRenderer.Alignment alignment = ((GutterIconRenderer) r).getAlignment();
            if (alignment == GutterIconRenderer.Alignment.LINE_NUMBERS && !isLineNumbersShown()) {
                alignment = GutterIconRenderer.Alignment.LEFT;
            }
            switch (alignment) {
                case LEFT -> {
                    processor.process(x, y + getTextAlignmentShift(icon), r);
                    x += icon.getIconWidth() + getGapBetweenIcons();
                }
                case CENTER -> {
                    middleCount++;
                    middleSize += icon.getIconWidth() + getGapBetweenIcons();
                }
                case LINE_NUMBERS -> processor.process(
                    getLineNumberAreaOffset() + getLineNumberAreaWidth() - icon.getIconWidth(),
                    y + getTextAlignmentShift(icon),
                    r
                );
            }
        }

        final int leftSize = x - getIconAreaOffset();

        x = getIconAreaOffset() + myIconsAreaWidth;
        for (GutterMark r : row) {
            if (!checkDumbAware(r)) {
                continue;
            }
            if (((GutterIconRenderer) r).getAlignment() == GutterIconRenderer.Alignment.RIGHT) {
                Icon icon = scaleIcon(r.getIcon());
                x -= icon.getIconWidth();
                processor.process(x, y + getTextAlignmentShift(icon), r);
                x -= getGapBetweenIcons();
            }
        }

        int rightSize = myIconsAreaWidth + getIconAreaOffset() - x + 1;

        if (middleCount > 0) {
            middleSize -= getGapBetweenIcons();
            x = getIconAreaOffset() + leftSize + (myIconsAreaWidth - leftSize - rightSize - middleSize) / 2;
            for (GutterMark r : row) {
                if (!checkDumbAware(r)) {
                    continue;
                }
                if (((GutterIconRenderer) r).getAlignment() == GutterIconRenderer.Alignment.CENTER) {
                    Icon icon = scaleIcon(r.getIcon());
                    processor.process(x, y + getTextAlignmentShift(icon), r);
                    x += icon.getIconWidth() + getGapBetweenIcons();
                }
            }
        }
    }

    private int getTextAlignmentShift(Icon icon) {
        int centerRelative = (myEditor.getLineHeight() - icon.getIconHeight()) / 2;
        int baselineRelative = myEditor.getAscent() - icon.getIconHeight();
        return Math.max(centerRelative, baselineRelative);
    }

    @Override
    public void registerTextAnnotation(@Nonnull TextAnnotationGutterProvider provider) {
        myTextAnnotationGutters.add(provider);
        myTextAnnotationGutterSizes.add(0);
        updateSize();
    }

    @Override
    public void registerTextAnnotation(@Nonnull TextAnnotationGutterProvider provider, @Nonnull EditorGutterAction action) {
        myTextAnnotationGutters.add(provider);
        myProviderToListener.put(provider, action);
        myTextAnnotationGutterSizes.add(0);
        updateSize();
    }

    @Nonnull
    @Override
    public List<TextAnnotationGutterProvider> getTextAnnotations() {
        return new ArrayList<>(myTextAnnotationGutters);
    }

    private void doPaintFoldingTree(@Nonnull Graphics2D g, @Nonnull Rectangle clip, int firstVisibleOffset, int lastVisibleOffset) {
        final double width = getFoldingAnchorWidth2D();

        Collection<DisplayedFoldingAnchor> anchorsToDisplay =
            myAnchorsDisplayStrategy.getAnchorsToDisplay(firstVisibleOffset, lastVisibleOffset, myActiveFoldRegions);
        for (DisplayedFoldingAnchor anchor : anchorsToDisplay) {
            boolean active = myAlphaContext.isVisible();

            drawFoldingAnchor(width, clip, g, anchor.visualLine, anchor.type, active);
        }
    }

    @Override
    public int getWhitespaceSeparatorOffset() {
        return getWidth() - 3;
    }

    void setActiveFoldRegions(@Nonnull List<FoldRegion> activeFoldRegions) {
        if (!myActiveFoldRegions.equals(activeFoldRegions)) {
            myActiveFoldRegions = activeFoldRegions;
            repaint();
        }
    }

    private int getLineCenterY(int line) {
        return myEditor.visualLineToY(line) + myEditor.getLineHeight() / 2;
    }

    private double getFoldAnchorY(int line, double width) {
        return myEditor.visualLineToY(line) + myEditor.getAscent() - width;
    }

    int getHeadCenterY(FoldRegion foldRange) {
        return getLineCenterY(myEditor.offsetToVisualLine(foldRange.getStartOffset()));
    }

    private void drawFoldingAnchor(
        double width,
        @Nonnull Rectangle clip,
        @Nonnull Graphics2D g,
        int visualLine,
        @Nonnull DisplayedFoldingAnchor.Type type,
        boolean active
    ) {
        double off = width / 4;
        double height = width + off;
        double baseHeight = height - width / 2;
        double y = getFoldAnchorY(visualLine, width);
        double centerX = LinePainter2D.getStrokeCenter(g, getFoldingMarkerCenterOffset2D(), StrokeType.CENTERED, getStrokeWidth());
        double strokeOff = centerX - getFoldingMarkerCenterOffset2D();
        // need to have the same sub-device-pixel offset as centerX for the square_with_plus rect to have equal dev width/height
        double centerY = PaintUtil.alignToInt(y + width / 2, g) + strokeOff;
        switch (type) {
            case COLLAPSED, COLLAPSED_SINGLE_LINE -> {
                if (y <= clip.y + clip.height && y + height >= clip.y) {
                    drawSquareWithPlusOrMinus(g, centerX, centerY, width, true, active, visualLine);
                }
            }
            case EXPANDED_SINGLE_LINE -> {
                if (y <= clip.y + clip.height && y + height >= clip.y) {
                    drawSquareWithPlusOrMinus(g, centerX, centerY, width, false, active, visualLine);
                }
            }
            case EXPANDED_TOP -> {
                if (y <= clip.y + clip.height && y + height >= clip.y) {
                    drawDirectedBox(g, centerX, centerY, width, height, baseHeight, active, visualLine);
                }
            }
            case EXPANDED_BOTTOM -> {
                y += width;
                if (y - height <= clip.y + clip.height && y >= clip.y) {
                    drawDirectedBox(g, centerX, centerY, width, -height, -baseHeight, active, visualLine);
                }
            }
        }
    }

    private double getFoldingMarkerCenterOffset2D() {
        JBUI.ScaleContext ctx = JBUI.ScaleContext.create(myEditor.getComponent());
        return PaintUtil.alignToInt(getFoldingAreaOffset() + getFoldingAnchorWidth(), ctx, RoundingMode.ROUND, null);
    }

    private void drawDirectedBox(
        Graphics2D g,
        double centerX,
        double centerY,
        double width,
        double height,
        double baseHeight,
        boolean active,
        int visualLine
    ) {
        double sw = getStrokeWidth();

        if (height <= 0 && !EditorSettingsExternalizable.getInstance().isFoldingEndingsShown()) {
            //do not paint folding endings in new UI by default
            return;
        }

        myAlphaContext.paintWithComposite(g, () -> {
            Icon icon = scaleIcon(height > 0 ? PlatformIconGroup.gutterFold() : PlatformIconGroup.gutterFoldbottom());
            icon.paintIcon(this, g, getFoldingAreaOffset(), getFoldingIconY(visualLine, icon));
        });
    }

    private void drawSquareWithPlusOrMinus(
        @Nonnull Graphics2D g,
        double centerX,
        double centerY,
        double width,
        boolean plus,
        boolean active,
        int visualLine
    ) {
        Icon icon = scaleIcon(PlatformIconGroup.gutterUnfold());
        icon.paintIcon(this, g, getFoldingAreaOffset(), getFoldingIconY(visualLine, icon));
    }

    private int getFoldingIconY(int visualLine, Icon icon) {
        return (int) (myEditor.visualLineToY(visualLine) + (myEditor.getLineHeight() - icon.getIconHeight()) / 2f + 0.5f);
    }

    private double scale(double v) {
        return JBUIScale.scale((float) v) * myEditor.getScale();
    }

    private int getFoldingAnchorWidth() {
        return (int) Math.round(getFoldingAnchorWidth2D());
    }

    private double getFoldingAnchorWidth2D() {
        return scale(PlatformIconGroup.gutterFold().getWidth());
    }

    private double getStrokeWidth() {
        double sw = UIUtil.isJreHiDPIEnabled() || scale(1f) < 2 ? 1 : 2;
        JBUI.ScaleContext ctx = JBUI.ScaleContext.create(myEditor.getComponent());
        return PaintUtil.alignToInt(sw, ctx, PaintUtil.devValue(1, ctx) > 2 ? RoundingMode.FLOOR : RoundingMode.ROUND, null);
    }

    private int getFoldingAreaOffset() {
        return myLayout.getFoldingAreaOffset();
    }

    protected int getFoldingAreaWidth() {
        return isFoldingOutlineShown() ? getFoldingAnchorWidth() + JBUIScale.scale(2) : isRealEditor() ? getFoldingAnchorWidth() : 0;
    }

    private boolean isRealEditor() {
        return EditorUtil.isRealFileEditor(myEditor);
    }

    boolean isLineMarkersShown() {
        return myEditor.getSettings().isLineMarkerAreaShown();
    }

    boolean isShowGapAfterAnnotations() {
        return isAnnotationsShown() && (myGapAfterAnnotations || myTextAnnotationExtraSize > 0);
    }

    boolean areIconsShown() {
        return myEditor.getSettings().areGutterIconsShown();
    }

    boolean isLineNumbersShown() {
        return myEditor.getSettings().isLineNumbersShown();
    }

    @Override
    public boolean isAnnotationsShown() {
        return !myTextAnnotationGutters.isEmpty();
    }

    private boolean isFoldingOutlineShown() {
        return myEditor.getSettings().isFoldingOutlineShown() && myEditor.getFoldingModel()
            .isFoldingEnabled() && !myEditor.isInPresentationMode();
    }

    protected static int getGapBetweenAreas() {
        return GAP_BETWEEN_AREAS.get();
    }

    private static int getAreaWidthWithGap(int width) {
        if (width > 0) {
            return width + getGapBetweenAreas();
        }
        return 0;
    }

    private static int getGapBetweenIcons() {
        return GAP_BETWEEN_ICONS.get();
    }

    private static int getGapBetweenAnnotations() {
        return GAP_BETWEEN_ANNOTATIONS.get();
    }

    int getLineNumberAreaWidth() {
        if (isLineNumbersShown()) {
            return myLineNumberAreaWidth + getAreaWidthWithGap(myAdditionalLineNumberAreaWidth);
        }

        if (isRealEditor()) {
            //todo[kb] recalculate gutters renderers and return 0 if there are none in EditorMouseEventArea.LINE_NUMBERS_AREA
            return 14;
        }
        return 0;
    }

    private int getLineMarkerAreaWidth() {
        return isLineMarkersShown() ? getLeftFreePaintersAreaWidth() + myIconsAreaWidth + getGapAfterIconsArea() + getRightFreePaintersAreaWidth() : 0;
    }

    private void calcLineNumberAreaWidth() {
        if (!isLineNumbersShown()) {
            return;
        }

        String maxLineNumber = myLineNumberConverter.getMaxLineNumberString(myEditor);
        myLineNumberAreaWidth = Math.max(getInitialLineNumberWidth(), maxLineNumber == null ? 0 : calcLineNumbersAreaWidth(maxLineNumber));

        myAdditionalLineNumberAreaWidth = 0;
        if (myAdditionalLineNumberConverter != null) {
            String maxAdditionalLineNumber = myAdditionalLineNumberConverter.getMaxLineNumberString(myEditor);
            myAdditionalLineNumberAreaWidth = maxAdditionalLineNumber == null ? 0 : calcLineNumbersAreaWidth(maxAdditionalLineNumber);
        }
    }

    private static int getInitialLineNumberWidth() {
        if (ExperimentalUI.isNewUI()) {
            //have a placeholder for breakpoints
            return 12;
        }
        return 0;
    }

    @Nullable
    EditorMouseEventArea getEditorMouseAreaByOffset(int offset) {
        return myLayout.getEditorMouseAreaByOffset(offset);
    }

    int getLineNumberAreaOffset() {
        return myLayout.getLineNumberAreaOffset();
    }

    @Override
    public int getAnnotationsAreaOffset() {
        return myLayout.getAnnotationsAreaOffset();
    }

    @Override
    public int getAnnotationsAreaWidth() {
        return myTextAnnotationGuttersSize;
    }

    private int getAnnotationsAreaWidthEx() {
        return myTextAnnotationGuttersSize + myTextAnnotationExtraSize;
    }

    @Override
    public int getLineMarkerAreaOffset() {
        return myLayout.getLineMarkerAreaOffset();
    }

    @Override
    public int getIconAreaOffset() {
        return myLayout.getIconAreaOffset();
    }

    private int getLeftFreePaintersAreaOffset() {
        return getLineMarkerAreaOffset();
    }

    @Override
    public int getLineMarkerFreePaintersAreaOffset() {
        return myLayout.getLineMarkerFreePaintersAreaOffset();
    }

    protected int getLeftFreePaintersAreaWidth() {
        if (!myLeftFreePaintersAreaShown) {
            return 0;
        }

        if (ExperimentalUI.isNewUI()) {
            return (int) scale(FREE_PAINTERS_LEFT_AREA_WIDTH.get()) + 2;
        }

        return FREE_PAINTERS_LEFT_AREA_WIDTH.get();
    }

    protected int getRightFreePaintersAreaWidth() {
        if (!myRightFreePaintersAreaShown) {
            return 0;
        }

        int width = FREE_PAINTERS_RIGHT_AREA_WIDTH.get();
        if (ExperimentalUI.isNewUI() && width != 0) {
            return (int) Math.max(width, scale(JBUI.getInt("Gutter.VcsChanges.width", 4)) + JBUI.scale(5));
        }
        return FREE_PAINTERS_RIGHT_AREA_WIDTH.get();
    }

    @Override
    public int getIconsAreaWidth() {
        return myIconsAreaWidth;
    }

    protected int getGapAfterIconsArea() {
        return isRealEditor() && areIconsShown() ? ExperimentalUI.isNewUI() ? scaleWidth(2) : getGapBetweenAreas() : 0;
    }

    private boolean isMirrored() {
        return myEditor.getVerticalScrollbarOrientation() != EditorEx.VERTICAL_SCROLLBAR_RIGHT;
    }

    @Nullable
    private AffineTransform setMirrorTransformIfNeeded(Graphics2D g, int offset, int width) {
        if (isMirrored()) {
            AffineTransform old = g.getTransform();
            AffineTransform transform = new AffineTransform(old);

            transform.scale(-1, 1);
            transform.translate(-offset * 2 - width, 0);
            g.setTransform(transform);
            return old;
        }
        else {
            return null;
        }
    }

    @Nullable
    @Override
    public FoldRegion findFoldingAnchorAt(int x, int y) {
        if (!myEditor.getSettings().isFoldingOutlineShown()) {
            return null;
        }

        int anchorX = getFoldingAreaOffset();
        int anchorWidth = getFoldingAnchorWidth();

        int visualLine = myEditor.yToVisualLine(y);
        int neighbourhoodStartOffset =
            myEditor.logicalPositionToOffset(myEditor.visualToLogicalPosition(new VisualPosition(visualLine, 0)));
        int neighbourhoodEndOffset =
            myEditor.logicalPositionToOffset(myEditor.visualToLogicalPosition(new VisualPosition(visualLine, Integer.MAX_VALUE)));

        Collection<DisplayedFoldingAnchor> displayedAnchors =
            myAnchorsDisplayStrategy.getAnchorsToDisplay(neighbourhoodStartOffset, neighbourhoodEndOffset, Collections.emptyList());
        x = convertX(x);
        for (DisplayedFoldingAnchor anchor : displayedAnchors) {
            Rectangle r = rectangleByFoldOffset(anchor.visualLine, anchorWidth, anchorX);
            if (r.x < x && x <= r.x + r.width && r.y < y && y <= r.y + r.height) {
                return anchor.foldRegion;
            }
        }

        return null;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Rectangle rectangleByFoldOffset(int foldStart, int anchorWidth, int anchorX) {
        if (ExperimentalUI.isNewUI()) {
            //in new ui folding anchor click area has full line height, see IDEA-296393
            return new Rectangle(anchorX, myEditor.visualLineToY(foldStart), anchorWidth, myEditor.getLineHeight());
        }

        return new Rectangle(anchorX, (int) getFoldAnchorY(foldStart, anchorWidth), anchorWidth, anchorWidth);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        TooltipController.getInstance().cancelTooltips();
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        updateFreePainters(e);

        final GutterIconRenderer renderer = getGutterRenderer(e);
        if (renderer == null) {
            TextAnnotationGutterProvider provider = getProviderAtPoint(e.getPoint());
            LocalizeValue toolTip = LocalizeValue.empty();
            if (provider == null) {
                ActiveGutterRenderer lineRenderer = getActiveRendererByMouseEvent(e);
                if (lineRenderer != null) {
                    toolTip = lineRenderer.getTooltipValue();
                }
            }
            else {
                final int line = getLineNumAtPoint(e.getPoint());
                toolTip = provider.getToolTipValue(line, myEditor);
                if (!Comparing.equal(toolTip, myLastGutterToolTip)) {
                    TooltipController.getInstance().cancelTooltip(GUTTER_TOOLTIP_GROUP, e, true);
                    myLastGutterToolTip = toolTip;
                }
            }
            tooltipAvailable(toolTip, e, null);
        }
        else {
            computeTooltipInBackground(renderer, e);
        }
    }

    private void updateFreePainters(MouseEvent e) {
        if (!isLineMarkersShown() || !ExperimentalUI.isNewUI()) {
            return;
        }

        Point point = e.getPoint();
        int x = convertX(point.x);

        int hoveredLine;
        if (x >= getLineMarkerAreaOffset() &&
            x <= getLineMarkerAreaOffset() + getLeftFreePaintersAreaWidth() + getRightFreePaintersAreaWidth()) {
            hoveredLine = getEditor().xyToLogicalPosition(point).line;
        }
        else {
            hoveredLine = -1;
        }

        if (myHoveredFreeMarkersLine != hoveredLine) {
            myHoveredFreeMarkersLine = hoveredLine;
            repaint();
        }
    }

    private GutterIconRenderer myCalculatingInBackground;
    private ProgressIndicator myBackgroundIndicator = new EmptyProgressIndicator();

    private void computeTooltipInBackground(@Nonnull GutterIconRenderer renderer, @Nonnull MouseEvent e) {
        if (myCalculatingInBackground == renderer && !myBackgroundIndicator.isCanceled()) {
            return; // not yet calculated
        }
        myCalculatingInBackground = renderer;
        myBackgroundIndicator.cancel();
        myBackgroundIndicator = new ProgressIndicatorBase();
        myBackgroundIndicator.setModalityProgress(null);
        AtomicReference<LocalizeValue> tooltip = new AtomicReference<>(LocalizeValue.empty());
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(
            new Task.Backgroundable(myEditor.getProject(), "Constructing Tooltip") {
                @Override
                public void run(@Nonnull ProgressIndicator indicator) {
                    tooltip.set(ReadAction.compute(renderer::getTooltipValue));
                }

                @Override
                @RequiredUIAccess
                public void onSuccess() {
                    tooltipAvailable(tooltip.get(), e, renderer);
                }
            },
            myBackgroundIndicator
        );
    }

    void tooltipAvailable(@Nonnull LocalizeValue toolTip, @Nonnull MouseEvent e, @Nullable GutterMark renderer) {
        myCalculatingInBackground = null;
        TooltipController controller = TooltipController.getInstance();
        if (toolTip == LocalizeValue.empty() || myEditor.isDisposed()) {
            controller.cancelTooltip(GUTTER_TOOLTIP_GROUP, e, false);
        }
        else {
            final Ref<Point> t = new Ref<>(e.getPoint());
            int line = myEditor.yToVisualLine(e.getY());
            List<GutterMark> row = getGutterRenderers(line);
            Balloon.Position ballPosition = Balloon.Position.atRight;
            if (!row.isEmpty()) {
                Map<Integer, GutterMark> xPos = new TreeMap<>();
                final int[] currentPos = {0};
                processIconsRow(line, row, (x, y, r) -> {
                    xPos.put(x, r);
                    if (renderer == r) {
                        currentPos[0] = x;
                        Icon icon = scaleIcon(r.getIcon());
                        t.set(new Point(x + icon.getIconWidth() / 2, y + icon.getIconHeight() / 2));
                    }
                });

                List<Integer> xx = new ArrayList<>(xPos.keySet());
                int posIndex = xx.indexOf(currentPos[0]);
                if (xPos.size() > 1 && posIndex == 0) {
                    ballPosition = Balloon.Position.below;
                }
            }

            RelativePoint showPoint = new RelativePoint(this, t.get());

            TooltipRenderer tr = myEditor.getMarkupModel().getErrorStripTooltipRendererProvider().calcTooltipRenderer(toolTip.get());
            HintHint hint = new HintHint(this, t.get())
                .setAwtTooltip(true)
                .setPreferredPosition(ballPosition)
                .setRequestFocus(ScreenReader.isActive());
            if (myEditor.getComponent().getRootPane() != null) {
                controller.showTooltipByMouseMove(myEditor, showPoint, tr, false, GUTTER_TOOLTIP_GROUP, hint);
            }
        }
    }

    private Point getClickedIconCenter(@Nonnull MouseEvent e) {
        GutterMark renderer = getGutterRenderer(e);
        final Ref<Point> point = new Ref<>(e.getPoint());
        int line = myEditor.yToVisualLine(e.getY());
        List<GutterMark> row = getGutterRenderers(line);
        processIconsRow(
            line,
            row,
            (x, y, r) -> {
                if (renderer == r) {
                    Icon icon = scaleIcon(r.getIcon());
                    point.set(new Point(x + icon.getIconWidth() / 2, y + icon.getIconHeight() / 2));
                }
            }
        );
        return point.get();
    }

    void validateMousePointer(@Nonnull MouseEvent e) {
        if (IdeGlassPaneImpl.hasPreProcessedCursor(this)) {
            return;
        }

        FoldRegion foldingAtCursor = findFoldingAnchorAt(e.getX(), e.getY());
        setActiveFoldRegions(getGroupRegions(foldingAtCursor));
        Cursor cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        if (foldingAtCursor != null) {
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        }
        GutterIconRenderer renderer = getGutterRenderer(e);
        if (renderer != null) {
            if (renderer.isNavigateAction()) {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            }
        }
        else {
            ActiveGutterRenderer lineRenderer = getActiveRendererByMouseEvent(e);
            if (lineRenderer != null) {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            }
            else {
                TextAnnotationGutterProvider provider = getProviderAtPoint(e.getPoint());
                if (provider != null) {
                    EditorGutterAction action = myProviderToListener.get(provider);
                    if (action != null) {
                        int line = getLineNumAtPoint(e.getPoint());
                        cursor = action.getCursor(line);
                    }
                }
            }
        }
        UIUtil.setCursor(this, cursor);
    }

    @Nonnull
    private List<FoldRegion> getGroupRegions(@Nullable FoldRegion foldingAtCursor) {
        if (foldingAtCursor == null) {
            return Collections.emptyList();
        }
        else {
            FoldingGroup group = foldingAtCursor.getGroup();
            if (group == null) {
                return Collections.singletonList(foldingAtCursor);
            }
            return myEditor.getFoldingModel().getGroupedRegions(group);
        }
    }

    @Override
    @RequiredUIAccess
    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger()) {
            invokePopup(e);
        }
    }

    private void fireEventToTextAnnotationListeners(final MouseEvent e) {
        if (myEditor.getMouseEventArea(e) == EditorMouseEventArea.ANNOTATIONS_AREA) {
            final Point clickPoint = e.getPoint();

            final TextAnnotationGutterProvider provider = getProviderAtPoint(clickPoint);

            if (provider == null) {
                return;
            }

            EditorGutterAction action = myProviderToListener.get(provider);
            if (action != null) {
                int line = getLineNumAtPoint(clickPoint);

                if (line >= 0 && line < myEditor.getDocument().getLineCount() && UIUtil.isActionClick(e, MouseEvent.MOUSE_RELEASED)) {
                    action.doAction(line);
                }
            }
        }
    }

    private int getLineNumAtPoint(final Point clickPoint) {
        return EditorUtil.yPositionToLogicalLine(myEditor, clickPoint);
    }

    private boolean isGutterContextMenuShown() {
        return getClientProperty(EDITOR_GUTTER_CONTEXT_MENU_KEY) != null;
    }

    @Nullable
    private TextAnnotationGutterProvider getProviderAtPoint(final Point clickPoint) {
        int current = getAnnotationsAreaOffset();
        if (clickPoint.x < current) {
            return null;
        }
        for (int i = 0; i < myTextAnnotationGutterSizes.size(); i++) {
            current += myTextAnnotationGutterSizes.getInt(i);
            if (clickPoint.x <= current) {
                return myTextAnnotationGutters.get(i);
            }
        }

        return null;
    }

    @Override
    @RequiredUIAccess
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger() || isPopupAction(e)) {
            invokePopup(e);
        }
        else if (UIUtil.isCloseClick(e)) {
            processClose(e);
        }
    }

    private boolean isPopupAction(MouseEvent e) {
        GutterIconRenderer renderer = getGutterRenderer(e);
        return renderer != null && renderer.getClickAction() == null && renderer.getPopupMenuActions() != null;
    }

    @Override
    @RequiredUIAccess
    public void mouseReleased(final MouseEvent e) {
        if (e.isPopupTrigger()) {
            invokePopup(e);
            return;
        }

        GutterIconRenderer renderer = getGutterRenderer(e);
        AnAction clickAction = null;
        if (renderer != null && e.getButton() < 4) {
            clickAction = consulo.util.lang.BitUtil.isSet(e.getModifiers(), InputEvent.BUTTON2_MASK)
                ? renderer.getMiddleButtonClickAction()
                : renderer.getClickAction();
        }
        if (clickAction != null) {
      /*PluginInfo pluginInfo = PluginInfoDetectorKt.getPluginInfo(renderer.getClass());
      FeatureUsageData usageData = new FeatureUsageData();
      usageData.addPluginInfo(pluginInfo);
      Project project = myEditor.getProject();
      if (project != null) {
        usageData.addProject(project);
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(myEditor.getDocument());
        if (file != null) {
          usageData.addCurrentFile(file.getLanguage());
        }
      }
      usageData.addData("icon_id", renderer.getFeatureId());

      FUCounterUsageLogger.getInstance().logEvent("gutter.icon.click", "clicked", usageData);*/

            performAction(clickAction, e, ActionPlaces.EDITOR_GUTTER, myEditor.getDataContext());
            repaint();
            e.consume();
        }
        else {
            ActiveGutterRenderer lineRenderer = getActiveRendererByMouseEvent(e);
            if (lineRenderer != null) {
                lineRenderer.doAction(myEditor, e);
            }
            else {
                fireEventToTextAnnotationListeners(e);
            }
        }
    }

    private boolean isDumbMode() {
        Project project = myEditor.getProject();
        return project != null && DumbService.isDumb(project);
    }

    private boolean checkDumbAware(@Nonnull Object possiblyDumbAware) {
        return !isDumbMode() || DumbService.isDumbAware(possiblyDumbAware);
    }

    private void notifyNotDumbAware() {
        Project project = myEditor.getProject();
        if (project != null) {
            DumbService.getInstance(project).showDumbModeNotification("This functionality is not available during indexing");
        }
    }

    @RequiredUIAccess
    private void performAction(@Nonnull AnAction action, @Nonnull InputEvent e, @Nonnull String place, @Nonnull DataContext context) {
        if (!checkDumbAware(action)) {
            notifyNotDumbAware();
            return;
        }

        AnActionEvent actionEvent = AnActionEvent.createFromAnAction(action, e, place, context);
        action.update(actionEvent);
        if (actionEvent.getPresentation().isEnabledAndVisible()) {
            ActionUtil.performActionDumbAwareWithCallbacks(action, actionEvent, context);
        }
    }

    @Nullable
    private ActiveGutterRenderer getActiveRendererByMouseEvent(final MouseEvent e) {
        if (findFoldingAnchorAt(e.getX(), e.getY()) != null) {
            return null;
        }
        if (e.getX() > getWhitespaceSeparatorOffset()) {
            return null;
        }
        final ActiveGutterRenderer[] gutterRenderer = {null};
        final int[] layer = {-1};
        Rectangle clip = myEditor.getScrollingModel().getVisibleArea();
        int firstVisibleOffset = myEditor.logicalPositionToOffset(
            myEditor.xyToLogicalPosition(new Point(0, clip.y - myEditor.getLineHeight())));
        int lastVisibleOffset = myEditor.logicalPositionToOffset(
            myEditor.xyToLogicalPosition(new Point(0, clip.y + clip.height + myEditor.getLineHeight())));

        processRangeHighlighters(firstVisibleOffset, lastVisibleOffset, highlighter -> {
            LineMarkerRenderer renderer = highlighter.getLineMarkerRenderer();
            if (renderer == null) {
                return;
            }
            if (gutterRenderer[0] != null && layer[0] >= highlighter.getLayer()) {
                return;
            }
            Rectangle rectangle = getLineRendererRectangle(highlighter);
            if (rectangle == null) {
                return;
            }

            int startY = rectangle.y;
            int endY = startY + rectangle.height;
            if (startY == endY) {
                endY += myEditor.getLineHeight();
            }

            if (startY < e.getY() &&
                e.getY() <= endY &&
                renderer instanceof ActiveGutterRenderer &&
                ((ActiveGutterRenderer) renderer).canDoAction(myEditor, e)) {
                gutterRenderer[0] = (ActiveGutterRenderer) renderer;
                layer[0] = highlighter.getLayer();
            }
        });
        return gutterRenderer[0];
    }

    @Override
    public void closeAllAnnotations() {
        closeTextAnnotations(myTextAnnotationGutters);
    }

    @Override
    public void closeTextAnnotations(@Nonnull Collection<? extends TextAnnotationGutterProvider> annotations) {
        if (!myCanCloseAnnotations) {
            return;
        }

        Set<TextAnnotationGutterProvider> toClose = Sets.newHashSet(annotations, HashingStrategy.identity());
        for (int i = myTextAnnotationGutters.size() - 1; i >= 0; i--) {
            TextAnnotationGutterProvider provider = myTextAnnotationGutters.get(i);
            if (toClose.contains(provider)) {
                provider.gutterClosed();
                myTextAnnotationGutters.remove(i);
                myTextAnnotationGutterSizes.removeInt(i);
                myProviderToListener.remove(provider);
            }
        }

        updateSize();
    }

    private class CloseAnnotationsAction extends DumbAwareAction {
        CloseAnnotationsAction() {
            super(CodeEditorLocalize.closeEditorAnnotationsActionName());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            closeAllAnnotations();
        }
    }

    @Override
    @Nullable
    public Point getCenterPoint(final GutterIconRenderer renderer) {
        final Ref<Point> result = Ref.create();
        if (!areIconsShown()) {
            processGutterRenderers((line, renderers) -> {
                if (ContainerUtil.find(renderers, renderer) != null) {
                    result.set(new Point(getIconAreaOffset(), getLineCenterY(line)));
                    return false;
                }
                return true;
            });
        }
        else {
            processGutterRenderers((line, renderers) -> {
                processIconsRow(line, renderers, (x, y, r) -> {
                    if (result.isNull() && r.equals(renderer)) {
                        Icon icon = scaleIcon(r.getIcon());
                        result.set(new Point(x + icon.getIconWidth() / 2, y + icon.getIconHeight() / 2));
                    }
                });

                return result.isNull();
            });
        }
        return result.get();
    }

    @Override
    public void setLineNumberConverter(@Nonnull LineNumberConverter primaryConverter, @Nullable LineNumberConverter additionalConverter) {
        myAdditionalLineNumberConverter = primaryConverter;
        myAdditionalLineNumberConverter = additionalConverter;
        repaint();
    }

    @Override
    public void setShowDefaultGutterPopup(boolean show) {
        myShowDefaultGutterPopup = show;
    }

    @Override
    public void setCanCloseAnnotations(boolean canCloseAnnotations) {
        myCanCloseAnnotations = canCloseAnnotations;
    }

    @Override
    public void setGutterPopupGroup(@Nullable ActionGroup group) {
        myCustomGutterPopupGroup = group;
    }

    @Override
    public void setPaintBackground(boolean value) {
        myPaintBackground = value;
    }

    @Override
    public void setForceShowLeftFreePaintersArea(boolean value) {
        myForceLeftFreePaintersAreaShown = value;
    }

    @Override
    public void setForceShowRightFreePaintersArea(boolean value) {
        myForceRightFreePaintersAreaShown = value;
    }

    @Override
    public void setInitialIconAreaWidth(int width) {
        myStartIconAreaWidth = width;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public boolean canImpactSize(@Nonnull RangeHighlighterEx highlighter) {
        if (highlighter.getGutterIconRenderer() != null) {
            return true;
        }

        LineMarkerRenderer lineMarkerRenderer = highlighter.getLineMarkerRenderer();
        if (lineMarkerRenderer == null) {
            return false;
        }

// TODO unsupported
//        LineMarkerRenderer.Position position = getLineMarkerPosition(lineMarkerRenderer);
//        return position == LineMarkerRenderer.Position.LEFT && myLeftFreePaintersAreaState == EditorGutterFreePainterAreaState.ON_DEMAND ||
//            position == LineMarkerRenderer.Position.RIGHT && myRightFreePaintersAreaState == EditorGutterFreePainterAreaState.ON_DEMAND;
        return false;
    }

    @RequiredUIAccess
    private void invokePopup(MouseEvent e) {
        int logicalLineAtCursor = EditorUtil.yPositionToLogicalLine(myEditor, e);
        myLastActionableClick = new ClickInfo(logicalLineAtCursor, getClickedIconCenter(e));
        final ActionManager actionManager = ActionManager.getInstance();
        if (myEditor.getMouseEventArea(e) == EditorMouseEventArea.ANNOTATIONS_AREA) {
            final List<AnAction> addActions = new ArrayList<>();
            if (myCanCloseAnnotations) {
                addActions.add(new CloseAnnotationsAction());
            }
            //if (line >= myEditor.getDocument().getLineCount()) return;

            for (TextAnnotationGutterProvider gutterProvider : myTextAnnotationGutters) {
                final List<AnAction> list = gutterProvider.getPopupActions(logicalLineAtCursor, myEditor);
                if (list != null) {
                    for (AnAction action : list) {
                        if (!addActions.contains(action)) {
                            addActions.add(action);
                        }
                    }
                }
            }
            if (!addActions.isEmpty()) {
                DefaultActionGroup actionGroup = new DefaultActionGroup(CodeEditorLocalize.editorAnnotationsActionGroupName(), true);
                for (AnAction addAction : addActions) {
                    actionGroup.add(addAction);
                }
                JPopupMenu menu = actionManager.createActionPopupMenu("", actionGroup).getComponent();
                menu.show(this, e.getX(), e.getY());
                e.consume();
            }
        }
        else {
            GutterIconRenderer renderer = getGutterRenderer(e);
            if (renderer != null) {
                AnAction rightButtonAction = renderer.getRightButtonClickAction();
                if (rightButtonAction != null) {
                    performAction(rightButtonAction, e, ActionPlaces.EDITOR_GUTTER_POPUP, myEditor.getDataContext());
                    e.consume();
                }
                else {
                    ActionGroup actionGroup = renderer.getPopupMenuActions();
                    if (actionGroup != null) {
                        if (checkDumbAware(actionGroup)) {
                            actionManager.createActionPopupMenu(ActionPlaces.EDITOR_GUTTER_POPUP, actionGroup)
                                .getComponent()
                                .show(this, e.getX(), e.getY());
                        }
                        else {
                            notifyNotDumbAware();
                        }
                        e.consume();
                    }
                }
            }
            else {
                ActionGroup group = myCustomGutterPopupGroup;
                if (group == null && myShowDefaultGutterPopup) {
                    group = (ActionGroup) CustomActionsSchemaImpl.getInstance().getCorrectedAction(IdeActions.GROUP_EDITOR_GUTTER);
                }
                if (group != null) {
                    ActionPopupMenu popupMenu = actionManager.createActionPopupMenu(ActionPlaces.EDITOR_GUTTER_POPUP, group);
                    popupMenu.getComponent().show(this, e.getX(), e.getY());
                }
                e.consume();
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        TooltipController.getInstance().cancelTooltip(GUTTER_TOOLTIP_GROUP, e, false);
        updateFreePainters(e);
    }

    private int convertPointToLineNumber(final Point p) {
        DocumentEx document = myEditor.getDocument();
        int line = EditorUtil.yPositionToLogicalLine(myEditor, p);
        if (!isValidLine(document, line)) {
            return -1;
        }

        int startOffset = document.getLineStartOffset(line);
        final FoldRegion region = myEditor.getFoldingModel().getCollapsedRegionAtOffset(startOffset);
        if (region != null) {
            return document.getLineNumber(region.getEndOffset());
        }
        return line;
    }

    @Override
    public @Nullable GutterIconRenderer getGutterRenderer(Point p) {
        PointInfo info = getPointInfo(p);
        return info == null ? null : info.renderer;
    }

    @Nullable
    private GutterIconRenderer getGutterRenderer(MouseEvent e) {
        return getGutterRenderer(e.getPoint());
    }

    private @Nullable PointInfo getPointInfo(@Nonnull Inlay<?> inlay, int inlayY, int x, int y) {
        GutterIconRenderer renderer = inlay.getGutterIconRenderer();
        if (!shouldBeShown(renderer) || !checkDumbAware(renderer)) return null;
        Icon icon = scaleIcon(renderer.getIcon());
        int iconHeight = icon.getIconHeight();
        if ((y - inlayY) >= Math.max(iconHeight, myEditor.getLineHeight()) || iconHeight > inlay.getHeightInPixels()) return null;
        int iconWidth = icon.getIconWidth();
        int rightX = getIconAreaOffset() + getIconsAreaWidth();
        if (x < rightX - iconWidth || x > rightX) return null;
        PointInfo pointInfo = new PointInfo(renderer, new Point(rightX - iconWidth / 2,
            inlayY + getTextAlignmentShiftForInlayIcon(icon, inlay) + iconHeight / 2));
        pointInfo.renderersInLine = 1;
        return pointInfo;
    }

    private @Nullable PointInfo getPointInfo(@Nonnull Point p) {
        int cX = convertX((int) p.getX());
        int line = myEditor.yToVisualLine(p.y);
        int[] yRange = myEditor.visualLineToYRange(line);
        if (p.y >= yRange[0] && p.y < yRange[0] + myEditor.getLineHeight()) {
            List<GutterMark> renderers = getGutterRenderers(line);
            PointInfo[] result = {null};
            Int2IntRBTreeMap xPos = new Int2IntRBTreeMap();
            processIconsRowForY(yRange[0], renderers, (x, y, renderer) -> {
                Icon icon = scaleIcon(renderer.getIcon());
                int iconWidth = icon.getIconWidth();
                int centerX = x + iconWidth / 2;
                xPos.put(x, centerX);
                if (x <= cX && cX <= x + iconWidth) {
                    int iconHeight = icon.getIconHeight();
                    result[0] = new PointInfo((GutterIconRenderer) renderer, new Point(centerX, y + iconHeight / 2));
                }
            });
            if (result[0] != null) {
                result[0].renderersInLine = xPos.size();
                result[0].rendererPosition = new ArrayList<>(xPos.values()).indexOf(result[0].iconCenterPosition.x);
                result[0].visualLine = line;
            }
            return result[0];
        }
        if (myHasInlaysWithGutterIcons) {
            if (p.y < yRange[0]) {
                List<Inlay<?>> inlays = myEditor.getInlayModel().getBlockElementsForVisualLine(line, true);
                int yDiff = yRange[0] - p.y;
                for (int i = inlays.size() - 1; i >= 0; i--) {
                    Inlay<?> inlay = inlays.get(i);
                    int height = inlay.getHeightInPixels();
                    if (yDiff <= height) {
                        return getPointInfo(inlay, p.y + yDiff - height, cX, p.y);
                    }
                    yDiff -= height;
                }
            }
            else if (p.y >= yRange[1]) {
                List<Inlay<?>> inlays = myEditor.getInlayModel().getBlockElementsForVisualLine(line, false);
                int yDiff = p.y - yRange[1];
                for (Inlay<?> inlay : inlays) {
                    int height = inlay.getHeightInPixels();
                    if (yDiff < height) {
                        return getPointInfo(inlay, p.y - yDiff, cX, p.y);
                    }
                    yDiff -= height;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isInsideMarkerArea(@Nonnull MouseEvent e) {
        if (ExperimentalUI.isNewUI()) {
            int x = e.getX();
            int offset = getLineMarkerFreePaintersAreaOffset();
            int width = myLayout.getAreaWidth(EditorGutterLayout.RIGHT_FREE_PAINTERS_AREA);
            return offset < x && x <= offset + width;
        }
        return e.getX() > getLineMarkerFreePaintersAreaOffset();
    }

    private boolean isMergedWithLineNumbers(GutterMark renderer) {
        return isLineNumbersShown() &&
            renderer instanceof GutterIconRenderer &&
            ((GutterIconRenderer) renderer).getAlignment() == GutterIconRenderer.Alignment.LINE_NUMBERS;
    }

    @Nonnull
    static LineMarkerRenderer.Position getLineMarkerPosition(@Nonnull LineMarkerRenderer renderer) {
        return renderer.getPosition();
    }

    int convertX(int x) {
        if (!isMirrored()) {
            return x;
        }
        return getWidth() - x;
    }

    public void dispose() {
        for (TextAnnotationGutterProvider gutterProvider : myTextAnnotationGutters) {
            gutterProvider.gutterClosed();
        }
        myProviderToListener.clear();
    }

    @Override
    public boolean isFocusable() {
        return ScreenReader.isActive();
    }

    @Override
    public int getHoveredFreeMarkersLine() {
        return myHoveredFreeMarkersLine;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
            };
        }
        return accessibleContext;
    }

    void setCurrentAccessibleLine(@Nullable AccessibleGutterLine line) {
        myAccessibleGutterLine = line;
    }

    @Nullable
    AccessibleGutterLine getCurrentAccessibleLine() {
        return myAccessibleGutterLine;
    }

    void escapeCurrentAccessibleLine() {
        if (myAccessibleGutterLine != null) {
            myAccessibleGutterLine.escape(true);
        }
    }

    private static class ClickInfo {
        final int myLogicalLineAtCursor;
        final Point myIconCenterPosition;
        int myProgressVisualLine;
        GutterMark myProgressGutterMark;
        Runnable myProgressRemover;

        ClickInfo(int logicalLineAtCursor, Point iconCenterPosition) {
            myLogicalLineAtCursor = logicalLineAtCursor;
            myIconCenterPosition = iconCenterPosition;
        }
    }

    private static final class PointInfo {
        final @Nonnull GutterIconRenderer renderer;
        final @Nonnull Point iconCenterPosition;
        int renderersInLine;
        int rendererPosition;
        int visualLine;

        PointInfo(@Nonnull GutterIconRenderer renderer, @Nonnull Point iconCenterPosition) {
            this.renderer = renderer;
            this.iconCenterPosition = iconCenterPosition;
        }
    }
}
