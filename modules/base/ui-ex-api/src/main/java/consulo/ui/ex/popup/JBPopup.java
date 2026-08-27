// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.popup;

import consulo.component.ComponentManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.LightweightWindow;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.RelativePoint2D;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.popup.event.JBPopupListener;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

/**
 * Base interface for popup windows.
 *
 * @author mike
 * @see JBPopupFactory
 */
public interface JBPopup extends Disposable, LightweightWindow {
    String KEY = "JBPopup";

    /**
     * Shows the popup at the bottom left corner of the specified component.
     *
     * @param componentUnder the component near which the popup should be displayed.
     */
    void showUnderneathOf(Component componentUnder);

    /**
     * Shows the popup under whatever the action was invoked from. What that is depends on the frontend - an awt
     * event carries the component it happened on, a browser one carries the point it happened at.
     *
     * @param e the event the action is performed with.
     */
    @RequiredUIAccess
    void showUnderneathOf(AnActionEvent e);

    /**
     * Shows the popup at the specified point.
     *
     * @param point the relative point where the popup should be displayed.
     */
    void show(RelativePoint point);

    /**
     * Shows the popup at a point of a component, named in the types of the platform rather than of a frontend.
     */
    @RequiredUIAccess
    void show(RelativePoint2D point);

    void showInScreenCoordinates(Component owner, Point point);

    /**
     * Returns location most appropriate for the specified data context.
     *
     * @see #showInBestPositionFor(DataContext)
     * @see #setLocation(Point)
     */
    Point getBestPositionFor(DataContext dataContext);

    /**
     * Shows the popup in the position most appropriate for the specified data context.
     *
     * @param dataContext the data context to which the popup is related.
     * @see JBPopupFactory#guessBestPopupLocation(DataContext)
     * @see #getBestPositionFor(DataContext)
     */
    void showInBestPositionFor(DataContext dataContext);

    /**
     * Shows the popup near the cursor location in the specified editor.
     *
     * @param editor the editor relative to which the popup should be displayed.
     * @see JBPopupFactory#guessBestPopupLocation(Editor)
     */
    //void showInBestPositionFor(Editor editor);

    /**
     * Shows the popup in the center of the specified component.
     *
     * @param component the component at which the popup should be centered.
     */
    void showInCenterOf(Component component);

    /**
     * Shows the popups in the center of currently focused component
     */
    void showInFocusCenter();

    /**
     * Shows in best position with a given owner
     */
    void show(Component owner);

    /**
     * Shows popup inside position by event
     */
    default void showBy(ComponentEvent<? extends consulo.ui.Component> uiEvent) {
        showBy(uiEvent.getComponent(), Objects.requireNonNull(uiEvent.getInputDetails()));
    }

    void showBy(consulo.ui.Component component, @Nullable InputDetails inputDetails);

    /**
     * Whether the user may resize the popup. Set it before the popup is shown.
     */
    @RequiredUIAccess
    default void setResizable(boolean resizable) {
    }

    /**
     * Shows the popup in the center of the active window in the IDE frame for the specified project.
     *
     * @param project the project in which the popup should be displayed.
     */
    void showCenteredInCurrentWindow(ComponentManager project);

    /**
     * Hides popup as if <kbd>Enter</kbd> was pressed or or any other "accept" action.
     */
    void closeOk(@Nullable InputEvent e);

    /**
     * Cancels the popup as if <kbd>Esc</kbd> was pressed or any other "cancel" action.
     */
    void cancel();

    /**
     * @param b {@code true} if popup should request focus.
     */
    void setRequestFocus(boolean b);

    /**
     * Cancels the popup as a response to some mouse action. All the subsequent mouse events originated from the event's point
     * will be consumed.
     */
    void cancel(@Nullable InputEvent e);

    /**
     * Checks if it's currently allowed to close the popup.
     *
     * @return {@code true} if the popup can be closed, {@code false} if a callback disallowed closing the popup.
     * @see ComponentPopupBuilder#setCancelCallback(java.util.function.Supplier)
     */
    boolean canClose();

    /**
     * Checks if the popup is currently visible.
     *
     * @return {@code true} if the popup is visible, {@code false} otherwise.
     */
    boolean isVisible();

    /**
     * Returns the Swing component contained in the popup.
     *
     * @return the contents of the popup.
     */
    JComponent getContent();

    /**
     * The UI this popup belongs to, or {@code null} while it is attached to none. A frontend may serve
     * several UIs at once, so background code that needs to get back to the UI thread must go through
     * the popup it is updating rather than through any application-wide access.
     * <p>
     * A {@code null} means there is no UI to get back to, and the caller has nothing to do.
     */
    @Nullable UIAccess getUIAccess();

    /**
     * Moves popup to the given point. Does nothing if popup is invisible.
     *
     * @param screenPoint Point to move to.
     */
    void setLocation(Point screenPoint);

    void setSize(Dimension size);

    Dimension getSize();

    void setCaption(String title);

    boolean isPersistent();

    boolean isModalContext();

    boolean isNativePopup();

    void setUiVisible(boolean visible);

    <T> @Nullable T getUserData(Class<T> userDataClass);

    boolean isFocused();

    boolean isCancelKeyEnabled();

    void addListener(JBPopupListener listener);

    void removeListener(JBPopupListener listener);

    boolean isDisposed();

    Component getOwner();

    void setMinimumSize(@Nullable Dimension size);

    void setFinalRunnable(@Nullable Runnable runnable);

    void moveToFitScreen();

    
    Point getLocationOnScreen();

    void pack(boolean width, boolean height);

    default void setAdText(String s) {
        setAdText(s, SwingConstants.LEFT);
    }

    void setAdText(String s, int alignment);

    void setDataProvider(DataProvider dataProvider);

    /**
     * This callback is called when new key event from the event queue is being processed.
     * <p/>
     * The popup has a right to decide if its further processing should be continued (method return value).
     *
     * @param e new key event being processed
     * @return {@code true} if the event is completely dispatched, i.e. no further processing is necessary;
     * {@code false} otherwise
     */
    boolean dispatchKeyEvent(KeyEvent e);

    /**
     * Whether it's OK to invoke one of the 'show' methods. Some implementation might prohibit it e.g. if the popup is shown already.
     */
    default boolean canShow() {
        return !isDisposed();
    }

    default void registerAction(String aActionName, int aKeyCode, int aModifier, Action aAction) {
        throw new UnsupportedOperationException();
    }

    default void registerAction(String aActionName, KeyStroke keyStroke, Action aAction) {
        throw new UnsupportedOperationException();
    }
}
