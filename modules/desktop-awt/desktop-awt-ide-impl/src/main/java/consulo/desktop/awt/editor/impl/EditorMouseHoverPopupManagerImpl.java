// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.editor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.*;
import consulo.codeEditor.event.*;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.component.ProcessCanceledException;
import consulo.desktop.awt.language.editor.documentation.DocumentationComponent;
import consulo.desktop.awt.language.editor.documentation.DocumentationManagerImpl;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import consulo.ide.impl.idea.codeInsight.daemon.impl.tooltips.TooltipActionProvider;
import consulo.ide.impl.idea.codeInsight.documentation.QuickDocUtil;
import consulo.ide.impl.idea.codeInsight.hint.LineTooltipRenderer;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.codeEditor.internal.EditorMouseHoverPopupControl;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.ide.impl.idea.ui.WidthBasedLayout;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.ide.impl.idea.ui.popup.PopupPositionManager;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.language.editor.impl.internal.hint.EditorMouseHoverPopupManager;
import consulo.language.editor.impl.internal.hint.TooltipAction;
import consulo.language.editor.impl.internal.hint.TooltipGroup;
import consulo.language.editor.impl.internal.hint.TooltipRenderer;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.language.editor.impl.internal.markup.ErrorStripTooltipRendererProvider;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.plain.psi.PsiPlainText;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SimpleReference;
import consulo.util.lang.ref.SoftReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Singleton
@ServiceImpl
public final class EditorMouseHoverPopupManagerImpl implements EditorMouseHoverPopupManager, Disposable {
  private static final Logger LOG = Logger.getInstance(EditorMouseHoverPopupManagerImpl.class);
  private static final Key<Boolean> DISABLE_BINDING = Key.create("EditorMouseHoverPopupManagerImpl.disable.binding");
  private static final TooltipGroup EDITOR_INFO_GROUP = new TooltipGroup("EDITOR_INFO_GROUP", 0);
  private static final int MAX_POPUP_WIDTH = 650;

  private final Application myApplication;
  private final Alarm myAlarm;
  private final MouseMovementTracker myMouseMovementTracker = new MouseMovementTracker();
  private boolean myKeepPopupOnMouseMove;
  private WeakReference<Editor> myCurrentEditor;
  private WeakReference<AbstractPopup> myPopupReference;
  private Context myContext;
  private ProgressIndicator myCurrentProgress;
  private boolean mySkipNextMovement;

  @Inject
  public EditorMouseHoverPopupManagerImpl(Application application,
                                          EditorFactory editorFactory,
                                          EditorMouseHoverPopupControl editorMouseHoverPopupControl) {
    myApplication = application;
    myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    EditorEventMulticaster multicaster = editorFactory.getEventMulticaster();
    multicaster.addCaretListener(new CaretListener() {
      @Override
      public void caretPositionChanged(@Nonnull CaretEvent event) {
        Editor editor = event.getEditor();
        if (editor == SoftReference.dereference(myCurrentEditor)) {
          DocumentationManager.getInstance(editor.getProject()).setAllowContentUpdateFromContext(true);
        }
      }
    }, this);
    multicaster.addVisibleAreaListener(e -> {
      cancelProcessingAndCloseHint();
    }, this);

    editorMouseHoverPopupControl.addListener(() -> {
      Editor editor = SoftReference.dereference(myCurrentEditor);
      if (editor != null && EditorMouseHoverPopupControl.arePopupsDisabled(editor)) {
        closeHint();
      }
    });

    IdeEventQueueProxy.getInstance().addActivityListener(this::onActivity, this);
  }

  @Override
  public void dispose() {
  }

  @Override
  public void cancelCurrentProcessing() {
    myAlarm.cancelAllRequests();
    if (myCurrentProgress != null) {
      myCurrentProgress.cancel();
      myCurrentProgress = null;
    }
  }

  @Override
  public void skipNextMovement() {
    mySkipNextMovement = true;
  }

