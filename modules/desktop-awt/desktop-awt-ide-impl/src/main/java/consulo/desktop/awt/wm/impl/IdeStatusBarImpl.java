/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.wm.impl;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.desktop.awt.wm.impl.status.InfoAndProgressPanel;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.project.Project;
import consulo.project.ui.impl.internal.wm.statusBar.StatusBarCustomWidgetHolder;
import consulo.project.ui.impl.internal.wm.statusBar.StatusBarWidgetButton;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.*;
import consulo.ui.NotificationType;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.TabsUtil;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.BalloonHandler;
import consulo.ui.image.Image;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author spLeaner
 */
public class IdeStatusBarImpl extends JPanel implements StatusBarEx, UiDataProvider {
    private static final Key<String> WIDGET_ID = Key.create("STATUS_BAR_WIDGET_ID");

    private static final int MIN_ICON_HEIGHT = 24 + 1 + 1;

    private final Application myApplication;
    private IdeFrame myFrame;

    private static final String uiClassID = "IdeStatusBarUI";

    private final Map<String, WidgetBean> myWidgetMap = new LinkedHashMap<>();

    private JPanel myLeftPanel;
    private JPanel myRightPanel;
    private consulo.ui.@Nullable Component myUIComponent;

    private final Set<IdeStatusBarImpl> myChildren = new HashSet<>();

    private record WidgetBean(
        StatusBarWidget widget,
        consulo.ui.Component uiComponent,
        JComponent component,
        @Nullable StatusBarWidgetButton button
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

    @Override
    public StatusBar findChild(Component c) {
        Component eachParent = c;
        IdeFrame frame = null;
        while (eachParent != null) {
            if (eachParent instanceof Window) {
                consulo.ui.Window uiWindow = TargetAWT.from((Window) eachParent);
                frame = uiWindow.getUserData(IdeFrame.KEY);
            }
            eachParent = eachParent.getParent();
        }

        return frame != null ? frame.getStatusBar() : this;
    }

    @Override
    public void install(IdeFrame frame) {
        myFrame = frame;
    }

    @Override
    public @Nullable Project getProject() {
        return myFrame == null ? null : myFrame.getProject();
    }

    private void updateChildren(@RequiredUIAccess Consumer<IdeStatusBarImpl> action) {
        for (IdeStatusBarImpl child : myChildren) {
            action.accept(child);
        }
    }

    @Override
    @RequiredUIAccess
    public StatusBar createChild() {
        UIAccess.assertIsUIThread();
        IdeStatusBarImpl bar = new IdeStatusBarImpl(myApplication, this);
        bar.setVisible(isVisible());
        myChildren.add(bar);
        Disposer.register(this, bar);
        Disposer.register(bar, () -> myChildren.remove(bar));

        JPanel rightPanel = rightPanel();

        List<String> order = new ArrayList<>();
        for (int i = 0; i < rightPanel.getComponentCount(); i++) {
            Component component = rightPanel.getComponent(i);

            String widgetId = UIUtil.getClientProperty(component, WIDGET_ID);
            if (widgetId != null) {
                order.add(widgetId);
            }
        }

        for (WidgetBean eachBean : myWidgetMap.values()) {
            if (eachBean.widget() instanceof StatusBarWidget.Multiframe multiframe) {
                StatusBarWidget copy = multiframe.copy();
                bar.addWidget(copy, order);
            }
        }
        bar.repaint();

        return bar;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public consulo.ui.Component getUIComponent() {
        consulo.ui.Component component = myUIComponent;
        if (component == null) {
            component = TargetAWT.wrap(this);
            myUIComponent = component;
        }
        return component;
    }

    @RequiredUIAccess
    public IdeStatusBarImpl(Application application) {
        this(application, null);
    }

    @RequiredUIAccess
    public IdeStatusBarImpl(Application application, @Nullable IdeStatusBarImpl master) {
        myApplication = application;
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(1, 0, 0, 6));

        setOpaque(true);
        updateUI();

        if (master == null) {
            DesktopToolWindowsSwitcher switcher = new DesktopToolWindowsSwitcher(this);
            switcher.update();

            Disposer.register(this, switcher);

            leftPanel().add(TargetAWT.to(switcher.getUIComponent()));
        }
    }

    public void addToLeft(JComponent component) {
        leftPanel().add(component);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (size == null) {
            return null;
        }

        Insets insets = getInsets();
        int minHeight = Math.max(insets.top + insets.bottom + MIN_ICON_HEIGHT, TabsUtil.getRealTabsHeight());
        return new Dimension(size.width, Math.max(size.height, minHeight));
    }

    @Override
    public void addWidget(StatusBarWidget widget, List<String> order, Disposable parentDisposable) {
        myApplication.invokeLater(() -> addWidget(widget, order));
        Disposer.register(parentDisposable, () -> removeWidget(widget.getId()));
    }

    @Override
    public void updateUI() {
        setUI(UIManager.getUI(this));
    }

    @Override
    public void dispose() {
        myWidgetMap.clear();
        myChildren.clear();

        if (myLeftPanel != null) {
            myLeftPanel.removeAll();
        }
        if (myRightPanel != null) {
            myRightPanel.removeAll();
        }
    }

    @RequiredUIAccess
    private void addWidget(StatusBarWidget widget, List<String> order) {
        UIAccess.assertIsUIThread();

        JPanel panel = rightPanel();
        WidgetBean bean = wrap(widget);
        JComponent c = bean.component();
        c.putClientProperty(WIDGET_ID, widget.getId());

        List<Component> children = new ArrayList<>(List.of(panel.getComponents()));
        children.add(c);

        Lists.weightSort(children, value -> {
            String widgetId = UIUtil.getClientProperty(value, WIDGET_ID);
            if (widgetId == null) {
                return 0;
            }

            return order.indexOf(widgetId);
        });

        myWidgetMap.put(widget.getId(), bean);

        widget.install(this);

        bean.update();

        panel.removeAll();
        for (Component child : children) {
            panel.add(child);
        }
        panel.revalidate();

        Disposer.register(this, widget);

        if (widget instanceof StatusBarWidget.Multiframe) {
            StatusBarWidget.Multiframe multiFrameWidget = (StatusBarWidget.Multiframe) widget;
            updateChildren(child -> child.addWidget(multiFrameWidget.copy(), order));
        }
    }

    private JPanel rightPanel() {
        if (myRightPanel == null) {
            myRightPanel = new JPanel();
            myRightPanel.setBorder(JBUI.Borders.emptyLeft(1));
            myRightPanel.setLayout(new BoxLayout(myRightPanel, BoxLayout.X_AXIS));
            myRightPanel.setOpaque(false);
            add(myRightPanel, BorderLayout.EAST);
        }
        return myRightPanel;
    }

    private JPanel leftPanel() {
        if (myLeftPanel == null) {
            myLeftPanel = new JPanel();
            myLeftPanel.setBorder(JBUI.Borders.empty(0, 4, 0, 1));
            myLeftPanel.setLayout(new BoxLayout(myLeftPanel, BoxLayout.X_AXIS));
            myLeftPanel.setOpaque(false);
            add(myLeftPanel, BorderLayout.WEST);
        }
        return myLeftPanel;
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        sink.lazy(Project.KEY, this::getProject);
        sink.set(StatusBar.KEY, this);
    }

    @Override
    public void addProgress(ProgressIndicator indicator, TaskInfo info) {
        this.<InfoAndProgressPanel>findWidget(widget -> widget instanceof InfoAndProgressPanel).ifPresent(widget -> {
            widget.addProgress(indicator, info);
        });
    }

    @Override
    public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
        Optional<InfoAndProgressPanel> optional = this.<InfoAndProgressPanel>findWidget(widget -> widget instanceof InfoAndProgressPanel);
        if (!optional.isPresent()) {
            return List.of();
        }
        return optional.get().getBackgroundProcesses();
    }

