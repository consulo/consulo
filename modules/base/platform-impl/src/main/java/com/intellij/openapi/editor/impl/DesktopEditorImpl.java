// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.codeInsight.hint.EditorFragmentComponent;
import com.intellij.diagnostic.Dumpable;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.dnd.DnDManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.*;
import com.intellij.openapi.editor.colors.EditorColorKey;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.colors.impl.DelegateColorScheme;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.ex.SoftWrapChangeListener;
import com.intellij.openapi.editor.ex.util.EditorUIUtil;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterClient;
import com.intellij.openapi.editor.impl.view.EditorView;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.AbstractPainter;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeGlassPane;
import com.intellij.openapi.wm.IdeGlassPaneUtil;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettingsChangeEvent;
import com.intellij.psi.codeStyle.CodeStyleSettingsListener;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.ui.*;
import com.intellij.ui.components.JBScrollBar;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.mac.MacGestureSupportForEditor;
import com.intellij.ui.mac.touchbar.TouchBarsManager;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.ui.paint.PaintUtil.RoundingMode;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.*;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.*;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import consulo.annotation.DeprecationInfo;
import consulo.application.TransactionGuardEx;
import consulo.awt.TargetAWT;
import consulo.desktop.editor.impl.DesktopEditorLayeredPanel;
import consulo.desktop.editor.impl.StatusComponentContainer;
import consulo.desktop.util.awt.migration.AWTComponentProviderUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.editor.impl.*;
import consulo.editor.internal.EditorInternal;
import consulo.fileEditor.impl.EditorsSplitters;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.util.dataholder.Key;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;
import org.intellij.lang.annotations.JdkConstants;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ScrollPaneUI;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.font.TextHitInfo;
import java.awt.geom.Point2D;
import java.awt.im.InputMethodRequests;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Deprecated
@DeprecationInfo("Desktop implementation")
@SuppressWarnings("deprecation")
public final class DesktopEditorImpl extends CodeEditorBase implements EditorInternal, HighlighterClient, Queryable, Dumpable, CodeStyleSettingsListener {
  public static final int TEXT_ALIGNMENT_LEFT = 0;
  public static final int TEXT_ALIGNMENT_RIGHT = 1;

  private static final Logger LOG = Logger.getInstance(DesktopEditorImpl.class);
  private static final Key DND_COMMAND_KEY = Key.create("DndCommand");

  private static final Key<JComponent> PERMANENT_HEADER = Key.create("PERMANENT_HEADER");

  private static final boolean HONOR_CAMEL_HUMPS_ON_TRIPLE_CLICK = Boolean.parseBoolean(System.getProperty("idea.honor.camel.humps.on.triple.click"));
  private static final Key<BufferedImage> BUFFER = Key.create("buffer");

  private final JPanel myPanel;
  @Nonnull
  private final JScrollPane myScrollPane;
  @Nonnull
  private final EditorComponentImpl myEditorComponent;
  @Nonnull
  private final EditorGutterComponentImpl myGutterComponent;
  private final FocusModeModel myFocusModeModel;
  private volatile long myLastTypedActionTimestamp = -1;
  private String myLastTypedAction;
  private final LatencyListener myLatencyPublisher;

  private static final Cursor EMPTY_CURSOR;
  private final Map<Object, Cursor> myCustomCursors = new LinkedHashMap<>();
  private Cursor myDefaultCursor;
  boolean myCursorSetExternally;

  static {
    Cursor emptyCursor = null;
    if (!GraphicsEnvironment.isHeadless()) {
      try {
        emptyCursor = Toolkit.getDefaultToolkit().createCustomCursor(ImageUtil.createImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(), "Empty cursor");
      }
      catch (Exception e) {
        LOG.warn("Couldn't create an empty cursor", e);
      }
    }
    EMPTY_CURSOR = emptyCursor;
  }

  @Nonnull
  private final MyVerticalScrollBar myVerticalScrollBar;

  @Nonnull
  private final CaretCursor myCaretCursor;
  private final ScrollingTimer myScrollingTimer = new ScrollingTimer();

  @SuppressWarnings("RedundantStringConstructorCall")
  private final Object MOUSE_DRAGGED_GROUP = new String("MouseDraggedGroup");

  @Nullable
  private MouseEvent myMousePressedEvent;
  @Nullable
  private MouseEvent myMouseMovedEvent;

  private final MouseListener myMouseListener = new MyMouseAdapter();
  private final MouseMotionListener myMouseMotionListener = new MyMouseMotionListener();

  /**
   * Holds information about area where mouse was pressed.
   */
  @Nullable
  private EditorMouseEventArea myMousePressArea;
  private int mySavedSelectionStart = -1;
  private int mySavedSelectionEnd = -1;

  @Nonnull
  private static final RepaintCursorCommand ourCaretBlinkingCommand = new RepaintCursorCommand();

  @MouseSelectionState
  private int myMouseSelectionState;
  @Nullable
  private FoldRegion myMouseSelectedRegion;

  private int myHorizontalTextAlignment = TEXT_ALIGNMENT_LEFT;

  @MagicConstant(intValues = {MOUSE_SELECTION_STATE_NONE, MOUSE_SELECTION_STATE_LINE_SELECTED, MOUSE_SELECTION_STATE_WORD_SELECTED})
  private @interface MouseSelectionState {
  }

  private static final int MOUSE_SELECTION_STATE_NONE = 0;
  private static final int MOUSE_SELECTION_STATE_WORD_SELECTED = 1;
  private static final int MOUSE_SELECTION_STATE_LINE_SELECTED = 2;

  private final TextDrawingCallback myTextDrawingCallback = new MyTextDrawingCallback();

  @MagicConstant(intValues = {VERTICAL_SCROLLBAR_LEFT, VERTICAL_SCROLLBAR_RIGHT})
  private int myScrollBarOrientation;
  private boolean myKeepSelectionOnMousePress;

  private boolean myUpdateCursor;

  private long myMouseSelectionChangeTimestamp;
  private int mySavedCaretOffsetForDNDUndoHack;

  private MyInputMethodHandler myInputMethodRequestsHandler;
  private InputMethodRequests myInputMethodRequestsSwingWrapper;
  @Nullable
  private ColorValue myForcedBackground;
  @Nullable
  private Dimension myPreferredSize;

  private final Alarm myMouseSelectionStateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private Runnable myMouseSelectionStateResetRunnable;

  private boolean myEmbeddedIntoDialogWrapper;
  private int myDragOnGutterSelectionStartLine = -1;
  private RangeMarker myDraggedRange;
  private boolean myDragStarted;

  @Nonnull
  private final JPanel myHeaderPanel;

  @Nullable
  private MouseEvent myInitialMouseEvent;
  private boolean myIgnoreMouseEventsConsecutiveToInitial;

  private EditorDropHandler myDropHandler;

  private boolean myPaintSelection;

  private final EditorSizeAdjustmentStrategy mySizeAdjustmentStrategy = new EditorSizeAdjustmentStrategy();

  private List<CaretState> myCaretStateBeforeLastPress;
  LogicalPosition myLastMousePressedLocation;
  private VisualPosition myTargetMultiSelectionPosition;
  private boolean myMultiSelectionInProgress;
  private boolean myRectangularSelectionInProgress;
  private boolean myLastPressCreatedCaret;
  // Set when the selection (normal or block one) initiated by mouse drag becomes noticeable (at least one character is selected).
  // Reset on mouse press event.
  private boolean myCurrentDragIsSubstantial;

  private DesktopCaretImpl myPrimaryCaret;

  public final boolean myDisableRtl = Registry.is("editor.disable.rtl");
  public final Object myFractionalMetricsHintValue = Registry.is("editor.text.fractional.metrics") ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF;
  public final EditorView myView;

  private boolean myCharKeyPressed;
  private boolean myNeedToSelectPreviousChar;

  private boolean myErrorStripeNeedsRepaint;

  private boolean myUseEditorAntialiasing = true;

  private final ImmediatePainter myImmediatePainter;

  static {
    ourCaretBlinkingCommand.start();
  }

  private boolean myBackgroundImageSet;

  private boolean myScrollingToCaret;

  private StatusComponentContainer myStatusComponentContainer = new StatusComponentContainer();

