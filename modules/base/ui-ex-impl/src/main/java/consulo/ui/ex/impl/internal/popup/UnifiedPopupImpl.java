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
package consulo.ui.ex.impl.internal.popup;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.LightPopup;
import consulo.ui.Point2D;
import consulo.ui.RelativePoint2D;
import consulo.ui.PopupOwner;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.AnchoredPopup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import org.jspecify.annotations.Nullable;

import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * What every popup built on {@link LightPopup} shares - the listeners, whether a choice was made, and the swing
 * half of {@link JBPopup} which a frontend without swing cannot answer.
 * <p/>
 * A subclass owns the surface and decides what closing means; it reports a choice with {@link #markOk()} and ends
 * the popup with {@link #finish()}.
 *
 * @author VISTALL
 * @since 2026-08-03
 */
public abstract class UnifiedPopupImpl implements JBPopup, AnchoredPopup {
    private static final Logger LOG = Logger.getInstance(UnifiedPopupImpl.class);

    private final List<JBPopupListener> myListeners = new ArrayList<>();

    private boolean myDisposed;
    private boolean myOk;
    private @Nullable Runnable myFinalRunnable;

    protected final boolean isOk() {
        return myOk;
    }

    /**
     * Records that the popup was completed rather than abandoned. The close which follows is what answers the
     * listeners, so this only marks how it will answer.
     */
    protected final void markOk() {
        myOk = true;
    }

    protected final void fireBeforeShown() {
        LightweightWindowEvent event = new LightweightWindowEvent(this);

        for (JBPopupListener listener : new ArrayList<>(myListeners)) {
            try {
                listener.beforeShown(event);
            }
            catch (Throwable e) {
                LOG.error("Popup listener failed on beforeShown", e);
            }
        }
    }

    /**
     * Ends the popup once, whichever way it was closed.
     */
    @RequiredUIAccess
    protected final void finish() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;

        LightweightWindowEvent event = new LightweightWindowEvent(this, myOk);

        for (JBPopupListener listener : new ArrayList<>(myListeners)) {
            try {
                listener.onClosed(event);
            }
            catch (Throwable e) {
                LOG.error("Popup listener failed on close", e);
            }
        }

        Runnable finalRunnable = myFinalRunnable;
        if (finalRunnable != null) {
            myFinalRunnable = null;

            try {
                finalRunnable.run();
            }
            catch (Throwable e) {
                LOG.error("Popup final runnable failed", e);
            }
        }

        Disposer.dispose(this);
    }

    @Override
    public void addListener(JBPopupListener listener) {
        myListeners.add(listener);
    }

    @Override
    public void removeListener(JBPopupListener listener) {
        myListeners.remove(listener);
    }

    @Override
    public boolean isDisposed() {
        return myDisposed;
    }

    @Override
    public boolean canClose() {
        return true;
    }

    @Override
    public void setFinalRunnable(@Nullable Runnable runnable) {
        myFinalRunnable = runnable;
    }

    protected final @Nullable Runnable getFinalRunnable() {
        return myFinalRunnable;
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public boolean isModalContext() {
        return false;
    }

    @Override
    public boolean isNativePopup() {
        return false;
    }

    @Override
    public boolean isFocused() {
        return isVisible();
    }

    @Override
    public boolean isCancelKeyEnabled() {
        return true;
    }

    @Override
    public void setRequestFocus(boolean b) {
    }

    @Override
    public void setUiVisible(boolean visible) {
    }

    @Override
    public <T> @Nullable T getUserData(Class<T> userDataClass) {
        return null;
    }

    @Override
    public void setDataProvider(DataProvider dataProvider) {
    }

    @Override
    public void setAdText(String s, int alignment) {
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        return false;
    }

    @Override
    @RequiredUIAccess
    public void showUnderneathOf(Component componentUnder) {
        consulo.ui.Component component = TargetAWT.from(componentUnder);

        if (component != null) {
            showBy(component, null);
        }
        else {
            showCenteredInCurrentWindow(null);
        }
    }

    @Override
    @RequiredUIAccess
    public void showUnderneathOf(AnActionEvent e) {
        consulo.ui.Component component = e.getData(consulo.ui.Component.KEY);

        if (component != null) {
            showBy(component, e.getInputDetails());
        }
        else {
            showCenteredInCurrentWindow(null);
        }
    }

    @Override
    @RequiredUIAccess
    public void show(RelativePoint point) {
        show((RelativePoint2D)point);
    }

    /**
     * A point of the screen is the one placement which does not survive the trip - there is no screen to measure
     * against, and converting it needs a live layout the frontend does not have.
     */
    @Override
    @RequiredUIAccess
    public void showInScreenCoordinates(Component owner, Point point) {
        showCenteredInCurrentWindow(null);
    }

    /**
     * A point of a component is the one placement which survives the trip: the frontend anchors to the component and
     * offsets inside it, where a screen coordinate means nothing to it.
     */
    @Override
    @RequiredUIAccess
    public void show(RelativePoint2D point) {
        consulo.ui.Component target = point.getUIComponent();

        if (target == null) {
            showCenteredInCurrentWindow(null);
            return;
        }

        Point2D uiPoint = point.getUIPoint();

        showAtPoint(target, uiPoint.x(), uiPoint.y(), 0);
    }

    // TODO accelerators of a popup are not bound yet, and the interface default throws rather than ignoring them -
    // which took down every caller that offers one, the breakpoint variant chooser among them
    @Override
    public void registerAction(String aActionName, int aKeyCode, @JdkConstants.InputEventMask int aModifier, Action aAction) {
    }

    @Override
    public void registerAction(String aActionName, KeyStroke keyStroke, Action aAction) {
    }

    @Override
    @RequiredUIAccess
    public void showInBestPositionFor(DataContext dataContext) {
        Editor editor = dataContext.getData(Editor.KEY);
        if (editor != null) {
            EditorPopupHelper.getInstance().showPopupInBestPositionFor(editor, this);
            return;
        }

        consulo.ui.Component component = dataContext.getData(consulo.ui.Component.KEY);

        if (component == null) {
            showCenteredInCurrentWindow(null);
            return;
        }

        Point2D position = component instanceof PopupOwner popupOwner ? popupOwner.getBestPopupPosition() : null;

        if (position != null) {
            showAtPoint(component, position.x(), position.y(), 0);
        }
        else {
            showBy(component, null);
        }
    }

    @Override
    @RequiredUIAccess
    public void showAtPoint(consulo.ui.Component target, int x, int y, int anchorHeight) {
        showBy(target, null);
    }

    @Override
    @RequiredUIAccess
    public void showInCenterOf(Component component) {
        showCenteredInCurrentWindow(null);
    }

    @Override
    @RequiredUIAccess
    public void showInFocusCenter() {
        showCenteredInCurrentWindow(null);
    }

    @Override
    @RequiredUIAccess
    public void show(Component owner) {
        showCenteredInCurrentWindow(null);
    }

    @Override
    public Point getBestPositionFor(DataContext dataContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JComponent getContent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocation(Point screenPoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSize(Dimension size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dimension getSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Component getOwner() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMinimumSize(@Nullable Dimension size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveToFitScreen() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Point getLocationOnScreen() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pack(boolean width, boolean height) {
        throw new UnsupportedOperationException();
    }

    @Override
    @RequiredUIAccess
    public void cancel() {
        cancel(null);
    }

    @Override
    @RequiredUIAccess
    public void closeOk(@Nullable InputEvent e) {
        markOk();

        cancel(null);
    }
}
