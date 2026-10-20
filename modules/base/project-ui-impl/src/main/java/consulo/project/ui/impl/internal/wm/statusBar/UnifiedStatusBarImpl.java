/*
 * Copyright 2013-2017 consulo.io
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
package consulo.project.ui.impl.internal.wm.statusBar;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ui.impl.internal.notification.StatusBarBalloonHolder;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.util.collection.Lists;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class UnifiedStatusBarImpl implements StatusBarEx {
    private record WidgetBean(
        StatusBarWidget widget,
        @Nullable StatusBarWidgetButton button,
        consulo.ui.@Nullable Component uiComponent
    ) {
        @RequiredUIAccess
        void update() {
            if (button != null) {
                button.beforeUpdate();
            }
            else {
                widget.beforeUpdate();
            }
        }
    }

    private IdeFrame myFrame;

    private final Map<String, WidgetBean> myWidgetMap = new LinkedHashMap<>();

    private @Nullable HorizontalLayout myLeftPanel;
    private @Nullable HorizontalLayout myRightPanel;
    private @Nullable HorizontalLayout myCenterPanel;


    private final DockLayout myComponent = DockLayout.create();

    private UnifiedInfoAndProgressPanel myInfoAndProgressPanel;

    private final @Nullable StatusBar myMaster;

    private UnifiedToolWindowsSwitcher mySwitcher;

    private final StatusBarBalloonHolder myBalloons = new StatusBarBalloonHolder();

    @RequiredUIAccess
    public UnifiedStatusBarImpl(@Nullable StatusBar master) {
        myMaster = master;

        myInfoAndProgressPanel = new UnifiedInfoAndProgressPanel(this::uiAccess);
        Disposer.register(this, myInfoAndProgressPanel);

        centerPanel().add(myInfoAndProgressPanel.getUIComponent());

        myComponent.putUserData(UiDataProvider.KEY, sink -> {
            sink.set(Project.KEY, getProject());
            sink.set(StatusBar.KEY, this);
        });

        StatusBarWidgetContextMenu.install(myComponent, null);

        myComponent.borderBuilder().topSet().apply();
    }

    /**
     * The bar is shown in one frame, and on the web a frame belongs to a single browser session - the access of
     * its own component is the one its widgets may be pushed into. While the bar is not attached yet, the ui of
     * its project is.
     */
    private @Nullable UIAccess uiAccess() {
        UIAccess uiAccess = myComponent.getUIAccess();
        if (uiAccess != null) {
            return uiAccess;
        }

        Project project = getProject();
        if (project != null) {
            return project.getUIAccess();
        }
        return UIAccess.isUIThread() ? UIAccess.current() : null;
    }

    private void giveIfNeed(@RequiredUIAccess Runnable runnable) {
        UIAccess uiAccess = uiAccess();
        if (uiAccess != null && uiAccess.isValid()) {
            uiAccess.giveIfNeed(runnable);
        }
    }

    @Override
    public void addWidget(StatusBarWidget widget, List<String> order, Disposable parentDisposable) {
        giveIfNeed(() -> addWidget(widget, order));

        Disposer.register(parentDisposable, () -> removeWidget(widget.getId()));
    }

    @RequiredUIAccess
    private void addWidget(StatusBarWidget widget, List<String> order) {
        UIAccess.assertIsUIThread();

        WidgetBean bean = wrap(widget);
        myWidgetMap.put(widget.getId(), bean);

        layoutWidgets(order);

        widget.install(this);
        bean.update();

        Disposer.register(this, widget);
    }

    @RequiredUIAccess
    private void layoutWidgets(List<String> order) {
        List<WidgetBean> beans = new ArrayList<>(myWidgetMap.values());
        Lists.weightSort(beans, bean -> order.indexOf(bean.widget().getId()));

        HorizontalLayout panel = rightPanel();
        panel.removeAll();
        for (WidgetBean bean : beans) {
            if (bean.uiComponent() != null) {
                panel.add(bean.uiComponent());
            }
        }
    }

    @RequiredUIAccess
    private static WidgetBean wrap(StatusBarWidget widget) {
        if (widget instanceof CustomStatusBarWidget customWidget) {
            return new WidgetBean(widget, null, new StatusBarCustomWidgetHolder(customWidget).getComponent());
        }

        StatusBarWidget.WidgetPresentation presentation = widget.getPresentation();
        if (presentation == null) {
            return new WidgetBean(widget, null, null);
        }

        StatusBarWidgetButton button = new StatusBarWidgetButton(widget, presentation);
        return new WidgetBean(widget, button, button.getComponent());
    }

    @RequiredUIAccess
    private HorizontalLayout rightPanel() {
        if (myRightPanel == null) {
            myRightPanel = HorizontalLayout.create(Space.NONE);
            myComponent.right(myRightPanel);
        }
        return myRightPanel;
    }

    @RequiredUIAccess
    private HorizontalLayout leftPanel() {
        if (myLeftPanel == null) {
            myLeftPanel = HorizontalLayout.create();
            myComponent.left(myLeftPanel);
        }
        return myLeftPanel;
    }

    @RequiredUIAccess
    private HorizontalLayout centerPanel() {
        if (myCenterPanel == null) {
            myCenterPanel = HorizontalLayout.create();
            myComponent.center(myCenterPanel);
        }
        return myCenterPanel;
    }

    @Override
    public void removeWidget(String id) {
        UIAccess uiAccess = uiAccess();
        if (uiAccess != null && uiAccess.isValid()) {
            uiAccess.give(() -> {
                WidgetBean bean = myWidgetMap.remove(id);
                if (bean != null) {
                    if (bean.uiComponent() != null) {
                        rightPanel().remove(bean.uiComponent());
                    }
                    Disposer.dispose(bean.widget());
                }
            });
        }
        else {
            WidgetBean bean = myWidgetMap.remove(id);
            if (bean != null) {
                Disposer.dispose(bean.widget());
            }
        }
    }

    @Override
    public consulo.ui.@Nullable Component getWidgetComponent(String id) {
        WidgetBean bean = myWidgetMap.get(id);
        return bean == null ? null : bean.uiComponent();
    }

    @Override
    public consulo.ui.Component getUIComponent() {
        return myComponent;
    }

    @Override
    @RequiredUIAccess
    public void notifyByBalloon(
        String widgetId,
        NotificationType type,
        String htmlBody,
        @Nullable Image icon,
        @Nullable ComponentEventListener<consulo.ui.Component, HyperlinkEvent> linkListener
    ) {
        consulo.ui.Component target;
        if (PROGRESS_WIDGET_ID.equals(widgetId)) {
            UnifiedInfoAndProgressPanel panel = myInfoAndProgressPanel;
            target = panel == null ? null : panel.getUIComponent();
        }
        else {
            target = getWidgetComponent(widgetId);
        }
        myBalloons.show(widgetId, target, type, htmlBody, icon, linkListener);
    }

    @Override
    @RequiredUIAccess
    public void hideWidgetBalloon(String widgetId) {
        myBalloons.hide(widgetId);
    }

    @Override
    public StatusBar createChild() {
        return null;
    }

    @Override
    public StatusBar findChild(Component c) {
        return null;
    }

    @Override
    public IdeFrame getFrame() {
        return myFrame;
    }

    @Override
    public void install(IdeFrame frame) {
        myFrame = frame;

        // the switcher hides itself while getProject() is null, and the project is only reachable through the frame
        if (myMaster == null) {
            giveIfNeed(this::installToolWindowsSwitcher);
        }
    }

    /** The switcher is the leftmost item of the bar. */
    @RequiredUIAccess
    protected void installToolWindowsSwitcher() {
        if (mySwitcher == null) {
            mySwitcher = new UnifiedToolWindowsSwitcher(this);
            Disposer.register(this, mySwitcher);
            addToLeft(mySwitcher.getUIComponent());
        }

        mySwitcher.update();
    }

    /**
     * Consulo renders the bottom tool window stripe and the status bar as one row, the stripe buttons taking
     * the left side of the bar.
     */
    @RequiredUIAccess
    public void addToLeft(consulo.ui.Component component) {
        leftPanel().add(component);
    }

    @Override
    public @Nullable Project getProject() {
        return myFrame == null ? null : myFrame.getProject();
    }

    @Override
    public void dispose() {
        myWidgetMap.clear();
    }

    @Override
    public void startRefreshIndication(String tooltipText) {
    }

    @Override
    public void stopRefreshIndication() {
    }

    @Override
    public void addProgress(ProgressIndicator indicator, TaskInfo info) {
        UnifiedInfoAndProgressPanel panel = myInfoAndProgressPanel;
        if (panel == null) {
            return;
        }

        // a task announces itself from whatever thread starts it, and the panel it goes into is built of components
        giveIfNeed(() -> panel.addProgress(indicator, info));
    }

    @Override
    public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
        return myInfoAndProgressPanel == null ? List.of() : myInfoAndProgressPanel.getBackgroundProcesses();
    }

    @Override
    public void updateWidgets() {
        for (String id : myWidgetMap.keySet()) {
            updateWidget(id);
        }
    }

    @Override
    public void updateWidget(String id) {
        giveIfNeed(() -> {
            WidgetBean bean = myWidgetMap.get(id);
            if (bean != null) {
                bean.update();
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <W extends StatusBarWidget> Optional<W> findWidget(Predicate<StatusBarWidget> predicate) {
        return myWidgetMap
            .values()
            .stream()
            .filter(widgetBean -> predicate.test(widgetBean.widget()))
            .map(widgetBean -> (W) widgetBean.widget())
            .findFirst();
    }

    @Override
    public void updateWidget(Predicate<StatusBarWidget> widgetPredicate) {
        myWidgetMap
            .entrySet()
            .stream()
            .filter(it -> widgetPredicate.test(it.getValue().widget()))
            .forEach(it -> updateWidget(it.getKey()));
    }

    @Override
    public boolean isProcessWindowOpen() {
        return myInfoAndProgressPanel != null && myInfoAndProgressPanel.isProcessWindowOpen();
    }

    @Override
    @RequiredUIAccess
    public void setProcessWindowOpen(boolean open) {
        if (myInfoAndProgressPanel != null) {
            myInfoAndProgressPanel.setProcessWindowOpen(open);
        }
    }

    @Override
    public Dimension getSize() {
        return null;
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
