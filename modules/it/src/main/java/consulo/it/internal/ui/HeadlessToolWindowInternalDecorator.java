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
package consulo.it.internal.ui;

import consulo.project.ui.impl.internal.wm.ToolWindowBase;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.internal.ToolWindowEx;
import consulo.ui.ex.toolWindow.InternalDecoratorListener;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowInternalDecorator;
import consulo.ui.ex.toolWindow.WindowInfo;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Headless {@link ToolWindowInternalDecorator}: no UI, it only holds the {@link WindowInfoImpl} and the
 * tool window so {@code ToolWindowManagerBase} registration/state logic can run. Listeners are stored and
 * fired so decorator events (hidden/activated/...) still reach the manager.
 *
 * @author VISTALL
 */
public class HeadlessToolWindowInternalDecorator implements ToolWindowInternalDecorator {
    private final ToolWindowEx myToolWindow;
    private final List<InternalDecoratorListener> myListeners = new CopyOnWriteArrayList<>();

    private WindowInfoImpl myWindowInfo;

    public HeadlessToolWindowInternalDecorator(WindowInfoImpl windowInfo, ToolWindowEx toolWindow) {
        myWindowInfo = windowInfo;
        myToolWindow = toolWindow;

        if (toolWindow instanceof ToolWindowBase toolWindowBase) {
            toolWindowBase.setDecorator(this);
        }
    }

    @Override
    public WindowInfo getWindowInfo() {
        return myWindowInfo;
    }

    @Override
    public void apply(WindowInfo windowInfo) {
        if (windowInfo instanceof WindowInfoImpl windowInfoImpl) {
            myWindowInfo = windowInfoImpl;
        }
    }

    @Override
    public ToolWindow getToolWindow() {
        return myToolWindow;
    }

    @Override
    public void addInternalDecoratorListener(InternalDecoratorListener l) {
        myListeners.add(l);
    }

    @Override
    public void removeInternalDecoratorListener(InternalDecoratorListener l) {
        myListeners.remove(l);
    }

    @Override
    public void fireActivated() {
        for (InternalDecoratorListener listener : myListeners) {
            listener.activated(this);
        }
    }

    @Override
    public void fireHidden() {
        for (InternalDecoratorListener listener : myListeners) {
            listener.hidden(this);
        }
    }

    @Override
    public void fireHiddenSide() {
        for (InternalDecoratorListener listener : myListeners) {
            listener.hiddenSide(this);
        }
    }

    @Override
    public @Nullable ActionGroup createPopupGroup() {
        return null;
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public boolean hasFocus() {
        return false;
    }

    @Override
    public void dispose() {
    }
}