  private void scheduleProcessing(@Nonnull Editor editor,
                                  @Nonnull Context context,
                                  boolean updateExistingPopup,
                                  boolean forceShowing,
                                  boolean requestFocus) {
    ProgressIndicatorBase progress = new ProgressIndicatorBase();
    myCurrentProgress = progress;
    myAlarm.addRequest(() -> {
      ProgressManager.getInstance().executeProcessUnderProgress(() -> {
        Info info = context.calcInfo(editor);
        myApplication.invokeLater(() -> {
          if (progress != myCurrentProgress) {
            return;
          }

          myCurrentProgress = null;
          if (info == null || !editor.getContentComponent().isShowing() || (!forceShowing && isPopupDisabled(editor))) {
            return;
          }

          PopupBridge popupBridge = new PopupBridge();
          JComponent component = info.createComponent(editor, popupBridge, requestFocus);
          if (component == null) {
            closeHint();
          }
          else {
            if (updateExistingPopup && isHintShown()) {
              updateHint(component, popupBridge);
            }
            else {
              AbstractPopup hint = createHint(component, popupBridge, requestFocus);
              showHintInEditor(hint, editor, context);
              myPopupReference = new WeakReference<>(hint);
              myCurrentEditor = new WeakReference<>(editor);
            }
            myContext = context;
          }
        });
      }, progress);
    }, context.getShowingDelay());
  }

  private void onActivity() {
    cancelCurrentProcessing();
  }

  private boolean ignoreEvent(EditorMouseEvent e) {
    if (mySkipNextMovement) {
      mySkipNextMovement = false;
      return true;
    }
    Rectangle currentHintBounds = getCurrentHintBounds(e.getEditor());
    return myMouseMovementTracker.isMovingTowards(e.getMouseEvent(),
                                                  currentHintBounds) || currentHintBounds != null && myKeepPopupOnMouseMove;
  }

  private static boolean isPopupDisabled(Editor editor) {
    return isAnotherAppInFocus() || EditorMouseHoverPopupControl.arePopupsDisabled(editor) || LookupManager.getActiveLookup(editor) != null;
  }

  private static boolean isAnotherAppInFocus() {
    return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow() == null;
  }

  private Rectangle getCurrentHintBounds(Editor editor) {
    JBPopup popup = getCurrentHint();
    if (popup == null) return null;
    Dimension size = popup.getSize();
    if (size == null) return null;
    Rectangle result = new Rectangle(popup.getLocationOnScreen(), size);
    int borderTolerance = editor.getLineHeight() / 3;
    result.grow(borderTolerance, borderTolerance);
    return result;
  }

