// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.impl.status;

import consulo.application.Application;
import consulo.application.PowerSaveMode;
import consulo.application.PowerSaveModeListener;
import consulo.application.internal.AbstractProgressIndicatorExBase;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;
import consulo.application.util.registry.Registry;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.progress.impl.ProgressSuspender;
import consulo.ide.impl.idea.openapi.progress.impl.ProgressSuspenderListener;
import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.ide.impl.idea.openapi.wm.impl.status.InlineProgressIndicator;
import consulo.ide.impl.project.ui.impl.StatusWidgetBorders;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.HorizontalLayout;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.LinkLabel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonHandler;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.util.collection.*;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SoftReference;
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

public class InfoAndProgressPanel extends JPanel implements Disposable, CustomStatusBarWidget {
    private static final Logger LOG = Logger.getInstance(InfoAndProgressPanel.class);
    private final ProcessPopup myPopup;

    private final List<ProgressIndicatorEx> myOriginals = new ArrayList<>();
    private final List<TaskInfo> myInfos = new ArrayList<>();
    private final Map<InlineProgressIndicator, ProgressIndicatorEx> myInline2Original = new HashMap<>();
    private final MultiValuesMap<ProgressIndicatorEx, MyInlineProgressIndicator> myOriginal2Inlines = new MultiValuesMap<>();

    private final MergingUpdateQueue myUpdateQueue;

    private boolean myShouldClosePopupAndOnProcessFinish;

    private boolean myDisposed;
    private WeakReference<Balloon> myLastShownBalloon;

    private final Set<InlineProgressIndicator> myDirtyIndicators = Sets.newHashSet(HashingStrategy.identity());
    private final Update myUpdateIndicators = new Update("UpdateIndicators", false, 1) {
        @Override
        @RequiredUIAccess
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
        super(new HorizontalLayout(0, SwingConstants.CENTER));
        setOpaque(false);
        setBorder(StatusWidgetBorders.DEFAULT_BORDER);
        setEnabled(false);

        myUpdateQueue = new MergingUpdateQueue("Progress indicator", 50, true, MergingUpdateQueue.ANY_COMPONENT);
        myPopup = new ProcessPopup(this);
    }

