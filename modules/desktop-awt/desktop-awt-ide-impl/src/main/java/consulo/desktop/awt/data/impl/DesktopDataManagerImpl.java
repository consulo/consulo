/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.data.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.ui.wm.FocusableFrame;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.dataContext.AsyncDataContext;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.dataContext.UiDataProvider;
import consulo.desktop.awt.editor.impl.internal.EditorComponentImpl;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.facade.FromSwingWindowWrapper;
import consulo.desktop.awt.ui.ProhibitAWTEvents;
import consulo.desktop.awt.ui.keymap.IdeKeyEventDispatcher;
import consulo.ide.impl.dataContext.BaseDataManager;
import consulo.ide.impl.dataContext.PreCachedDataContext;
import consulo.ide.impl.dataContext.UiDataProviderAdapter;
import consulo.language.editor.PlatformDataKeys;
import consulo.logging.Logger;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ModalityState;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.toolWindow.ToolWindowFloatingDecorator;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

@Singleton
@ServiceImpl
public class DesktopDataManagerImpl extends BaseDataManager {
    private static final Logger LOG = Logger.getInstance(DesktopDataManagerImpl.class);

    public static class MyDataContext extends BaseDataContext<DesktopDataManagerImpl, Component> {
        public MyDataContext(DesktopDataManagerImpl dataManager, Component component) {
            super(dataManager, component);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> @Nullable T doGetData(Key<T> dataId) {
            Component component = getComponent();
            if (PlatformDataKeys.IS_MODAL_CONTEXT == dataId) {
                if (component == null) {
                    return null;
                }
                return (T) (Boolean) IdeKeyEventDispatcher.isModalContext(component);
            }
            if (UIExAWTDataKey.CONTEXT_COMPONENT == dataId) {
                return (T) component;
            }

            if (consulo.ui.Component.KEY == dataId) {
                if (component instanceof FromSwingComponentWrapper fromSwingComponentWrapper) {
                    return (T) fromSwingComponentWrapper.toUIComponent();
                }

                return (T) TargetAWT.wrap(component);
            }

            if (Application.KEY == dataId) {
                return (T) Application.get();
            }

            if (ModalityState.KEY == dataId) {
                return (T) (component != null ? ModalityState.nonModal() : ModalityState.nonModal());
            }

            return (T) calcData(dataId, component);
        }

        protected Object calcData(Key<?> dataId, Component component) {
            return getDataManager().getData(dataId, component);
        }
    }

    @Inject
    public DesktopDataManagerImpl(Application application, Provider<WindowManager> windowManagerProvider) {
        super(application, windowManagerProvider);
    }

    private WindowManagerEx windowManager() {
        return (WindowManagerEx) myWindowManager.get();
    }

    private @Nullable <T> T getData(Key<T> dataId, Component focusedComponent) {
        try (AccessToken ignored = ProhibitAWTEvents.start("getData")) {
            return captureAwtHierarchy(focusedComponent, false).resolve(dataId);
        }
    }

    /**
     * The ui hierarchy of this frontend cannot be walked on its own - only the awt one below it can - so every
     * capture asked for in terms of ui components is answered by walking awt.
     */
    @Override
    protected PreCachedDataContext.Capture captureHierarchy(consulo.ui.@Nullable Component focusedComponent, boolean forAsync) {
        return captureAwtHierarchy(TargetAWT.to(focusedComponent), forAsync);
    }

    /**
     * Captures the awt hierarchy above the component, the focused one first.
     */
    public PreCachedDataContext.Capture captureAwtHierarchy(@Nullable Component focusedComponent, boolean forAsync) {
        PreCachedDataContext.Capture capture =
            forAsync ? PreCachedDataContext.captureForAsync(myApplication) : PreCachedDataContext.capture(myApplication);
        for (Component c = focusedComponent; c != null; c = c.getParent()) {
            hideParentEditorIfNeeded(capture, c);
            capture.collect(getDataProviderEx(c));
        }
        return capture;
    }