  private void showHintInEditor(AbstractPopup hint, Editor editor, Context context) {
    closeHint();
    myMouseMovementTracker.reset();
    myKeepPopupOnMouseMove = false;
    editor.putUserData(EditorPopupHelper.ANCHOR_POPUP_POSITION, context.getPopupPosition(editor));
    try {
      PopupPositionManager.positionPopupInBestPosition(hint, editor, null);
    }
    finally {
      editor.putUserData(EditorPopupHelper.ANCHOR_POPUP_POSITION, null);
    }
    Window window = hint.getPopupWindow();
    if (window != null) {
      window.setFocusableWindowState(true);
      IdeEventQueue.getInstance().addDispatcher(e -> {
        if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getSource() == window) {
          myKeepPopupOnMouseMove = true;
        }
        return false;
      }, hint);
    }
  }

  private static AbstractPopup createHint(JComponent component, PopupBridge popupBridge, boolean requestFocus) {
    WrapperPanel wrapper = new WrapperPanel(component);
    AbstractPopup popup =
      (AbstractPopup)JBPopupFactory.getInstance()
                                   .createComponentPopupBuilder(wrapper, component)
                                   .setResizable(true)
                                   .setFocusable(requestFocus)
                                   .setRequestFocus(requestFocus)
                                   .createPopup();
    popupBridge.setPopup(popup);
    return popup;
  }

  private void updateHint(JComponent component, PopupBridge popupBridge) {
    AbstractPopup popup = getCurrentHint();
    if (popup != null) {
      WrapperPanel wrapper = (WrapperPanel)popup.getComponent();
      wrapper.setContent(component);
      validatePopupSize(popup);
      popupBridge.setPopup(popup);
    }
  }

  private static void validatePopupSize(@Nonnull AbstractPopup popup) {
    JComponent component = popup.getComponent();
    if (component != null) popup.setSize(component.getPreferredSize());
  }

  private static int getTargetOffset(EditorMouseEvent event) {
    Editor editor = event.getEditor();
    Point point = event.getMouseEvent().getPoint();
    if (editor instanceof EditorEx &&
      editor.getProject() != null &&
      event.getArea() == EditorMouseEventArea.EDITING_AREA &&
      event.getMouseEvent().getModifiers() == 0 &&
      EditorUtil.isPointOverText(editor, point) &&
      ((EditorEx)editor).getFoldingModel().getFoldingPlaceholderAt(point) == null) {
      LogicalPosition logicalPosition = editor.xyToLogicalPosition(point);
      return editor.logicalPositionToOffset(logicalPosition);
    }
    return -1;
  }

  @Override
  public void handleMouseMoved(@Nonnull EditorMouseEvent e) {
    cancelCurrentProcessing();

    if (ignoreEvent(e)) return;

    Editor editor = e.getEditor();
    if (isPopupDisabled(editor)) {
      closeHint();
      return;
    }

    int targetOffset = getTargetOffset(e);
    if (targetOffset < 0) {
      closeHint();
      return;
    }
    Context context = createContext(editor, targetOffset);
    if (context == null) {
      closeHint();
      return;
    }
    Context.Relation relation = isHintShown() ? context.compareTo(myContext) : Context.Relation.DIFFERENT;
    if (relation == Context.Relation.SAME) {
      return;
    }
    else if (relation == Context.Relation.DIFFERENT) {
      closeHint();
    }
    scheduleProcessing(editor, context, relation == Context.Relation.SIMILAR, false, false);
  }

  private static Context createContext(Editor editor, int offset) {
    Project project = Objects.requireNonNull(editor.getProject());

    HighlightInfo info =
      ((DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project)).findHighlightByOffset(editor.getDocument(), offset, false);

    PsiElement elementForQuickDoc = null;
    if (EditorSettingsExternalizable.getInstance().isShowQuickDocOnMouseOverElement()) {
      PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (psiFile != null) {
        elementForQuickDoc = psiFile.findElementAt(offset);
        if (elementForQuickDoc instanceof PsiWhiteSpace || elementForQuickDoc instanceof PsiPlainText) {
          elementForQuickDoc = null;
        }
      }
    }

    return info == null && elementForQuickDoc == null ? null : new Context(offset, info, elementForQuickDoc);
  }

  @Override
  public void cancelProcessingAndCloseHint() {
    cancelCurrentProcessing();
    closeHint();
  }

  private void closeHint() {
    AbstractPopup hint = getCurrentHint();
    if (hint != null) {
      hint.cancel();
    }
    myPopupReference = null;
    myCurrentEditor = null;
    myContext = null;
  }

  private boolean isHintShown() {
    return getCurrentHint() != null;
  }

  private AbstractPopup getCurrentHint() {
    if (myPopupReference == null) return null;
    AbstractPopup hint = myPopupReference.get();
    if (hint == null || !hint.isVisible()) {
      if (hint != null) {
        // hint's window might've been hidden by AWT without notifying us
        // dispose to remove the popup from IDE hierarchy and avoid leaking components
        hint.cancel();
      }
      myPopupReference = null;
      myCurrentEditor = null;
      myContext = null;
      return null;
    }
    return hint;
  }

  @Override
  public void showInfoTooltip(@Nonnull Editor editor,
                              @Nonnull HighlightInfo info,
                              int offset,
                              boolean requestFocus,
                              boolean showImmediately) {
    cancelProcessingAndCloseHint();
    Context context = new Context(offset, info, null) {
      @Override
      long getShowingDelay() {
        return showImmediately ? 0 : super.getShowingDelay();
      }
    };
    scheduleProcessing(editor, context, false, true, requestFocus);
  }

  private static class Context {
    private final int targetOffset;
    private final WeakReference<HighlightInfo> highlightInfo;
    private final WeakReference<PsiElement> elementForQuickDoc;

    private Context(int targetOffset, HighlightInfo highlightInfo, PsiElement elementForQuickDoc) {
      this.targetOffset = targetOffset;
      this.highlightInfo = highlightInfo == null ? null : new WeakReference<>(highlightInfo);
      this.elementForQuickDoc = elementForQuickDoc == null ? null : new WeakReference<>(elementForQuickDoc);
    }

    private PsiElement getElementForQuickDoc() {
      return SoftReference.dereference(elementForQuickDoc);
    }

    private HighlightInfo getHighlightInfo() {
      return SoftReference.dereference(highlightInfo);
    }

    private Relation compareTo(Context other) {
      if (other == null) return Relation.DIFFERENT;
      HighlightInfo highlightInfo = getHighlightInfo();
      if (!Objects.equals(highlightInfo, other.getHighlightInfo())) return Relation.DIFFERENT;
      return Objects.equals(getElementForQuickDoc(),
                            other.getElementForQuickDoc()) ? Relation.SAME : highlightInfo == null ? Relation.DIFFERENT : Relation.SIMILAR;
    }

    long getShowingDelay() {
      return EditorSettingsExternalizable.getInstance().getTooltipsDelay();
    }

    @Nonnull
    private VisualPosition getPopupPosition(Editor editor) {
      HighlightInfo highlightInfo = getHighlightInfo();
      if (highlightInfo == null) {
        int offset = targetOffset;
        PsiElement elementForQuickDoc = getElementForQuickDoc();
        if (elementForQuickDoc != null) {
          offset = elementForQuickDoc.getTextRange().getStartOffset();
        }
        return editor.offsetToVisualPosition(offset);
      }
      else {
        VisualPosition targetPosition = editor.offsetToVisualPosition(targetOffset);
        VisualPosition endPosition = editor.offsetToVisualPosition(highlightInfo.getEndOffset());
        if (endPosition.line <= targetPosition.line) return targetPosition;
        Point targetPoint = editor.visualPositionToXY(targetPosition);
        Point endPoint = editor.visualPositionToXY(endPosition);
        Point resultPoint = new Point(targetPoint.x, endPoint.x > targetPoint.x ? endPoint.y : editor.visualLineToY(endPosition.line - 1));
        return editor.xyToVisualPosition(resultPoint);
      }
    }

    @Nullable
    private Info calcInfo(@Nonnull Editor editor) {
      HighlightInfo info = getHighlightInfo();
      if (info != null && (info.getDescription() == null || info.getToolTip() == null)) {
        info = null;
      }

      String quickDocMessage = null;
      SimpleReference<PsiElement> targetElementRef = new SimpleReference<>();
      if (elementForQuickDoc != null) {
        PsiElement element = getElementForQuickDoc();
        try {
          Project project = editor.getProject();
          if (project == null || project.isDisposed()) {
            return null;
          }

          DocumentationManager documentationManager = DocumentationManager.getInstance(project);
          QuickDocUtil.runInReadActionWithWriteActionPriorityWithRetries(() -> {
            if (element.isValid()) {
              targetElementRef.set(documentationManager.findTargetElement(editor, targetOffset, element.getContainingFile(), element));
            }
          }, 5000, 100);
          if (!targetElementRef.isNull()) {
            quickDocMessage = documentationManager.generateDocumentation(targetElementRef.get(), element, true);
          }
        }
        catch (IndexNotReadyException | ProcessCanceledException ignored) {
        }
        catch (Exception e) {
          LOG.warn(e);
        }
      }
      return info == null && quickDocMessage == null ? null : new Info(info, quickDocMessage, targetElementRef.get());
    }

    private enum Relation {
      SAME, // no need to update popup
      SIMILAR, // popup needs to be updated
      DIFFERENT // popup needs to be closed, and new one shown
    }
  }

  private static class Info {
    private final HighlightInfo highlightInfo;

    private final String quickDocMessage;
    private final WeakReference<PsiElement> quickDocElement;


    private Info(HighlightInfo highlightInfo, String quickDocMessage, PsiElement quickDocElement) {
      assert highlightInfo != null || quickDocMessage != null;
      this.highlightInfo = highlightInfo;
      this.quickDocMessage = quickDocMessage;
      this.quickDocElement = new WeakReference<>(quickDocElement);
    }

    private JComponent createComponent(Editor editor, PopupBridge popupBridge, boolean requestFocus) {
      boolean quickDocShownInPopup = quickDocMessage != null && ToolWindowManager.getInstance(Objects.requireNonNull(editor.getProject()))
                                                                                 .getToolWindow(ToolWindowId.DOCUMENTATION) == null;
      JComponent c1 = createHighlightInfoComponent(editor, !quickDocShownInPopup, popupBridge, requestFocus);
      DocumentationComponent c2 = createQuickDocComponent(editor, c1 != null, popupBridge);
      assert quickDocShownInPopup == (c2 != null);
      if (c1 == null && c2 == null) return null;
      JPanel p = new JPanel(new CombinedPopupLayout(c1, c2));
      p.setBorder(null);
      if (c1 != null) p.add(c1);
      if (c2 != null) p.add(c2);
      return p;
    }

    private JComponent createHighlightInfoComponent(Editor editor,
                                                    boolean highlightActions,
                                                    PopupBridge popupBridge,
                                                    boolean requestFocus) {
      if (highlightInfo == null) return null;
      TooltipAction action = TooltipActionProvider.calcTooltipAction(highlightInfo, editor);
      ErrorStripTooltipRendererProvider provider = ((EditorMarkupModel)editor.getMarkupModel()).getErrorStripTooltipRendererProvider();
      TooltipRenderer tooltipRenderer = provider.calcTooltipRenderer(Objects.requireNonNull(highlightInfo.getToolTip()), action, -1);
      if (!(tooltipRenderer instanceof LineTooltipRenderer)) return null;
      return createHighlightInfoComponent(editor, (LineTooltipRenderer)tooltipRenderer, highlightActions, popupBridge, requestFocus);
    }

    private static JComponent createHighlightInfoComponent(Editor editor,
                                                           LineTooltipRenderer renderer,
                                                           boolean highlightActions,
                                                           PopupBridge popupBridge,
                                                           boolean requestFocus) {
      SimpleReference<WrapperPanel> wrapperPanelRef = new SimpleReference<>();
      SimpleReference<LightweightHintImpl> mockHintRef = new SimpleReference<>();
      HintHint hintHint = new HintHint().setAwtTooltip(true).setRequestFocus(requestFocus);
      LightweightHintImpl hint =
        renderer.createHint(editor, new Point(), false, EDITOR_INFO_GROUP, hintHint, true, highlightActions, false, expand -> {
          LineTooltipRenderer newRenderer = renderer.createRenderer(renderer.getText(), expand ? 1 : 0);
          JComponent newComponent = createHighlightInfoComponent(editor, newRenderer, highlightActions, popupBridge, requestFocus);
          AbstractPopup popup = popupBridge.getPopup();
          WrapperPanel wrapper = wrapperPanelRef.get();
          if (newComponent != null && popup != null && wrapper != null) {
            LightweightHintImpl mockHint = mockHintRef.get();
            if (mockHint != null) closeHintIgnoreBinding(mockHint);
            wrapper.setContent(newComponent);
            validatePopupSize(popup);
          }
        });
      if (hint == null) return null;
      mockHintRef.set(hint);
      bindHintHiding(hint, popupBridge);
      JComponent component = hint.getComponent();
      LOG.assertTrue(component instanceof WidthBasedLayout, "Unexpected type of tooltip component: " + component.getClass());
      WrapperPanel wrapper = new WrapperPanel(component);
      wrapperPanelRef.set(wrapper);
      // emulating LightweightHint+IdeTooltipManager+BalloonImpl - they use the same background
      wrapper.setBackground(hintHint.getTextBackground());
      wrapper.setOpaque(true);
      return wrapper;
    }

    private static void bindHintHiding(LightweightHintImpl hint, PopupBridge popupBridge) {
      AtomicBoolean inProcess = new AtomicBoolean();
      hint.addHintListener(e -> {
        if (hint.getUserData(DISABLE_BINDING) == null && inProcess.compareAndSet(false, true)) {
          try {
            AbstractPopup popup = popupBridge.getPopup();
            if (popup != null) {
              popup.cancel();
            }
          }
          finally {
            inProcess.set(false);
          }
        }
      });
      popupBridge.performOnCancel(() -> {
        if (hint.getUserData(DISABLE_BINDING) == null && inProcess.compareAndSet(false, true)) {
          try {
            hint.hide();
          }
          finally {
            inProcess.set(false);
          }
        }
      });
    }

    private static void closeHintIgnoreBinding(LightweightHintImpl hint) {
      hint.putUserData(DISABLE_BINDING, Boolean.TRUE);
      hint.hide();
    }

    @Nullable
    private DocumentationComponent createQuickDocComponent(Editor editor, boolean deEmphasize, PopupBridge popupBridge) {
      if (quickDocMessage == null) return null;
      PsiElement element = quickDocElement.get();
      Project project = Objects.requireNonNull(editor.getProject());
      DocumentationManager documentationManager = DocumentationManager.getInstance(project);
      ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DOCUMENTATION);
      if (toolWindow != null) {
        if (element != null) {
          documentationManager.showJavaDocInfo(editor,
                                               element,
                                               DocumentationManagerHelper.getOriginalElement(element),
                                               null,
                                               quickDocMessage,
                                               true,
                                               false);
          documentationManager.setAllowContentUpdateFromContext(false);
        }
        return null;
      }
      class MyDocComponent extends DocumentationComponent {
        private MyDocComponent() {
          super((DocumentationManagerImpl)documentationManager, false);
        }

        @Override
        protected void showHint() {
          AbstractPopup popup = popupBridge.getPopup();
          if (popup != null) {
            validatePopupSize(popup);
          }
        }
      }
      DocumentationComponent component = new MyDocComponent();
      if (deEmphasize) {
        component.setBorder(IdeBorderFactory.createBorder(UIUtil.getTooltipSeparatorColor(), SideBorder.TOP));
      }
      component.setData(element, quickDocMessage, null, null, null);
      component.setToolwindowCallback(() -> {
        PsiElement docElement = component.getElement();
        documentationManager.createToolWindow(docElement, DocumentationManagerHelper.getOriginalElement(docElement));
        ToolWindow createdToolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DOCUMENTATION);
        if (createdToolWindow != null) {
          createdToolWindow.setAutoHide(false);
        }
        AbstractPopup popup = popupBridge.getPopup();
        if (popup != null) {
          popup.cancel();
        }
      });
      popupBridge.performWhenAvailable(component::setHint);
      EditorUtil.disposeWithEditor(editor, component);
      return component;
    }
  }

  @Nullable
  public DocumentationComponent getDocumentationComponent() {
    AbstractPopup hint = getCurrentHint();
    return hint == null ? null : UIUtil.findComponentOfType(hint.getComponent(), DocumentationComponent.class);
  }

  private static class PopupBridge {
    private AbstractPopup popup;
    private List<Consumer<AbstractPopup>> consumers = new ArrayList<>();

    private void setPopup(@Nonnull AbstractPopup popup) {
      assert this.popup == null;
      this.popup = popup;
      consumers.forEach(c -> c.accept(popup));
      consumers = null;
    }

    @Nullable
    private AbstractPopup getPopup() {
      return popup;
    }

    private void performWhenAvailable(@Nonnull Consumer<AbstractPopup> consumer) {
      if (popup == null) {
        consumers.add(consumer);
      }
      else {
        consumer.accept(popup);
      }
    }

    private void performOnCancel(@Nonnull Runnable runnable) {
      performWhenAvailable(popup -> popup.addListener(new JBPopupListener() {
        @Override
        public void onClosed(@Nonnull LightweightWindowEvent event) {
          runnable.run();
        }
      }));
    }
  }

  private static class WrapperPanel extends JPanel implements WidthBasedLayout {
    private WrapperPanel(JComponent content) {
      super(new BorderLayout());
      setBorder(null);
      setContent(content);
    }

    private void setContent(JComponent content) {
      removeAll();
      add(content, BorderLayout.CENTER);
    }

    private JComponent getComponent() {
      return (JComponent)getComponent(0);
    }

    @Override
    public int getPreferredWidth() {
      return WidthBasedLayout.getPreferredWidth(getComponent());
    }

    @Override
    public int getPreferredHeight(int width) {
      return WidthBasedLayout.getPreferredHeight(getComponent(), width);
    }
  }

  private static class CombinedPopupLayout implements LayoutManager {
    private final JComponent highlightInfoComponent;
    private final DocumentationComponent quickDocComponent;

    private CombinedPopupLayout(JComponent highlightInfoComponent, DocumentationComponent quickDocComponent) {
      this.highlightInfoComponent = highlightInfoComponent;
      this.quickDocComponent = quickDocComponent;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      int w1 = WidthBasedLayout.getPreferredWidth(highlightInfoComponent);
      int w2 = WidthBasedLayout.getPreferredWidth(quickDocComponent);
      int preferredWidth = Math.min(JBUI.scale(MAX_POPUP_WIDTH), Math.max(w1, w2));
      int h1 = WidthBasedLayout.getPreferredHeight(highlightInfoComponent, preferredWidth);
      int h2 = WidthBasedLayout.getPreferredHeight(quickDocComponent, preferredWidth);
      return new Dimension(preferredWidth, h1 + h2);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
      Dimension d1 = highlightInfoComponent == null ? new Dimension() : highlightInfoComponent.getMinimumSize();
      Dimension d2 = quickDocComponent == null ? new Dimension() : quickDocComponent.getMinimumSize();
      return new Dimension(Math.max(d1.width, d2.width), d1.height + d2.height);
    }

    @Override
    public void layoutContainer(Container parent) {
      int width = parent.getWidth();
      int height = parent.getHeight();
      if (highlightInfoComponent == null) {
        if (quickDocComponent != null) quickDocComponent.setBounds(0, 0, width, height);
      }
      else if (quickDocComponent == null) {
        highlightInfoComponent.setBounds(0, 0, width, height);
      }
      else {
        int h1 = Math.min(height, highlightInfoComponent.getPreferredSize().height);
        highlightInfoComponent.setBounds(0, 0, width, h1);
        quickDocComponent.setBounds(0, h1, width, height - h1);
      }
    }
  }
}
