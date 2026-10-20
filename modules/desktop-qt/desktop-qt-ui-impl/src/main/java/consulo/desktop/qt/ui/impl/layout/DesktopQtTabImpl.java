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
package consulo.desktop.qt.ui.impl.layout;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.qt.ui.impl.DesktopQtTextItemPresentation;
import consulo.desktop.qt.ui.impl.image.DesktopQtAnimationHost;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconRefresher;
import consulo.desktop.qt.ui.impl.image.DesktopQtLiveIconEngine;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.TextItemPresentation;
import consulo.ui.image.Image;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.TabSelectionUtil;
import consulo.ui.internal.PlainTextPresentation;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.widgets.QTabBar;
import io.qt.widgets.QTabWidget;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTabImpl implements Tab, DesktopQtAnimationHost, DesktopQtIconOwner {
    private static final int TRAILING_SPACER_WIDTH = 4;
    private static final String TRAILING_SPACER_NAME = "consuloTabTrailingSpacer";

    private BiConsumer<Tab, TextItemPresentation> myRenderer = (tab, presentation) -> presentation.append(toString());

    private @Nullable QtComponentDelegate<?> myComponent;

    private @Nullable DesktopQtTabbedLayoutImpl myTabbedLayout;

    private @Nullable QTabWidget myTabWidget;

    private @Nullable QWidget myContent;

    private @Nullable Image myImage;

    private @Nullable String myPopupGroupId;

    private @Nullable String myPopupPlace;

    private @Nullable BiConsumer<Tab, Component> myCloseHandler;

    private boolean myEnabled = true;

    private @Nullable QtComponentDelegate<?> mySuffixComponent;

    private @Nullable QWidget myObservedSuffix;

    @Override
    public void setCloseHandler(@Nullable BiConsumer<Tab, Component> closeHandler) {
        myCloseHandler = closeHandler;

        QTabWidget tabWidget = myTabWidget;
        if (tabWidget != null && !tabWidget.isDisposed()) {
            applyClosable(tabWidget);
        }
    }

    public @Nullable BiConsumer<Tab, Component> getCloseHandler() {
        return myCloseHandler;
    }

    @Override
    @RequiredUIAccess
    public void setSuffixComponent(@Nullable Component component) {
        QtComponentDelegate<?> oldSuffix = mySuffixComponent;
        QtComponentDelegate<?> newSuffix = (QtComponentDelegate<?>) component;
        if (oldSuffix == newSuffix) {
            return;
        }

        mySuffixComponent = newSuffix;

        QTabWidget tabWidget = myTabWidget;
        if (tabWidget != null && !tabWidget.isDisposed()) {
            applyClosable(tabWidget);
        }

        if (oldSuffix != null) {
            oldSuffix.setParent(null);
        }
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    public void applyClosable(QTabWidget tabWidget) {
        int index = getIndex();
        if (index == -1) {
            return;
        }

        QTabBar tabBar = tabWidget.tabBar();

        boolean closable = myCloseHandler != null;

        QWidget oldButton = tabBar.tabButton(index, QTabBar.ButtonPosition.RightSide);

        QtComponentDelegate<?> suffix = mySuffixComponent;
        if (suffix != null) {
            if (myTabbedLayout != null) {
                suffix.setParent(myTabbedLayout);
            }
            suffix.bind(tabBar, null);

            QWidget suffixWidget = suffix.toQtComponent();
            observeSuffix(suffixWidget);
            if (oldButton == suffixWidget) {
                return;
            }

            tabBar.setTabButton(index, QTabBar.ButtonPosition.RightSide, suffixWidget);
            if (oldButton instanceof DesktopQtTabCloseButton || isTrailingSpacer(oldButton)) {
                oldButton.disposeLater();
            }
            return;
        }

        if (oldButton != null && closable == oldButton instanceof DesktopQtTabCloseButton && (closable || isTrailingSpacer(oldButton))) {
            return;
        }

        QWidget newButton = closable ? createCloseButton(tabBar) : createTrailingSpacer(tabBar);

        tabBar.setTabButton(index, QTabBar.ButtonPosition.RightSide, newButton);

        if (oldButton instanceof DesktopQtTabCloseButton || isTrailingSpacer(oldButton)) {
            oldButton.disposeLater();
        }
    }

    private void observeSuffix(QWidget suffixWidget) {
        if (myObservedSuffix == suffixWidget) {
            return;
        }

        myObservedSuffix = suffixWidget;
        suffixWidget.installEventFilter(new QObject(suffixWidget) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                if (event.type() == QEvent.Type.LayoutRequest && !suffixWidget.sizeHint().equals(suffixWidget.size())) {
                    QTimer.singleShot(0, () -> relayoutSuffix(suffixWidget));
                }
                return false;
            }
        });
    }

    private void relayoutSuffix(QWidget suffixWidget) {
        QTabWidget tabWidget = myTabWidget;
        int index = getIndex();
        if (suffixWidget.isDisposed() || tabWidget == null || tabWidget.isDisposed() || index == -1) {
            return;
        }

        QTabBar tabBar = tabWidget.tabBar();
        if (tabBar.tabButton(index, QTabBar.ButtonPosition.RightSide) != suffixWidget) {
            return;
        }

        suffixWidget.resize(suffixWidget.sizeHint());
        tabBar.setTabText(index, tabBar.tabText(index));
    }

    private static boolean isTrailingSpacer(@Nullable QWidget widget) {
        return widget != null && TRAILING_SPACER_NAME.equals(widget.objectName());
    }

    private QWidget createCloseButton(QTabBar tabBar) {
        DesktopQtTabCloseButton button = new DesktopQtTabCloseButton(tabBar);
        button.clicked.connect(checked -> close());
        return button;
    }

    private static QWidget createTrailingSpacer(QTabBar tabBar) {
        QWidget spacer = new QWidget(tabBar);
        spacer.setObjectName(TRAILING_SPACER_NAME);
        spacer.setAttribute(Qt.WidgetAttribute.WA_TransparentForMouseEvents);
        spacer.resize(TRAILING_SPACER_WIDTH, 1);
        return spacer;
    }

    @RequiredUIAccess
    public void close() {
        BiConsumer<Tab, Component> closeHandler = myCloseHandler;
        if (closeHandler != null) {
            closeHandler.accept(this, myComponent);
        }
    }

    @Override
    public void setPopupGroup(String groupId, String place) {
        myPopupGroupId = groupId;
        myPopupPlace = place;
    }

    public @Nullable String getPopupGroupId() {
        return myPopupGroupId;
    }

    public @Nullable String getPopupPlace() {
        return myPopupPlace;
    }

    public DataContext createDataContext() {
        DataManager dataManager = DataManager.getInstance();

        // the group is expanded off the ui thread, the providers have to be snapshotted before that
        return dataManager.createAsyncDataContext(
            myComponent == null ? dataManager.getDataContext() : dataManager.getDataContext(myComponent)
        );
    }

    @Override
    @RequiredUIAccess
    public void setEnabled(boolean enabled) {
        if (myEnabled == enabled) {
            return;
        }

        myEnabled = enabled;

        QTabWidget tabWidget = myTabWidget;
        if (tabWidget != null && !tabWidget.isDisposed()) {
            applyEnabled(tabWidget);
        }
    }

    @Override
    public boolean isEnabled() {
        return myEnabled;
    }

    private void applyEnabled(QTabWidget tabWidget) {
        int index = getIndex();
        if (index == -1) {
            return;
        }

        if (!myEnabled && tabWidget.currentIndex() == index) {
            int target = TabSelectionUtil.findSelectionOnDisable(index, tabWidget.count(), tabWidget::isTabEnabled);
            if (target != -1) {
                tabWidget.setCurrentIndex(target);
            }
        }

        tabWidget.setTabEnabled(index, myEnabled);
    }

    @Override
    @RequiredUIAccess
    public void select() {
        int index = getIndex();
        if (myTabWidget != null && index != -1) {
            myTabWidget.setCurrentIndex(index);
            return;
        }

        DesktopQtTabbedLayoutImpl tabbedLayout = myTabbedLayout;
        if (tabbedLayout != null) {
            tabbedLayout.selectUnrealized(this);
        }
    }

    public void setTabbedLayout(@Nullable DesktopQtTabbedLayoutImpl tabbedLayout) {
        myTabbedLayout = tabbedLayout;
    }

    @Override
    public void setRenderer(BiConsumer<Tab, TextItemPresentation> renderer) {
        myRenderer = renderer;
    }

    @Override
    public LocalizeValue getTitle() {
        return PlainTextPresentation.textOf(presentation -> myRenderer.accept(this, presentation));
    }

    @Override
    public void update() {
        int index = getIndex();
        if (myTabWidget == null || index == -1) {
            return;
        }

        DesktopQtTextItemPresentation item = new DesktopQtTextItemPresentation();
        myRenderer.accept(this, item);

        myTabWidget.setTabText(index, item.toString());

        // the renderer names an icon for the file the tab stands for - the other frontends draw it beside the
        // label, and a tab bar which asks the renderer only for its text drops it
        myImage = item.getImage();

        applyIcon(myTabWidget, index);
    }

    private void applyIcon(QTabWidget tabWidget, int index) {
        tabWidget.setTabIcon(index, DesktopQtLiveIconEngine.toQIcon(myImage, this));
    }

    @Override
    public void refreshIcons() {
        QTabWidget tabWidget = myTabWidget;
        int index = getIndex();
        if (tabWidget != null && index != -1) {
            applyIcon(tabWidget, index);
        }
    }

    @RequiredUIAccess
    @Override
    public void repaintAnimatedImage() {
        QTabWidget tabWidget = myTabWidget;
        int index = getIndex();
        if (tabWidget == null || index < 0) {
            return;
        }

        QTabBar tabBar = tabWidget.tabBar();
        if (tabBar != null && !tabBar.isDisposed()) {
            tabBar.update(tabBar.tabRect(index));
        }
    }

    public void setComponent(Component component) {
        myComponent = (QtComponentDelegate<?>) component;
    }

    public @Nullable QtComponentDelegate<?> getComponent() {
        return myComponent;
    }

    public @Nullable QTabWidget getTabWidget() {
        QTabWidget tabWidget = myTabWidget;
        return tabWidget == null || tabWidget.isDisposed() ? null : tabWidget;
    }

    /**
     * A tab has no identity of its own inside a {@link QTabWidget} - only a position, which shifts whenever
     * another tab is dropped - so the position is asked of the widget every time instead of being remembered.
     */
    public int getIndex() {
        QTabWidget tabWidget = myTabWidget;
        QWidget content = myContent;

        // a project being closed takes the whole tree of widgets down before the editors it held are closed one
        // by one, so both of these outlive the native objects they stand for and answering either throws
        if (tabWidget == null || tabWidget.isDisposed() || content == null || content.isDisposed()) {
            return -1;
        }

        return tabWidget.indexOf(content);
    }

    public void initialize(QTabWidget tabWidget, Component parent, int index) {
        QWidget content = null;

        if (myComponent != null) {
            // the consulo.ui parent, not the qt one: everything that walks up from a component to its window
            // reads this chain, and an editor whose window cannot be found is never registered as active
            myComponent.setParent(parent);
            myComponent.bind(tabWidget, null);

            content = myComponent.toQtComponent();
        }

        if (content == null) {
            content = new QWidget();
        }

        myTabWidget = tabWidget;
        myContent = content;

        DesktopQtIconRefresher.register(this);

        tabWidget.insertTab(index, content, "");

        applyClosable(tabWidget);

        applyEnabled(tabWidget);

        update();
    }

    public void detach() {
        int index = getIndex();
        if (index != -1) {
            QtComponentDelegate<?> suffix = mySuffixComponent;
            QTabBar tabBar = myTabWidget.tabBar();
            if (suffix != null && suffix.toQtComponent() != null && tabBar.tabButton(index, QTabBar.ButtonPosition.RightSide) == suffix.toQtComponent()) {
                tabBar.setTabButton(index, QTabBar.ButtonPosition.RightSide, null);
                suffix.setParent(null);
            }

            myTabWidget.removeTab(index);
        }

        myTabWidget = null;
        myContent = null;

        if (myComponent != null) {
            myComponent.setParent(null);
        }
    }
}
