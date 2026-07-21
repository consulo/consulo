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
package consulo.it.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.dataContext.DataContext;
import consulo.it.internal.ui.HeadlessToolWindowInternalDecorator;
import consulo.it.internal.ui.HeadlessToolWindowStripeButton;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.project.ui.impl.internal.wm.ToolWindowManagerBase;
import consulo.project.ui.impl.internal.wm.UnifiedToolWindowImpl;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.NotificationType;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.internal.ToolWindowEx;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.toolWindow.InternalDecoratorListener;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.toolWindow.ToolWindowInternalDecorator;
import consulo.ui.ex.toolWindow.ToolWindowStripeButton;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Headless {@link ToolWindowManagerBase}: runs the real registration/layout/state logic, but every
 * factory/UI method returns null or is a no-op so no {@code consulo.ui} component is ever created.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl
@State(name = ToolWindowManagerBase.ID, storages = @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED))
public class HeadlessToolWindowManagerImpl extends ToolWindowManagerBase {
    @Inject
    public HeadlessToolWindowManagerImpl(Project project, Provider<WindowManager> windowManager) {
        super(project, windowManager);

        if (project.isDefault()) {
            return;
        }

        MessageBusConnection busConnection = project.getMessageBus().connect();
        busConnection.subscribe(
            ProjectManagerListener.class,
            new ProjectManagerListener() {
                @Override
                public void projectClosed(Project project, UIAccess uiAccess) {
                }
            }
        );
    }

    @Override
    @RequiredUIAccess
    public void initializeUI() {
    }

    @Override
    @RequiredUIAccess
    public void initializeEditorComponent() {
    }

    @Override
    @RequiredUIAccess
    protected Object createInitializingLabel() {
        return null;
    }

    @Override
    @RequiredUIAccess
    protected void doWhenFirstShown(Object component, Runnable runnable) {
        UIAccess.get().give(runnable);
    }

    @Override
    protected InternalDecoratorListener createInternalDecoratorListener() {
        return new MyInternalDecoratorListenerBase() {
            @Override
            public void resized(ToolWindowInternalDecorator source) {
            }
        };
    }

    @Override
    protected ToolWindowStripeButton createStripeButton(ToolWindowInternalDecorator internalDecorator) {
        return new HeadlessToolWindowStripeButton(internalDecorator);
    }

    @Override
    @RequiredUIAccess
    protected ToolWindowEx createToolWindow(
        String id,
        LocalizeValue displayName,
        boolean canCloseContent,
        @Nullable Object component,
        boolean shouldBeAvailable
    ) {
        return new UnifiedToolWindowImpl(this, id, displayName, canCloseContent, component, shouldBeAvailable);
    }

    @Override
    @RequiredUIAccess
    protected ToolWindowInternalDecorator createInternalDecorator(
        Project project,
        WindowInfoImpl info,
        ToolWindowEx toolWindow,
        boolean dumbAware
    ) {
        return new HeadlessToolWindowInternalDecorator(info, toolWindow);
    }

    @Override
    @RequiredUIAccess
    protected void addButton(ToolWindowStripeButton button, WindowInfoImpl info) {
        // base implementation renders into myToolWindowPanel, which never exists headless
    }

    @Override
    @RequiredUIAccess
    protected void addDecorator(ToolWindowInternalDecorator decorator, WindowInfoImpl info, boolean dirtyMode) {
        // base implementation renders into myToolWindowPanel, which never exists headless
    }

    @Override
    @RequiredUIAccess
    protected void removeButton(String id) {
        // base implementation renders into myToolWindowPanel, which never exists headless
    }

    @Override
    @RequiredUIAccess
    protected void removeDecorator(String id, boolean dirtyMode) {
        // base implementation renders into myToolWindowPanel, which never exists headless
    }

    /**
     * Mirrors {@code ToolWindowManagerBase.setSideTool} minus the trailing
     * {@code myToolWindowPanel.updateButtonPosition(id)} (the private {@code setSplitModeImpl} cannot be
     * overridden and would NPE headless). Hit at registration time for secondary tool windows.
     */
    @Override
    @RequiredUIAccess
    public void setSideTool(String id, boolean isSide) {
        checkId(id);
        WindowInfoImpl info = getInfo(id);
        if (isSide != info.isSplit()) {
            myLayout.setSplitMode(id, isSide);

            boolean wasActive = info.isActive();
            if (wasActive) {
                deactivateToolWindowImpl(id, true);
            }
            for (WindowInfoImpl each : myLayout.getInfos()) {
                applyWindowInfo(each);
            }
            if (wasActive) {
                activateToolWindowImpl(id, true, true);
            }
        }

        fireStateChanged();
    }

    @Override
    @RequiredUIAccess
    public void setSideToolAndAnchor(String id, ToolWindowAnchor anchor, int order, boolean isSide) {
        setToolWindowAnchor(id, anchor, order);
        setSideTool(id, isSide);
    }

    @Override
    public boolean isUnified() {
        return true;
    }

    @Override
    @RequiredUIAccess
    protected void requestFocusInToolWindow(String id, boolean forced) {
    }

    @Override
    @RequiredUIAccess
    protected void removeWindowedDecorator(WindowInfoImpl info) {
    }

    @Override
    @RequiredUIAccess
    protected void addFloatingDecorator(ToolWindowInternalDecorator internalDecorator, WindowInfoImpl toBeShownInfo) {
    }

    @Override
    @RequiredUIAccess
    protected void addWindowedDecorator(ToolWindowInternalDecorator internalDecorator, WindowInfoImpl toBeShownInfo) {
    }

    @Override
    @RequiredUIAccess
    protected void updateToolWindowsPane() {
    }

    @Override
    @RequiredUIAccess
    protected @Nullable Element getStateImpl() {
        return new Element("state");
    }

    @Override
    public boolean canShowNotification(String toolWindowId) {
        return false;
    }

    @Override
    public void activateEditorComponent() {
    }

    @Override
    public boolean isEditorComponentActive() {
        return false;
    }

    @Override
    public void notifyByBalloon(String toolWindowId, NotificationType type, String htmlBody) {
    }

    @Override
    public @Nullable Balloon getToolWindowBalloon(String id) {
        return null;
    }

    @Override
    public boolean isMaximized(ToolWindow wnd) {
        return false;
    }

    @Override
    public void setMaximized(ToolWindow wnd, boolean maximized) {
    }

    @Override
    public void doContentRename(
        DataContext dataContext,
        ToolWindow toolWindow,
        @Nullable Content content,
        LocalizeValue labelText,
        BiConsumer<Content, String> consumer
    ) {
    }
}