    /**
     * A text field stands for the editor it sits in: the find and replace fields of the editor header are
     * inside one, and were it answered above them the editor would claim the keystrokes typed into the field
     * through the copy, cut and paste providers a rule derives from it.
     */
    private static void hideParentEditorIfNeeded(PreCachedDataContext.Capture capture, Component component) {
        if (!(component instanceof JTextComponent) || component instanceof EditorComponentImpl) {
            return;
        }
        capture.hide(Editor.KEY);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nullable DataProvider getDataProviderEx(Component component) {
        // UiDataProvider takes priority over DataProvider
        if (component instanceof UiDataProvider uiProvider) {
            return new UiDataProviderAdapter(myApplication, uiProvider);
        }

        if (component instanceof DataProvider dataProvider) {
            return dataProvider;
        }

        if (component instanceof JComponent jComponent) {
            // Check for registered UiDataProvider first (via DataManager.registerUiDataProvider)
            Object uiDataObj = jComponent.getClientProperty(UiDataProvider.KEY);
            if (uiDataObj instanceof UiDataProvider uiProvider) {
                return new UiDataProviderAdapter(myApplication, uiProvider);
            }
        }

        // special case for desktop impl. Later removed since we don't want use AWT
        if (component instanceof FromSwingComponentWrapper) {
            consulo.ui.Component uiComponent = ((FromSwingComponentWrapper) component).toUIComponent();

            if (uiComponent != null) {
                UiDataProvider provider = uiComponent.getUserData(UiDataProvider.KEY);
                if (provider != null) {
                    return new UiDataProviderAdapter(myApplication, provider);
                }
            }
        }

        // special case for desktop impl. Later removed since we don't want use AWT
        if (component instanceof FromSwingWindowWrapper) {
            consulo.ui.Window uiWindow = ((FromSwingWindowWrapper) component).toUIWindow();
            if (uiWindow != null) {
                UiDataProvider provider = uiWindow.getUserData(UiDataProvider.KEY);
                if (provider != null) {
                    return new UiDataProviderAdapter(myApplication, provider);
                }
            }
        }

        return null;
    }

    @Override
    public AsyncDataContext createAsyncDataContext(DataContext dataContext) {
        return new DesktopAsyncDataContext(this, dataContext, myApplication);
    }

    @Override
    public DataContext getDataContext(@Nullable Component component) {
        return new MyDataContext(this, component);
    }

    @Override
    public DataContext getDataContext(Component component, int x, int y) {
        if (x < 0 || x >= component.getWidth() || y < 0 || y >= component.getHeight()) {
            throw new IllegalArgumentException("wrong point: x=" + x + "; y=" + y);
        }

        // Point inside JTabbedPane has special meaning. If point is inside tab bounds then
        // we construct DataContext by the component which corresponds to the (x, y) tab.
        if (component instanceof JTabbedPane) {
            JTabbedPane tabbedPane = (JTabbedPane) component;
            int index = tabbedPane.getUI().tabForCoordinate(tabbedPane, x, y);
            return getDataContext(index != -1 ? tabbedPane.getComponentAt(index) : tabbedPane);
        }
        else {
            return getDataContext(component);
        }
    }

    @Override
    public DataContext getDataContext() {
        return getDataContext(getFocusedComponent());
    }

    private @Nullable Component getFocusedComponent() {
        Window activeWindow = TargetAWT.to(windowManager().getMostRecentFocusedWindow());
        if (activeWindow == null) {
            activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            if (activeWindow == null) {
                activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
                if (activeWindow == null) {
                    return null;
                }
            }
        }

        // In case we have an active floating toolwindow and some component in another window focused,
        // we want this other component to receive key events.
        // Walking up the window ownership hierarchy from the floating toolwindow would have led us to the main IdeFrame
        // whereas we want to be able to type in other frames as well.
        if (activeWindow instanceof ToolWindowFloatingDecorator) {
            IdeFocusManager ideFocusManager = IdeFocusManager.findInstanceByComponent(activeWindow);
            FocusableFrame lastFocusedFrame = ideFocusManager.getLastFocusedFrame();
            JComponent frameComponent = lastFocusedFrame != null ? lastFocusedFrame.getComponent() : null;
            Window lastFocusedWindow = frameComponent != null ? SwingUtilities.getWindowAncestor(frameComponent) : null;
            boolean toolWindowIsNotFocused = windowManager().getFocusedComponent(activeWindow) == null;
            if (toolWindowIsNotFocused && lastFocusedWindow != null) {
                activeWindow = lastFocusedWindow;
            }
        }

        // try to find first parent window that has focus
        Window window = activeWindow;
        Component focusedComponent = null;
        while (window != null) {
            focusedComponent = windowManager().getFocusedComponent(window);
            if (focusedComponent != null) {
                break;
            }
            window = window.getOwner();
        }
        if (focusedComponent == null) {
            focusedComponent = activeWindow;
        }

        return focusedComponent;
    }

    @Override
    // FIXME [VISTALL] hack until not all UI code will return consulo.ui.Component
    protected <T> T getData(Key<T> dataId, consulo.ui.@Nullable Component focusedComponent) {
        return getData(dataId, TargetAWT.to(focusedComponent));
    }
}