    private void runOnProgressRelatedChange(@Nonnull Runnable runnable, Disposable parentDisposable) {
        synchronized (myOriginals) {
            if (!myDisposed) {
                MessageBusConnection connection = Application.get().getMessageBus().connect(parentDisposable);
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

    @RequiredUIAccess
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
            ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.UNKNOWN, group)
                .getComponent()
                .show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void dispose() {
        synchronized (myOriginals) {
            for (InlineProgressIndicator indicator : myInline2Original.keySet()) {
                Disposer.dispose(indicator);
            }
            myInline2Original.clear();
            myOriginal2Inlines.clear();

            myDisposed = true;
        }
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Nonnull
    public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
        synchronized (myOriginals) {
            if (myOriginals.isEmpty()) {
                return Collections.emptyList();
            }

            List<Pair<TaskInfo, ProgressIndicator>> result = new ArrayList<>(myOriginals.size());
            for (int i = 0; i < myOriginals.size(); i++) {
                result.add(Pair.create(myInfos.get(i), myOriginals.get(i)));
            }

            return Collections.unmodifiableList(result);
        }
    }

    @RequiredUIAccess
    public void addProgress(@Nonnull ProgressIndicator original, @Nonnull TaskInfo info) {
        synchronized (myOriginals) {
            boolean veryFirst = !hasProgressIndicators();

            myOriginals.add((ProgressIndicatorEx)original);
            myInfos.add(info);

            MyInlineProgressIndicator expanded = createInlineDelegate(info, (ProgressIndicatorEx)original, false);
            MyInlineProgressIndicator compact = createInlineDelegate(info, (ProgressIndicatorEx)original, true);

            myPopup.addIndicator(expanded);

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

            setEnabled(true);
        }
    }

    private boolean hasProgressIndicators() {
        synchronized (myOriginals) {
            return !myOriginals.isEmpty();
        }
    }

    @RequiredUIAccess
    private void removeProgress(@Nonnull MyInlineProgressIndicator progress) {
        synchronized (myOriginals) {
            if (!myInline2Original.containsKey(progress)) {
                return; // already disposed
            }

            boolean last = myOriginals.size() == 1;
            boolean beforeLast = myOriginals.size() == 2;

            myPopup.removeIndicator(progress);

            ProgressIndicatorEx original = removeFromMaps(progress);
            if (myOriginals.contains(original)) {
                Disposer.dispose(progress);
                return;
            }

            if (last) {
                if (myShouldClosePopupAndOnProcessFinish) {
                    hideProcessPopup();
                }
            }
            else if (myPopup.isShowing() || myOriginals.size() > 1) {
                buildInProcessCount();
            }
            else if (beforeLast) {
                buildInInlineIndicator(createInlineDelegate(myInfos.get(0), myOriginals.get(0), true));
            }

            runQuery();

            setEnabled(!myInfos.isEmpty());
        }
        Disposer.dispose(progress);
    }

    private ProgressIndicatorEx removeFromMaps(@Nonnull MyInlineProgressIndicator progress) {
        ProgressIndicatorEx original = myInline2Original.get(progress);

        myInline2Original.remove(progress);
        synchronized (myDirtyIndicators) {
            myDirtyIndicators.remove(progress);
        }

        myOriginal2Inlines.remove(original, progress);
        if (myOriginal2Inlines.get(original) == null) {
            int originalIndex = myOriginals.indexOf(original);
            myOriginals.remove(originalIndex);
            myInfos.remove(originalIndex);
        }

        return original;
    }

    @RequiredUIAccess
    private void openProcessPopup(boolean requestFocus) {
        synchronized (myOriginals) {
            if (myPopup.isShowing()) {
                return;
            }
            myPopup.show(requestFocus);
            if (hasProgressIndicators()) {
                myShouldClosePopupAndOnProcessFinish = true;
                buildInProcessCount();
            }
            else {
                myShouldClosePopupAndOnProcessFinish = false;
            }
        }
    }

    @RequiredUIAccess
    void hideProcessPopup() {
        synchronized (myOriginals) {
            if (!myPopup.isShowing()) {
                return;
            }
            myPopup.hide();

            if (myOriginals.size() == 1) {
                buildInInlineIndicator(createInlineDelegate(myInfos.get(0), myOriginals.get(0), true));
            }
            else if (!hasProgressIndicators()) {
                // nothing?
            }
            else {
                buildInProcessCount();
            }
        }
    }

    private void buildInProcessCount() {
        removeAll();

        myMultiProcessLink = new LinkLabel<>(getMultiProgressLinkText(), null, (aSource, aLinkData) -> triggerPopupShowing());

        add(myMultiProcessLink);

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

    @RequiredUIAccess
    private void buildInInlineIndicator(@Nonnull MyInlineProgressIndicator inline) {
        removeAll();
        
        JRootPane pane = getRootPane();
        if (pane == null) {
            return; // e.g. project frame is closed
        }

        inline.updateProgressNow();

        add(inline.getComponent());

        revalidate();

        repaint();
    }

    public BalloonHandler notifyByBalloon(MessageType type, String htmlBody, @Nullable Image icon, @Nullable HyperlinkListener listener) {
        Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                htmlBody.replace("\n", "<br>"),
                icon != null ? icon : type.getDefaultIcon(),
                type.getPopupBackground(),
                listener
            )
            .createBalloon();

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
                JRootPane rootPane = SwingUtilities.getRootPane(comp);
                if (rootPane != null && rootPane.isShowing()) {
                    Container contentPane = rootPane.getContentPane();
                    Rectangle bounds = contentPane.getBounds();
                    Point target = UIUtil.getCenterPoint(bounds, JBUI.size(1, 1));
                    target.y = bounds.height - 3;
                    balloon.show(new RelativePoint(contentPane, target), Balloon.Position.above);
                }
            }
        });

