// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.impl.status;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.PowerSaveMode;
import consulo.application.PowerSaveModeListener;
import consulo.application.impl.internal.progress.AbstractProgressIndicatorExBase;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;
import consulo.application.util.NotNullLazyValue;
import consulo.application.util.registry.Registry;
import consulo.component.messagebus.MessageBusConnection;
import consulo.desktop.awt.internal.notification.EventLog;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorsSplitters;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.impl.desktop.awt.migration.AWTComponentProviderUtil;
import consulo.ide.impl.idea.openapi.progress.impl.ProgressSuspender;
import consulo.ide.impl.idea.openapi.progress.impl.ProgressSuspenderListener;
import consulo.ide.impl.idea.openapi.project.ProjectUtil;
import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.ide.impl.idea.openapi.wm.impl.status.InlineProgressIndicator;
import consulo.ide.impl.idea.openapi.wm.impl.status.PresentationModeProgressPanel;
import consulo.ide.impl.idea.openapi.wm.impl.status.ProgressButton;
import consulo.ide.impl.idea.reference.SoftReference;
import consulo.ide.impl.idea.ui.InplaceButton;
import consulo.ide.impl.project.ui.impl.StatusWidgetBorders;
import consulo.ide.impl.ui.impl.ToolWindowPanelImplEx;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.internal.BalloonLayoutEx;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.ex.Gray;
import consulo.ui.ex.PositionTracker;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionsBundle;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonHandler;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.util.collection.*;
import consulo.util.lang.Comparing;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

public class InfoAndProgressPanel extends JPanel implements Disposable {
  private static final Logger LOG = Logger.getInstance(InfoAndProgressPanel.class);
  private final ProcessPopup myPopup;

  private final StatusPanel myInfoPanel;
  private final JPanel myRefreshAndInfoPanel = new JPanel();
  private final NotNullLazyValue<AsyncProcessIcon> myProgressIcon = new NotNullLazyValue<>() {
    @Nonnull
    @Override
    protected AsyncProcessIcon compute() {
      AsyncProcessIcon icon = new AsyncProcessIcon("Background process");
      icon.setOpaque(false);

      icon.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          handle(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          handle(e);
        }
      });

