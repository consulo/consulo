/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.project.ui.wm;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import javax.swing.event.HyperlinkListener;

/**
 * If you want to register a toolwindow, which will be enabled during the dumb mode, please use {@link ToolWindowManager}'s
 * registration methods which have 'canWorkInDumMode' parameter.
 */
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
public abstract class ToolWindowManager {
    public abstract boolean canShowNotification(String toolWindowId);

    public static ToolWindowManager getInstance(Project project) {
        return project.getInstance(ToolWindowManager.class);
    }

    @Deprecated
    @DeprecationInfo("Use extension ToolWindowFactory")
    @RequiredUIAccess
    public abstract ToolWindow registerToolWindow(String id, boolean canCloseContent, ToolWindowAnchor anchor);

    @Deprecated
    @DeprecationInfo("Use extension ToolWindowFactory")
    @RequiredUIAccess
    public abstract ToolWindow registerToolWindow(
        String id,
        boolean canCloseContent,
        ToolWindowAnchor anchor,
        boolean secondary
    );

    @Deprecated
    @DeprecationInfo("Use extension ToolWindowFactory")
    @RequiredUIAccess
    public abstract ToolWindow registerToolWindow(
        String id,
        boolean canCloseContent,
        ToolWindowAnchor anchor,
        Disposable parentDisposable,
        boolean canWorkInDumbMode
    );

    @RequiredUIAccess
    @Deprecated
    @DeprecationInfo("Use extension ToolWindowFactory")
    public abstract ToolWindow registerToolWindow(
        String id,
        boolean canCloseContent,
        ToolWindowAnchor anchor,
        Disposable parentDisposable,
        boolean canWorkInDumbMode,
        boolean secondary
    );

    @Deprecated
    @DeprecationInfo("Use extension ToolWindowFactory")
    @RequiredUIAccess
    public ToolWindow registerToolWindow(
        String id,
        boolean canCloseContent,
        ToolWindowAnchor anchor,
        Disposable parentDisposable
    ) {
        return registerToolWindow(id, canCloseContent, anchor, parentDisposable, false);
    }

    /**
     * does nothing if tool window with specified isn't registered.
     */
    @RequiredUIAccess
    public abstract void unregisterToolWindow(String id);

    public abstract void activateEditorComponent();

    /**
     * @return <code>true</code> if and only if editor component is active.
     */
    public abstract boolean isEditorComponentActive();

    /**
     * @return array of <code>id</code>s of all registered tool windows.
     */
    public abstract String[] getToolWindowIds();

    /**
     * @return <code>ID</code> of currently active tool window or <code>null</code> if there is no active
     * tool window.
     */
    @Nullable
    @RequiredUIAccess
    public abstract String getActiveToolWindowId();

    /**
     * @return registered tool window with specified <code>id</code>. If there is no registered
     * tool window with specified <code>id</code> then the method returns <code>null</code>.
     */
    @Nullable
    public abstract ToolWindow getToolWindow(String id);

    /**
     * Puts specified runnable to the tail of current command queue.
     */
    public abstract void invokeLater(@RequiredUIAccess Runnable runnable);

    /**
     * Utility method for quick access to the focus manager
     */
    public abstract IdeFocusManager getFocusManager();

    public abstract void notifyByBalloon(String toolWindowId, NotificationType type, String htmlBody);

    @Nullable
    public abstract Balloon getToolWindowBalloon(String id);

    public abstract boolean isMaximized(ToolWindow wnd);

    public abstract void setMaximized(ToolWindow wnd, boolean maximized);

    public Image getLocationIcon(String toolWindowId, Image fallbackImage) {
        return fallbackImage;
    }

    // region AWT & Swing dependency

    /**
     * @deprecated {@link ToolWindowManager#registerToolWindow(String, boolean, ToolWindowAnchor)}
     */

    @Deprecated
    public void notifyByBalloon(
        String toolWindowId,
        NotificationType type,
        String htmlBody,
        @Nullable Image icon,
        @Nullable HyperlinkListener listener
    ) {
        throw new AbstractMethodError("AWT & Swing dependency");
    }

    // endregion
}
