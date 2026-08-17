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

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.PopupCloseEvent;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A popover rather than a dialog - it hangs off its target, it does not dim what is behind it, and it leaves the
 * focus where it was. A dialog is the wrong shape for this: it is placed in the middle of the frame and has to be
 * pushed into position afterwards, and it draws a curtain to catch the click which dismisses it.
 * <p/>
 * Positioning is the popover's own - it measures its target and turns itself around when there is no room, which is
 * work that cannot be done from the server without asking the browser for every box first.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public class WebLightPopupImpl extends VaadinComponentDelegate<WebLightPopupImpl.Vaadin> implements LightPopup {
    private static final String RESIZABLE_CLASS = "consulo-resizable-popup";
    private static final String MIN_WIDTH_PROPERTY = "--consulo-popup-min-width";

    @StyleSheet("/popup/webLightPopup.css")
    public class Vaadin extends Popover implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebLightPopupImpl.this;
        }
    }

    /**
     * A zero sized element parked where the popup should hang from, for anchoring to a place inside a component
     * rather than to the component - a caret has no element of its own to point at.
     */
    private @Nullable Div myAnchor;

    /**
     * The popups escape closes, innermost first, kept on the ui because a browser session has one of its own.
     * <p/>
     * A popup which never takes the focus cannot answer the key by itself: the keymap owns escape, so the frontend
     * hands it to the platform before anything on the page sees it. {@link consulo.web.ui.impl.internal.base.WebShortcutDispatcher}
     * asks here first, which is the order the desktop already runs in - the hint manager takes escape ahead of the
     * editor action bound to it.
     */
    private static final String ESCAPABLE_POPUPS = "consulo.escapable.popups";

    private boolean myEscapable;

    private final PopupOptions myOptions;

    private boolean myDisposed;

    public WebLightPopupImpl(PopupOptions options) {
        myOptions = options;

        Vaadin popover = getVaadinComponent();

        popover.setPosition(options.getPosition() == PopupPosition.END ? PopoverPosition.END_TOP : PopoverPosition.BOTTOM_START);
        popover.setOpenOnClick(false);
        popover.setCloseOnEsc(options.isCancelOnEscape());
        popover.setCloseOnOutsideClick(options.isCancelOnClickOutside());
        popover.setModal(false);
        // the popup reports on what the user is doing somewhere else - the lookup is driven from the editor, and the
        // caret has to stay there while it is up
        popover.setAutofocus(options.isRequestFocus());
        popover.setFocusDelay(0);
        popover.setHoverDelay(0);

        // a popover has no grip of its own, so the browser's is used. the box it draws is in the shadow root of the
        // popover and is only reachable as a part of it, which is why the class goes on the popover itself
        if (options.isResizable()) {
            popover.getElement().getClassList().add(RESIZABLE_CLASS);
        }

        popover.addOpenedChangeListener(event -> {
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
        getVaadinComponent().setAriaLabel(title);
    }

    @Override
    @RequiredUIAccess
    public void setMinimumWidth(int width) {
        // the overlay lives in the shadow root, so it is reached through a property the part it exposes reads
        getVaadinComponent().getElement().getStyle().set(MIN_WIDTH_PROPERTY, width <= 0 ? null : width + "px");
    }

    @Override
    @RequiredUIAccess
    public void setContent(Component content) {
        Vaadin popover = getVaadinComponent();

        popover.removeAll();
        popover.add(TargetVaadin.to(content));
    }

    @Override
    @RequiredUIAccess
    public void showBy(Component target) {
        checkNotDisposed();

        Vaadin popover = getVaadinComponent();

        attachToUI();

        popover.setTarget(TargetVaadin.to(target));
        popover.setOpened(true);
        listenForEscape();
    }

    /**
     * A popover is not a dialog: it does not put itself on the page when it opens, so an unattached one opens into
     * nothing at all.
     */
    @RequiredUIAccess
    private void attachToUI() {
        Vaadin popover = getVaadinComponent();
        if (popover.getParent().isPresent()) {
            return;
        }

        UI ui = ui();
        if (ui != null) {
            ui.add(popover);
        }
    }

    /**
     * Only for a popup which asked not to take the focus - one which has it is given the key by the browser and the
     * popover closes itself.
     */
    @RequiredUIAccess
    private void listenForEscape() {
        if (myEscapable || !myOptions.isCancelOnEscape() || myOptions.isRequestFocus()) {
            return;
        }

        Deque<WebLightPopupImpl> popups = escapablePopups(ui());
        if (popups != null) {
            popups.push(this);
            myEscapable = true;
        }
    }

    private @Nullable UI ui() {
        return getVaadinComponent().getUI().orElseGet(UI::getCurrent);
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Deque<WebLightPopupImpl> escapablePopups(@Nullable UI ui) {
        if (ui == null) {
            return null;
        }

        Deque<WebLightPopupImpl> popups = (Deque<WebLightPopupImpl>) ComponentUtil.getData(ui, ESCAPABLE_POPUPS);
        if (popups == null) {
            popups = new ArrayDeque<>();
            ComponentUtil.setData(ui, ESCAPABLE_POPUPS, popups);
        }
        return popups;
    }

    /**
     * Closes the innermost popup escape belongs to.
     *
     * @return whether the key was taken, so the caller leaves it alone rather than passing it to the keymap
     */
    @RequiredUIAccess
    public static boolean closeTopEscapable(@Nullable UI ui) {
        Deque<WebLightPopupImpl> popups = escapablePopups(ui);
        WebLightPopupImpl popup = popups == null ? null : popups.peek();
        if (popup == null) {
            return false;
        }

        popup.close();
        return true;
    }

    @Override
    @RequiredUIAccess
    public void showAt(Component target, int x, int y, int anchorHeight) {
        checkNotDisposed();

        // the anchor is put where the point is and the popover hangs off that, so the placement - and the turning
        // around when there is no room below - stays the popover's own rather than being computed here.
        // it cannot live inside the target: a component which owns its light dom - a grid does - drops whatever it
        // does not recognise, so it is placed on the page and moved to where the target is instead
        Div anchor = myAnchor;
        if (anchor == null) {
            anchor = new Div();
            anchor.getStyle()
                .set("position", "fixed")
                .set("width", "0")
                .set("pointer-events", "none");

            UI.getCurrent().add(anchor);
            myAnchor = anchor;
        }

        anchor.getStyle().set("height", anchorHeight + "px");

        // a popup which is already up is only moved. re-targeting an open popover tears the overlay down and builds
        // it again, which is every row of the list back over the wire for one typed character
        Vaadin popover = getVaadinComponent();
        boolean open = !popover.isOpened();

        // an element which is not on the page yet cannot be passed to a script, and the popover is one of the two
        // the placement below needs
        attachToUI();

        Div anchorToShow = anchor;
        // only the browser knows where the target ended up, and the popover has to be opened against an anchor
        // which is already in place - opening first would measure it at the top left of the page
        // vaadin-overlay only re-measures its position target from scroll, resize and its own observeMove, and none
        // of those see a target moved by a style change - PositionMixin leaves the overlay at the last place it
        // computed, so an open popup has to be told to measure again
        TargetVaadin.to(target).getElement().executeJs(
            """
            const rect = this.getBoundingClientRect();
            $0.style.left = (rect.left + $1) + 'px';
            $0.style.top = (rect.top + $2) + 'px';
            const overlay = $3.shadowRoot && $3.shadowRoot.querySelector('vaadin-popover-overlay');
            if (overlay && overlay._updatePosition) {
                overlay._updatePosition();
            }
            return true;
            """,
            anchorToShow.getElement(), x, y, popover.getElement()
        ).then(Boolean.class, positioned -> {
            if (myDisposed || !open) {
                return;
            }

            popover.setTarget(anchorToShow);
            popover.setOpened(true);
            listenForEscape();
        });
    }

    @RequiredUIAccess
    private void checkNotDisposed() {
        if (myDisposed) {
            throw new IllegalArgumentException("LightPopup already disposed");
        }
    }

    @Override
    @RequiredUIAccess
    public void close() {
        if (myDisposed) {
            return;
        }

        getVaadinComponent().setOpened(false);

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

        if (myEscapable) {
            myEscapable = false;

            Deque<WebLightPopupImpl> popups = escapablePopups(ui());
            if (popups != null) {
                popups.remove(this);
            }
        }

        if (myAnchor != null) {
            myAnchor.getElement().removeFromParent();
            myAnchor = null;
        }

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