      icon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      icon.setBorder(StatusWidgetBorders.INSTANCE);
      icon.setToolTipText(ActionsBundle.message("action.ShowProcessWindow.double.click"));
      return icon;
    }
  };

  private final List<ProgressIndicatorEx> myOriginals = new ArrayList<>();
  private final List<TaskInfo> myInfos = new ArrayList<>();
  private final Map<InlineProgressIndicator, ProgressIndicatorEx> myInline2Original = new HashMap<>();
  private final MultiValuesMap<ProgressIndicatorEx, MyInlineProgressIndicator> myOriginal2Inlines = new MultiValuesMap<>();

  private final MergingUpdateQueue myUpdateQueue;
  private final Alarm myQueryAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

  private boolean myShouldClosePopupAndOnProcessFinish;

  private final JLabel myRefreshIcon = new JLabel(new AnimatedIcon.Default());

  private String myCurrentRequestor;
  private boolean myDisposed;
  private WeakReference<Balloon> myLastShownBalloon;

  private final Set<InlineProgressIndicator> myDirtyIndicators = Sets.newHashSet(HashingStrategy.identity());
  private final Update myUpdateIndicators = new Update("UpdateIndicators", false, 1) {
    @Override
    public void run() {
      List<InlineProgressIndicator> indicators;
      synchronized (myDirtyIndicators) {
        indicators = new ArrayList<>(myDirtyIndicators);
        myDirtyIndicators.clear();
      }
      for (InlineProgressIndicator indicator : indicators) {
        indicator.updateAndRepaint();
      }
    }
  };
  @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
  private LinkLabel<Object> myMultiProcessLink;

  public InfoAndProgressPanel(@Nonnull Supplier<Project> getProjectSupplier) {
    setOpaque(false);
    setBorder(JBUI.Borders.empty());

    myRefreshIcon.setVisible(false);

    myInfoPanel = new StatusPanel(getProjectSupplier);
    myRefreshAndInfoPanel.setLayout(new BorderLayout());
    myRefreshAndInfoPanel.setOpaque(false);
    myRefreshAndInfoPanel.add(myRefreshIcon, BorderLayout.WEST);
    myRefreshAndInfoPanel.add(myInfoPanel, BorderLayout.CENTER);

    myUpdateQueue = new MergingUpdateQueue("Progress indicator", 50, true, MergingUpdateQueue.ANY_COMPONENT);
    myPopup = new ProcessPopup(this);

    setRefreshVisible(false);

    restoreEmptyStatus();

    runOnProgressRelatedChange(this::updateProgressIcon, this);
  }

  private void runOnProgressRelatedChange(@Nonnull Runnable runnable, Disposable parentDisposable) {
    synchronized (myOriginals) {
      if (!myDisposed) {
        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(parentDisposable);
        connection.subscribe(PowerSaveModeListener.class, () -> UIUtil.invokeLaterIfNeeded(runnable));
        connection.subscribe(ProgressSuspenderListener.class, new ProgressSuspenderListener() {
          @Override
          public void suspendableProgressAppeared(@Nonnull ProgressSuspender suspender) {
            UIUtil.invokeLaterIfNeeded(runnable);
          }

          @Override
          public void suspendedStatusChanged(@Nonnull ProgressSuspender suspender) {
            UIUtil.invokeLaterIfNeeded(runnable);
          }
        });
      }
    }
  }

  private void handle(MouseEvent e) {
    if (UIUtil.isActionClick(e, MouseEvent.MOUSE_PRESSED)) {
      if (!myPopup.isShowing()) {
        openProcessPopup(true);
      }
      else {
        hideProcessPopup();
      }
    }
    else if (e.isPopupTrigger()) {
      ActionGroup group = (ActionGroup)ActionManager.getInstance().getAction("BackgroundTasks");
      ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group).getComponent().show(e.getComponent(), e.getX(), e.getY());
    }
  }

  @Override
  public void dispose() {
    setRefreshVisible(false);
    synchronized (myOriginals) {
      restoreEmptyStatus();
      for (InlineProgressIndicator indicator : myInline2Original.keySet()) {
        Disposer.dispose(indicator);
      }
      myInline2Original.clear();
      myOriginal2Inlines.clear();

      myDisposed = true;
    }
  }

  public JComponent getComponent() {
    return this;
  }

  @Nonnull
  public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
    synchronized (myOriginals) {
      if (myOriginals.isEmpty()) return Collections.emptyList();

      List<Pair<TaskInfo, ProgressIndicator>> result = new ArrayList<>(myOriginals.size());
      for (int i = 0; i < myOriginals.size(); i++) {
        result.add(Pair.create(myInfos.get(i), myOriginals.get(i)));
      }

      return Collections.unmodifiableList(result);
    }
  }

  public void addProgress(@Nonnull ProgressIndicator original, @Nonnull TaskInfo info) {
    synchronized (myOriginals) {
      final boolean veryFirst = !hasProgressIndicators();

      myOriginals.add((ProgressIndicatorEx)original);
      myInfos.add(info);

      MyInlineProgressIndicator expanded = createInlineDelegate(info, (ProgressIndicatorEx)original, false);
      MyInlineProgressIndicator compact = createInlineDelegate(info, (ProgressIndicatorEx)original, true);

      myPopup.addIndicator(expanded);
      updateProgressIcon();

      if (veryFirst && !myPopup.isShowing()) {
        buildInInlineIndicator(compact);
      }
      else {
        buildInProcessCount();
        if (myInfos.size() > 1 && Registry.is("ide.windowSystem.autoShowProcessPopup")) {
          openProcessPopup(false);
        }
      }

      runQuery();
    }
  }

  private boolean hasProgressIndicators() {
    synchronized (myOriginals) {
      return !myOriginals.isEmpty();
    }
  }

  private void removeProgress(@Nonnull MyInlineProgressIndicator progress) {
    synchronized (myOriginals) {
      if (!myInline2Original.containsKey(progress)) return; // already disposed

      final boolean last = myOriginals.size() == 1;
      final boolean beforeLast = myOriginals.size() == 2;

      myPopup.removeIndicator(progress);

      final ProgressIndicatorEx original = removeFromMaps(progress);
      if (myOriginals.contains(original)) {
        Disposer.dispose(progress);
        return;
      }

      if (last) {
        restoreEmptyStatus();
        if (myShouldClosePopupAndOnProcessFinish) {
          hideProcessPopup();
        }
      }
      else {
        if (myPopup.isShowing() || myOriginals.size() > 1) {
          buildInProcessCount();
        }
        else if (beforeLast) {
          buildInInlineIndicator(createInlineDelegate(myInfos.get(0), myOriginals.get(0), true));
        }
        else {
          restoreEmptyStatus();
        }
      }

      runQuery();
    }
    Disposer.dispose(progress);

  }

  private ProgressIndicatorEx removeFromMaps(@Nonnull MyInlineProgressIndicator progress) {
    final ProgressIndicatorEx original = myInline2Original.get(progress);

    myInline2Original.remove(progress);
    synchronized (myDirtyIndicators) {
      myDirtyIndicators.remove(progress);
    }

    myOriginal2Inlines.remove(original, progress);
    if (myOriginal2Inlines.get(original) == null) {
      final int originalIndex = myOriginals.indexOf(original);
      myOriginals.remove(originalIndex);
      myInfos.remove(originalIndex);
    }

    return original;
  }

  private void openProcessPopup(boolean requestFocus) {
    synchronized (myOriginals) {
      if (myPopup.isShowing()) return;
      myPopup.show(requestFocus);
      if (hasProgressIndicators()) {
        myShouldClosePopupAndOnProcessFinish = true;
        buildInProcessCount();
      }
      else {
        myShouldClosePopupAndOnProcessFinish = false;
        restoreEmptyStatus();
      }
    }
  }

  void hideProcessPopup() {
    synchronized (myOriginals) {
      if (!myPopup.isShowing()) return;
      myPopup.hide();

      if (myOriginals.size() == 1) {
        buildInInlineIndicator(createInlineDelegate(myInfos.get(0), myOriginals.get(0), true));
      }
      else if (!hasProgressIndicators()) {
        restoreEmptyStatus();
      }
      else {
        buildInProcessCount();
      }
    }
  }

  private void buildInProcessCount() {
    removeAll();
    setLayout(new InlineLayout());

    myMultiProcessLink = new LinkLabel<>(getMultiProgressLinkText(), null, (aSource, aLinkData) -> triggerPopupShowing());

    if (Platform.current().os().isMac()) myMultiProcessLink.setFont(JBUI.Fonts.label(11));

    myMultiProcessLink.setOpaque(false);

    JPanel iconAndProgress = new JPanel(new HorizontalLayout(5));
    iconAndProgress.setOpaque(false);

    iconAndProgress.add(myProgressIcon.get());
    iconAndProgress.add(myMultiProcessLink);

    add(myRefreshAndInfoPanel);

    add(iconAndProgress);

    revalidate();
    repaint();
  }

  @Nonnull
  private String getMultiProgressLinkText() {
    ProgressIndicatorEx latest = getLatestProgress();
    String latestText = latest == null ? null : latest.getText();
    if (StringUtil.isEmptyOrSpaces(latestText) || myPopup.isShowing()) {
      return myOriginals.size() + pluralizeProcess(myOriginals.size()) + " running...";
    }
    int others = myOriginals.size() - 1;
    String trimmed = latestText.length() > 55 ? latestText.substring(0, 50) + "..." : latestText;
    return trimmed + " (" + others + " more" + pluralizeProcess(others) + ")";
  }

  private static String pluralizeProcess(int count) {
    return count == 1 ? " process" : " processes";
  }

  private ProgressIndicatorEx getLatestProgress() {
    return ContainerUtil.getLastItem(myOriginals);
  }

  @Override
  public void removeAll() {
    myMultiProcessLink = null;
    super.removeAll();
  }

  private void buildInInlineIndicator(@Nonnull MyInlineProgressIndicator inline) {
    removeAll();
    setLayout(new InlineLayout());
    final JRootPane pane = getRootPane();
    if (pane == null) return; // e.g. project frame is closed
    add(myRefreshAndInfoPanel);

    final JPanel inlinePanel = new JPanel(new BorderLayout());
    inline.getComponent().setBorder(JBUI.Borders.empty(1, 0, 0, 2));

    final JComponent inlineComponent = inline.getComponent();
    inlineComponent.setOpaque(false);

    JPanel iconAndProgress = new JPanel(new HorizontalLayout(5));
    iconAndProgress.setOpaque(false);
    iconAndProgress.add(myProgressIcon.get());
    iconAndProgress.add(inlineComponent);

    inlinePanel.add(iconAndProgress, BorderLayout.CENTER);

    inline.updateProgressNow();
    inlinePanel.setOpaque(false);

    add(inlinePanel);

    myRefreshAndInfoPanel.revalidate();
    myRefreshAndInfoPanel.repaint();

    if (inline.myPresentationModeProgressPanel != null) return;

    inline.myPresentationModeProgressPanel = new PresentationModeProgressPanel(inline);

    Component anchor = getAnchor(pane);
    final BalloonLayoutEx balloonLayout = getBalloonLayout(pane);

    Balloon balloon =
            JBPopupFactory.getInstance().createBalloonBuilder(inline.myPresentationModeProgressPanel.getProgressPanel()).setFadeoutTime(0).setFillColor(Gray.TRANSPARENT).setShowCallout(false)
                    .setBorderColor(Gray.TRANSPARENT).setBorderInsets(JBUI.emptyInsets()).setAnimationCycle(0).setCloseButtonEnabled(false).setHideOnClickOutside(false).setDisposable(inline)
                    .setHideOnFrameResize(false).setHideOnKeyOutside(false).setBlockClicksThroughBalloon(true).setHideOnAction(false).setShadow(false).createBalloon();
    if (balloonLayout != null) {
      class MyListener implements JBPopupListener, Runnable {
        @Override
        public void beforeShown(@Nonnull LightweightWindowEvent event) {
          balloonLayout.addListener(this);
        }

        @Override
        public void onClosed(@Nonnull LightweightWindowEvent event) {
          balloonLayout.removeListener(this);
        }

        @Override
        public void run() {
          if (!balloon.isDisposed()) {
            balloon.revalidate();
          }
        }
      }
      balloon.addListener(new MyListener());
    }
    balloon.show(new PositionTracker<Balloon>(anchor) {
      @Override
      public RelativePoint recalculateLocation(Balloon object) {
        Component c = getAnchor(pane);
        int y = c.getHeight() - JBUIScale.scale(45);
        if (balloonLayout != null && !isBottomSideToolWindowsVisible(pane)) {
          Component component = balloonLayout.getTopBalloonComponent();
          if (component != null) {
            y = SwingUtilities.convertPoint(component, 0, -JBUIScale.scale(45), c).y;
          }
        }

        return new RelativePoint(c, new Point(c.getWidth() - JBUIScale.scale(150), y));
      }
    }, Balloon.Position.above);
  }

  @Nullable
  private static BalloonLayoutEx getBalloonLayout(@Nonnull JRootPane pane) {
    Component parent = UIUtil.findUltimateParent(pane);
    if (parent instanceof Window) {
      consulo.ui.Window uiWindow = TargetAWT.from((Window)parent);

      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
      if (ideFrame == null) {
        return null;
      }
      return (BalloonLayoutEx)ideFrame.getBalloonLayout();
    }
    return null;
  }

  @Nonnull
  private static Component getAnchor(@Nonnull JRootPane pane) {
    Component tabWrapper = UIUtil.findComponentOfType(pane, TabbedPaneWrapper.TabWrapper.class);
    if (tabWrapper != null) {
      return tabWrapper;
    }
    FileEditorsSplitters splitters = AWTComponentProviderUtil.findChild(pane, FileEditorsSplitters.class);
    if (splitters != null) {
      return splitters.isShowing() ? splitters.getComponent() : pane;
    }
    FileEditorManagerEx ex = FileEditorManagerEx.getInstanceEx(ProjectUtil.guessCurrentProject(pane));
    if (ex == null) return pane;
    splitters = ex.getSplitters();
    return splitters.isShowing() ? splitters.getComponent() : pane;
  }

  private static boolean isBottomSideToolWindowsVisible(@Nonnull JRootPane parent) {
    ToolWindowPanelImplEx pane = AWTComponentProviderUtil.findChild(parent, ToolWindowPanelImplEx.class);
    return pane != null && pane.isBottomSideToolWindowsVisible();
  }

  public Couple<String> setText(@Nullable final String text, @Nullable final String requestor) {
    if (StringUtil.isEmpty(text) && !Comparing.equal(requestor, myCurrentRequestor) && !EventLog.LOG_REQUESTOR.equals(requestor)) {
      return Couple.of(myInfoPanel.getText(), myCurrentRequestor);
    }

    boolean logMode = myInfoPanel.updateText(EventLog.LOG_REQUESTOR.equals(requestor) ? "" : text);
    myCurrentRequestor = logMode ? EventLog.LOG_REQUESTOR : requestor;
    return Couple.of(text, requestor);
  }

  public void setRefreshVisible(final boolean visible) {
    UIUtil.invokeLaterIfNeeded(() -> myRefreshIcon.setVisible(visible));
  }

  public void setRefreshToolTipText(final String tooltip) {
    myRefreshIcon.setToolTipText(tooltip);
  }

  public BalloonHandler notifyByBalloon(MessageType type, String htmlBody, @Nullable Image icon, @Nullable HyperlinkListener listener) {
    final Balloon balloon =
            JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(htmlBody.replace("\n", "<br>"), icon != null ? icon : type.getDefaultIcon(), type.getPopupBackground(), listener).createBalloon();

    SwingUtilities.invokeLater(() -> {
      Balloon oldBalloon = SoftReference.dereference(myLastShownBalloon);
      if (oldBalloon != null) {
        balloon.setAnimationEnabled(false);
        oldBalloon.setAnimationEnabled(false);
        oldBalloon.hide();
      }
      myLastShownBalloon = new WeakReference<>(balloon);

      Component comp = this;
      if (comp.isShowing()) {
        int offset = comp.getHeight() / 2;
        Point point = new Point(comp.getWidth() - offset, comp.getHeight() - offset);
        balloon.show(new RelativePoint(comp, point), Balloon.Position.above);
      }
      else {
        final JRootPane rootPane = SwingUtilities.getRootPane(comp);
        if (rootPane != null && rootPane.isShowing()) {
          final Container contentPane = rootPane.getContentPane();
          final Rectangle bounds = contentPane.getBounds();
          final Point target = UIUtil.getCenterPoint(bounds, JBUI.size(1, 1));
          target.y = bounds.height - 3;
          balloon.show(new RelativePoint(contentPane, target), Balloon.Position.above);
        }
      }
    });

    return () -> SwingUtilities.invokeLater(balloon::hide);
  }

  private static class InlineLayout extends AbstractLayoutManager {
    private int myProgressWidth;

    @Override
    public Dimension preferredLayoutSize(final Container parent) {
      Dimension result = new Dimension();
      for (int i = 0; i < parent.getComponentCount(); i++) {
        final Dimension prefSize = parent.getComponent(i).getPreferredSize();
        result.width += prefSize.width;
        result.height = Math.max(prefSize.height, result.height);
      }
      return result;
    }

    @Override
    public void layoutContainer(final Container parent) {
      assert parent.getComponentCount() == 2; // 1. info; 2. progress

      Component infoPanel = parent.getComponent(0);
      Component progressPanel = parent.getComponent(1);
      int progressPrefWidth = progressPanel.getPreferredSize().width;

      final Dimension size = parent.getSize();
      int maxProgressWidth = (int)(size.width * 0.8);
      int minProgressWidth = (int)(size.width * 0.5);
      if (progressPrefWidth > myProgressWidth) {
        myProgressWidth = progressPrefWidth;
      }
      if (myProgressWidth > maxProgressWidth) {
        myProgressWidth = maxProgressWidth;
      }
      if (myProgressWidth < minProgressWidth) {
        myProgressWidth = minProgressWidth;
      }
      infoPanel.setBounds(0, 0, size.width - myProgressWidth, size.height);
      progressPanel.setBounds(size.width - myProgressWidth, 0, myProgressWidth, size.height);
    }
  }

  @Nonnull
  private MyInlineProgressIndicator createInlineDelegate(@Nonnull TaskInfo info, @Nonnull ProgressIndicatorEx original, boolean compact) {
    Collection<MyInlineProgressIndicator> inlines = myOriginal2Inlines.get(original);
    if (inlines != null) {
      for (MyInlineProgressIndicator eachInline : inlines) {
        if (eachInline.isCompact() == compact) return eachInline;
      }
    }

    MyInlineProgressIndicator inline = new MyInlineProgressIndicator(compact, info, original);

    myInline2Original.put(inline, original);
    myOriginal2Inlines.put(original, inline);

    if (compact) {
      inline.getComponent().addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          handle(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          handle(e);
        }
      });
    }

    return inline;
  }

  private void triggerPopupShowing() {
    if (myPopup.isShowing()) {
      hideProcessPopup();
    }
    else {
      openProcessPopup(true);
    }
  }

  private void restoreEmptyStatusInner() {
    removeAll();
    updateProgressIcon();
    AsyncProcessIcon progressIcon = myProgressIcon.isComputed() ? myProgressIcon.getValue() : null;
    Container iconParent = progressIcon == null ? null : progressIcon.getParent();
    if (iconParent != null) {
      // to prevent leaks to this removed parent via progress icon
      iconParent.remove(progressIcon);
    }
  }

  private void updateProgressIcon() {
    AsyncProcessIcon progressIcon = myProgressIcon.isComputed() ? myProgressIcon.getValue() : null;
    if (progressIcon == null) {
      return;
    }

    if (myOriginals.isEmpty() || PowerSaveMode.isEnabled() || myOriginals.stream().map(ProgressSuspender::getSuspender).allMatch(s -> s != null && s.isSuspended())) {
      progressIcon.suspend();
    }
    else {
      progressIcon.resume();
    }
  }

  private void restoreEmptyStatus() {
    restoreEmptyStatusInner();
    setLayout(new BorderLayout());
    add(myRefreshAndInfoPanel, BorderLayout.CENTER);

    myRefreshAndInfoPanel.revalidate();
    myRefreshAndInfoPanel.repaint();
  }

  //private String formatTime(long t) {
  //  if (t < 1000) return "< 1 sec";
  //  if (t < 60 * 1000) return (t / 1000) + " sec";
  //  return "~" + (int)Math.ceil(t / (60 * 1000f)) + " min";
  //}

  public boolean isProcessWindowOpen() {
    return myPopup.isShowing();
  }

  public void setProcessWindowOpen(final boolean open) {
    if (open) {
      openProcessPopup(true);
    }
    else {
      hideProcessPopup();
    }
  }

  private class MyInlineProgressIndicator extends InlineProgressIndicator {
    private ProgressIndicatorEx myOriginal;
    private PresentationModeProgressPanel myPresentationModeProgressPanel;

    MyInlineProgressIndicator(final boolean compact, @Nonnull TaskInfo task, @Nonnull ProgressIndicatorEx original) {
      super(compact, task);
      myOriginal = original;
      original.addStateDelegate(this);
      addStateDelegate(new AbstractProgressIndicatorExBase() {
        @Override
        public void cancel() {
          super.cancel();
          updateProgress();
        }
      });
      runOnProgressRelatedChange(this::queueProgressUpdate, this);
    }

    @Nonnull
    @Override
    public LocalizeValue getTextValue() {
      LocalizeValue textValue = super.getTextValue();
      ProgressSuspender suspender = getSuspender();
      return suspender != null && suspender.isSuspended() ? LocalizeValue.of(suspender.getSuspendedText()) : textValue;
    }

    @Override
    protected JBIterable<ProgressButton> createEastButtons() {
      return JBIterable.of(createSuspendButton()).append(super.createEastButtons());
    }

    private ProgressButton createSuspendButton() {
      InplaceButton suspendButton = new InplaceButton("", AllIcons.Actions.Pause, e -> {
        ProgressSuspender suspender = getSuspender();
        if (suspender == null) {
          LOG.assertTrue(myOriginal == null, "The process is expected to be finished at this point");
          return;
        }

        if (suspender.isSuspended()) {
          suspender.resumeProcess();
        }
        else {
          suspender.suspendProcess(null);
        }
        //UIEventLogger.logUIEvent(suspender.isSuspended() ? UIEventId.ProgressPaused : UIEventId.ProgressResumed);
      }).setFillBg(false);
      suspendButton.setVisible(false);

      return new ProgressButton(suspendButton, () -> {
        ProgressSuspender suspender = getSuspender();
        suspendButton.setVisible(suspender != null);
        if (suspender != null) {
          String toolTipText = suspender.isSuspended() ? "Resume" : "Pause";
          if (!toolTipText.equals(suspendButton.getToolTipText())) {
            updateProgressIcon();
          }
          suspendButton.setIcon(suspender.isSuspended() ? AllIcons.Actions.Resume : AllIcons.Actions.Pause);
          suspendButton.setToolTipText(toolTipText);
        }
      });
    }

    @Nullable
    private ProgressSuspender getSuspender() {
      ProgressIndicatorEx original = myOriginal;
      return original == null ? null : ProgressSuspender.getSuspender(original);
    }

    @Override
    public void stop() {
      super.stop();
      updateProgress();
    }

    @Override
    protected boolean isFinished() {
      TaskInfo info = getInfo();
      return info == null || isFinished(info);
    }

    @Override
    public void finish(@Nonnull final TaskInfo task) {
      super.finish(task);
      queueRunningUpdate(() -> removeProgress(this));
    }

    @Override
    public void dispose() {
      super.dispose();
      myOriginal = null;
    }

    @Override
    protected void cancelRequest() {
      myOriginal.cancel();
    }

    @Override
    protected void queueProgressUpdate() {
      synchronized (myDirtyIndicators) {
        myDirtyIndicators.add(this);
      }
      myUpdateQueue.queue(myUpdateIndicators);
    }

    @Override
    protected void queueRunningUpdate(@Nonnull final Runnable update) {
      myUpdateQueue.queue(new Update(new Object(), false, 0) {
        @Override
        public void run() {
          ApplicationManager.getApplication().invokeLater(update);
        }
      });
    }

    @Override
    public void updateProgressNow() {
      myProgress.setVisible(!PowerSaveMode.isEnabled() || !isPaintingIndeterminate());
      super.updateProgressNow();
      if (myPresentationModeProgressPanel != null) myPresentationModeProgressPanel.update();
      if (myOriginal == getLatestProgress() && myMultiProcessLink != null) {
        myMultiProcessLink.setText(getMultiProgressLinkText());
      }
    }
  }

  private void runQuery() {
    if (getRootPane() == null) return;

    Set<InlineProgressIndicator> indicators = getCurrentInlineIndicators();
    if (indicators.isEmpty()) return;

    for (InlineProgressIndicator each : indicators) {
      each.updateProgress();
    }
    myQueryAlarm.cancelAllRequests();
    myQueryAlarm.addRequest(this::runQuery, 2000);
  }

  @Nonnull
  private Set<InlineProgressIndicator> getCurrentInlineIndicators() {
    synchronized (myOriginals) {
      return myInline2Original.keySet();
    }
  }
}
