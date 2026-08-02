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
package consulo.web.internal.ui;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.dialog.Dialog;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.LightPopup;
import consulo.ui.LightPopupOptions;
import consulo.ui.LightPopupPosition;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.LightPopupCloseEvent;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaadin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

/**
 * A popup is a dialog without the chrome of one - a vaadin popover would anchor itself to a target, but it draws
 * nothing at all unless that target is a real element, and a popup raised from a menu has no element to point at.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public class WebLightPopupImpl extends VaadinComponentDelegate<WebLightPopupImpl.Vaadin> implements LightPopup {
    public class Vaadin extends Dialog implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebLightPopupImpl.this;
        }
    }

    /**
     * How far a stacked popup is moved off the one which owns it, so a cascade reads as a cascade.
     */
    private static final int CASCADE_STEP = 24;

    private final LightPopupPosition myPosition;

    private boolean myDisposed;

    private com.vaadin.flow.component.@Nullable Component myContent;

    public WebLightPopupImpl(LightPopupOptions options) {
        myPosition = options.getPosition();

        Vaadin dialog = getVaadinComponent();

        dialog.setCloseOnEsc(options.isCancelOnEscape());
        dialog.setCloseOnOutsideClick(options.isCancelOnClickOutside());
        dialog.setResizable(options.isResizable());
        dialog.setDraggable(false);
        // a click outside only reaches the popup when something is there to catch it, so dismissing that way needs
        // a curtain - a visual one, which dismisses without blocking the ide the way a real modal would
        dialog.setModality(options.isCancelOnClickOutside() ? ModalityMode.VISUAL : ModalityMode.MODELESS);

        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                closed();
            }
        });
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    @RequiredUIAccess
    public void setTitle(@Nullable String title) {
        getVaadinComponent().setHeaderTitle(title == null ? "" : title);
    }

    @Override
    @RequiredUIAccess
    public void setContent(Component content) {
        Vaadin dialog = getVaadinComponent();

        if (myContent != null) {
            dialog.remove(myContent);
        }

        myContent = TargetVaadin.to(content);
        dialog.add(myContent);
    }

    @Override
    @RequiredUIAccess
    public void showBy(Component target) {
        if (myDisposed) {
            throw new IllegalArgumentException("LightPopup already disposed");
        }

        if (myPosition == LightPopupPosition.END) {
            // offset off the centre rather than measured against the target - a box is only measurable once it is
            // on screen, and that answer comes back from the browser too late to place this one
            getVaadinComponent().setLeft("calc(50% + " + CASCADE_STEP + "px)");
            getVaadinComponent().setTop("calc(50% - " + CASCADE_STEP + "px)");
        }

        getVaadinComponent().open();
    }

    @Override
    @RequiredUIAccess
    public void showInCenterOf(@Nullable Window window) {
        if (myDisposed) {
            throw new IllegalArgumentException("LightPopup already disposed");
        }

        getVaadinComponent().open();
    }

    @Override
    @RequiredUIAccess
    public void close() {
        if (myDisposed) {
            return;
        }

        getVaadinComponent().close();

        closed();
    }

    /**
     * Runs for both an api close and a dismissal from the browser, so anything registered through
     * {@link #addCloseListener} sees every close.
     */
    @RequiredUIAccess
    private void closed() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;

        getVaadinComponent().getElement().removeFromParent();

        getListenerDispatcher(LightPopupCloseEvent.class).onEvent(new LightPopupCloseEvent(this));

        Disposer.dispose(this);
    }

    @Override
    public boolean isVisible() {
        return !myDisposed && getVaadinComponent().isOpened();
    }

    @Override
    public void dispose() {
    }
}
