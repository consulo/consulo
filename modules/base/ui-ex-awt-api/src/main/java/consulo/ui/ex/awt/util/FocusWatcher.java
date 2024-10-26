/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.awt.util;

import consulo.ui.ex.util.LafProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.ref.WeakReference;

/**
 * Spies how focus goes in the component.
 *
 * @author Vladimir Kondratyev
 */
public class FocusWatcher implements ContainerListener, FocusListener {
    private Component myTopComponent;
    /**
     * Last component that had focus.
     */
    private WeakReference<Component> myFocusedComponent;

    /**
     * @return top component on which focus watcher was installed.
     * The method always return <code>null</code> if focus watcher was installed
     * on some component hierarchy.
     */
    public Component getTopComponent() {
        return myTopComponent;
    }

    @Override
    public final void componentAdded(final ContainerEvent e) {
        installImpl(e.getChild());
    }

    @Override
    public final void componentRemoved(final ContainerEvent e) {
        Component removedChild = e.getChild();
        deinstall(removedChild, e);
    }

    public final void deinstall(final Component component) {
        deinstall(component, null);
    }

    public final void deinstall(final Component component, @Nullable AWTEvent cause) {
        if (component instanceof Container) {
            Container container = (Container) component;
            int componentCount = container.getComponentCount();
            for (int i = 0; i < componentCount; i++) {
                deinstall(container.getComponent(i));
            }
            container.removeContainerListener(this);
        }

        component.removeFocusListener(this);

        if (getFocusedComponent() == component) {
            setFocusedComponentImpl(null, cause);
        }
    }

    @Override
    public final void focusGained(final FocusEvent e) {
        final Component component = e.getComponent();
        if (e.isTemporary() || !component.isShowing()) {
            return;
        }

        setFocusedComponentImpl(component, e);
    }

    @Override
    public final void focusLost(final FocusEvent e) {
        Component component = e.getOppositeComponent();
        if (component != null && !SwingUtilities.isDescendingFrom(component, myTopComponent)) {
            focusLostImpl(e);
        }
    }

    /**
     * @return last focused component or <code>null</code>.
     */
    public final Component getFocusedComponent() {
        return myFocusedComponent != null ? myFocusedComponent.get() : null;
    }

    public final void install(@Nonnull Component component) {
        myTopComponent = component;
        installImpl(component);
    }

    private void installImpl(Component component) {
        if (component instanceof Container) {
            Container container = (Container) component;
            int componentCount = container.getComponentCount();
            for (int i = 0; i < componentCount; i++) {
                installImpl(container.getComponent(i));
            }
            container.addContainerListener(this);
        }
        if (component instanceof JMenuItem || component instanceof JMenuBar) {
            return;
        }
        component.addFocusListener(this);
    }

    public void setFocusedComponentImpl(Component component) {
        setFocusedComponentImpl(component, null);
    }

    public void setFocusedComponentImpl(Component component, @Nullable AWTEvent cause) {
        if (!isFocusedComponentChangeValid(component, cause)) {
            return;
        }

        if (LafProperty.isFocusProxy(component)) {
            _setFocused(getFocusedComponent(), cause);
            return;
        }

        _setFocused(component, cause);
    }

    private void _setFocused(final Component component, final AWTEvent cause) {
        setFocusedComponent(component);
        focusedComponentChanged(component, cause);
    }

    protected boolean isFocusedComponentChangeValid(final Component comp, final AWTEvent cause) {
        return comp != null || cause != null;
    }

    /**
     * Override this method to get notifications about focus. <code>FocusWatcher</code> invokes
     * this method each time one of the populated  component gains focus. All "temporary" focus
     * event are ignored.
     *
     * @param component currenly focused component. The component can be <code>null</code>
     * @param cause
     */
    protected void focusedComponentChanged(Component component, @Nullable final AWTEvent cause) {
    }

    protected void focusLostImpl(final FocusEvent e) {
    }

    private void setFocusedComponent(final Component focusedComponent) {
        myFocusedComponent = new WeakReference<>(focusedComponent);
    }
}