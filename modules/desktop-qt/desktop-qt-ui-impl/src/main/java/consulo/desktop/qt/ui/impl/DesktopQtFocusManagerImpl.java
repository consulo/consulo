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
package consulo.desktop.qt.ui.impl;

import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.FocusManager;
import consulo.ui.event.GlobalFocusListener;
import io.qt.widgets.QApplication;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Which component the user last worked in, which is what a data context has to be built from - the frontend has no
 * awt focus owner for {@code BaseDataManager#getDataContextTest} to ask for, and a context taken from the frame
 * root only ever reaches the project.
 * <p/>
 * The counterpart of the web {@code WebFocusTracker}, except that qt reports the focus itself and there is nothing
 * to track by hand: the widget qt hands over is mapped back to the component which owns it, walking up the widgets
 * a component builds itself out of.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtFocusManagerImpl implements FocusManager {
    public static final DesktopQtFocusManagerImpl INSTANCE = new DesktopQtFocusManagerImpl();

    private static final Logger LOG = Logger.getInstance(DesktopQtFocusManagerImpl.class);

    private final List<GlobalFocusListener> myListeners = new CopyOnWriteArrayList<>();

    private volatile @Nullable Component myFocusedComponent;

    private boolean myConnected;

    @Override
    public Disposable addListener(GlobalFocusListener listener) {
        connect();

        myListeners.add(listener);

        return () -> myListeners.remove(listener);
    }

    /**
     * The component the focus is in, or the last one it was in - a click on the navigation bar or on the menu says
     * nothing about which scope the user is working in, and the context has to keep answering with that scope.
     */
    public @Nullable Component getFocusedComponent() {
        connect();

        return myFocusedComponent;
    }

    private void connect() {
        if (myConnected) {
            return;
        }

        myConnected = true;

        DesktopQtUIAccess.INSTANCE.giveIfNeed(() -> {
            QApplication application = QApplication.instance();
            if (application == null) {
                LOG.warn("Qt application is not up, focus is not tracked");
                return;
            }

            application.focusChanged.connect((from, to) -> focusChanged(to));

            focusChanged(QApplication.focusWidget());
        });
    }

    private void focusChanged(@Nullable QWidget widget) {
        Component component = toComponent(widget);

        // qt reports a null focus while the pointer is between two windows, and a widget a component does not own
        // is one of the pieces a component is built from - neither says the user left where they were
        if (component == null || component == myFocusedComponent) {
            return;
        }

        myFocusedComponent = component;

        for (GlobalFocusListener listener : myListeners) {
            try {
                listener.focusChanged();
            }
            catch (Throwable e) {
                LOG.error("Focus listener failed", e);
            }
        }
    }

    private static @Nullable Component toComponent(@Nullable QWidget widget) {
        for (QWidget current = widget; current != null && !current.isDisposed(); current = current.parentWidget()) {
            Component component = TargetQt.from(current);
            if (component != null) {
                return component;
            }
        }

        return null;
    }
}