    @Override
    public void setProcessWindowOpen(boolean open) {
        this.<InfoAndProgressPanel>findWidget(widget -> widget instanceof InfoAndProgressPanel).ifPresent(widget -> {
            widget.setProcessWindowOpen(open);
        });
    }

    @Override
    public boolean isProcessWindowOpen() {
        Optional<InfoAndProgressPanel> optional = this.<InfoAndProgressPanel>findWidget(widget -> widget instanceof InfoAndProgressPanel);
        return optional.isPresent() && optional.get().isProcessWindowOpen();
    }

    @Override
    public void startRefreshIndication(String tooltipText) {
    }

    @Override
    public void stopRefreshIndication() {
    }

    @Override
    public BalloonHandler notifyProgressByBalloon(NotificationType type, String htmlBody, @Nullable Image icon, @Nullable HyperlinkListener listener) {
        Optional<InfoAndProgressPanel> optional = this.<InfoAndProgressPanel>findWidget(widget -> widget instanceof InfoAndProgressPanel);
        if (!optional.isPresent()) {
            return null;
        }
        return optional.get().notifyByBalloon(MessageType.from(type), htmlBody, icon, listener);
    }


    @RequiredUIAccess
    private static WidgetBean wrap(StatusBarWidget widget) {
        if (widget instanceof CustomStatusBarWidget customWidget) {
            consulo.ui.Component component = new StatusBarCustomWidgetHolder(customWidget).getComponent();
            return new WidgetBean(widget, component, (JComponent) TargetAWT.to(component), null);
        }

        StatusBarWidgetButton button = new StatusBarWidgetButton(widget, Objects.requireNonNull(widget.getPresentation()));
        consulo.ui.Component component = button.getComponent();
        return new WidgetBean(widget, component, (JComponent) TargetAWT.to(component), button);
    }

    @Override
    public consulo.ui.@Nullable Component getWidgetComponent(String id) {
        WidgetBean bean = myWidgetMap.get(id);
        return bean == null ? null : bean.uiComponent();
    }

    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    @Override
    public void removeWidget(String id) {
        myApplication.invokeLater(() -> {
            WidgetBean bean = myWidgetMap.remove(id);
            if (bean != null) {
                JPanel targetPanel = rightPanel();
                targetPanel.remove(bean.component());
                targetPanel.revalidate();
                Disposer.dispose(bean.widget());
            }
            updateChildren(child -> child.removeWidget(id));
        });
    }

    @Override
    public void updateWidgets() {
        for (String s : myWidgetMap.keySet()) {
            updateWidget(s);
        }

        updateChildren(IdeStatusBarImpl::updateWidgets);
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
    public void updateWidget(String id) {
        myApplication.invokeLater(() -> {
            WidgetBean bean = myWidgetMap.get(id);
            if (bean != null) {
                bean.update();
                bean.component().repaint();
            }

            updateChildren(child -> child.updateWidget(id));
        });
    }

    @Override
    public IdeFrame getFrame() {
        return myFrame;
    }
}