  public DesktopEditorImpl(@Nonnull Document document, boolean viewer, @Nullable Project project, @Nonnull EditorKind kind) {
    super(document, viewer, project, kind);
    
    myScrollPane = new MyScrollPane(); // create UI after scheme initialization

    getScrollingModel().registerListeners();

    myImmediatePainter = new ImmediatePainter(this);

    myCaretModel.addCaretListener(new CaretListener() {
      @Nullable
      private LightweightHint myCurrentHint;
      @Nullable
      private IndentGuideDescriptor myCurrentCaretGuide;

      @Override
      public void caretPositionChanged(@Nonnull CaretEvent e) {
        if (myStickySelection) {
          int selectionStart = Math.min(myStickySelectionStart, getDocument().getTextLength());
          mySelectionModel.setSelection(selectionStart, myCaretModel.getVisualPosition(), myCaretModel.getOffset());
        }

        final IndentGuideDescriptor newGuide = myIndentsModel.getCaretIndentGuide();
        if (!Comparing.equal(myCurrentCaretGuide, newGuide)) {
          repaintGuide(newGuide);
          repaintGuide(myCurrentCaretGuide);
          myCurrentCaretGuide = newGuide;

          if (myCurrentHint != null) {
            myCurrentHint.hide();
            myCurrentHint = null;
          }

          if (newGuide != null) {
            final Rectangle visibleArea = getScrollingModel().getVisibleArea();
            final int endLine = newGuide.startLine;
            if (logicalLineToY(endLine) < visibleArea.y) {
              int startLine = Math.max(newGuide.codeConstructStartLine, endLine - EditorFragmentComponent.getAvailableVisualLinesAboveEditor(DesktopEditorImpl.this) + 1);
              TextRange textRange = new TextRange(myDocument.getLineStartOffset(startLine), myDocument.getLineEndOffset(endLine));
              myCurrentHint = EditorFragmentComponent.showEditorFragmentHint(DesktopEditorImpl.this, textRange, false, false);
            }
          }
        }
      }

      @Override
      public void caretAdded(@Nonnull CaretEvent e) {
        if (myPrimaryCaret != null) {
          myPrimaryCaret.updateVisualPosition(); // repainting old primary caret's row background
        }
        repaintCaretRegion(e);
        myPrimaryCaret = (DesktopCaretImpl)myCaretModel.getPrimaryCaret();
      }

      @Override
      public void caretRemoved(@Nonnull CaretEvent e) {
        repaintCaretRegion(e);
        myPrimaryCaret = (DesktopCaretImpl)myCaretModel.getPrimaryCaret(); // repainting new primary caret's row background
        myPrimaryCaret.updateVisualPosition();
      }
    });

    myCaretCursor = new CaretCursor();

    myFoldingModel.disableScrollingPositionAdjustment();
    myScrollBarOrientation = VERTICAL_SCROLLBAR_RIGHT;

    mySoftWrapModel.addSoftWrapChangeListener(new SoftWrapChangeListener() {
      @Override
      public void recalculationEnds() {
        if (myCaretModel.isUpToDate()) {
          myCaretModel.updateVisualPosition();
        }
      }

      @Override
      public void softWrapsChanged() {
        myGutterComponent.clearLineToGutterRenderersCache();
      }
    });

    EditorHighlighter highlighter = new NullEditorHighlighter();
    setHighlighter(highlighter);

    new FoldingPopupManager(this);

    myEditorComponent = new EditorComponentImpl(this);
    myVerticalScrollBar = (MyVerticalScrollBar)myScrollPane.getVerticalScrollBar();
    myPanel = new JPanel(new BorderLayout());

    getMarkupModel().updateUI();
    
    UIUtil.putClientProperty(myPanel, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, (Iterable<JComponent>)() -> {
      JComponent component = getPermanentHeaderComponent();
      if (component != null && component.getParent() == null) {
        return Collections.singleton(component).iterator();
      }
      return Collections.emptyIterator();
    });

    myHeaderPanel = new MyHeaderPanel();
    myGutterComponent = new EditorGutterComponentImpl(this);
    initComponent();

    myView = new EditorView(this);
    myView.reinitSettings();

    myInlayModel.addListener(new InlayModel.SimpleAdapter() {
      @Override
      public void onUpdated(@Nonnull Inlay inlay) {
        onInlayUpdated(inlay);
      }
    }, myCaretModel);

    if (UISettings.getInstance().getPresentationMode()) {
      setFontSize(UISettings.getInstance().getPresentationModeFontSize());
    }

    myGutterComponent.updateSize();
    Dimension preferredSize = getPreferredSize();
    myEditorComponent.setSize(preferredSize);

    updateCaretCursor();

    if (SystemInfo.isMacIntel64 && SystemInfo.isJetBrainsJvm && Registry.is("ide.mac.forceTouch")) {
      new MacGestureSupportForEditor(getComponent());
    }

    myScrollingModel.addVisibleAreaListener(this::moveCaretIntoViewIfCoveredByToolWindowBelow);

    PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        if (Document.PROP_WRITABLE.equals(e.getPropertyName())) {
          myEditorComponent.repaint();
        }
      }
    };
    myDocument.addPropertyChangeListener(propertyChangeListener);
    Disposer.register(myDisposable, () -> myDocument.removePropertyChangeListener(propertyChangeListener));

    CodeStyleSettingsManager.getInstance(myProject).addListener(this);

    myFocusModeModel = new FocusModeModel(this);
    Disposer.register(myDisposable, myFocusModeModel);

    myLatencyPublisher = ApplicationManager.getApplication().getMessageBus().syncPublisher(LatencyListener.TOPIC);
  }

  @Override
  protected CodeEditorSelectionModelBase createSelectionModel() {
    return new DesktopSelectionModelImpl(this);
  }

  @Override
  protected MarkupModelImpl createMarkupModel() {
    return new DesktopEditorMarkupModelImpl(this);
  }

  @Override
  protected CodeEditorFoldingModelBase createFoldingModel() {
    return new DesktopFoldingModelImpl(this);
  }

  @Override
  protected CodeEditorCaretModelBase createCaretModel() {
    return new DesktopCaretModelImpl(this);
  }

  @Override
  protected CodeEditorScrollingModelBase createScrollingModel() {
    return new DesktopScrollingModelImpl(this);
  }

  @Override
  protected CodeEditorInlayModelBase createInlayModel() {
    return new InlayModelImpl(this);
  }

  @Override
  protected CodeEditorSoftWrapModelBase createSoftWrapModel() {
    return new SoftWrapModelImpl(this);
  }

  @Nonnull
  @Override
  protected DataContext getComponentContext() {
    return DataManager.getInstance().getDataContext(getContentComponent());
  }

  public void applyFocusMode() {
    myFocusModeModel.applyFocusMode(myCaretModel.getPrimaryCaret());
  }

  public boolean isInFocusMode(FoldRegion region) {
    return myFocusModeModel.isInFocusMode(region);
  }

  public Segment getFocusModeRange() {
    return myFocusModeModel.getFocusModeRange();
  }

  public FocusModeModel getFocusModeModel() {
    return myFocusModeModel;
  }

  @Override
  protected boolean canImpactGutterSize(@Nonnull RangeHighlighterEx highlighter) {
    if (highlighter.getGutterIconRenderer() != null) return true;
    LineMarkerRenderer lineMarkerRenderer = highlighter.getLineMarkerRenderer();
    if (lineMarkerRenderer == null) return false;
    LineMarkerRendererEx.Position position = EditorGutterComponentImpl.getLineMarkerPosition(lineMarkerRenderer);
    return position == LineMarkerRendererEx.Position.LEFT && !myGutterComponent.myForceLeftFreePaintersAreaShown ||
           position == LineMarkerRendererEx.Position.RIGHT && !myGutterComponent.myForceRightFreePaintersAreaShown;
  }

  @Override
  protected void onHighlighterChanged(@Nonnull RangeHighlighterEx highlighter, boolean canImpactGutterSize, boolean fontStyleOrColorChanged, boolean remove) {
    if (myDocument.isInBulkUpdate()) return; // bulkUpdateFinished() will repaint anything

    if (canImpactGutterSize) {
      updateGutterSize();
    }

    boolean errorStripeNeedsRepaint = highlighter.getErrorStripeMarkColor() != null;
    if (myDocumentChangeInProgress) {
      // postpone repaint request, as folding model can be in inconsistent state and so coordinate
      // conversions might give incorrect results
      myErrorStripeNeedsRepaint |= errorStripeNeedsRepaint;
      return;
    }

    int textLength = myDocument.getTextLength();

    int start = Math.min(Math.max(highlighter.getAffectedAreaStartOffset(), 0), textLength);
    int end = Math.min(Math.max(highlighter.getAffectedAreaEndOffset(), 0), textLength);

    if (getGutterComponentEx().getCurrentAccessibleLine() != null && AccessibleGutterLine.isAccessibleGutterElement(highlighter.getGutterIconRenderer())) {
      escapeGutterAccessibleLine(start, end);
    }
    int startLine = start == -1 ? 0 : myDocument.getLineNumber(start);
    int endLine = end == -1 ? myDocument.getLineCount() : myDocument.getLineNumber(end);
    if (start != end && fontStyleOrColorChanged) {
      myView.invalidateRange(start, end);
    }
    if (!myFoldingModel.isInBatchFoldingOperation()) { // at the end of batch folding operation everything is repainted
      repaintLines(Math.max(0, startLine - 1), Math.min(endLine + 1, getDocument().getLineCount()));
    }

    // optimization: there is no need to repaint error stripe if the highlighter is invisible on it
    if (errorStripeNeedsRepaint) {
      if (myFoldingModel.isInBatchFoldingOperation()) {
        myErrorStripeNeedsRepaint = true;
      }
      else {
        getMarkupModel().repaint(start, end);
      }
    }

    updateCaretCursor();
  }

  private void onInlayUpdated(@Nonnull Inlay inlay) {
    if (myDocument.isInEventsHandling() || myDocument.isInBulkUpdate()) return;
    validateSize();
    int offset = inlay.getOffset();
    Inlay.Placement placement = inlay.getPlacement();
    if (placement == Inlay.Placement.INLINE) {
      repaint(offset, offset, false);
    }
    else if (placement == Inlay.Placement.AFTER_LINE_END) {
      int lineEndOffset = DocumentUtil.getLineEndOffset(offset, myDocument);
      repaint(lineEndOffset, lineEndOffset, false);
    }
    else {
      int visualLine = offsetToVisualLine(offset);
      int y = visualLineToY(visualLine) - EditorUtil.getTotalInlaysHeight(myInlayModel.getBlockElementsForVisualLine(visualLine, true));
      repaintToScreenBottomStartingFrom(y);
    }
  }

  private void moveCaretIntoViewIfCoveredByToolWindowBelow(VisibleAreaEvent e) {
    Rectangle oldRectangle = e.getOldRectangle();
    Rectangle newRectangle = e.getNewRectangle();
    if (!myScrollingToCaret && oldRectangle != null && oldRectangle.height != newRectangle.height && oldRectangle.y == newRectangle.y && newRectangle.height > 0) {
      int caretY = myView.visualLineToY(myCaretModel.getVisualPosition().line);
      if (caretY < oldRectangle.getMaxY() && caretY > newRectangle.getMaxY()) {
        myScrollingToCaret = true;
        ApplicationManager.getApplication().invokeLater(() -> {
          myScrollingToCaret = false;
          if (!isReleased) EditorUtil.runWithAnimationDisabled(this, () -> myScrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE));
        }, ModalityState.any());
      }
    }
  }

  private void repaintCaretRegion(CaretEvent e) {
    DesktopCaretImpl caretImpl = (DesktopCaretImpl)e.getCaret();
    if (caretImpl != null) {
      caretImpl.updateVisualPosition();
      if (caretImpl.hasSelection()) {
        repaint(caretImpl.getSelectionStart(), caretImpl.getSelectionEnd(), false);
      }
    }
  }

  private void repaintGuide(@Nullable IndentGuideDescriptor guide) {
    if (guide != null) {
      repaintLines(guide.startLine, guide.endLine);
    }
  }

  @Override
  public int getPrefixTextWidthInPixels() {
    return (int)myView.getPrefixTextWidthInPixels();
  }

  @Override
  public void setPrefixTextAndAttributes(@Nullable String prefixText, @Nullable TextAttributes attributes) {
    super.setPrefixTextAndAttributes(prefixText, attributes);
    myView.setPrefix(prefixText, attributes);
  }

  @Override
  public void registerScrollBarRepaintCallback(@Nullable Consumer<Graphics> callback) {
    myVerticalScrollBar.registerRepaintCallback(callback);
  }

  @Nullable
  private Cursor getCustomCursor() {
    return ContainerUtil.getFirstItem(myCustomCursors.values());
  }

  @Override
  public void setCustomCursor(@Nonnull Object requestor, @Nullable Cursor cursor) {
    if (cursor == null) {
      myCustomCursors.remove(requestor);
    }
    else {
      myCustomCursors.put(requestor, cursor);
    }
    updateEditorCursor();
  }

  @Override
  @Nonnull
  public DesktopSelectionModelImpl getSelectionModel() {
    return (DesktopSelectionModelImpl)super.getSelectionModel();
  }

  @Override
  @Nonnull
  public DesktopEditorMarkupModelImpl getMarkupModel() {
    return (DesktopEditorMarkupModelImpl)super.getMarkupModel();
  }

  @Override
  @Nonnull
  public DesktopFoldingModelImpl getFoldingModel() {
    return (DesktopFoldingModelImpl)super.getFoldingModel();
  }

  @Override
  @Nonnull
  public DesktopCaretModelImpl getCaretModel() {
    return (DesktopCaretModelImpl)super.getCaretModel();
  }

  @Override
  @Nonnull
  public DesktopScrollingModelImpl getScrollingModel() {
    return (DesktopScrollingModelImpl)super.getScrollingModel();
  }

  @Override
  @Nonnull
  public SoftWrapModelImpl getSoftWrapModel() {
    return (SoftWrapModelImpl)super.getSoftWrapModel();
  }

  @Nonnull
  @Override
  public InlayModelImpl getInlayModel() {
    return (InlayModelImpl)super.getInlayModel();
  }

  public void resetSizes() {
    myView.reset();
  }

  @Override
  public void reinitSettings() {
    reinitSettings(true);
  }

  @Override
  public void reinitViewSettings() {
    myView.reinitSettings();
  }

  private void reinitSettings(boolean updateGutterSize) {
    assertIsDispatchThread();

    for (EditorColorsScheme scheme = myScheme; scheme instanceof DelegateColorScheme; scheme = ((DelegateColorScheme)scheme).getDelegate()) {
      if (scheme instanceof MyColorSchemeDelegate) {
        ((MyColorSchemeDelegate)scheme).updateGlobalScheme();
        break;
      }
    }

    boolean softWrapsUsedBefore = mySoftWrapModel.isSoftWrappingEnabled();

    mySettings.reinitSettings();
    mySoftWrapModel.reinitSettings();
    myCaretModel.reinitSettings();
    mySelectionModel.reinitSettings();
    ourCaretBlinkingCommand.setBlinkCaret(mySettings.isBlinkCaret());
    ourCaretBlinkingCommand.setBlinkPeriod(mySettings.getCaretBlinkPeriod());
    myView.reinitSettings();
    myFoldingModel.refreshSettings();
    myFoldingModel.rebuild();
    myInlayModel.reinitSettings();

    if (softWrapsUsedBefore ^ mySoftWrapModel.isSoftWrappingEnabled()) {
      validateSize();
    }

    myHighlighter.setColorScheme(myScheme);

    myGutterComponent.reinitSettings(updateGutterSize);
    myGutterComponent.revalidate();

    myEditorComponent.repaint();

    updateCaretCursor();

    if (myInitialMouseEvent != null) {
      myIgnoreMouseEventsConsecutiveToInitial = true;
    }

    myCaretModel.updateVisualPosition();

    // make sure carets won't appear at invalid positions (e.g. on Tab width change)
    getCaretModel().doWithCaretMerging(() -> myCaretModel.getAllCarets().forEach(caret -> caret.moveToOffset(caret.getOffset())));

    if (myVirtualFile != null && myProject != null) {
      final EditorNotifications editorNotifications = EditorNotifications.getInstance(myProject);
      if (editorNotifications != null) {
        editorNotifications.updateNotifications(myVirtualFile);
      }
    }

    if (myFocusModeModel != null) {
      myFocusModeModel.clearFocusMode();
    }
  }

  // EditorFactory.releaseEditor should be used to release editor
  @Override
  public void release() {
    assertIsDispatchThread();
    if (isReleased) {
      throwDisposalError("Double release of editor:");
    }
    myTraceableDisposable.kill(null);

    isReleased = true;
    mySizeAdjustmentStrategy.cancelAllRequests();
    cancelAutoResetForMouseSelectionState();

    myFoldingModel.dispose();
    mySoftWrapModel.release();
    myMarkupModel.dispose();

    myScrollingModel.dispose();
    myGutterComponent.dispose();
    myMousePressedEvent = null;
    myMouseMovedEvent = null;
    Disposer.dispose(myCaretModel);
    Disposer.dispose(mySoftWrapModel);
    Disposer.dispose(myView);
    clearCaretThread();

    myFocusListeners.clear();
    myMouseListeners.clear();
    myMouseMotionListeners.clear();

    myEditorComponent.removeMouseListener(myMouseListener);
    myGutterComponent.removeMouseListener(myMouseListener);
    myEditorComponent.removeMouseMotionListener(myMouseMotionListener);
    myGutterComponent.removeMouseMotionListener(myMouseMotionListener);

    CodeStyleSettingsManager.removeListener(myProject, this);

    Disposer.dispose(myDisposable);
    myVerticalScrollBar.setUI(null); // clear error panel's cached image
  }

  private void clearCaretThread() {
    synchronized (ourCaretBlinkingCommand) {
      if (ourCaretBlinkingCommand.myEditor == this) {
        ourCaretBlinkingCommand.myEditor = null;
      }
    }
  }

  private void initComponent() {
    myPanel.add(myHeaderPanel, BorderLayout.NORTH);

    myGutterComponent.setOpaque(true);

    myScrollPane.setViewportView(myEditorComponent);
    //myScrollPane.setBorder(null);
    myScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    myScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    myScrollPane.setRowHeaderView(myGutterComponent);

    myEditorComponent.setTransferHandler(new MyTransferHandler());
    myEditorComponent.setAutoscrolls(false); // we have our own auto-scrolling code

    DesktopEditorLayeredPanel layeredPanel = new DesktopEditorLayeredPanel(this);
    layeredPanel.setMainPanel(myScrollPane);
    if(mayShowToolbar()) {
      layeredPanel.addLayerPanel(new ContextMenuImpl(myScrollPane, this));
    }
    layeredPanel.addLayerPanel(myStatusComponentContainer.getPanel());

    myPanel.add(layeredPanel.getPanel(), BorderLayout.CENTER);

    myEditorComponent.addKeyListener(new KeyListener() {
      @Override
      public void keyPressed(@Nonnull KeyEvent e) {
        if (e.getKeyCode() >= KeyEvent.VK_A && e.getKeyCode() <= KeyEvent.VK_Z) {
          myCharKeyPressed = true;
        }
      }

      @Override
      public void keyTyped(@Nonnull KeyEvent event) {
        myNeedToSelectPreviousChar = false;
        if (event.isConsumed()) {
          return;
        }
        if (processKeyTyped(event)) {
          event.consume();
        }
      }

      @Override
      public void keyReleased(KeyEvent e) {
        myCharKeyPressed = false;
      }
    });

    myEditorComponent.addMouseListener(myMouseListener);
    myGutterComponent.addMouseListener(myMouseListener);
    myEditorComponent.addMouseMotionListener(myMouseMotionListener);
    myGutterComponent.addMouseMotionListener(myMouseMotionListener);

    myEditorComponent.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(@Nonnull FocusEvent e) {
        myCaretCursor.activate();
        for (Caret caret : myCaretModel.getAllCarets()) {
          int caretLine = caret.getLogicalPosition().line;
          repaintLines(caretLine, caretLine);
        }
        fireFocusGained(e);
      }

      @Override
      public void focusLost(@Nonnull FocusEvent e) {
        clearCaretThread();
        for (Caret caret : myCaretModel.getAllCarets()) {
          int caretLine = caret.getLogicalPosition().line;
          repaintLines(caretLine, caretLine);
        }
        fireFocusLost(e);
      }
    });

    UiNotifyConnector connector = new UiNotifyConnector(myEditorComponent, new Activatable.Adapter() {
      @Override
      public void showNotify() {
        myGutterComponent.updateSizeOnShowNotify();
      }
    });
    Disposer.register(getDisposable(), connector);

    try {
      final DropTarget dropTarget = myEditorComponent.getDropTarget();
      if (dropTarget != null) { // might be null in headless environment
        dropTarget.addDropTargetListener(new DropTargetAdapter() {
          @Override
          public void drop(@Nonnull DropTargetDropEvent e) {
          }

          @Override
          public void dragOver(@Nonnull DropTargetDragEvent e) {
            Point location = e.getLocation();

            getCaretModel().moveToVisualPosition(getTargetPosition(location.x, location.y, true));
            getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            requestFocus();
          }
        });
      }
    }
    catch (TooManyListenersException e) {
      LOG.error(e);
    }
    // update area available for soft wrapping on component shown/hidden
    myPanel.addHierarchyListener(e -> mySoftWrapModel.getApplianceManager().updateAvailableArea());

    myPanel.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(@Nonnull ComponentEvent e) {
        getMarkupModel().recalcEditorDimensions();
        getMarkupModel().repaint(-1, -1);
        if (!isRightAligned()) return;
        updateCaretCursor();
        myCaretCursor.repaint();
      }
    });
  }

  private boolean mayShowToolbar() {
    return !isEmbeddedIntoDialogWrapper() && !isOneLineMode() && ContextMenuImpl.mayShowToolbar(myDocument);
  }

  @Override
  public void setStatusComponent(@Nonnull JComponent component) {
    myStatusComponentContainer.setComponent(component);
  }

  @Override
  public void setFontSize(final int fontSize) {
    setFontSize(fontSize, null);
  }

  /**
   * Changes editor font size, attempting to keep a given point unmoved. If point is not given, top left screen corner is assumed.
   *
   * @param fontSize   new font size
   * @param zoomCenter zoom point, relative to viewport
   */
  private void setFontSize(int fontSize, @Nullable Point zoomCenter) {
    int oldFontSize = myScheme.getEditorFontSize();

    Rectangle visibleArea = myScrollingModel.getVisibleArea();
    Point zoomCenterRelative = zoomCenter == null ? new Point() : zoomCenter;
    Point zoomCenterAbsolute = new Point(visibleArea.x + zoomCenterRelative.x, visibleArea.y + zoomCenterRelative.y);
    LogicalPosition zoomCenterLogical = xyToLogicalPosition(zoomCenterAbsolute);
    int oldLineHeight = getLineHeight();
    int intraLineOffset = zoomCenterAbsolute.y % oldLineHeight;

    myScheme.setEditorFontSize(fontSize);
    fontSize = myScheme.getEditorFontSize(); // resulting font size might be different due to applied min/max limits
    myPropertyChangeSupport.firePropertyChange(PROP_FONT_SIZE, oldFontSize, fontSize);
    // Update vertical scroll bar bounds if necessary (we had a problem that use increased editor font size and it was not possible
    // to scroll to the bottom of the document).
    myScrollPane.getViewport().invalidate();

    Point shiftedZoomCenterAbsolute = logicalPositionToXY(zoomCenterLogical);
    myScrollingModel.disableAnimation();
    try {
      myScrollingModel.scroll(visibleArea.x == 0 ? 0 : shiftedZoomCenterAbsolute.x - zoomCenterRelative.x, // stick to left border if it's visible
                              shiftedZoomCenterAbsolute.y - zoomCenterRelative.y + (intraLineOffset * getLineHeight() + oldLineHeight / 2) / oldLineHeight);
    }
    finally {
      myScrollingModel.enableAnimation();
    }
  }

  @Nonnull
  public ActionCallback type(@Nonnull final String text) {
    final ActionCallback result = new ActionCallback();

    for (int i = 0; i < text.length(); i++) {
      myLastTypedActionTimestamp = System.currentTimeMillis();
      char c = text.charAt(i);
      myLastTypedAction = Character.toString(c);
      if (!processKeyTyped(c)) {
        result.setRejected();
        return result;
      }
    }

    result.setDone();

    return result;
  }

  private boolean processKeyTyped(char c) {

    if (ProgressManager.getInstance().hasModalProgressIndicator()) {
      return false;
    }
    FileDocumentManager manager = FileDocumentManager.getInstance();
    final VirtualFile file = manager.getFile(myDocument);
    if (file != null && !file.isValid()) {
      return false;
    }

    DataContext context = getDataContext();

    Graphics graphics = GraphicsUtil.safelyGetGraphics(myEditorComponent);
    if (graphics != null) { // editor component is not showing
      PaintUtil.alignTxToInt((Graphics2D)graphics, PaintUtil.insets2offset(getInsets()), true, false, RoundingMode.CEIL);
      processKeyTypedImmediately(c, graphics, context);
      graphics.dispose();
    }

    ActionManagerEx.getInstanceEx().fireBeforeEditorTyping(c, context);
    EditorUIUtil.hideCursorInEditor(this);
    processKeyTypedNormally(c, context);

    return true;
  }

  void processKeyTypedImmediately(char c, Graphics graphics, DataContext dataContext) {
    EditorActionPlan plan = new EditorActionPlan(this);
    EditorActionManager.getInstance().getTypedAction().beforeActionPerformed(this, c, dataContext, plan);
    if (myImmediatePainter.paint(graphics, plan)) {
      measureTypingLatency();
      myLastTypedActionTimestamp = -1;
    }
  }

  void processKeyTypedNormally(char c, DataContext dataContext) {
    EditorActionManager.getInstance().getTypedAction().actionPerformed(this, c, dataContext);
  }

  @Override
  @Nonnull
  public EditorComponentImpl getContentComponent() {
    return myEditorComponent;
  }

  @Nonnull
  @Override
  public consulo.ui.Component getContentUIComponent() {
    return TargetAWT.wrap(myEditorComponent);
  }

  @Nonnull
  @Override
  public EditorGutterComponentImpl getGutterComponentEx() {
    return myGutterComponent;
  }

  @Override
  public int yToVisualLine(int y) {
    return myView.yToVisualLine(y);
  }

  @Override
  @Nonnull
  public VisualPosition xyToVisualPosition(@Nonnull Point p) {
    return myView.xyToVisualPosition(p);
  }

  @Override
  @Nonnull
  public VisualPosition xyToVisualPosition(@Nonnull Point2D p) {
    return myView.xyToVisualPosition(p);
  }

  @Override
  @Nonnull
  public Point2D offsetToPoint2D(int offset, boolean leanTowardsLargerOffsets, boolean beforeSoftWrap) {
    return myView.offsetToXY(offset, leanTowardsLargerOffsets, beforeSoftWrap);
  }

  @Override
  @Nonnull
  public Point offsetToXY(int offset, boolean leanForward, boolean beforeSoftWrap) {
    Point2D point2D = offsetToPoint2D(offset, leanForward, beforeSoftWrap);
    return new Point((int)point2D.getX(), (int)point2D.getY());
  }

  @Override
  @Nonnull
  public VisualPosition offsetToVisualPosition(int offset) {
    return offsetToVisualPosition(offset, false, false);
  }

  @Override
  @Nonnull
  public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
    return myView.offsetToVisualPosition(offset, leanForward, beforeSoftWrap);
  }

  public int offsetToVisualColumnInFoldRegion(@Nonnull FoldRegion region, int offset, boolean leanTowardsLargerOffsets) {
    assertIsDispatchThread();
    return myView.offsetToVisualColumnInFoldRegion(region, offset, leanTowardsLargerOffsets);
  }

  public int visualColumnToOffsetInFoldRegion(@Nonnull FoldRegion region, int visualColumn, boolean leansRight) {
    assertIsDispatchThread();
    return myView.visualColumnToOffsetInFoldRegion(region, visualColumn, leansRight);
  }

  @Override
  @Nonnull
  public LogicalPosition offsetToLogicalPosition(int offset) {
    return myView.offsetToLogicalPosition(offset);
  }

  @TestOnly
  public void setCaretActive() {
    synchronized (ourCaretBlinkingCommand) {
      ourCaretBlinkingCommand.myEditor = this;
    }
  }

  // optimization: do not do column calculations here since we are interested in line number only
  @Override
  public int offsetToVisualLine(int offset, boolean beforeSoftWrap) {
    return myView.offsetToVisualLine(offset, beforeSoftWrap);
  }

  @Override
  public int visualLineStartOffset(int visualLine) {
    return myView.visualLineToOffset(visualLine);
  }

  @Override
  @Nonnull
  public LogicalPosition xyToLogicalPosition(@Nonnull Point p) {
    Point pp = p.x >= 0 && p.y >= 0 ? p : new Point(Math.max(p.x, 0), Math.max(p.y, 0));
    return visualToLogicalPosition(xyToVisualPosition(pp));
  }

  private int logicalToVisualLine(int logicalLine) {
    return logicalLine < myDocument.getLineCount() ? offsetToVisualLine(myDocument.getLineStartOffset(logicalLine)) : logicalToVisualPosition(new LogicalPosition(logicalLine, 0)).line;
  }

  private int logicalLineToY(int line) {
    int visualLine = logicalToVisualLine(line);
    return visualLineToY(visualLine);
  }

  @Override
  @Nonnull
  public Point logicalPositionToXY(@Nonnull LogicalPosition pos) {
    VisualPosition visible = logicalToVisualPosition(pos);
    return visualPositionToXY(visible);
  }

  @Override
  @Nonnull
  public Point visualPositionToXY(@Nonnull VisualPosition visible) {
    Point2D point2D = myView.visualPositionToXY(visible);
    return new Point((int)point2D.getX(), (int)point2D.getY());
  }

  @Override
  @Nonnull
  public Point2D visualPositionToPoint2D(@Nonnull VisualPosition visible) {
    return myView.visualPositionToXY(visible);
  }

  /**
   * Returns how much current line height bigger than the normal (16px)
   * This method is used to scale editors elements such as gutter icons, folding elements, and others
   */
  public float getScale() {
    if (!Registry.is("editor.scale.gutter.icons")) return 1f;
    float normLineHeight = getLineHeight() / myScheme.getLineSpacing(); // normalized, as for 1.0f line spacing
    return normLineHeight / JBUIScale.scale(16f);
  }

  public int findNearestDirectionBoundary(int offset, boolean lookForward) {
    return myView.findNearestDirectionBoundary(offset, lookForward);
  }

  @Override
  public int visualLineToY(int line) {
    return myView.visualLineToY(line);
  }

  @Override
  public void repaint(int startOffset, int endOffset, boolean invalidateTextLayout) {
    if (myDocument.isInBulkUpdate()) {
      return;
    }
    assertIsDispatchThread();
    endOffset = Math.min(endOffset, myDocument.getTextLength());

    if (invalidateTextLayout) {
      myView.invalidateRange(startOffset, endOffset);
    }

    if (!isGutterShowing()) {
      return;
    }
    // We do repaint in case of equal offsets because there is a possible case that there is a soft wrap at the same offset and
    // it does occupy particular amount of visual space that may be necessary to repaint.
    if (startOffset <= endOffset) {
      int startLine = myView.offsetToVisualLine(startOffset, false);
      int endLine = myView.offsetToVisualLine(endOffset, true);
      doRepaint(startLine, endLine);
    }
  }

  private boolean isGutterShowing() {
    return myGutterComponent.isShowing();
  }

  private void repaintToScreenBottom(int startLine) {
    int yStartLine = logicalLineToY(startLine);
    repaintToScreenBottomStartingFrom(yStartLine);
  }

  private void repaintToScreenBottomStartingFrom(int y) {
    Rectangle visibleArea = getScrollingModel().getVisibleArea();
    int yEndLine = visibleArea.y + visibleArea.height;

    myEditorComponent.repaintEditorComponent(visibleArea.x, y, visibleArea.x + visibleArea.width, yEndLine - y);
    myGutterComponent.repaint(0, y, myGutterComponent.getWidth(), yEndLine - y);
    ((DesktopEditorMarkupModelImpl)getMarkupModel()).repaint(-1, -1);
  }

  /**
   * Asks to repaint all logical lines from the given {@code [start; end]} range.
   *
   * @param startLine start logical line to repaint (inclusive)
   * @param endLine   end logical line to repaint (inclusive)
   */
  private void repaintLines(int startLine, int endLine) {
    if (!isGutterShowing()) return;

    int startVisualLine = logicalToVisualLine(startLine);
    int endVisualLine = myDocument.getTextLength() <= 0 ? 0 : offsetToVisualLine(myDocument.getLineEndOffset(Math.min(myDocument.getLineCount() - 1, endLine)));
    doRepaint(startVisualLine, endVisualLine);
  }

  /**
   * Repaints visual lines in provided range (inclusive on both ends)
   */
  private void doRepaint(int startVisualLine, int endVisualLine) {
    Rectangle visibleArea = getScrollingModel().getVisibleArea();
    int yStart = visualLineToY(startVisualLine);
    int height = visualLineToY(endVisualLine) + getLineHeight() + 2 - yStart;
    myEditorComponent.repaintEditorComponent(visibleArea.x, yStart, visibleArea.x + visibleArea.width, height);
    myGutterComponent.repaint(0, yStart, myGutterComponent.getWidth(), height);
  }

  @Override
  protected void bulkUpdateStarted() {
    myView.getPreferredSize(); // make sure size is calculated (in case it will be required while bulk mode is active)

    ((DesktopScrollingModelImpl)myScrollingModel).onBulkDocumentUpdateStarted();
  }

  @Override
  protected void bulkUpdateFinished() {
    myFoldingModel.onBulkDocumentUpdateFinished();
    mySoftWrapModel.onBulkDocumentUpdateFinished();
    myView.reset();
    myCaretModel.onBulkDocumentUpdateFinished();

    setMouseSelectionState(MOUSE_SELECTION_STATE_NONE);

    validateSize();

    updateGutterSize();
    repaintToScreenBottom(0);
    updateCaretCursor();

    if (!Boolean.TRUE.equals(getUserData(DISABLE_CARET_POSITION_KEEPING))) {
      myScrollingPositionKeeper.restorePosition(true);
    }
  }

  void invokeDelayedErrorStripeRepaint() {
    if (myErrorStripeNeedsRepaint) {
      getMarkupModel().repaint(-1, -1);
      myErrorStripeNeedsRepaint = false;
    }
  }

  protected void changedUpdate(DocumentEvent e) {
    myDocumentChangeInProgress = false;
    if (myDocument.isInBulkUpdate()) return;

    if (myErrorStripeNeedsRepaint) {
      getMarkupModel().repaint(e.getOffset(), e.getOffset() + e.getNewLength());
      myErrorStripeNeedsRepaint = false;
    }

    setMouseSelectionState(MOUSE_SELECTION_STATE_NONE);

    if (getGutterComponentEx().getCurrentAccessibleLine() != null) {
      escapeGutterAccessibleLine(e.getOffset(), e.getOffset() + e.getNewLength());
    }

    validateSize();

    int startLine = offsetToLogicalLine(e.getOffset());
    int endLine = offsetToLogicalLine(e.getOffset() + e.getNewLength());

    boolean painted = false;
    if (myDocument.getTextLength() > 0) {
      if (startLine != endLine || StringUtil.indexOf(e.getOldFragment(), '\n') != -1) {
        myGutterComponent.clearLineToGutterRenderersCache();
      }

      if (countLineFeeds(e.getOldFragment()) != countLineFeeds(e.getNewFragment())) {
        // Lines removed. Need to repaint till the end of the screen
        repaintToScreenBottom(startLine);
        painted = true;
      }
    }

    updateCaretCursor();
    if (!painted) {
      repaintLines(startLine, endLine);
    }

    if (myRestoreScrollingPosition && !Boolean.TRUE.equals(getUserData(DISABLE_CARET_POSITION_KEEPING))) {
      myScrollingPositionKeeper.restorePosition(true);
    }
  }

  @Override
  public void setInsertMode(boolean mode) {
    super.setInsertMode(mode);
    myCaretCursor.repaint();
  }

  @Override
  @Nonnull
  public String dumpState() {
    return super.dumpState() +
           "\ninsets: " +
           myEditorComponent.getInsets() +
           (myView == null ? "" : "\nview: " + myView.dumpState());
  }

  @Override
  public void setHighlighter(@Nonnull EditorHighlighter highlighter) {
    super.setHighlighter(highlighter);

    if (myPanel != null) {
      reinitSettings();
    }
  }

  private void escapeGutterAccessibleLine(int offsetStart, int offsetEnd) {
    int startVisLine = offsetToVisualLine(offsetStart);
    int endVisLine = offsetToVisualLine(offsetEnd);
    int line = getCaretModel().getPrimaryCaret().getVisualPosition().line;
    if (startVisLine <= line && endVisLine >= line) {
      getGutterComponentEx().escapeCurrentAccessibleLine();
    }
  }

  public void hideCursor() {
    if (!myIsViewer && EMPTY_CURSOR != null && Registry.is("ide.hide.cursor.when.typing")) {
      myDefaultCursor = EMPTY_CURSOR;
      updateEditorCursor();
    }
  }

  private static int countLineFeeds(@Nonnull CharSequence c) {
    return StringUtil.countNewLines(c);
  }

  private boolean updatingSize; // accessed from EDT only

  private void updateGutterSize() {
    assertIsDispatchThread();
    if (!updatingSize) {
      updatingSize = true;
      ApplicationManager.getApplication().invokeLater(() -> {
        try {
          if (!isDisposed()) {
            myGutterComponent.updateSize();
          }
        }
        finally {
          updatingSize = false;
        }
      }, ModalityState.any(), __ -> isDisposed());
    }
  }

  @Override
  public void validateSize() {
    if (isReleased) return;

    Dimension dim = getPreferredSize();

    if (!dim.equals(myPreferredSize) && !myDocument.isInBulkUpdate()) {
      dim = mySizeAdjustmentStrategy.adjust(dim, myPreferredSize, this);
      if (!dim.equals(myPreferredSize)) {
        myPreferredSize = dim;

        updateGutterSize();

        myEditorComponent.setSize(dim);
        myEditorComponent.fireResized();

        getMarkupModel().recalcEditorDimensions();
        getMarkupModel().repaint(-1, -1);
      }
    }
  }

  void recalculateSizeAndRepaint() {
    validateSize();
    myEditorComponent.repaintEditorComponent();
  }

  @Override
  @Nonnull
  public JComponent getComponent() {
    return myPanel;
  }

  @Nonnull
  @Override
  public consulo.ui.Component getUIComponent() {
    return TargetAWT.wrap(myPanel);
  }

  public void setHorizontalTextAlignment(@MagicConstant(intValues = {TEXT_ALIGNMENT_LEFT, TEXT_ALIGNMENT_RIGHT}) int alignment) {
    myHorizontalTextAlignment = alignment;
  }

  public boolean isRightAligned() {
    return myHorizontalTextAlignment == TEXT_ALIGNMENT_RIGHT;
  }

  @Override
  protected void stopDumb() {
    putUserData(BUFFER, null);
  }

  /**
   * {@link #stopDumbLater} or {@link #stopDumb} must be performed in finally
   */
  @Override
  public void startDumb() {
    if (ApplicationManager.getApplication().isHeadlessEnvironment()) return;
    if (!Registry.is("editor.dumb.mode.available")) return;
    putUserData(BUFFER, null);
    Rectangle rect = ((JViewport)myEditorComponent.getParent()).getViewRect();
    // The LCD text loop is enabled only for opaque images
    BufferedImage image = UIUtil.createImage(myEditorComponent, rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
    Graphics imageGraphics = image.createGraphics();
    imageGraphics.translate(-rect.x, -rect.y);
    Graphics2D graphics = JBSwingUtilities.runGlobalCGTransform(myEditorComponent, imageGraphics);
    graphics.setClip(rect.x, rect.y, rect.width, rect.height);
    myEditorComponent.paintComponent(graphics);
    graphics.dispose();
    putUserData(BUFFER, image);
  }

  public boolean isDumb() {
    return getUserData(BUFFER) != null;
  }

  void paint(@Nonnull Graphics2D g) {
    Rectangle clip = g.getClipBounds();

    if (clip == null) {
      return;
    }

    BufferedImage buffer = Registry.is("editor.dumb.mode.available") ? getUserData(BUFFER) : null;
    if (buffer != null) {
      Rectangle rect = getContentComponent().getVisibleRect();
      UIUtil.drawImage(g, buffer, null, rect.x, rect.y);
      return;
    }

    if (isReleased) {
      g.setColor(getDisposedBackground());
      g.fillRect(clip.x, clip.y, clip.width, clip.height);
      return;
    }
    if (myUpdateCursor) {
      setCursorPosition();
      myUpdateCursor = false;
    }
    if (myProject != null && myProject.isDisposed()) return;

    myView.paint(g);

    //boolean isBackgroundImageSet = IdeBackgroundUtil.isEditorBackgroundImageSet(myProject);
    //if (myBackgroundImageSet != isBackgroundImageSet) {
    //  myBackgroundImageSet = isBackgroundImageSet;
    //  updateOpaque(myScrollPane.getHorizontalScrollBar());
    //  updateOpaque(myScrollPane.getVerticalScrollBar());
    //}
  }

  Color getDisposedBackground() {
    return new JBColor(new Color(128, 255, 128), new Color(128, 255, 128));
  }

  @Override
  public void setHeaderComponent(JComponent header) {
    myHeaderPanel.removeAll();
    header = header == null ? getPermanentHeaderComponent() : header;
    if (header != null) {
      myHeaderPanel.add(header);
    }

    myHeaderPanel.revalidate();
    myHeaderPanel.repaint();

    TouchBarsManager.onUpdateEditorHeader(this, header);
  }

  @Override
  public boolean hasHeaderComponent() {
    JComponent header = getHeaderComponent();
    return header != null && header != getPermanentHeaderComponent();
  }

  @Override
  @Nullable
  public JComponent getPermanentHeaderComponent() {
    return getUserData(PERMANENT_HEADER);
  }

  @Override
  public void setPermanentHeaderComponent(@Nullable JComponent component) {
    putUserData(PERMANENT_HEADER, component);
  }

  @Override
  @Nullable
  public JComponent getHeaderComponent() {
    if (myHeaderPanel.getComponentCount() > 0) {
      return (JComponent)myHeaderPanel.getComponent(0);
    }
    return null;
  }

  @Override
  public void setBackgroundColor(ColorValue color) {
    myScrollPane.setBackground(TargetAWT.to(color));

    if (getBackgroundIgnoreForced().equals(color)) {
      myForcedBackground = null;
      return;
    }
    myForcedBackground = color;
  }

  @Nonnull
  ColorValue getForegroundColor() {
    return myScheme.getDefaultForeground();
  }

  @Nonnull
  @Override
  public ColorValue getBackgroundColor() {
    if (myForcedBackground != null) return myForcedBackground;

    return getBackgroundIgnoreForced();
  }

  @Nonnull
  @Override
  public TextDrawingCallback getTextDrawingCallback() {
    return myTextDrawingCallback;
  }

  ColorValue getBackgroundColor(@Nonnull final TextAttributes attributes) {
    final ColorValue attrColor = attributes.getBackgroundColor();
    return Comparing.equal(attrColor, myScheme.getDefaultBackground()) ? getBackgroundColor() : attrColor;
  }

  @Nonnull
  private ColorValue getBackgroundIgnoreForced() {
    ColorValue color = myScheme.getDefaultBackground();
    if (myDocument.isWritable()) {
      return color;
    }
    ColorValue readOnlyColor = myScheme.getColor(EditorColors.READONLY_BACKGROUND_COLOR);
    return readOnlyColor != null ? readOnlyColor : color;
  }

  @Nullable
  public TextRange getComposedTextRange() {
    return myInputMethodRequestsHandler == null || myInputMethodRequestsHandler.composedText == null ? null : myInputMethodRequestsHandler.composedTextRange;
  }

  @Override
  public int getMaxWidthInRange(int startOffset, int endOffset) {
    return myView.getMaxWidthInRange(startOffset, endOffset);
  }

  public boolean isPaintSelection() {
    return myPaintSelection || !isOneLineMode() || IJSwingUtilities.hasFocus(getContentComponent());
  }

  public void setPaintSelection(boolean paintSelection) {
    myPaintSelection = paintSelection;
  }

  @Nullable
  public CaretRectangle[] getCaretLocations(boolean onlyIfShown) {
    return myCaretCursor.getCaretLocations(onlyIfShown);
  }

  @Override
  public int getAscent() {
    return myView.getAscent();
  }

  @Override
  public int getLineHeight() {
    return myView.getLineHeight();
  }

  public int getDescent() {
    return myView.getDescent();
  }

  public int getCharHeight() {
    return myView.getCharHeight();
  }

  @Nonnull
  public FontMetrics getFontMetrics(@JdkConstants.FontStyle int fontType) {
    EditorFontType ft;
    if (fontType == Font.PLAIN) ft = EditorFontType.PLAIN;
    else if (fontType == Font.BOLD) ft = EditorFontType.BOLD;
    else if (fontType == Font.ITALIC) ft = EditorFontType.ITALIC;
    else if (fontType == (Font.BOLD | Font.ITALIC)) ft = EditorFontType.BOLD_ITALIC;
    else {
      LOG.error("Unknown font type: " + fontType);
      ft = EditorFontType.PLAIN;
    }

    return myEditorComponent.getFontMetrics(myScheme.getFont(ft));
  }

  public int getPreferredHeight() {
    return isReleased ? 0 : myView.getPreferredHeight();
  }

  public Dimension getPreferredSize() {
    return isReleased ? new Dimension() : Registry.is("idea.true.smooth.scrolling.dynamic.scrollbars") ? new Dimension(getPreferredWidthOfVisibleLines(), myView.getPreferredHeight()) : myView.getPreferredSize();
  }

  /* When idea.true.smooth.scrolling=true, this method is used to compute width of currently visible line range
     rather than width of the whole document.

     As transparent scrollbars, by definition, prevent blit-acceleration of scrolling, and we really need blit-acceleration
     because not all hardware can render pixel-by-pixel scrolling with acceptable FPS without it (we now have 4K-5K displays, you know).
     To have both the hardware acceleration and the transparent scrollbars we need to completely redesign JViewport machinery to support
     independent layers, which is (probably) possible, but it's a rather cumbersome task.

     Another approach is to make scrollbars opaque, but only in the editor (as editor is a slow-to-draw component with large screen area).
     This is what "true smooth scrolling" option currently does. Interestingly, making the vertical scrollbar opaque might actually be
     a good thing because on modern displays (size, aspect ratio) code rarely extends beyond the right screen edge, and even
     when it does, its mixing with the navigation bar only reduces intelligibility of both the navigation bar and the code itself.

     Horizontal scrollbar is another story - a single long line of text forces horizontal scrollbar in the whole document,
     and in that case "transparent" scrollbar has some merits. However, instead of using transparency, we can hide horizontal
     scrollbar altogether when it's not needed for currently visible content. In a sense, this approach is superior,
     as even "transparent" scrollbar is only semi-transparent (thus we may prefer "on-demand" scrollbar in the general case).

     Hiding the horizontal scrollbar also solves another issue - when both scrollbars are visible, vertical scrolling with
     a high-precision touchpad can result in unintentional horizontal shifts (because of the touchpad sensitivity).
     When visible content fully fits horizontally (i.e. in most cases), hiding the unneeded scrollbar
     reliably prevents the horizontal  "jitter".

     Keep in mind that this functionality is experimental and may need more polishing.

     In principle, we can apply this method to other components by defining, for example,
     VariableWidth interface and supporting it in JBScrollPane. */
  private int getPreferredWidthOfVisibleLines() {
    Rectangle area = getScrollingModel().getVisibleArea();
    VisualPosition begin = xyToVisualPosition(area.getLocation());
    VisualPosition end = xyToVisualPosition(new Point(area.x + area.width, area.y + area.height));
    return Math.max(myView.getPreferredWidth(begin.line, end.line), getScrollingWidth());
  }

  /* Returns the width of current horizontal scrolling state.
     Complements the getPreferredWidthOfVisibleLines() method to allows to retain horizontal
     scrolling position that is beyond the width of currently visible lines. */
  private int getScrollingWidth() {
    JScrollBar scrollbar = myScrollPane.getHorizontalScrollBar();
    if (scrollbar != null) {
      BoundedRangeModel model = scrollbar.getModel();
      if (model != null) {
        return model.getValue() + model.getExtent();
      }
    }
    return 0;
  }

  @Nonnull
  @Override
  public Dimension getContentSize() {
    return myView.getPreferredSize();
  }

  @Nonnull
  @Override
  public JScrollPane getScrollPane() {
    return myScrollPane;
  }

  @Override
  public void setBorder(Border border) {
    myScrollPane.setBorder(border);
  }

  @Override
  public Insets getInsets() {
    return myScrollPane.getInsets();
  }

  @Override
  public int logicalPositionToOffset(@Nonnull LogicalPosition pos) {
    return myView.logicalPositionToOffset(pos);
  }

  /**
   * @return information about total number of lines that can be viewed by user. I.e. this is a number of all document
   * lines (considering that single logical document line may be represented on multiple visual lines because of
   * soft wraps appliance) minus number of folded lines
   */
  @Override
  public int getVisibleLineCount() {
    return getVisibleLogicalLinesCount() + getSoftWrapModel().getSoftWrapsIntroducedLinesNumber();
  }

  /**
   * @return number of visible logical lines. Generally, that is a total logical lines number minus number of folded lines
   */
  private int getVisibleLogicalLinesCount() {
    return getDocument().getLineCount() - myFoldingModel.getTotalNumberOfFoldedLines();
  }

  @Override
  @Nonnull
  public VisualPosition logicalToVisualPosition(@Nonnull LogicalPosition logicalPos) {
    return myView.logicalToVisualPosition(logicalPos, false);
  }

  @Override
  @Nonnull
  public LogicalPosition visualToLogicalPosition(@Nonnull VisualPosition visiblePos) {
    return myView.visualToLogicalPosition(visiblePos);
  }

  private int offsetToLogicalLine(int offset) {
    int textLength = myDocument.getTextLength();
    if (textLength == 0) return 0;

    if (offset > textLength || offset < 0) {
      throw new IndexOutOfBoundsException("Wrong offset: " + offset + " textLength: " + textLength);
    }

    int lineIndex = myDocument.getLineNumber(offset);
    LOG.assertTrue(lineIndex >= 0 && lineIndex < myDocument.getLineCount());

    return lineIndex;
  }

  private VisualPosition getTargetPosition(int x, int y, boolean trimToLineWidth) {
    if (myDocument.getLineCount() == 0) {
      return new VisualPosition(0, 0);
    }
    if (x < 0) {
      x = 0;
    }
    if (y < 0) {
      y = 0;
    }
    int visualLineCount = getVisibleLineCount();
    if (yToVisualLine(y) >= visualLineCount) {
      y = visualLineToY(Math.max(0, visualLineCount - 1));
    }
    VisualPosition visualPosition = xyToVisualPosition(new Point(x, y));
    if (trimToLineWidth && !mySettings.isVirtualSpace()) {
      LogicalPosition logicalPosition = visualToLogicalPosition(visualPosition);
      LogicalPosition lineEndPosition = offsetToLogicalPosition(myDocument.getLineEndOffset(logicalPosition.line));
      if (logicalPosition.column > lineEndPosition.column) {
        visualPosition = logicalToVisualPosition(lineEndPosition.leanForward(true));
      }
      else if (mySoftWrapModel.isInsideSoftWrap(visualPosition)) {
        VisualPosition beforeSoftWrapPosition = myView.logicalToVisualPosition(logicalPosition, true);
        if (visualPosition.line == beforeSoftWrapPosition.line) {
          visualPosition = beforeSoftWrapPosition;
        }
        else {
          visualPosition = myView.logicalToVisualPosition(logicalPosition, false);
        }
      }
    }
    return visualPosition;
  }

  private boolean checkIgnore(@Nonnull MouseEvent e) {
    if (!myIgnoreMouseEventsConsecutiveToInitial) {
      myInitialMouseEvent = null;
      return false;
    }

    if (myInitialMouseEvent != null && (e.getComponent() != myInitialMouseEvent.getComponent() || !e.getPoint().equals(myInitialMouseEvent.getPoint()))) {
      myIgnoreMouseEventsConsecutiveToInitial = false;
      myInitialMouseEvent = null;
      return false;
    }

    myIgnoreMouseEventsConsecutiveToInitial = false;
    myInitialMouseEvent = null;

    e.consume();

    return true;
  }

  private void processMouseReleased(@Nonnull MouseEvent e) {
    if (checkIgnore(e)) return;

    if (e.getSource() == myGutterComponent && !(myMousePressedEvent != null && myMousePressedEvent.isConsumed())) {
      myGutterComponent.mouseReleased(e);
    }

    if (getMouseEventArea(e) != EditorMouseEventArea.EDITING_AREA || e.getY() < 0 || e.getX() < 0) {
      return;
    }

    final FoldRegion region = getFoldingModel().getFoldingPlaceholderAt(e.getPoint());
    if (region != null && region == myMouseSelectedRegion) {
      getFoldingModel().runBatchFoldingOperation(() -> {
        myFoldingModel.disableScrollingPositionAdjustment();
        region.setExpanded(true);
      });
    }

    // The general idea is to check if the user performed 'caret position change click' (left click most of the time) inside selection
    // and, in the case of the positive answer, clear selection. Please note that there is a possible case that mouse click
    // is performed inside selection but it triggers context menu. We don't want to drop the selection then.
    if (myMousePressedEvent != null &&
        myMousePressedEvent.getClickCount() == 1 &&
        myKeepSelectionOnMousePress &&
        !myDragStarted &&
        !myMousePressedEvent.isShiftDown() &&
        !myMousePressedEvent.isPopupTrigger() &&
        !isToggleCaretEvent(myMousePressedEvent) &&
        !isCreateRectangularSelectionEvent(myMousePressedEvent)) {
      getSelectionModel().removeSelection();
    }
  }

  private boolean isInsideGutterWhitespaceArea(@Nonnull MouseEvent e) {
    EditorMouseEventArea area = getMouseEventArea(e);
    return area == EditorMouseEventArea.FOLDING_OUTLINE_AREA && myGutterComponent.convertX(e.getX()) > myGutterComponent.getWhitespaceSeparatorOffset();
  }

  @Override
  public EditorMouseEventArea getMouseEventArea(@Nonnull MouseEvent e) {
    if (myGutterComponent != e.getSource()) return EditorMouseEventArea.EDITING_AREA;

    int x = myGutterComponent.convertX(e.getX());

    return myGutterComponent.getEditorMouseAreaByOffset(x);
  }

  private void requestFocus() {
    final IdeFocusManager focusManager = IdeFocusManager.getInstance(myProject);
    if (focusManager.getFocusOwner() != myEditorComponent) { //IDEA-64501
      focusManager.requestFocus(myEditorComponent, true);
    }
  }

  private void validateMousePointer(@Nonnull MouseEvent e) {
    if (e.getSource() == myGutterComponent) {
      myGutterComponent.validateMousePointer(e);
    }
    else {
      myGutterComponent.setActiveFoldRegions(Collections.emptyList());
      myDefaultCursor = getDefaultCursor(e);
      updateEditorCursor();
    }
  }

  private void updateEditorCursor() {
    Cursor customCursor = getCustomCursor();
    if (customCursor == null && myCursorSetExternally && myEditorComponent.isCursorSet()) {
      Cursor cursor = myEditorComponent.getCursor();
      if (cursor != Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) &&
          cursor != Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR) &&
          cursor != EMPTY_CURSOR &&
          (!SystemInfo.isMac || cursor != MacUIUtil.getInvertedTextCursor())) {
        // someone else has set cursor, don't touch it
        return;
      }
    }

    UIUtil.setCursor(myEditorComponent, customCursor == null ? myDefaultCursor : customCursor);
    myCursorSetExternally = false;
  }

  private Cursor getDefaultCursor(@Nonnull MouseEvent e) {
    if (getSelectionModel().hasSelection() && (e.getModifiersEx() & (InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK)) == 0) {
      int offset = logicalPositionToOffset(xyToLogicalPosition(e.getPoint()));
      if (getSelectionModel().getSelectionStart() <= offset && offset < getSelectionModel().getSelectionEnd()) {
        return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
      }
    }
    return UIUtil.getTextCursor(TargetAWT.to(getBackgroundColor()));
  }

  private void runMouseDraggedCommand(@Nonnull final MouseEvent e) {
    if (myCommandProcessor == null || myMousePressedEvent != null && myMousePressedEvent.isConsumed()) {
      return;
    }
    myCommandProcessor.executeCommand(myProject, () -> processMouseDragged(e), "", MOUSE_DRAGGED_GROUP, UndoConfirmationPolicy.DEFAULT, getDocument());
  }

  private void processMouseDragged(@Nonnull MouseEvent e) {
    if (!SwingUtilities.isLeftMouseButton(e) && !SwingUtilities.isMiddleMouseButton(e) || (Registry.is("editor.disable.drag.with.right.button") && SwingUtilities.isRightMouseButton(e))) {
      return;
    }

    EditorMouseEventArea eventArea = getMouseEventArea(e);
    if (eventArea == EditorMouseEventArea.ANNOTATIONS_AREA) return;
    if (eventArea == EditorMouseEventArea.LINE_MARKERS_AREA || eventArea == EditorMouseEventArea.FOLDING_OUTLINE_AREA && !isInsideGutterWhitespaceArea(e)) {
      // The general idea is that we don't want to change caret position on gutter marker area click (e.g. on setting a breakpoint)
      // but do want to allow bulk selection on gutter marker mouse drag. However, when a drag is performed, the first event is
      // a 'mouse pressed' event, that's why we remember target line on 'mouse pressed' processing and use that information on
      // further dragging (if any).
      if (myDragOnGutterSelectionStartLine >= 0) {
        mySelectionModel.removeSelection();
        myCaretModel.moveToOffset(myDragOnGutterSelectionStartLine < myDocument.getLineCount() ? myDocument.getLineStartOffset(myDragOnGutterSelectionStartLine) : myDocument.getTextLength());
      }
      myDragOnGutterSelectionStartLine = -1;
    }

    boolean columnSelectionDragEvent = isColumnSelectionDragEvent(e);
    boolean toggleCaretEvent = isToggleCaretEvent(e);
    boolean addRectangularSelectionEvent = isAddRectangularSelectionEvent(e);
    boolean columnSelectionDrag = isColumnMode() && !myLastPressCreatedCaret || columnSelectionDragEvent;
    if (!columnSelectionDragEvent && toggleCaretEvent && !myLastPressCreatedCaret) {
      return; // ignoring drag after removing a caret
    }

    Rectangle visibleArea = getScrollingModel().getVisibleArea();

    int x = e.getX();

    if (e.getSource() == myGutterComponent) {
      x = 0;
    }

    int dx = 0;
    if (x < visibleArea.x && visibleArea.x > 0) {
      dx = x - visibleArea.x;
    }
    else {
      if (x > visibleArea.x + visibleArea.width) {
        dx = x - visibleArea.x - visibleArea.width;
      }
    }

    int dy = 0;
    int y = e.getY();
    if (y < visibleArea.y && visibleArea.y > 0) {
      dy = y - visibleArea.y;
    }
    else {
      if (y > visibleArea.y + visibleArea.height && visibleArea.y + visibleArea.height < myEditorComponent.getHeight()) {
        dy = y - visibleArea.y - visibleArea.height;
      }
    }
    if (dx == 0 && dy == 0) {
      myScrollingTimer.stop();

      SelectionModel selectionModel = getSelectionModel();
      Caret leadCaret = getLeadCaret();
      int oldSelectionStart = leadCaret.getLeadSelectionOffset();
      VisualPosition oldVisLeadSelectionStart = leadCaret.getLeadSelectionPosition();
      int oldCaretOffset = getCaretModel().getOffset();
      boolean multiCaretSelection = columnSelectionDrag || toggleCaretEvent;
      VisualPosition newVisualCaret = getTargetPosition(x, y, !multiCaretSelection);
      LogicalPosition newLogicalCaret = visualToLogicalPosition(newVisualCaret);
      if (multiCaretSelection) {
        myMultiSelectionInProgress = true;
        myRectangularSelectionInProgress = columnSelectionDrag || addRectangularSelectionEvent;
        myTargetMultiSelectionPosition = xyToVisualPosition(new Point(Math.max(x, 0), Math.max(y, 0)));
      }
      else {
        getCaretModel().moveToVisualPosition(newVisualCaret);
      }

      int newCaretOffset = getCaretModel().getOffset();
      newVisualCaret = getCaretModel().getVisualPosition();
      int caretShift = newCaretOffset - mySavedSelectionStart;

      if (myMousePressedEvent != null && getMouseEventArea(myMousePressedEvent) != EditorMouseEventArea.EDITING_AREA &&
          getMouseEventArea(myMousePressedEvent) != EditorMouseEventArea.LINE_NUMBERS_AREA) {
        selectionModel.setSelection(oldSelectionStart, newCaretOffset);
      }
      else {
        if (multiCaretSelection) {
          if (myLastMousePressedLocation != null && (myCurrentDragIsSubstantial || !newLogicalCaret.equals(myLastMousePressedLocation))) {
            createSelectionTill(newLogicalCaret);
            blockActionsIfNeeded(e, myLastMousePressedLocation, newLogicalCaret);
          }
        }
        else {
          if (getMouseSelectionState() != MOUSE_SELECTION_STATE_NONE) {
            if (caretShift < 0) {
              int newSelection = newCaretOffset;
              if (getMouseSelectionState() == MOUSE_SELECTION_STATE_WORD_SELECTED) {
                newSelection = myCaretModel.getWordAtCaretStart();
              }
              else {
                if (getMouseSelectionState() == MOUSE_SELECTION_STATE_LINE_SELECTED) {
                  newSelection = logicalPositionToOffset(visualToLogicalPosition(new VisualPosition(getCaretModel().getVisualPosition().line, 0)));
                }
              }
              if (newSelection < 0) newSelection = newCaretOffset;
              selectionModel.setSelection(mySavedSelectionEnd, newSelection);
              getCaretModel().moveToOffset(newSelection);
            }
            else {
              int newSelection = newCaretOffset;
              if (getMouseSelectionState() == MOUSE_SELECTION_STATE_WORD_SELECTED) {
                newSelection = myCaretModel.getWordAtCaretEnd();
              }
              else {
                if (getMouseSelectionState() == MOUSE_SELECTION_STATE_LINE_SELECTED) {
                  newSelection = logicalPositionToOffset(visualToLogicalPosition(new VisualPosition(getCaretModel().getVisualPosition().line + 1, 0)));
                }
              }
              if (newSelection < 0) newSelection = newCaretOffset;
              selectionModel.setSelection(mySavedSelectionStart, newSelection);
              getCaretModel().moveToOffset(newSelection);
            }
            cancelAutoResetForMouseSelectionState();
            return;
          }

          if (!myKeepSelectionOnMousePress) {
            // There is a possible case that lead selection position should be adjusted in accordance with the mouse move direction.
            // E.g. consider situation when user selects the whole line by clicking at 'line numbers' area. 'Line end' is considered
            // to be lead selection point then. However, when mouse is dragged down we want to consider 'line start' to be
            // lead selection point.
            if ((myMousePressArea == EditorMouseEventArea.LINE_NUMBERS_AREA || myMousePressArea == EditorMouseEventArea.LINE_MARKERS_AREA) && selectionModel.hasSelection()) {
              if (newCaretOffset >= selectionModel.getSelectionEnd()) {
                oldSelectionStart = selectionModel.getSelectionStart();
                oldVisLeadSelectionStart = selectionModel.getSelectionStartPosition();
              }
              else if (newCaretOffset <= selectionModel.getSelectionStart()) {
                oldSelectionStart = selectionModel.getSelectionEnd();
                oldVisLeadSelectionStart = selectionModel.getSelectionEndPosition();
              }
            }
            if (oldVisLeadSelectionStart != null) {
              setSelectionAndBlockActions(e, oldVisLeadSelectionStart, oldSelectionStart, newVisualCaret, newCaretOffset);
            }
            else {
              setSelectionAndBlockActions(e, oldSelectionStart, newCaretOffset);
            }
            cancelAutoResetForMouseSelectionState();
          }
          else {
            if (caretShift != 0) {
              if (myMousePressedEvent != null) {
                if (mySettings.isDndEnabled()) {
                  if (!myDragStarted) {
                    myDragStarted = true;
                    boolean isCopy = UIUtil.isControlKeyDown(e) || isViewer() || !getDocument().isWritable();
                    mySavedCaretOffsetForDNDUndoHack = oldCaretOffset;
                    getContentComponent().getTransferHandler().exportAsDrag(getContentComponent(), e, isCopy ? TransferHandler.COPY : TransferHandler.MOVE);
                  }
                }
                else {
                  selectionModel.removeSelection();
                }
              }
            }
          }
        }
      }
    }
    else {
      myScrollingTimer.start(dx, dy);
      onSubstantialDrag(e);
    }
  }

  private void clearDnDContext() {
    if (myDraggedRange != null) {
      myDraggedRange.dispose();
      myDraggedRange = null;
    }
    myGutterComponent.myDnDInProgress = false;
  }

  private void createSelectionTill(@Nonnull LogicalPosition targetPosition) {
    List<CaretState> caretStates = new ArrayList<>(myCaretStateBeforeLastPress);
    if (myRectangularSelectionInProgress) {
      caretStates.addAll(EditorModificationUtil.calcBlockSelectionState(this, myLastMousePressedLocation, targetPosition));
    }
    else {
      LogicalPosition selectionStart = myLastMousePressedLocation;
      LogicalPosition selectionEnd = targetPosition;
      if (getMouseSelectionState() != MOUSE_SELECTION_STATE_NONE) {
        int newCaretOffset = logicalPositionToOffset(targetPosition);
        if (newCaretOffset < mySavedSelectionStart) {
          selectionStart = offsetToLogicalPosition(mySavedSelectionEnd);
          if (getMouseSelectionState() == MOUSE_SELECTION_STATE_LINE_SELECTED) {
            targetPosition = selectionEnd = visualToLogicalPosition(new VisualPosition(offsetToVisualLine(newCaretOffset), 0));
          }
        }
        else {
          selectionStart = offsetToLogicalPosition(mySavedSelectionStart);
          int selectionEndOffset = Math.max(newCaretOffset, mySavedSelectionEnd);
          if (getMouseSelectionState() == MOUSE_SELECTION_STATE_WORD_SELECTED) {
            targetPosition = selectionEnd = offsetToLogicalPosition(selectionEndOffset);
          }
          else if (getMouseSelectionState() == MOUSE_SELECTION_STATE_LINE_SELECTED) {
            targetPosition = selectionEnd = visualToLogicalPosition(new VisualPosition(offsetToVisualLine(selectionEndOffset) + 1, 0));
          }
        }
        cancelAutoResetForMouseSelectionState();
      }
      caretStates.add(new CaretState(targetPosition, selectionStart, selectionEnd));
    }
    myCaretModel.setCaretsAndSelections(caretStates);
  }

  private Caret getLeadCaret() {
    List<Caret> allCarets = myCaretModel.getAllCarets();
    Caret firstCaret = allCarets.get(0);
    if (firstCaret == myCaretModel.getPrimaryCaret()) {
      return allCarets.get(allCarets.size() - 1);
    }
    return firstCaret;
  }

  private void setSelectionAndBlockActions(@Nonnull MouseEvent mouseDragEvent, int startOffset, int endOffset) {
    mySelectionModel.setSelection(startOffset, endOffset);
    if (myCurrentDragIsSubstantial || startOffset != endOffset) {
      onSubstantialDrag(mouseDragEvent);
    }
  }

  private void setSelectionAndBlockActions(@Nonnull MouseEvent mouseDragEvent, VisualPosition startPosition, int startOffset, VisualPosition endPosition, int endOffset) {
    mySelectionModel.setSelection(startPosition, startOffset, endPosition, endOffset);
    if (myCurrentDragIsSubstantial || startOffset != endOffset || !Comparing.equal(startPosition, endPosition)) {
      onSubstantialDrag(mouseDragEvent);
    }
  }

  private void blockActionsIfNeeded(@Nonnull MouseEvent mouseDragEvent, @Nonnull LogicalPosition startPosition, @Nonnull LogicalPosition endPosition) {
    if (myCurrentDragIsSubstantial || !startPosition.equals(endPosition)) {
      onSubstantialDrag(mouseDragEvent);
    }
  }

  private void onSubstantialDrag(@Nonnull MouseEvent mouseDragEvent) {
    IdeEventQueue.getInstance().blockNextEvents(mouseDragEvent, IdeEventQueue.BlockMode.ACTIONS);
    myCurrentDragIsSubstantial = true;
  }

  private static class RepaintCursorCommand implements Runnable {
    private long mySleepTime = 500;
    private boolean myIsBlinkCaret = true;
    @Nullable
    private DesktopEditorImpl myEditor;
    private ScheduledFuture<?> mySchedulerHandle;

    public void start() {
      if (mySchedulerHandle != null) {
        mySchedulerHandle.cancel(false);
      }
      mySchedulerHandle = EdtExecutorService.getScheduledExecutorInstance().scheduleWithFixedDelay(this, mySleepTime, mySleepTime, TimeUnit.MILLISECONDS);
    }

    private void setBlinkPeriod(int blinkPeriod) {
      mySleepTime = Math.max(blinkPeriod, 10);
      start();
    }

    private void setBlinkCaret(boolean value) {
      myIsBlinkCaret = value;
    }

    @Override
    public void run() {
      if (myEditor != null) {
        CaretCursor activeCursor = myEditor.myCaretCursor;

        long time = System.currentTimeMillis();
        time -= activeCursor.myStartTime;

        if (time > mySleepTime) {
          boolean toRepaint = true;
          if (myIsBlinkCaret) {
            activeCursor.myIsShown = !activeCursor.myIsShown;
          }
          else {
            toRepaint = !activeCursor.myIsShown;
            activeCursor.myIsShown = true;
          }

          if (toRepaint) {
            activeCursor.repaint();
          }
        }
      }
    }
  }

  public void updateCaretCursor() {
    myUpdateCursor = true;
    if (myCaretCursor.myIsShown) {
      myCaretCursor.myStartTime = System.currentTimeMillis();
    }
    else {
      myCaretCursor.myIsShown = true;
      myCaretCursor.repaint();
    }
  }

  @Override
  public boolean isRtlLocation(@Nonnull VisualPosition visualPosition) {
    return myView.isRtlLocation(visualPosition);
  }

  @Override
  public boolean isAtBidiRunBoundary(@Nonnull VisualPosition visualPosition) {
    return myView.isAtBidiRunBoundary(visualPosition);
  }

  private void setCursorPosition() {
    final List<CaretRectangle> caretPoints = new ArrayList<>();
    for (Caret caret : getCaretModel().getAllCarets()) {
      boolean isRtl = caret.isAtRtlLocation();
      VisualPosition caretPosition = caret.getVisualPosition();
      Point2D pos1 = visualPositionToPoint2D(caretPosition.leanRight(!isRtl));
      Point2D pos2 = visualPositionToPoint2D(new VisualPosition(caretPosition.line, Math.max(0, caretPosition.column + (isRtl ? -1 : 1)), isRtl));
      float width = (float)Math.abs(pos2.getX() - pos1.getX());
      if (!isRtl && myInlayModel.hasInlineElementAt(caretPosition)) {
        width = Math.min(width, (float)Math.ceil(myView.getPlainSpaceWidth()));
      }
      caretPoints.add(new CaretRectangle(pos1, width, caret, isRtl));
    }
    myCaretCursor.setPositions(caretPoints.toArray(new CaretRectangle[0]));
  }

  @Override
  public boolean setCaretVisible(boolean b) {
    boolean old = myCaretCursor.isActive();
    if (b) {
      myCaretCursor.activate();
    }
    else {
      myCaretCursor.passivate();
    }
    return old;
  }

  @Override
  public boolean setCaretEnabled(boolean enabled) {
    boolean old = myCaretCursor.isEnabled();
    myCaretCursor.setEnabled(enabled);
    return old;
  }

  @Override
  public boolean isEmbeddedIntoDialogWrapper() {
    return myEmbeddedIntoDialogWrapper;
  }

  @Override
  public void setEmbeddedIntoDialogWrapper(boolean b) {
    assertIsDispatchThread();

    myEmbeddedIntoDialogWrapper = b;
    myScrollPane.setFocusable(!b);
    myEditorComponent.setFocusCycleRoot(!b);
    myEditorComponent.setFocusable(b);
  }

  public static class CaretRectangle {
    public final Point2D myPoint;
    public final float myWidth;
    public final Caret myCaret;
    public final boolean myIsRtl;

    private CaretRectangle(Point2D point, float width, Caret caret, boolean isRtl) {
      myPoint = point;
      myWidth = Math.max(width, 2);
      myCaret = caret;
      myIsRtl = isRtl;
    }
  }

  class CaretCursor {
    private CaretRectangle[] myLocations;
    private boolean myEnabled;

    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private boolean myIsShown;
    private long myStartTime;

    private CaretCursor() {
      myLocations = new CaretRectangle[]{new CaretRectangle(new Point(0, 0), 0, null, false)};
      setEnabled(true);
    }

    public boolean isEnabled() {
      return myEnabled;
    }

    public void setEnabled(boolean enabled) {
      myEnabled = enabled;
    }

    private void activate() {
      final boolean blink = mySettings.isBlinkCaret();
      final int blinkPeriod = mySettings.getCaretBlinkPeriod();
      synchronized (ourCaretBlinkingCommand) {
        ourCaretBlinkingCommand.myEditor = DesktopEditorImpl.this;
        ourCaretBlinkingCommand.setBlinkCaret(blink);
        ourCaretBlinkingCommand.setBlinkPeriod(blinkPeriod);
        myIsShown = true;
      }
    }

    public boolean isActive() {
      synchronized (ourCaretBlinkingCommand) {
        return myIsShown;
      }
    }

    private void passivate() {
      synchronized (ourCaretBlinkingCommand) {
        myIsShown = false;
      }
    }

    private void setPositions(CaretRectangle[] locations) {
      myStartTime = System.currentTimeMillis();
      myLocations = locations;
    }

    private void repaint() {
      myView.repaintCarets();
    }

    @Nullable
    CaretRectangle[] getCaretLocations(boolean onlyIfShown) {
      if (onlyIfShown && (!isEnabled() || !myIsShown || isRendererMode() || !IJSwingUtilities.hasFocus(getContentComponent()))) return null;
      return myLocations;
    }
  }

  private class ScrollingTimer {
    private Timer myTimer;
    private static final int TIMER_PERIOD = 100;
    private static final int CYCLE_SIZE = 20;
    private int myXCycles;
    private int myYCycles;
    private int myDx;
    private int myDy;
    private int xPassedCycles;
    private int yPassedCycles;

    private void start(int dx, int dy) {
      myDx = 0;
      myDy = 0;
      if (dx > 0) {
        myXCycles = CYCLE_SIZE / dx + 1;
        myDx = 1 + dx / CYCLE_SIZE;
      }
      else {
        if (dx < 0) {
          myXCycles = -CYCLE_SIZE / dx + 1;
          myDx = -1 + dx / CYCLE_SIZE;
        }
      }

      if (dy > 0) {
        myYCycles = CYCLE_SIZE / dy + 1;
        myDy = 1 + dy / CYCLE_SIZE;
      }
      else {
        if (dy < 0) {
          myYCycles = -CYCLE_SIZE / dy + 1;
          myDy = -1 + dy / CYCLE_SIZE;
        }
      }

      if (myTimer != null) {
        return;
      }


      myTimer = TimerUtil.createNamedTimer("Editor scroll timer", TIMER_PERIOD, e -> {
        if (isDisposed()) {
          stop();
          return;
        }
        myCommandProcessor.executeCommand(myProject, new DocumentRunnable(myDocument, myProject) {
          @Override
          public void run() {
            int oldSelectionStart = mySelectionModel.getLeadSelectionOffset();
            VisualPosition caretPosition = myMultiSelectionInProgress ? myTargetMultiSelectionPosition : getCaretModel().getVisualPosition();
            int column = caretPosition.column;
            xPassedCycles++;
            if (xPassedCycles >= myXCycles) {
              xPassedCycles = 0;
              column += myDx;
            }

            int line = caretPosition.line;
            yPassedCycles++;
            if (yPassedCycles >= myYCycles) {
              yPassedCycles = 0;
              line += myDy;
            }

            line = Math.max(0, line);
            column = Math.max(0, column);
            VisualPosition pos = new VisualPosition(line, column);
            if (!myMultiSelectionInProgress) {
              getCaretModel().moveToVisualPosition(pos);
              getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            }

            int newCaretOffset = getCaretModel().getOffset();
            int caretShift = newCaretOffset - mySavedSelectionStart;

            if (getMouseSelectionState() != MOUSE_SELECTION_STATE_NONE) {
              if (caretShift < 0) {
                int newSelection = newCaretOffset;
                if (getMouseSelectionState() == MOUSE_SELECTION_STATE_WORD_SELECTED) {
                  newSelection = myCaretModel.getWordAtCaretStart();
                }
                else {
                  if (getMouseSelectionState() == MOUSE_SELECTION_STATE_LINE_SELECTED) {
                    newSelection = logicalPositionToOffset(visualToLogicalPosition(new VisualPosition(getCaretModel().getVisualPosition().line, 0)));
                  }
                }
                if (newSelection < 0) newSelection = newCaretOffset;
                mySelectionModel.setSelection(validateOffset(mySavedSelectionEnd), newSelection);
                getCaretModel().moveToOffset(newSelection);
              }
              else {
                int newSelection = newCaretOffset;
                if (getMouseSelectionState() == MOUSE_SELECTION_STATE_WORD_SELECTED) {
                  newSelection = myCaretModel.getWordAtCaretEnd();
                }
                else {
                  if (getMouseSelectionState() == MOUSE_SELECTION_STATE_LINE_SELECTED) {
                    newSelection = logicalPositionToOffset(visualToLogicalPosition(new VisualPosition(getCaretModel().getVisualPosition().line + 1, 0)));
                  }
                }
                if (newSelection < 0) newSelection = newCaretOffset;
                mySelectionModel.setSelection(validateOffset(mySavedSelectionStart), newSelection);
                getCaretModel().moveToOffset(newSelection);
              }
              return;
            }

            if (myMultiSelectionInProgress && myLastMousePressedLocation != null) {
              myTargetMultiSelectionPosition = pos;
              LogicalPosition newLogicalPosition = visualToLogicalPosition(pos);
              getScrollingModel().scrollTo(newLogicalPosition, ScrollType.RELATIVE);
              createSelectionTill(newLogicalPosition);
            }
            else {
              mySelectionModel.setSelection(oldSelectionStart, getCaretModel().getOffset());
            }
          }
        }, EditorBundle.message("move.cursor.command.name"), DocCommandGroupId.noneGroupId(getDocument()), UndoConfirmationPolicy.DEFAULT, getDocument());
      });
      myTimer.start();
    }

    private void stop() {
      if (myTimer != null) {
        myTimer.stop();
        myTimer = null;
      }
    }

    private int validateOffset(int offset) {
      if (offset < 0) return 0;
      if (offset > myDocument.getTextLength()) return myDocument.getTextLength();
      return offset;
    }
  }

  private class MyHorizontalScrollBar extends JBScrollBar {
    private MyHorizontalScrollBar(@JdkConstants.AdjustableOrientation int orientation) {
      super(orientation);
      UIUtil.putClientProperty(this, EditorColorKey.FUNCTION_KEY, key -> getColorsScheme().getColor(key));
    }
  }

  class MyVerticalScrollBar extends JBScrollBar implements IdeGlassPane.TopComponent {
    private Consumer<Graphics> myRepaintCallback;

    private MyVerticalScrollBar(@JdkConstants.AdjustableOrientation int orientation) {
      super(orientation);
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);
      if (myRepaintCallback != null) {
        myRepaintCallback.consume(g);
      }
    }

    @Override
    public int getUnitIncrement(int direction) {
      JViewport vp = myScrollPane.getViewport();
      Rectangle vr = vp.getViewRect();
      return myEditorComponent.getScrollableUnitIncrement(vr, SwingConstants.VERTICAL, direction);
    }

    @Override
    public int getBlockIncrement(int direction) {
      JViewport vp = myScrollPane.getViewport();
      Rectangle vr = vp.getViewRect();
      return myEditorComponent.getScrollableBlockIncrement(vr, SwingConstants.VERTICAL, direction);
    }

    private void registerRepaintCallback(@Nullable Consumer<Graphics> callback) {
      myRepaintCallback = callback;
    }
  }

  @Override
  public void setVerticalScrollbarOrientation(int type) {
    assertIsDispatchThread();
    if (myScrollBarOrientation == type) return;
    int currentHorOffset = myScrollingModel.getHorizontalScrollOffset();
    myScrollBarOrientation = type;
    if (type == VERTICAL_SCROLLBAR_LEFT) {
      myScrollPane.setLayout(new LeftHandScrollbarLayout());
    }
    else {
      myScrollPane.setLayout(new ScrollPaneLayout());
    }
    getMarkupModel().updateErrorStripePanel();
    myScrollingModel.scrollHorizontally(currentHorOffset);
  }

  @Override
  public void setVerticalScrollbarVisible(boolean b) {
    myScrollPane.setVerticalScrollBarPolicy(b ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
  }

  @Override
  public void setHorizontalScrollbarVisible(boolean b) {
    myScrollPane.setHorizontalScrollBarPolicy(b ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
  }

  @Override
  public int getVerticalScrollbarOrientation() {
    return myScrollBarOrientation;
  }

  public boolean isMirrored() {
    return myScrollBarOrientation != EditorEx.VERTICAL_SCROLLBAR_RIGHT;
  }

  @Nonnull
  public MyVerticalScrollBar getVerticalScrollBar() {
    return myVerticalScrollBar;
  }

  public JPanel getPanel() {
    return myPanel;
  }

  @Nonnull
  MyVerticalScrollBar getHorizontalScrollBar() {
    return (MyVerticalScrollBar)myScrollPane.getHorizontalScrollBar();
  }

  @MouseSelectionState
  private int getMouseSelectionState() {
    return myMouseSelectionState;
  }

  private void setMouseSelectionState(@MouseSelectionState int mouseSelectionState) {
    if (getMouseSelectionState() == mouseSelectionState) return;

    myMouseSelectionState = mouseSelectionState;
    myMouseSelectionChangeTimestamp = System.currentTimeMillis();

    myMouseSelectionStateAlarm.cancelAllRequests();
    if (myMouseSelectionState != MOUSE_SELECTION_STATE_NONE) {
      if (myMouseSelectionStateResetRunnable == null) {
        myMouseSelectionStateResetRunnable = () -> resetMouseSelectionState(null);
      }
      myMouseSelectionStateAlarm.addRequest(myMouseSelectionStateResetRunnable, Registry.intValue("editor.mouseSelectionStateResetTimeout"), ModalityState.stateForComponent(myEditorComponent));
    }
  }

  private void resetMouseSelectionState(@Nullable MouseEvent event) {
    setMouseSelectionState(MOUSE_SELECTION_STATE_NONE);

    MouseEvent e = event != null ? event : myMouseMovedEvent;
    if (e != null) {
      validateMousePointer(e);
    }
  }

  private void cancelAutoResetForMouseSelectionState() {
    myMouseSelectionStateAlarm.cancelAllRequests();
  }

  void replaceInputMethodText(@Nonnull InputMethodEvent e) {
    if (isReleased) return;
    getInputMethodRequests();
    myInputMethodRequestsHandler.replaceInputMethodText(e);
  }

  void inputMethodCaretPositionChanged(@Nonnull InputMethodEvent e) {
    if (isReleased) return;
    getInputMethodRequests();
    myInputMethodRequestsHandler.setInputMethodCaretPosition(e);
  }

  @Nonnull
  InputMethodRequests getInputMethodRequests() {
    if (myInputMethodRequestsHandler == null) {
      myInputMethodRequestsHandler = new MyInputMethodHandler();
      myInputMethodRequestsSwingWrapper = new MyInputMethodHandleSwingThreadWrapper(myInputMethodRequestsHandler);
    }
    return myInputMethodRequestsSwingWrapper;
  }

  @Override
  public boolean processKeyTyped(@Nonnull KeyEvent e) {
    myLastTypedActionTimestamp = -1;
    if (e.getID() != KeyEvent.KEY_TYPED) return false;
    char c = e.getKeyChar();
    if (UIUtil.isReallyTypedEvent(e)) { // Hack just like in javax.swing.text.DefaultEditorKit.DefaultKeyTypedAction
      myLastTypedActionTimestamp = e.getWhen();
      myLastTypedAction = Character.toString(c);
      processKeyTyped(c);
      return true;
    }
    else {
      return false;
    }
  }

  public void recordLatencyAwareAction(String actionId, long timestampMs) {
    myLastTypedActionTimestamp = timestampMs;
    myLastTypedAction = actionId;
  }

  void measureTypingLatency() {
    if (myLastTypedActionTimestamp != -1) {
      myLatencyPublisher.recordTypingLatency(this, myLastTypedAction, System.currentTimeMillis() - myLastTypedActionTimestamp);
      myLastTypedActionTimestamp = -1;
    }
  }

  public boolean isProcessingTypedAction() {
    return myLastTypedActionTimestamp != -1;
  }

  public void beforeModalityStateChanged() {
    ((DesktopScrollingModelImpl)myScrollingModel).beforeModalityStateChanged();
  }

  EditorDropHandler getDropHandler() {
    return myDropHandler;
  }

  public void setDropHandler(@Nonnull EditorDropHandler dropHandler) {
    myDropHandler = dropHandler;
  }

  private static class MyInputMethodHandleSwingThreadWrapper implements InputMethodRequests {
    private final InputMethodRequests myDelegate;

    private MyInputMethodHandleSwingThreadWrapper(InputMethodRequests delegate) {
      myDelegate = delegate;
    }

    @Nonnull
    @Override
    public Rectangle getTextLocation(final TextHitInfo offset) {
      return execute(() -> myDelegate.getTextLocation(offset));
    }

    @Override
    public TextHitInfo getLocationOffset(final int x, final int y) {
      return execute(() -> myDelegate.getLocationOffset(x, y));
    }

    @Override
    public int getInsertPositionOffset() {
      return execute(myDelegate::getInsertPositionOffset);
    }

    @Nonnull
    @Override
    public AttributedCharacterIterator getCommittedText(final int beginIndex, final int endIndex, final AttributedCharacterIterator.Attribute[] attributes) {
      return execute(() -> myDelegate.getCommittedText(beginIndex, endIndex, attributes));
    }

    @Override
    public int getCommittedTextLength() {
      return execute(myDelegate::getCommittedTextLength);
    }

    @Override
    @Nullable
    public AttributedCharacterIterator cancelLatestCommittedText(AttributedCharacterIterator.Attribute[] attributes) {
      return null;
    }

    @Override
    public AttributedCharacterIterator getSelectedText(final AttributedCharacterIterator.Attribute[] attributes) {
      return execute(() -> myDelegate.getSelectedText(attributes));
    }

    private static <T> T execute(final Computable<T> computable) {
      return UIUtil.invokeAndWaitIfNeeded(computable);
    }
  }

  private class MyInputMethodHandler implements InputMethodRequests {
    private String composedText;
    private ProperTextRange composedTextRange;

    @Nonnull
    @Override
    public Rectangle getTextLocation(TextHitInfo offset) {
      if (isDisposed()) return new Rectangle();
      Point caret = logicalPositionToXY(getCaretModel().getLogicalPosition());
      Rectangle r = new Rectangle(caret, new Dimension(1, getLineHeight()));
      Point p = getLocationOnScreen(getContentComponent());
      r.translate(p.x, p.y);
      return r;
    }

    @Override
    @Nullable
    public TextHitInfo getLocationOffset(int x, int y) {
      if (composedText != null) {
        Point p = getLocationOnScreen(getContentComponent());
        p.x = x - p.x;
        p.y = y - p.y;
        int pos = logicalPositionToOffset(xyToLogicalPosition(p));
        if (composedTextRange.containsOffset(pos)) {
          return TextHitInfo.leading(pos - composedTextRange.getStartOffset());
        }
      }
      return null;
    }

    private Point getLocationOnScreen(Component component) {
      Point location = new Point();
      SwingUtilities.convertPointToScreen(location, component);
      if (LOG.isDebugEnabled() && !component.isShowing()) {
        Class<?> type = component.getClass();
        Component parent = component.getParent();
        while (parent != null && !parent.isShowing()) {
          type = parent.getClass();
          parent = parent.getParent();
        }
        String message = type.getName() + " is not showing";
        if (parent != null) message += " on visible  " + parent.getClass().getName();
        LOG.debug(message);
      }
      return location;
    }

    @Override
    public int getInsertPositionOffset() {
      int composedStartIndex = 0;
      int composedEndIndex = 0;
      if (composedText != null) {
        composedStartIndex = composedTextRange.getStartOffset();
        composedEndIndex = composedTextRange.getEndOffset();
      }

      int caretIndex = getCaretModel().getOffset();

      if (caretIndex < composedStartIndex) {
        return caretIndex;
      }
      if (caretIndex < composedEndIndex) {
        return composedStartIndex;
      }
      return caretIndex - (composedEndIndex - composedStartIndex);
    }

    private String getText(int startIdx, int endIdx) {
      if (startIdx >= 0 && endIdx > startIdx) {
        CharSequence chars = getDocument().getImmutableCharSequence();
        return chars.subSequence(startIdx, endIdx).toString();
      }

      return "";
    }

    @Nonnull
    @Override
    public AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex, AttributedCharacterIterator.Attribute[] attributes) {
      int composedStartIndex = 0;
      int composedEndIndex = 0;
      if (composedText != null) {
        composedStartIndex = composedTextRange.getStartOffset();
        composedEndIndex = composedTextRange.getEndOffset();
      }

      String committed;
      if (beginIndex < composedStartIndex) {
        if (endIndex <= composedStartIndex) {
          committed = getText(beginIndex, endIndex - beginIndex);
        }
        else {
          int firstPartLength = composedStartIndex - beginIndex;
          committed = getText(beginIndex, firstPartLength) + getText(composedEndIndex, endIndex - beginIndex - firstPartLength);
        }
      }
      else {
        committed = getText(beginIndex + composedEndIndex - composedStartIndex, endIndex - beginIndex);
      }

      return new AttributedString(committed).getIterator();
    }

    @Override
    public int getCommittedTextLength() {
      int length = getDocument().getTextLength();
      if (composedText != null) {
        length -= composedText.length();
      }
      return length;
    }

    @Override
    @Nullable
    public AttributedCharacterIterator cancelLatestCommittedText(AttributedCharacterIterator.Attribute[] attributes) {
      return null;
    }

    @Override
    @Nullable
    public AttributedCharacterIterator getSelectedText(AttributedCharacterIterator.Attribute[] attributes) {
      if (myCharKeyPressed) {
        myNeedToSelectPreviousChar = true;
      }
      String text = getSelectionModel().getSelectedText();
      return text == null ? null : new AttributedString(text).getIterator();
    }

    private void createComposedString(int composedIndex, @Nonnull AttributedCharacterIterator text) {
      StringBuffer strBuf = new StringBuffer();

      // create attributed string with no attributes
      for (char c = text.setIndex(composedIndex); c != CharacterIterator.DONE; c = text.next()) {
        strBuf.append(c);
      }

      composedText = new String(strBuf);
    }

    private void setInputMethodCaretPosition(@Nonnull InputMethodEvent e) {
      if (composedText != null) {
        int dot = composedTextRange.getStartOffset();

        TextHitInfo caretPos = e.getCaret();
        if (caretPos != null) {
          dot += caretPos.getInsertionIndex();
        }

        getCaretModel().moveToOffset(dot);
        getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
      }
    }

    private void runUndoTransparent(@Nonnull final Runnable runnable) {
      CommandProcessor.getInstance().runUndoTransparentAction(() -> CommandProcessor.getInstance()
              .executeCommand(myProject, () -> ApplicationManager.getApplication().runWriteAction(runnable), "", getDocument(), UndoConfirmationPolicy.DEFAULT, getDocument()));
    }

    private boolean hasRelevantCommittedText(@Nonnull InputMethodEvent e) {
      if (e.getCommittedCharacterCount() <= 0) return false;
      AttributedCharacterIterator text = e.getText();
      return text == null || text.first() != 0xA5 /* Yen character */;
    }

    private void replaceInputMethodText(@Nonnull InputMethodEvent e) {
      if (myNeedToSelectPreviousChar && SystemInfo.isMac &&
          (Registry.is("ide.mac.pressAndHold.brute.workaround") || Registry.is("ide.mac.pressAndHold.workaround") && (hasRelevantCommittedText(e) || e.getCaret() == null))) {
        // This is required to support input of accented characters using press-and-hold method (http://support.apple.com/kb/PH11264).
        // JDK currently properly supports this functionality only for TextComponent/JTextComponent descendants.
        // For our editor component we need this workaround.
        // After https://bugs.openjdk.java.net/browse/JDK-8074882 is fixed, this workaround should be replaced with a proper solution.
        myNeedToSelectPreviousChar = false;
        getCaretModel().runForEachCaret(caret -> {
          int caretOffset = caret.getOffset();
          if (caretOffset > 0) {
            caret.setSelection(caretOffset - 1, caretOffset);
          }
        });
      }

      int commitCount = e.getCommittedCharacterCount();
      AttributedCharacterIterator text = e.getText();

      // old composed text deletion
      final Document doc = getDocument();

      if (composedText != null) {
        if (!isViewer() && doc.isWritable()) {
          runUndoTransparent(() -> {
            int docLength = doc.getTextLength();
            ProperTextRange range = composedTextRange.intersection(new TextRange(0, docLength));
            if (range != null) {
              doc.deleteString(range.getStartOffset(), range.getEndOffset());
            }
          });
        }
        composedText = null;
      }

      if (text != null) {
        text.first();

        // committed text insertion
        if (commitCount > 0) {
          for (char c = text.current(); commitCount > 0; c = text.next(), commitCount--) {
            if (c >= 0x20 && c != 0x7F) { // Hack just like in javax.swing.text.DefaultEditorKit.DefaultKeyTypedAction
              processKeyTyped(c);
            }
          }
        }

        // new composed text insertion
        if (!isViewer() && doc.isWritable()) {
          int composedTextIndex = text.getIndex();
          if (composedTextIndex < text.getEndIndex()) {
            createComposedString(composedTextIndex, text);

            runUndoTransparent(() -> EditorModificationUtil.insertStringAtCaret(DesktopEditorImpl.this, composedText, false, false));

            composedTextRange = ProperTextRange.from(getCaretModel().getOffset(), composedText.length());
          }
        }
      }
    }
  }

  private class MyMouseAdapter extends MouseAdapter {
    @Override
    public void mousePressed(@Nonnull MouseEvent e) {
      requestFocus();
      runMousePressedCommand(e);
    }

    @Override
    public void mouseReleased(@Nonnull MouseEvent e) {
      myMousePressArea = null;
      myLastMousePressedLocation = null;
      runMouseReleasedCommand(e);
      if (!e.isConsumed() && myMousePressedEvent != null && !myMousePressedEvent.isConsumed() &&
          Math.abs(e.getX() - myMousePressedEvent.getX()) < EditorUtil.getSpaceWidth(Font.PLAIN, DesktopEditorImpl.this) &&
          Math.abs(e.getY() - myMousePressedEvent.getY()) < getLineHeight()) {
        runMouseClickedCommand(e);
      }
    }

    @Override
    public void mouseEntered(@Nonnull MouseEvent e) {
      runMouseEnteredCommand(e);
    }

    @Override
    public void mouseExited(@Nonnull MouseEvent e) {
      runMouseExitedCommand(e);
      EditorMouseEvent event = new EditorMouseEvent(DesktopEditorImpl.this, e, getMouseEventArea(e));
      if (event.getArea() == EditorMouseEventArea.LINE_MARKERS_AREA) {
        myGutterComponent.mouseExited(e);
      }
    }

    private void runMousePressedCommand(@Nonnull final MouseEvent e) {
      myLastMousePressedLocation = xyToLogicalPosition(e.getPoint());
      myCaretStateBeforeLastPress = isToggleCaretEvent(e) ? myCaretModel.getCaretsAndSelections() : Collections.emptyList();
      myCurrentDragIsSubstantial = false;
      myDragStarted = false;
      clearDnDContext();

      boolean forceProcessing = false;
      myMousePressedEvent = e;
      EditorMouseEvent event = new EditorMouseEvent(DesktopEditorImpl.this, e, getMouseEventArea(e));

      myExpectedCaretOffset = logicalPositionToOffset(myLastMousePressedLocation);
      try {
        for (EditorMouseListener mouseListener : myMouseListeners) {
          boolean wasConsumed = event.isConsumed();
          mouseListener.mousePressed(event);
          //noinspection deprecation
          if (!wasConsumed && event.isConsumed() && mouseListener instanceof com.intellij.util.EditorPopupHandler) {
            // compatibility with legacy code, this logic should be removed along with EditorPopupHandler
            forceProcessing = true;
          }
          if (isReleased) return;
        }
      }
      finally {
        myExpectedCaretOffset = -1;
      }

      if (event.getArea() == EditorMouseEventArea.LINE_MARKERS_AREA || event.getArea() == EditorMouseEventArea.FOLDING_OUTLINE_AREA && !isInsideGutterWhitespaceArea(e)) {
        myDragOnGutterSelectionStartLine = EditorUtil.yPositionToLogicalLine(DesktopEditorImpl.this, e);
      }

      if (event.isConsumed() && !forceProcessing) return;

      if (myCommandProcessor != null) {
        Runnable runnable = () -> {
          if (processMousePressed(e) && myProject != null && !myProject.isDefault()) {
            IdeDocumentHistory.getInstance(myProject).includeCurrentCommandAsNavigation();
          }
        };
        myCommandProcessor.executeCommand(myProject, runnable, "", DocCommandGroupId.noneGroupId(getDocument()), UndoConfirmationPolicy.DEFAULT, getDocument());
      }
      else {
        processMousePressed(e);
      }

      invokePopupIfNeeded(event);
    }

    private void runMouseClickedCommand(@Nonnull final MouseEvent e) {
      EditorMouseEvent event = new EditorMouseEvent(DesktopEditorImpl.this, e, getMouseEventArea(e));
      for (EditorMouseListener listener : myMouseListeners) {
        listener.mouseClicked(event);
        if (isReleased) return;
        if (event.isConsumed()) {
          e.consume();
          return;
        }
      }
    }

    private void runMouseReleasedCommand(@Nonnull final MouseEvent e) {
      myMultiSelectionInProgress = false;
      myDragOnGutterSelectionStartLine = -1;
      myScrollingTimer.stop();

      if (e.isConsumed()) {
        return;
      }

      EditorMouseEvent event = new EditorMouseEvent(DesktopEditorImpl.this, e, getMouseEventArea(e));
      for (EditorMouseListener listener : myMouseListeners) {
        listener.mouseReleased(event);
        if (isReleased || event.isConsumed()) {
          return;
        }
      }

      invokePopupIfNeeded(event);
      if (event.isConsumed()) {
        return;
      }

      if (myCommandProcessor != null) {
        Runnable runnable = () -> processMouseReleased(e);
        myCommandProcessor.executeCommand(myProject, runnable, "", DocCommandGroupId.noneGroupId(getDocument()), UndoConfirmationPolicy.DEFAULT, getDocument());
      }
      else {
        processMouseReleased(e);
      }
    }

    private void runMouseEnteredCommand(@Nonnull MouseEvent e) {
      EditorMouseEvent event = new EditorMouseEvent(DesktopEditorImpl.this, e, getMouseEventArea(e));
      for (EditorMouseListener listener : myMouseListeners) {
        listener.mouseEntered(event);
        if (isReleased) return;
        if (event.isConsumed()) {
          e.consume();
          return;
        }
      }
    }

    private void runMouseExitedCommand(@Nonnull MouseEvent e) {
      EditorMouseEvent event = new EditorMouseEvent(DesktopEditorImpl.this, e, getMouseEventArea(e));
      for (EditorMouseListener listener : myMouseListeners) {
        listener.mouseExited(event);
        if (isReleased) return;
        if (event.isConsumed()) {
          e.consume();
          return;
        }
      }
    }

    private boolean processMousePressed(@Nonnull final MouseEvent e) {
      myInitialMouseEvent = e;

      if (myMouseSelectionState != MOUSE_SELECTION_STATE_NONE && System.currentTimeMillis() - myMouseSelectionChangeTimestamp > Registry.intValue("editor.mouseSelectionStateResetTimeout")) {
        resetMouseSelectionState(e);
      }

      int x = e.getX();
      int y = e.getY();

      if (x < 0) x = 0;
      if (y < 0) y = 0;

      final EditorMouseEventArea eventArea = getMouseEventArea(e);
      myMousePressArea = eventArea;
      if (eventArea == EditorMouseEventArea.FOLDING_OUTLINE_AREA) {
        final FoldRegion range = myGutterComponent.findFoldingAnchorAt(x, y);
        if (range != null) {
          final boolean expansion = !range.isExpanded();

          int scrollShift = y - getScrollingModel().getVerticalScrollOffset();
          Runnable processor = () -> {
            myFoldingModel.disableScrollingPositionAdjustment();
            range.setExpanded(expansion);
            if (e.isAltDown()) {
              for (FoldRegion region : myFoldingModel.getAllFoldRegions()) {
                if (region.getStartOffset() >= range.getStartOffset() && region.getEndOffset() <= range.getEndOffset()) {
                  region.setExpanded(expansion);
                }
              }
            }
          };
          getFoldingModel().runBatchFoldingOperation(processor);
          y = myGutterComponent.getHeadCenterY(range);
          getScrollingModel().scrollVertically(y - scrollShift);
          myGutterComponent.updateSize();
          validateMousePointer(e);
          e.consume();
          return false;
        }
      }

      if (e.getSource() == myGutterComponent) {
        if (eventArea == EditorMouseEventArea.LINE_MARKERS_AREA || eventArea == EditorMouseEventArea.ANNOTATIONS_AREA || eventArea == EditorMouseEventArea.LINE_NUMBERS_AREA) {
          if (!tweakSelectionIfNecessary(e)) {
            myGutterComponent.mousePressed(e);
          }
          if (e.isConsumed()) return false;
        }
        x = 0;
      }

      int oldSelectionStart = mySelectionModel.getLeadSelectionOffset();

      final int oldStart = mySelectionModel.getSelectionStart();
      final int oldEnd = mySelectionModel.getSelectionEnd();

      boolean toggleCaret = e.getSource() != myGutterComponent && isToggleCaretEvent(e);
      boolean lastPressCreatedCaret = myLastPressCreatedCaret;
      if (e.getClickCount() == 1) {
        myLastPressCreatedCaret = false;
      }
      // Don't move caret on mouse press above gutter line markers area (a place where break points, 'override', 'implements' etc icons
      // are drawn) and annotations area. E.g. we don't want to change caret position if a user sets new break point (clicks
      // at 'line markers' area).
      boolean insideEditorRelatedAreas = eventArea == EditorMouseEventArea.LINE_NUMBERS_AREA || eventArea == EditorMouseEventArea.EDITING_AREA || isInsideGutterWhitespaceArea(e);
      if (insideEditorRelatedAreas) {
        VisualPosition visualPosition = getTargetPosition(x, y, true);
        LogicalPosition pos = visualToLogicalPosition(visualPosition);
        if (toggleCaret) {
          Caret caret = getCaretModel().getCaretAt(visualPosition);
          if (e.getClickCount() == 1) {
            if (caret == null) {
              myLastPressCreatedCaret = getCaretModel().addCaret(visualPosition) != null;
            }
            else {
              getCaretModel().removeCaret(caret);
            }
          }
          else if (e.getClickCount() == 3 && lastPressCreatedCaret) {
            getCaretModel().moveToVisualPosition(visualPosition);
          }
        }
        else if (e.getSource() != myGutterComponent && isCreateRectangularSelectionEvent(e)) {
          CaretState anchorCaretState = myCaretModel.getCaretsAndSelections().get(0);
          LogicalPosition anchor = Objects.equals(anchorCaretState.getCaretPosition(), anchorCaretState.getSelectionStart()) ? anchorCaretState.getSelectionEnd() : anchorCaretState.getSelectionStart();
          if (anchor == null) anchor = myCaretModel.getLogicalPosition();
          mySelectionModel.setBlockSelection(anchor, pos);
        }
        else {
          getCaretModel().removeSecondaryCarets();
          getCaretModel().moveToVisualPosition(visualPosition);
        }
      }

      if (e.isPopupTrigger()) return false;

      requestFocus();

      int caretOffset = getCaretModel().getOffset();

      int newStart = mySelectionModel.getSelectionStart();
      int newEnd = mySelectionModel.getSelectionEnd();

      myMouseSelectedRegion = myFoldingModel.getFoldingPlaceholderAt(new Point(x, y));
      myKeepSelectionOnMousePress = mySelectionModel.hasSelection() &&
                                    caretOffset >= mySelectionModel.getSelectionStart() &&
                                    caretOffset <= mySelectionModel.getSelectionEnd() &&
                                    (SwingUtilities.isLeftMouseButton(e) && mySettings.isDndEnabled() || SwingUtilities.isRightMouseButton(e));

      boolean isNavigation = oldStart == oldEnd && newStart == newEnd && oldStart != newStart;
      if (getMouseEventArea(e) == EditorMouseEventArea.LINE_NUMBERS_AREA && e.getClickCount() == 1) {
        mySelectionModel.selectLineAtCaret();
        setMouseSelectionState(MOUSE_SELECTION_STATE_LINE_SELECTED);
        mySavedSelectionStart = mySelectionModel.getSelectionStart();
        mySavedSelectionEnd = mySelectionModel.getSelectionEnd();
        return isNavigation;
      }

      if (insideEditorRelatedAreas) {
        if (e.isShiftDown() && !e.isControlDown() && !e.isAltDown()) {
          if (getMouseSelectionState() != MOUSE_SELECTION_STATE_NONE) {
            if (caretOffset < mySavedSelectionStart) {
              mySelectionModel.setSelection(mySavedSelectionEnd, caretOffset);
            }
            else {
              mySelectionModel.setSelection(mySavedSelectionStart, caretOffset);
            }
          }
          else {
            int startToUse = oldSelectionStart;
            if (mySelectionModel.isUnknownDirection() && caretOffset > startToUse) {
              startToUse = Math.min(oldStart, oldEnd);
            }
            mySelectionModel.setSelection(startToUse, caretOffset);
          }
        }
        else {
          if (!myKeepSelectionOnMousePress && getSelectionModel().hasSelection() && !isCreateRectangularSelectionEvent(e) && e.getClickCount() == 1) {
            setMouseSelectionState(MOUSE_SELECTION_STATE_NONE);
            mySelectionModel.setSelection(caretOffset, caretOffset);
          }
          else {
            if (e.getButton() == MouseEvent.BUTTON1 && (eventArea == EditorMouseEventArea.EDITING_AREA || eventArea == EditorMouseEventArea.LINE_NUMBERS_AREA) && (!toggleCaret || lastPressCreatedCaret)) {
              switch (e.getClickCount()) {
                case 2:
                  selectWordAtCaret(mySettings.isMouseClickSelectionHonorsCamelWords() && mySettings.isCamelWords());
                  break;

                case 3:
                  if (HONOR_CAMEL_HUMPS_ON_TRIPLE_CLICK && mySettings.isCamelWords()) {
                    // We want to differentiate between triple and quadruple clicks when 'select by camel humps' is on. The former
                    // is assumed to select 'hump' while the later points to the whole word.
                    selectWordAtCaret(false);
                    break;
                  }
                case 4:
                  mySelectionModel.selectLineAtCaret();
                  setMouseSelectionState(MOUSE_SELECTION_STATE_LINE_SELECTED);
                  mySavedSelectionStart = mySelectionModel.getSelectionStart();
                  mySavedSelectionEnd = mySelectionModel.getSelectionEnd();
                  mySelectionModel.setUnknownDirection(true);
                  break;
              }
            }
          }
        }
      }

      return isNavigation;
    }
  }

  private static boolean isColumnSelectionDragEvent(@Nonnull MouseEvent e) {
    return isMouseActionEvent(e, IdeActions.ACTION_EDITOR_CREATE_RECTANGULAR_SELECTION_ON_MOUSE_DRAG);
  }

  private static boolean isToggleCaretEvent(@Nonnull MouseEvent e) {
    return isMouseActionEvent(e, IdeActions.ACTION_EDITOR_ADD_OR_REMOVE_CARET) || isAddRectangularSelectionEvent(e);
  }

  private static boolean isAddRectangularSelectionEvent(@Nonnull MouseEvent e) {
    return isMouseActionEvent(e, IdeActions.ACTION_EDITOR_ADD_RECTANGULAR_SELECTION_ON_MOUSE_DRAG);
  }

  private static boolean isCreateRectangularSelectionEvent(@Nonnull MouseEvent e) {
    return isMouseActionEvent(e, IdeActions.ACTION_EDITOR_CREATE_RECTANGULAR_SELECTION);
  }

  private static boolean isMouseActionEvent(@Nonnull MouseEvent e, String actionId) {
    KeymapManager keymapManager = KeymapManager.getInstance();
    if (keymapManager == null) return false;
    Keymap keymap = keymapManager.getActiveKeymap();
    MouseShortcut mouseShortcut = KeymapUtil.createMouseShortcut(e);
    String[] mappedActions = keymap.getActionIds(mouseShortcut);
    if (!ArrayUtil.contains(actionId, mappedActions)) return false;
    if (mappedActions.length < 2 || e.getID() == MouseEvent.MOUSE_DRAGGED /* 'normal' actions are not invoked on mouse drag */) return true;
    ActionManager actionManager = ActionManager.getInstance();
    for (String mappedActionId : mappedActions) {
      if (actionId.equals(mappedActionId)) continue;
      AnAction action = actionManager.getAction(mappedActionId);
      AnActionEvent actionEvent = AnActionEvent.createFromAnAction(action, e, ActionPlaces.MAIN_MENU, DataManager.getInstance().getDataContext(e.getComponent()));
      if (ActionUtil.lastUpdateAndCheckDumb(action, actionEvent, false)) return false;
    }
    return true;
  }

  private void selectWordAtCaret(boolean honorCamelCase) {
    mySelectionModel.selectWordAtCaret(honorCamelCase);
    setMouseSelectionState(MOUSE_SELECTION_STATE_WORD_SELECTED);
    mySavedSelectionStart = mySelectionModel.getSelectionStart();
    mySavedSelectionEnd = mySelectionModel.getSelectionEnd();
    getCaretModel().moveToOffset(mySavedSelectionEnd);
  }

  /**
   * Allows to answer if given event should tweak editor selection.
   *
   * @param e event for occurred mouse action
   * @return {@code true} if action that produces given event will trigger editor selection change; {@code false} otherwise
   */
  private boolean tweakSelectionEvent(@Nonnull MouseEvent e) {
    return getSelectionModel().hasSelection() && e.getButton() == MouseEvent.BUTTON1 && e.isShiftDown() && getMouseEventArea(e) == EditorMouseEventArea.LINE_NUMBERS_AREA;
  }

  /**
   * Checks if editor selection should be changed because of click at the given point at gutter and proceeds if necessary.
   * <p/>
   * The main idea is that selection can be changed during left mouse clicks on the gutter line numbers area with hold
   * {@code Shift} button. The selection should be adjusted if necessary.
   *
   * @param e event for mouse click on gutter area
   * @return {@code true} if editor's selection is changed because of the click; {@code false} otherwise
   */
  private boolean tweakSelectionIfNecessary(@Nonnull MouseEvent e) {
    if (!tweakSelectionEvent(e)) {
      return false;
    }

    int startSelectionOffset = getSelectionModel().getSelectionStart();
    int startVisLine = offsetToVisualLine(startSelectionOffset);

    int endSelectionOffset = getSelectionModel().getSelectionEnd();
    int endVisLine = offsetToVisualLine(endSelectionOffset - 1);

    int clickVisLine = yToVisualLine(e.getPoint().y);

    if (clickVisLine < startVisLine) {
      // Expand selection at backward direction.
      int startOffset = logicalPositionToOffset(visualToLogicalPosition(new VisualPosition(clickVisLine, 0)));
      getSelectionModel().setSelection(startOffset, endSelectionOffset);
      getCaretModel().moveToOffset(startOffset);
    }
    else if (clickVisLine > endVisLine) {
      // Expand selection at forward direction.
      int endLineOffset = EditorUtil.getVisualLineEndOffset(this, clickVisLine);
      getSelectionModel().setSelection(getSelectionModel().getSelectionStart(), endLineOffset);
      getCaretModel().moveToOffset(endLineOffset, true);
    }
    else if (startVisLine == endVisLine) {
      // Remove selection
      getSelectionModel().removeSelection();
    }
    else {
      // Reduce selection in backward direction.
      if (getSelectionModel().getLeadSelectionOffset() == endSelectionOffset) {
        if (clickVisLine == startVisLine) {
          clickVisLine++;
        }
        int startOffset = logicalPositionToOffset(visualToLogicalPosition(new VisualPosition(clickVisLine, 0)));
        getSelectionModel().setSelection(startOffset, endSelectionOffset);
        getCaretModel().moveToOffset(startOffset);
      }
      else {
        // Reduce selection is forward direction.
        if (clickVisLine == endVisLine) {
          clickVisLine--;
        }
        int endLineOffset = EditorUtil.getVisualLineEndOffset(this, clickVisLine);
        getSelectionModel().setSelection(startSelectionOffset, endLineOffset);
        getCaretModel().moveToOffset(endLineOffset);
      }
    }
    e.consume();
    return true;
  }

  boolean useEditorAntialiasing() {
    return myUseEditorAntialiasing;
  }

  public void setUseEditorAntialiasing(boolean value) {
    myUseEditorAntialiasing = value;
  }

  private class MyMouseMotionListener implements MouseMotionListener {
    @Override
    public void mouseDragged(@Nonnull MouseEvent e) {
      if (myDraggedRange != null || myGutterComponent.myDnDInProgress) return; // on Mac we receive events even if drag-n-drop is in progress
      validateMousePointer(e);
      ((TransactionGuardEx)TransactionGuard.getInstance()).performUserActivity(() -> runMouseDraggedCommand(e));
      EditorMouseEvent event = new EditorMouseEvent(DesktopEditorImpl.this, e, getMouseEventArea(e));
      if (event.getArea() == EditorMouseEventArea.LINE_MARKERS_AREA) {
        myGutterComponent.mouseDragged(e);
      }

      for (EditorMouseMotionListener listener : myMouseMotionListeners) {
        listener.mouseDragged(event);
        if (isReleased) return;
      }
    }

    @Override
    public void mouseMoved(@Nonnull MouseEvent e) {
      if (getMouseSelectionState() != MOUSE_SELECTION_STATE_NONE) {
        if (myMousePressedEvent != null && myMousePressedEvent.getComponent() == e.getComponent()) {
          Point lastPoint = myMousePressedEvent.getPoint();
          Point point = e.getPoint();
          int deadZone = Registry.intValue("editor.mouseSelectionStateResetDeadZone");
          if (Math.abs(lastPoint.x - point.x) >= deadZone || Math.abs(lastPoint.y - point.y) >= deadZone) {
            resetMouseSelectionState(e);
          }
        }
        else {
          validateMousePointer(e);
        }
      }
      else {
        validateMousePointer(e);
      }

      myMouseMovedEvent = e;

      EditorMouseEvent event = new EditorMouseEvent(DesktopEditorImpl.this, e, getMouseEventArea(e));
      if (e.getSource() == myGutterComponent) {
        myGutterComponent.mouseMoved(e);
      }

      for (EditorMouseMotionListener listener : myMouseMotionListeners) {
        listener.mouseMoved(event);
        if (isReleased) return;
      }
    }
  }

  private static class ExplosionPainter extends AbstractPainter {
    private final Point myExplosionLocation;
    private final Image myImage;
    @Nonnull
    private final Disposable myPainterListenersDisposable;

    private static final long TIME_PER_FRAME = 30;
    private final int myWidth;
    private final int myHeight;
    private long lastRepaintTime = System.currentTimeMillis();
    private int frameIndex;
    private static final int TOTAL_FRAMES = 8;

    private final AtomicBoolean nrp = new AtomicBoolean(true);

    ExplosionPainter(final Point explosionLocation, Image image, @Nonnull Disposable painterListenersDisposable) {
      myExplosionLocation = new Point(explosionLocation.x, explosionLocation.y);
      myImage = image;
      myPainterListenersDisposable = painterListenersDisposable;
      myWidth = myImage.getWidth(null);
      myHeight = myImage.getHeight(null);
    }

    @Override
    public void executePaint(Component component, Graphics2D g) {
      if (!nrp.get()) return;

      long currentTimeMillis = System.currentTimeMillis();

      float alpha = 1 - (float)frameIndex / TOTAL_FRAMES;
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
      Image scaledImage = ImageUtil.scaleImage(myImage, alpha);

      int x = myExplosionLocation.x - scaledImage.getWidth(null) / 2;
      int y = myExplosionLocation.y - scaledImage.getHeight(null) / 2;

      if (currentTimeMillis - lastRepaintTime < TIME_PER_FRAME) {
        UIUtil.drawImage(g, scaledImage, x, y, null);
        EdtExecutorService.getScheduledExecutorInstance().schedule(() -> component.repaint(x, y, myWidth, myHeight), TIME_PER_FRAME, TimeUnit.MILLISECONDS);
        return;
      }
      lastRepaintTime = currentTimeMillis;
      frameIndex++;
      UIUtil.drawImage(g, scaledImage, x, y, null);
      if (frameIndex == TOTAL_FRAMES) {
        nrp.set(false);
        ApplicationManager.getApplication().invokeLater(() -> Disposer.dispose(myPainterListenersDisposable));
        component.repaint(x, y, myWidth, myHeight);
      }
      component.repaint(x, y, myWidth, myHeight);
    }

    @Override
    public boolean needsRepaint() {
      return nrp.get();
    }

  }

  static boolean handleDrop(@Nonnull DesktopEditorImpl editor, @Nonnull final Transferable t, int dropAction) {
    final EditorDropHandler dropHandler = editor.getDropHandler();

    if (Registry.is("debugger.click.disable.breakpoints")) {
      try {
        if (t.isDataFlavorSupported(GutterDraggableObject.flavor)) {
          Object attachedObject = t.getTransferData(GutterDraggableObject.flavor);
          if (attachedObject instanceof GutterIconRenderer) {
            GutterDraggableObject object = ((GutterIconRenderer)attachedObject).getDraggableObject();
            if (object != null) {
              object.remove();
              Point mouseLocationOnScreen = MouseInfo.getPointerInfo().getLocation();
              JComponent editorComponent = editor.getComponent();
              Point editorComponentLocationOnScreen = editorComponent.getLocationOnScreen();
              Disposable painterListenersDisposable = Disposable.newDisposable("PainterListenersDisposable");
              Disposer.register(editor.getDisposable(), painterListenersDisposable);
              ExplosionPainter painter = new ExplosionPainter(new Point(mouseLocationOnScreen.x - editorComponentLocationOnScreen.x, mouseLocationOnScreen.y - editorComponentLocationOnScreen.y),
                                                              editor.getGutterComponentEx().getDragImage((GutterIconRenderer)attachedObject), painterListenersDisposable);
              IdeGlassPaneUtil.installPainter(editorComponent, painter, painterListenersDisposable);
              return true;
            }
          }
        }
      }
      catch (UnsupportedFlavorException | IOException e) {
        LOG.warn(e);
      }
    }

    if (dropHandler != null && dropHandler.canHandleDrop(t.getTransferDataFlavors())) {
      dropHandler.handleDrop(t, editor.getProject(), null, dropAction);
      return true;
    }

    final int caretOffset = editor.getCaretModel().getOffset();
    if (editor.myDraggedRange != null && editor.myDraggedRange.getStartOffset() <= caretOffset && caretOffset < editor.myDraggedRange.getEndOffset()) {
      return false;
    }

    if (editor.myDraggedRange != null) {
      editor.getCaretModel().moveToOffset(editor.mySavedCaretOffsetForDNDUndoHack);
    }

    CommandProcessor.getInstance().executeCommand(editor.myProject, () -> {
      try {
        editor.getSelectionModel().removeSelection();

        final int offset;
        if (editor.myDraggedRange != null) {
          editor.getCaretModel().moveToOffset(caretOffset);
          offset = caretOffset;
        }
        else {
          offset = editor.getCaretModel().getOffset();
        }
        if (editor.getDocument().getRangeGuard(offset, offset) != null) {
          return;
        }

        editor.putUserData(LAST_PASTED_REGION, null);

        EditorActionHandler pasteHandler = EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_PASTE);
        LOG.assertTrue(pasteHandler instanceof EditorTextInsertHandler, String.valueOf(pasteHandler));
        ((EditorTextInsertHandler)pasteHandler).execute(editor, editor.getDataContext(), () -> t);

        TextRange range = editor.getUserData(LAST_PASTED_REGION);
        if (range != null) {
          editor.getCaretModel().moveToOffset(range.getStartOffset());
          editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
        }
      }
      catch (Exception exception) {
        LOG.error(exception);
      }
    }, EditorBundle.message("paste.command.name"), DND_COMMAND_KEY, UndoConfirmationPolicy.DEFAULT, editor.getDocument());

    return true;
  }

  private static class MyTransferHandler extends TransferHandler {
    private static DesktopEditorImpl getEditor(@Nonnull JComponent comp) {
      EditorComponentImpl editorComponent = (EditorComponentImpl)comp;
      return editorComponent.getEditor();
    }

    @Override
    public boolean importData(TransferSupport support) {
      Component comp = support.getComponent();
      return comp instanceof JComponent && handleDrop(getEditor((JComponent)comp), support.getTransferable(), support.getDropAction());
    }

    @Override
    public boolean canImport(@Nonnull JComponent comp, @Nonnull DataFlavor[] transferFlavors) {
      DesktopEditorImpl editor = getEditor(comp);
      final EditorDropHandler dropHandler = editor.getDropHandler();
      if (dropHandler != null && dropHandler.canHandleDrop(transferFlavors)) {
        return true;
      }

      //should be used a better representation class
      if (Registry.is("debugger.click.disable.breakpoints") && ArrayUtil.contains(GutterDraggableObject.flavor, transferFlavors)) {
        return true;
      }

      if (editor.isViewer()) return false;

      int offset = editor.getCaretModel().getOffset();
      if (editor.getDocument().getRangeGuard(offset, offset) != null) return false;

      return ArrayUtil.contains(DataFlavor.stringFlavor, transferFlavors);
    }

    @Override
    @Nullable
    protected Transferable createTransferable(JComponent c) {
      DesktopEditorImpl editor = getEditor(c);
      String s = editor.getSelectionModel().getSelectedText();
      if (s == null) return null;
      int selectionStart = editor.getSelectionModel().getSelectionStart();
      int selectionEnd = editor.getSelectionModel().getSelectionEnd();
      editor.myDraggedRange = editor.getDocument().createRangeMarker(selectionStart, selectionEnd);

      return new StringSelection(s);
    }

    @Override
    public int getSourceActions(@Nonnull JComponent c) {
      return COPY_OR_MOVE;
    }

    @Override
    protected void exportDone(@Nonnull final JComponent source, @Nullable Transferable data, int action) {
      if (data == null) return;

      final Component last = DnDManager.getInstance().getLastDropHandler();

      if (last != null && !(last instanceof EditorComponentImpl) && !(last instanceof EditorGutterComponentImpl)) return;

      final DesktopEditorImpl editor = getEditor(source);
      if (action == MOVE && !editor.isViewer() && editor.myDraggedRange != null) {
        ((TransactionGuardEx)TransactionGuard.getInstance()).performUserActivity(() -> removeDraggedOutFragment(editor));
      }

      editor.clearDnDContext();
    }

    private static void removeDraggedOutFragment(DesktopEditorImpl editor) {
      if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), editor.getProject())) {
        return;
      }
      CommandProcessor.getInstance().executeCommand(editor.myProject, () -> ApplicationManager.getApplication().runWriteAction(() -> {
        Document doc = editor.getDocument();
        doc.startGuardedBlockChecking();
        try {
          doc.deleteString(editor.myDraggedRange.getStartOffset(), editor.myDraggedRange.getEndOffset());
        }
        catch (ReadOnlyFragmentModificationException e) {
          EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(doc).handle(e);
        }
        finally {
          doc.stopGuardedBlockChecking();
        }
      }), EditorBundle.message("move.selection.command.name"), DND_COMMAND_KEY, UndoConfirmationPolicy.DEFAULT, editor.getDocument());
    }
  }

  @Override
  @Nonnull
  public EditorGutter getGutter() {
    return getGutterComponentEx();
  }

  public boolean isInDistractionFreeMode() {
    return EditorUtil.isRealFileEditor(this) && (Registry.is("editor.distraction.free.mode") || isInPresentationMode());
  }

  boolean isInPresentationMode() {
    return UISettings.getInstance().getPresentationMode() && EditorUtil.isRealFileEditor(this);
  }

  @Override
  public void codeStyleSettingsChanged(@Nonnull CodeStyleSettingsChangeEvent event) {
    if (myProject != null) {
      if (event.getPsiFile() != null) {
        PsiFile editorFile = PsiDocumentManager.getInstance(myProject).getCachedPsiFile(getDocument());
        if (editorFile != event.getPsiFile()) return;
      }
      int oldTabSize = EditorUtil.getTabSize(this);
      mySettings.reinitSettings();
      int newTabSize = EditorUtil.getTabSize(this);
      if (oldTabSize != newTabSize) {
        reinitSettings(false);
      }
      else {
        // cover the case of right margin update
        myEditorComponent.repaint();
      }
    }
  }

  @TestOnly
  void validateState() {
    myView.validateState();
    mySoftWrapModel.validateState();
    myFoldingModel.validateState();
    myCaretModel.validateState();
    myInlayModel.validateState();
  }

  private class MyScrollPane extends JBScrollPane {
    private MyScrollPane() {
      super(0);
      setupCorners();
    }

    @Override
    public void setUI(ScrollPaneUI ui) {
      super.setUI(ui);
      // disable standard Swing keybindings
      setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
    }

    @Override
    public void layout() {
      if (isInDistractionFreeMode()) {
        // re-calc gutter extra size after editor size is set
        // & layout once again to avoid blinking
        myGutterComponent.updateSize(true, true);
      }
      super.layout();
    }

    @Override
    protected void processMouseWheelEvent(@Nonnull MouseWheelEvent e) {
      if (mySettings.isWheelFontChangeEnabled()) {
        if (EditorUtil.isChangeFontSize(e)) {
          int size = myScheme.getEditorFontSize() - e.getWheelRotation();
          if (size >= MIN_FONT_SIZE) {
            setFontSize(size, SwingUtilities.convertPoint(this, e.getPoint(), getViewport()));
          }
          return;
        }
      }

      super.processMouseWheelEvent(e);
    }

    @Nonnull
    @Override
    public JScrollBar createHorizontalScrollBar() {
      return new MyHorizontalScrollBar(Adjustable.HORIZONTAL);
    }

    @Nonnull
    @Override
    public JScrollBar createVerticalScrollBar() {
      return new MyVerticalScrollBar(Adjustable.VERTICAL);
    }

    @Override
    protected void setupCorners() {
      super.setupCorners();
      setBorder(new TablessBorder());
    }
  }

  private class TablessBorder extends SideBorder {
    private TablessBorder() {
      super(JBColor.border(), SideBorder.ALL);
    }

    @Override
    public void paintBorder(@Nonnull Component c, @Nonnull Graphics g, int x, int y, int width, int height) {
      if (c instanceof JComponent) {
        Insets insets = ((JComponent)c).getInsets();
        if (insets.left > 0) {
          super.paintBorder(c, g, x, y, width, height);
        }
        else {
          g.setColor(UIUtil.getPanelBackground());
          g.fillRect(x, y, width, 1);
          g.setColor(Gray._50.withAlpha(90));
          g.fillRect(x, y, width, 1);
        }
      }
    }

    @Nonnull
    @Override
    public Insets getBorderInsets(Component c) {
      EditorsSplitters splitters = AWTComponentProviderUtil.findParent(c, EditorsSplitters.class);

      boolean thereIsSomethingAbove =
              !SystemInfo.isMac || UISettings.getInstance().getShowMainToolbar() || UISettings.getInstance().getShowNavigationBar() || toolWindowIsNotEmpty();
      //noinspection ConstantConditions
      Component header = myHeaderPanel == null ? null : ArrayUtil.getFirstElement(myHeaderPanel.getComponents());
      boolean paintTop = thereIsSomethingAbove && header == null && UISettings.getInstance().getEditorTabPlacement() != SwingConstants.TOP;
      return splitters == null ? super.getBorderInsets(c) : JBUI.insetsTop(paintTop ? 1 : 0);
    }

    private boolean toolWindowIsNotEmpty() {
      if (myProject == null) return false;
      ToolWindowManagerEx m = ToolWindowManagerEx.getInstanceEx(myProject);
      return m != null && !m.getIdsOn(ToolWindowAnchor.TOP).isEmpty();
    }

    @Override
    public boolean isBorderOpaque() {
      return true;
    }
  }

  private class MyHeaderPanel extends JPanel {
    private int myOldHeight;

    private MyHeaderPanel() {
      super(new BorderLayout());
    }

    @Override
    public void revalidate() {
      myOldHeight = getHeight();
      super.revalidate();
    }

    @Override
    protected void validateTree() {
      int height = myOldHeight;
      super.validateTree();
      height -= getHeight();

      if (height != 0 && !(myOldHeight == 0 && getComponentCount() > 0 && getPermanentHeaderComponent() == getComponent(0))) {
        myVerticalScrollBar.setValue(myVerticalScrollBar.getValue() - height);
      }
      myOldHeight = getHeight();
    }
  }

  private class MyTextDrawingCallback implements TextDrawingCallback {
    @Override
    public void drawChars(@Nonnull Graphics g, @Nonnull char[] data, int start, int end, int x, int y, Color color, @Nonnull FontInfo fontInfo) {
      myView.drawChars(g, data, start, end, x, y, color, fontInfo);
    }
  }

  private static class NullEditorHighlighter extends EmptyEditorHighlighter {
    private static final TextAttributes NULL_ATTRIBUTES = new TextAttributes();

    NullEditorHighlighter() {
      super(NULL_ATTRIBUTES);
    }

    @Override
    public void setAttributes(TextAttributes attributes) {
    }

    @Override
    public void setColorScheme(@Nonnull EditorColorsScheme scheme) {
    }
  }
}
