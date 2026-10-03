/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.profiler.impl.internal.session;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.profiler.configuration.ProfilerFeature;
import consulo.execution.profiler.impl.internal.view.ProfilerCaptureViewFactory;
import consulo.execution.profiler.impl.internal.view.ProfilerUIUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerSessionPanel implements Disposable {
    private static final String TOOLBAR_PLACE = "ProfilerSessionToolbar";

    private static final int LIVE_AREA_PROPORTION = 40;

    private abstract class PanelAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        protected PanelAction(LocalizeValue text, Image icon) {
            super(text, LocalizeValue.empty(), icon);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!myDisposed && !mySession.isDisposed() && isActionEnabled());
        }

        protected abstract boolean isActionEnabled();
    }

    private final class StartCpuRecordingAction extends PanelAction {
        private StartCpuRecordingAction() {
            super(LocalizeValue.localizeTODO("Start CPU Recording"), PlatformIconGroup.actionsProfilecpu());
        }

        @Override
        protected boolean isActionEnabled() {
            return !mySession.isRecording() && !mySession.isCommandRunning() && mySession.isActive();
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            mySession.startCpuRecording();
        }
    }

    private final class StopCpuRecordingAction extends PanelAction {
        private StopCpuRecordingAction() {
            super(LocalizeValue.localizeTODO("Stop CPU Recording"), PlatformIconGroup.actionsProfilered());
        }

        @Override
        protected boolean isActionEnabled() {
            return mySession.isRecording() && !mySession.isCommandRunning();
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            mySession.stopCpuRecording();
        }
    }

    private final class DumpHeapAction extends PanelAction {
        private DumpHeapAction() {
            super(LocalizeValue.localizeTODO("Capture Heap Dump"), PlatformIconGroup.actionsProfilememory());
        }

        @Override
        protected boolean isActionEnabled() {
            return !mySession.isCommandRunning() && mySession.isActive();
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            mySession.dumpHeap();
        }
    }

    private final class DumpThreadsAction extends PanelAction {
        private DumpThreadsAction() {
            super(LocalizeValue.localizeTODO("Capture Thread Dump"), PlatformIconGroup.actionsDump());
        }

        @Override
        protected boolean isActionEnabled() {
            return !mySession.isCommandRunning() && mySession.isActive();
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            mySession.dumpThreads();
        }
    }

    private final class StopProfilingAction extends PanelAction {
        private StopProfilingAction() {
            super(LocalizeValue.localizeTODO("Stop Profiling"), PlatformIconGroup.actionsSuspend());
        }

        @Override
        protected boolean isActionEnabled() {
            return mySession.canStop();
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            mySession.stop();
        }
    }

    private final class CloseSessionAction extends PanelAction {
        private CloseSessionAction() {
            super(LocalizeValue.localizeTODO("Close Session"), PlatformIconGroup.actionsClose());
        }

        @Override
        protected boolean isActionEnabled() {
            return !mySession.isActive();
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            mySessionManager.closeSession(mySession);
        }
    }

    private final class ShowCapturesAction extends PanelAction {
        private ShowCapturesAction() {
            super(LocalizeValue.localizeTODO("Open Capture"), PlatformIconGroup.actionsListfiles());
        }

        @Override
        protected boolean isActionEnabled() {
            return !mySession.getCaptures().isEmpty();
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            List<ProfilerCapture> captures = mySession.getCaptures();
            if (captures.isEmpty()) {
                return;
            }

            String title = LocalizeValue.localizeTODO("Open Capture").get();
            BaseListPopupStep<ProfilerCapture> step = new BaseListPopupStep<ProfilerCapture>(title, captures) {
                @Override
                public String getTextFor(ProfilerCapture value) {
                    return value.getTitle().get();
                }

                @Override
                public Image getIconFor(ProfilerCapture value) {
                    return value.getIcon();
                }

                @Override
                public PopupStep<?> onChosen(ProfilerCapture selectedValue, boolean finalChoice) {
                    return doFinalStep(() -> mySessionManager.openCaptureEditor(selectedValue, true));
                }
            };
            myPopupFactory.createListPopup(step).showUnderneathOf(e);
        }
    }

    private final Project myProject;
    private final ProfilerSession mySession;
    private final ProfilerSessionLayout myLayout;
    private final ProfilerSessionManager mySessionManager;
    private final ProfilerCaptureViewFactory myCaptureViewFactory;
    private final JBPopupFactory myPopupFactory;
    private final ActionToolbar myToolbar;
    private final Label myStatusLabel;
    private final DockLayout myCaptureHolder;
    private final DockLayout myRoot;
    private final @Nullable ProfilerLiveView myLiveView;
    private final @Nullable Component myLiveArea;
    private final @Nullable DockLayout myLiveBody;
    private final Map<ProfilerCapture, Disposable> myCaptureDisposables = new IdentityHashMap<>();
    private final Map<ProfilerCapture, Tab> myCaptureTabMap = new IdentityHashMap<>();

    private Component myEmptyCapturesMessage;
    private @Nullable TabbedLayout myCaptureTabs;
    private boolean myCaptureAreaShown;
    private boolean myFailureShown;

    private volatile boolean myDisposed;

    @RequiredUIAccess
    public ProfilerSessionPanel(
        Project project,
        ProfilerSession session,
        ProfilerSessionLayout layout,
        ProfilerSessionManager sessionManager,
        ProfilerCaptureViewFactory captureViewFactory,
        ActionToolbarFactory toolbarFactory,
        JBPopupFactory popupFactory
    ) {
        myProject = project;
        mySession = session;
        myLayout = layout;
        mySessionManager = sessionManager;
        myCaptureViewFactory = captureViewFactory;
        myPopupFactory = popupFactory;

        DefaultActionGroup actions = new DefaultActionGroup();
        if (session.getFeatures().contains(ProfilerFeature.CPU_RECORDING)) {
            actions.add(new StartCpuRecordingAction());
            actions.add(new StopCpuRecordingAction());
        }
        if (session.getFeatures().contains(ProfilerFeature.HEAP_DUMP)) {
            actions.add(new DumpHeapAction());
        }
        if (session.getFeatures().contains(ProfilerFeature.THREAD_DUMP)) {
            actions.add(new DumpThreadsAction());
        }
        if (session.isActive()) {
            actions.add(new StopProfilingAction());
        }
        if (actions.getChildrenCount() > 0) {
            actions.addSeparator();
        }
        if (layout == ProfilerSessionLayout.COMPACT) {
            actions.add(new ShowCapturesAction());
        }
        actions.add(new CloseSessionAction());

        myToolbar = toolbarFactory.createActionToolbar(TOOLBAR_PLACE, actions, ActionToolbar.Style.HORIZONTAL);

        myStatusLabel = Label.create();

        myCaptureHolder = DockLayout.create(Space.NONE);
        myEmptyCapturesMessage = ProfilerUIUtil.hint(getEmptyCapturesText());

        myRoot = DockLayout.create(Space.NONE);
        DockLayout header = DockLayout.create(Space.SMALL);
        header.left(myToolbar.getUIComponent());
        header.center(myStatusLabel);
        myRoot.top(header);

        if (session.hasLiveMonitoring()) {
            ProfilerLiveHistory history = session.getLiveHistory();
            ProfilerLiveView liveView = new ProfilerLiveView(project, layout, history);
            Disposer.register(this, liveView);
            Disposer.register(liveView, history.attach(liveView));

            myLiveView = liveView;
            myLiveArea = liveView.getComponent();
            if (layout == ProfilerSessionLayout.COMPACT) {
                myLiveBody = null;
                myRoot.center(myLiveArea);
            }
            else {
                myLiveBody = DockLayout.create(Space.NONE);
                showLiveArea();
                myRoot.center(myLiveBody);
            }
        }
        else {
            myLiveView = null;
            myLiveArea = null;
            myLiveBody = null;
            myCaptureHolder.center(myEmptyCapturesMessage);
            myRoot.center(myCaptureHolder);
        }

        myToolbar.setTargetUIComponent(myRoot);

        Disposer.register(this, session.addListener(new ProfilerSessionListener() {
            @Override
            @RequiredUIAccess
            public void statusChanged(ProfilerSession changed) {
                applyStatus();
            }

            @Override
            @RequiredUIAccess
            public void captureAdded(ProfilerSession changed, ProfilerCapture capture) {
                showCapture(capture, true);
            }

            @Override
            @RequiredUIAccess
            public void captureRemoved(ProfilerSession changed, ProfilerCapture capture) {
                removeCaptureView(capture);
            }
        }));

        List<ProfilerCapture> captures = session.getCaptures();
        for (int i = 0; i < captures.size(); i++) {
            ProfilerCapture capture = captures.get(i);
            if (layout == ProfilerSessionLayout.FULL || !capture.isShown()) {
                showCapture(capture, i == captures.size() - 1);
            }
        }

        applyStatus();
    }

    public ProfilerSession getSession() {
        return mySession;
    }

    public Component getComponent() {
        return myRoot;
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    @RequiredUIAccess
    private void applyStatus() {
        if (myDisposed || myProject.isDisposed()) {
            return;
        }

        myStatusLabel.setText(mySession.getStatus());
        myStatusLabel.setForegroundColor(mySession.isStatusError() ? ComponentColors.ERROR_FOREGROUND : null);

        ProfilerLiveView liveView = myLiveView;
        if (liveView != null && !mySession.isActive()) {
            liveView.stopFollowingLatest();
        }

        LocalizeValue failure = mySession.getFailure();
        boolean noCaptures = myLayout == ProfilerSessionLayout.COMPACT ? mySession.getCaptures().isEmpty() : myCaptureDisposables.isEmpty();
        if (failure != null && !myFailureShown && noCaptures) {
            myFailureShown = true;
            showEmptyCapturesMessage(ProfilerUIUtil.error(failure));
        }

        updateToolbar();
    }

    @RequiredUIAccess
    private void showCapture(ProfilerCapture capture, boolean select) {
        if (myDisposed || myProject.isDisposed()) {
            return;
        }

        if (myLayout == ProfilerSessionLayout.COMPACT) {
            mySessionManager.openCaptureEditor(capture, false);
            if (myLiveArea == null && !myFailureShown) {
                showEmptyCapturesMessage(ProfilerUIUtil.hint(getEmptyCapturesText()));
            }
            updateToolbar();
            return;
        }

        capture.markShown();

        Disposable captureDisposable = Disposable.newDisposable("ProfilerCapture");
        Disposer.register(this, captureDisposable);

        Component view = myCaptureViewFactory.createView(capture.getData(), captureDisposable);
        myCaptureDisposables.put(capture, captureDisposable);

        if (!mySession.isLive()) {
            myCaptureHolder.removeAll();
            myCaptureHolder.center(view);
            return;
        }

        showCaptureArea();

        TabbedLayout tabs = myCaptureTabs;
        if (tabs == null) {
            tabs = TabbedLayout.create();
            myCaptureTabs = tabs;
            myCaptureHolder.removeAll();
            myCaptureHolder.center(tabs);
        }

        Tab tab = ProfilerUIUtil.addTab(
            tabs,
            capture.getTitle(),
            view,
            capture.isClosable() ? (closedTab, component) -> mySession.removeCapture(capture) : null
        );
        myCaptureTabMap.put(capture, tab);
        if (select) {
            tab.select();
        }
    }

    @RequiredUIAccess
    private void removeCaptureView(ProfilerCapture capture) {
        Disposable captureDisposable = myCaptureDisposables.remove(capture);
        Tab tab = myCaptureTabMap.remove(capture);
        TabbedLayout tabs = myCaptureTabs;
        if (tab != null && tabs != null) {
            tabs.removeTab(tab);
        }
        if (captureDisposable != null) {
            Disposer.dispose(captureDisposable);
        }

        if (myDisposed || tabs == null || !myCaptureTabMap.isEmpty()) {
            updateToolbar();
            return;
        }

        myCaptureTabs = null;
        myCaptureHolder.removeAll();
        if (myLiveBody != null) {
            showLiveArea();
        }
        else {
            myCaptureHolder.center(myEmptyCapturesMessage);
        }
        updateToolbar();
    }

    @RequiredUIAccess
    private void showLiveArea() {
        DockLayout liveBody = myLiveBody;
        Component liveArea = myLiveArea;
        if (liveBody == null || liveArea == null) {
            return;
        }

        myCaptureAreaShown = false;
        liveBody.removeAll();
        liveBody.center(liveArea);
        liveBody.bottom(myEmptyCapturesMessage);
    }

    @RequiredUIAccess
    private void showCaptureArea() {
        DockLayout liveBody = myLiveBody;
        Component liveArea = myLiveArea;
        if (myCaptureAreaShown || liveBody == null || liveArea == null) {
            return;
        }

        myCaptureAreaShown = true;
        liveBody.removeAll();

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        split.setFirstComponent(liveArea);
        split.setSecondComponent(myCaptureHolder);
        split.setProportion(LIVE_AREA_PROPORTION);
        liveBody.center(split);
    }

    @RequiredUIAccess
    private void showEmptyCapturesMessage(Component message) {
        myEmptyCapturesMessage = message;
        if (myLiveBody != null) {
            if (!myCaptureAreaShown) {
                myLiveBody.bottom(message);
            }
            return;
        }

        if (myLiveArea != null) {
            return;
        }

        myCaptureHolder.removeAll();
        myCaptureHolder.center(message);
    }

    @RequiredUIAccess
    private void updateToolbar() {
        myToolbar.updateActionsAsync();
    }

    @RequiredUIAccess
    private LocalizeValue getEmptyCapturesText() {
        if (myLayout == ProfilerSessionLayout.COMPACT) {
            return mySession.isLive() || !mySession.getCaptures().isEmpty()
                ? LocalizeValue.localizeTODO("Captures open in editor tabs")
                : LocalizeValue.localizeTODO("The results open in an editor tab when profiling ends");
        }
        if (!mySession.isLive()) {
            return LocalizeValue.localizeTODO("The results appear here when profiling ends");
        }
        if (mySession.getFeatures().contains(ProfilerFeature.CPU_RECORDING)) {
            return LocalizeValue.localizeTODO("Start a CPU recording to capture call stacks");
        }
        return LocalizeValue.localizeTODO("Captures appear here");
    }
}
