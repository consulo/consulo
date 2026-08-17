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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.HeavyPopup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.PopupCloseEvent;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

/**
 * A popup with nothing to point at, so it is placed rather than anchored - a dialog stripped of its chrome. Anything
 * which does have a target is a {@link consulo.ui.LightPopup} and hangs off it instead of taking the frame over.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public class WebHeavyPopupImpl extends VaadinComponentDelegate<WebHeavyPopupImpl.Vaadin> implements HeavyPopup {
    @StyleSheet("/popup/webHeavyPopup.css")
    public class Vaadin extends Dialog implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebHeavyPopupImpl.this;
        }
    }

    /**
     * How far a stacked popup is moved off the one which owns it, so a cascade reads as a cascade.
     */
    private static final int CASCADE_STEP = 24;

    private final PopupPosition myPosition;

    private boolean myDisposed;

    private com.vaadin.flow.component.@Nullable Component myContent;

    public WebHeavyPopupImpl(PopupOptions options) {
        myPosition = options.getPosition();

        Vaadin dialog = getVaadinComponent();

        dialog.setCloseOnEsc(options.isCancelOnEscape());
        dialog.setCloseOnOutsideClick(options.isCancelOnClickOutside());
        dialog.setResizable(options.isResizable());
        dialog.setDraggable(false);
        // a click outside only reaches the popup when something is there to catch it, so dismissing that way needs
        // a curtain - a visual one, which dismisses without blocking the ide the way a real modal would
        dialog.setModality(options.isCancelOnClickOutside() ? ModalityMode.VISUAL : ModalityMode.MODELESS);

        if (!options.isRequestFocus()) {
            // an overlay pulls the focus into itself and hands it back when it closes, which is right for a popup the
            // user works in and wrong for one which only reports on what they are doing somewhere else - the lookup
            // is driven from the editor, and the caret has to stay there while it is up
            dialog.getElement().setProperty("noFocusTrap", true);
            dialog.getElement().executeJs("this.$.overlay.restoreFocusOnClose = false;");
        }

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
    public void showAt(Component target, int x, int y, int anchorHeight) {
        Vaadin dialog = getVaadinComponent();

        // only the browser knows where the target ended up, and the dialog has to be placed before it opens -
        // opening first shows it centred for a frame
        TargetVaadin.to(target).getElement().executeJs(
            """
            const rect = this.getBoundingClientRect();
            return (rect.left + $0) + ',' + (rect.top + $1);
            """,
            x, y
        ).then(String.class, position -> {
            int comma = position.indexOf(',');
            dialog.setLeft(position.substring(0, comma) + "px");
            dialog.setTop(position.substring(comma + 1) + "px");
            dialog.setOpened(true);
        });
    }

    @Override
    @RequiredUIAccess
    public void setMinimumWidth(int width) {
        getVaadinComponent().setMinWidth(width <= 0 ? null : width + "px");
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
    public void showInCenterOf(@Nullable Window window) {
        if (myDisposed) {
            throw new IllegalArgumentException("HeavyPopup already disposed");
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

        getListenerDispatcher(PopupCloseEvent.class).onEvent(new PopupCloseEvent(this));

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