        return () -> SwingUtilities.invokeLater(balloon::hide);
    }

    @Nonnull
    @RequiredUIAccess
    private MyInlineProgressIndicator createInlineDelegate(@Nonnull TaskInfo info, @Nonnull ProgressIndicatorEx original, boolean compact) {
        Collection<MyInlineProgressIndicator> inlines = myOriginal2Inlines.get(original);
        if (inlines != null) {
            for (MyInlineProgressIndicator eachInline : inlines) {
                if (eachInline.isCompact() == compact) {
                    return eachInline;
                }
            }
        }

        MyInlineProgressIndicator inline = new MyInlineProgressIndicator(compact, info, original);

        myInline2Original.put(inline, original);
        myOriginal2Inlines.put(original, inline);

        if (compact) {
            inline.getComponent().addMouseListener(new MouseAdapter() {
                @Override
                @RequiredUIAccess
                public void mousePressed(MouseEvent e) {
                    handle(e);
                }

                @Override
                @RequiredUIAccess
                public void mouseReleased(MouseEvent e) {
                    handle(e);
                }
            });
        }

        return inline;
    }

    @RequiredUIAccess
    private void triggerPopupShowing() {
        if (myPopup.isShowing()) {
            hideProcessPopup();
        }
        else {
            openProcessPopup(true);
        }
    }

    public boolean isProcessWindowOpen() {
        return myPopup.isShowing();
    }

    @RequiredUIAccess
    public void setProcessWindowOpen(boolean open) {
        if (open) {
            openProcessPopup(true);
        }
        else {
            hideProcessPopup();
        }
    }

    @Nonnull
    @Override
    public String getId() {
        return "progress";
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation() {
        return null;
    }

    @Override
    public void install(@Nonnull StatusBar statusBar) {

    }

    private class MyInlineProgressIndicator extends InlineProgressIndicator {
        private class SuspendAction extends DumbAwareAction {
            public SuspendAction() {
                super(LocalizeValue.of(), LocalizeValue.of(), PlatformIconGroup.actionsPause());
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                ProgressSuspender suspender = getSuspender();
                if (suspender == null) {
                    LOG.assertTrue(myOriginal == null, "The process is expected to be finished at this point");
                    return;
                }

                if (suspender.isSuspended()) {
                    suspender.resumeProcess();
                }
                else {
                    suspender.suspendProcess(LocalizeValue.empty());
                }
                //UIEventLogger.logUIEvent(suspender.isSuspended() ? UIEventId.ProgressPaused : UIEventId.ProgressResumed);
            }

            @RequiredUIAccess
            @Override
            public void update(@Nonnull AnActionEvent e) {
                Presentation presentation = e.getPresentation();

                ProgressSuspender suspender = getSuspender();
                presentation.setEnabledAndVisible(suspender != null);
                if (suspender != null) {
                    presentation.setIcon(suspender.isSuspended() ? PlatformIconGroup.actionsResume() : PlatformIconGroup.actionsPause());
                    presentation.setText(suspender.isSuspended() ? "Resume" : "Pause");
                }
            }
        }

        private ProgressIndicatorEx myOriginal;

        @RequiredUIAccess
        MyInlineProgressIndicator(boolean compact, @Nonnull TaskInfo task, @Nonnull ProgressIndicatorEx original) {
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
            return suspender != null && suspender.isSuspended() ? suspender.getSuspendedText() : textValue;
        }

        @Override
        public List<AnAction> createEastButtons() {
            return Lists.append(super.createEastButtons(), new SuspendAction());
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
        public void finish(@Nonnull TaskInfo task) {
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
        @RequiredUIAccess
        protected void queueProgressUpdate() {
            synchronized (myDirtyIndicators) {
                myDirtyIndicators.add(this);
            }
            myUpdateQueue.queue(myUpdateIndicators);
        }

        @Override
        protected void queueRunningUpdate(@RequiredUIAccess @Nonnull final Runnable update) {
            myUpdateQueue.queue(new Update(new Object(), false, 0) {
                @Override
                public void run() {
                    Application.get().invokeLater(update);
                }
            });
        }

        @Override
        @RequiredUIAccess
        public void updateProgressNow() {
            myProgress.setVisible(!PowerSaveMode.isEnabled() || !isPaintingIndeterminate());
            super.updateProgressNow();
            if (myOriginal == getLatestProgress() && myMultiProcessLink != null) {
                myMultiProcessLink.setText(getMultiProgressLinkText());
            }
        }
    }

    private void runQuery() {
        if (getRootPane() == null) {
            return;
        }

        Set<InlineProgressIndicator> indicators = getCurrentInlineIndicators();
        if (indicators.isEmpty()) {
            return;
        }

        for (InlineProgressIndicator each : indicators) {
            each.updateProgress();
        }
    }

    @Nonnull
    private Set<InlineProgressIndicator> getCurrentInlineIndicators() {
        synchronized (myOriginals) {
            return myInline2Original.keySet();
        }
    }
}
