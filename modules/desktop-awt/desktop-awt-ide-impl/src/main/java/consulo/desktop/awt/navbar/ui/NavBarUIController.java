/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 *
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
package consulo.desktop.awt.navbar.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.UISettings;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.navigationBar.NavBarService;
import consulo.navigationBar.impl.internal.NavBarVmImpl;
import consulo.navigationBar.model.NavBarVmItem;
import consulo.navigationBar.model.NavBarVmListener;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Project service driving the navigation bar models: IDE activity events are merged through
 * a {@link MergingUpdateQueue} ({@link NavBarUi#DEFAULT_UI_RESPONSE_TIMEOUT} merging span),
 * each update recomputes the context model from the focused component, and an epoch counter
 * guarantees that only the latest update is applied.
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class NavBarUIController implements Disposable {
    private static final Logger LOG = Logger.getInstance(NavBarUIController.class);

    public static NavBarUIController getInstance(Project myProject) {
        return myProject.getInstance(NavBarUIController.class);
    }

    private final Project myProject;
    private final MergingUpdateQueue myUpdateQueue;
    private final AtomicInteger myUpdateEpoch = new AtomicInteger();

    // EDT only
    private final Map<StaticNavBarPanel, Window> myPanels = new LinkedHashMap<>();

    // floating bar job analogue
    private @Nullable LightweightHintImpl myFloatingHint;

    private final NavBarService myNavBarService;

    @Inject
    public NavBarUIController(Project project, NavBarService navBarService) {
        myProject = project;
        myNavBarService = navBarService;

        myUpdateQueue = new MergingUpdateQueue(
            "NavBarVm",
            (int) NavBarUi.DEFAULT_UI_RESPONSE_TIMEOUT,
            true,
            MergingUpdateQueue.ANY_COMPONENT,
            this
        );

        myNavBarService.subscribeActivity(this, this::requestUpdate);
        IdeEventQueue.getInstance().addActivityListener(() -> {
            AWTEvent currentEvent = EventQueue.getCurrentEvent();
            if (currentEvent != null && !skipActivityEvent(currentEvent)) {
                requestUpdate();
            }
        }, this);
    }

    @Override
    public void dispose() {
    }

    private static boolean skipActivityEvent(AWTEvent e) {
        return e instanceof MouseEvent mouseEvent
            && (mouseEvent.getID() == MouseEvent.MOUSE_PRESSED || mouseEvent.getID() == MouseEvent.MOUSE_RELEASED);
    }

    public void uiSettingsChanged(UISettings uiSettings) {
        if (isNavbarShown(uiSettings)) {
            hideFloatingNavbar();
        }
        requestUpdate();
    }

    public static boolean isNavbarShown(UISettings uiSettings) {
        return uiSettings.getShowNavigationBar() && !uiSettings.getPresentationMode();
    }

    @RequiredUIAccess
    public JComponent createNavBarPanel() {
        return new StaticNavBarPanel(myProject);
    }

    @RequiredUIAccess
    public void attach(StaticNavBarPanel panel, Window window) {
        myPanels.put(panel, window);
        UIAccess uiAccess = UIAccess.current();
        myNavBarService.defaultModel().whenCompleteAsync((item, throwable) -> {
            if (throwable != null || item == null) {
                return;
            }
            if (!myPanels.containsKey(panel) || panel.getModel() != null) {
                return;
            }
            NavBarVmImpl vm = new NavBarVmImpl(List.of(item));
            vm.addListener(new NavBarVmListener() {
                @Override
                public void activationRequested(NavBarVmItem activatedItem) {
                    requestNavigation(activatedItem);
                }
            });
            panel.setPanel(new NewNavBarPanel(vm, myProject, false));
            requestUpdate();
        }, uiAccess);
    }

    @RequiredUIAccess
    public void detach(StaticNavBarPanel panel) {
        myPanels.remove(panel);
        panel.setPanel(null);
    }

    public void showFloatingNavbar(DataContext dataContext) {
        if (myFloatingHint != null) {
            return;
        }

        UIAccess uiAccess = myProject.getUIAccess();
        contextModel(dataContext).whenCompleteAsync((model, throwable) -> {
            if (myFloatingHint != null) {
                return;
            }
            if (throwable == null && model != null && !model.isEmpty()) {
                showFloatingNavbar(dataContext, model);
            }
            else {
                myNavBarService.defaultModel().whenCompleteAsync((item, defaultThrowable) -> {
                    if (defaultThrowable == null && item != null && myFloatingHint == null) {
                        showFloatingNavbar(dataContext, List.of(item));
                    }
                }, uiAccess);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void showFloatingNavbar(DataContext dataContext, List<NavBarVmItem> model) {
        NavBarVmImpl vm = new NavBarVmImpl(model);
        NewNavBarPanel panel = new NewNavBarPanel(vm, myProject, true);
        vm.addListener(new NavBarVmListener() {
            @Override
            public void activationRequested(NavBarVmItem item) {
                hideFloatingNavbar();
                requestNavigation(item);
            }
        });
        panel.setOnFloatingCancel(this::hideFloatingNavbar);
        myFloatingHint = NavBarHints.showHint(dataContext, myProject, panel, () -> {
            panel.disconnect();
            myFloatingHint = null;
        });
        vm.selectTail(true);
    }

    private void hideFloatingNavbar() {
        LightweightHintImpl hint = myFloatingHint;
        if (hint != null) {
            myFloatingHint = null;
            hint.hide();
        }
    }

    private void requestNavigation(NavBarVmItem item) {
        myNavBarService
            .navigate(item)
            .whenComplete((ignored, throwable) -> requestUpdate());
    }

    private void requestUpdate() {
        myUpdateQueue.queue(Update.create("update", this::updateModels));
    }

    @RequiredUIAccess
    private void updateModels() {
        if (myPanels.isEmpty()) {
            return;
        }
        int epoch = myUpdateEpoch.incrementAndGet();
        for (Map.Entry<StaticNavBarPanel, Window> entry : new ArrayList<>(myPanels.entrySet())) {
            updatePanelModel(entry.getKey(), entry.getValue(), epoch);
        }
    }

    @RequiredUIAccess
    private void updatePanelModel(StaticNavBarPanel panel, Window window, int epoch) {
        UIAccess uiAccess = UIAccess.current();
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
            if (epoch != myUpdateEpoch.get()) {
                return;
            }
            if (!(panel.getModel() instanceof NavBarVmImpl vm)) {
                return;
            }
            DataContext ctx = dataContext(window, panel);
            if (ctx == null) {
                return;
            }
            contextModel(ctx).whenCompleteAsync((items, throwable) -> {
                if (epoch != myUpdateEpoch.get()) {
                    return;
                }
                if (throwable != null || items == null) {
                    return;
                }
                if (panel.getModel() == vm) {
                    vm.contextItemsChanged(items);
                }
            }, uiAccess);
        });
    }

    private CompletableFuture<List<NavBarVmItem>> contextModel(DataContext ctx) {
        if (ctx.getData(Project.KEY) != myProject) {
            return CompletableFuture.completedFuture(List.of());
        }
        return NavBarService.getInstance(myProject).contextModel(ctx).exceptionally(throwable -> {
            LOG.error(throwable);
            return List.of();
        });
    }

    /**
     * This method assumes that {@code window} is an ancestor of {@code panel}.
     *
     * @return data context of the focused component in the {@code window} of the {@code panel},
     * or {@code null} if {@code panel} has focus in hierarchy, or if the {@code window} of the {@code panel} is not focused
     */
    private static @Nullable DataContext dataContext(Window window, StaticNavBarPanel panel) {
        if (!window.isFocused()) {
            // Skip event when a window is out of focus (user is in a popup)
            return null;
        }
        Component focusedComponentInWindow = WindowManager.getInstance().getFocusedComponent(window);
        if (focusedComponentInWindow == null) {
            return null;
        }
        if (UIUtil.isDescendingFrom(focusedComponentInWindow, panel)) {
            // ignore updates while panel or one of its children has focus
            return null;
        }
        return DataManager.getInstance().getDataContext(focusedComponentInWindow);
    }
}
