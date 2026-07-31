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
package consulo.web.internal.ui.action;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.dom.DebouncePhase;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.ide.impl.idea.ide.ui.customization.CustomActionsSchemaImpl;
import consulo.logging.Logger;
import consulo.ui.MenuItem;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.impl.internal.action.MenuItemPresentationFactory;
import consulo.web.internal.ui.WebMenuItemImpl;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Popup menu of an action group, shown by the browser on right click. Web analog of
 * {@code consulo.ui.ex.awt.PopupHandler#installPopupMenu}.
 *
 * @author VISTALL
 * @since 2026-07-31
 */
public final class WebActionContextMenu {
    private static final Logger LOG = Logger.getInstance(WebActionContextMenu.class);

    /**
     * the browser opens the overlay on its own, there is no round trip left once the right click happened, so the
     * items are expanded while the pointer is still travelling over the target - same trick the main menu uses
     */
    private static final int REFRESH_DEBOUNCE_MS = 300;

    /**
     * how long the browser waits before opening the overlay, so the refresh the right click asked for can land
     */
    private static final int REPLAY_DELAY_MS = 350;

    private final ContextMenu myContextMenu = new ContextMenu();
    private final @Nullable String myGroupId;
    private final @Nullable ActionGroup myGroup;
    private final String myPlace;
    private final WebActionMenuExpander.DataContextSupplier myContextSupplier;
    private final MenuItemPresentationFactory myPresentationFactory = new MenuItemPresentationFactory();

    private String mySignature = "";
    private @Nullable ProgressIndicator myUpdateIndicator;

    @RequiredUIAccess
    public WebActionContextMenu(
        com.vaadin.flow.component.Component target,
        String groupId,
        String place,
        WebActionMenuExpander.DataContextSupplier contextSupplier
    ) {
        this(target, groupId, null, place, contextSupplier);
    }

    @RequiredUIAccess
    public WebActionContextMenu(
        com.vaadin.flow.component.Component target,
        ActionGroup group,
        String place,
        WebActionMenuExpander.DataContextSupplier contextSupplier
    ) {
        this(target, null, group, place, contextSupplier);
    }

    @RequiredUIAccess
    private WebActionContextMenu(
        com.vaadin.flow.component.Component target,
        @Nullable String groupId,
        @Nullable ActionGroup group,
        String place,
        WebActionMenuExpander.DataContextSupplier contextSupplier
    ) {
        myGroupId = groupId;
        myGroup = group;
        myPlace = place;
        myContextSupplier = contextSupplier;

        // created outside a VaadinComponentDelegate, so the small variant has to be added by hand here
        myContextMenu.getElement().getThemeList().add("small");

        myContextMenu.setTarget(target);

        target.getElement()
            .addEventListener("mouseover", event -> refresh())
            .debounce(REFRESH_DEBOUNCE_MS, DebouncePhase.LEADING, DebouncePhase.TRAILING);

        installContextMenuReplay(target);
    }

    /**
     * The vaadin context menu listens for {@code contextmenu} on the target itself, and a component that handles the
     * event on its own children - the orion editor does - never lets it get there. The event is taken in the capture
     * phase instead, which runs before any descendant, and dispatched again on the target.
     * <p>
     * The replay is delayed on purpose: the items are expanded ahead of the click, off the pointer entering the
     * target, and by then the right click has not yet moved the caret or the selection the group is expanded
     * against. Asking for a refresh here and waiting for it is what makes the menu answer for the clicked row.
     */
    private static void installContextMenuReplay(com.vaadin.flow.component.Component target) {
        target.getElement().executeJs("""
            const element = this;
            element.addEventListener('contextmenu', event => {
                if (event.$consuloMenuReplay) {
                    return;
                }

                event.preventDefault();
                event.stopPropagation();

                element.dispatchEvent(new MouseEvent('mouseover', { bubbles: true }));

                const replay = new MouseEvent('contextmenu', {
                    bubbles: true,
                    cancelable: true,
                    clientX: event.clientX,
                    clientY: event.clientY,
                    button: event.button
                });
                replay.$consuloMenuReplay = true;

                setTimeout(() => element.dispatchEvent(replay), $0);
            }, { capture: true });
            """, REPLAY_DELAY_MS);
    }

    @RequiredUIAccess
    public void refresh() {
        ActionGroup group = myGroup;
        if (group == null) {
            // the customization schema is what turns the id into the group the user actually configured
            AnAction correctedAction = CustomActionsSchemaImpl.getInstance().getCorrectedAction(myGroupId);
            if (!(correctedAction instanceof ActionGroup corrected)) {
                return;
            }
            group = corrected;
        }

        UIAccess uiAccess = UIAccess.current();

        ProgressIndicator previousIndicator = myUpdateIndicator;
        if (previousIndicator != null) {
            previousIndicator.cancel();
        }

        ProgressIndicator indicator = new EmptyProgressIndicator();
        myUpdateIndicator = indicator;

        WebActionMenuExpander.expandAsync(group, myContextSupplier.get(), myPlace, myPresentationFactory, uiAccess, indicator)
            .whenCompleteAsync((nodes, throwable) -> {
                if (myUpdateIndicator != indicator) {
                    return;
                }

                myUpdateIndicator = null;

                if (throwable != null) {
                    if (!WebActionMenuExpander.isProcessCanceled(throwable)) {
                        LOG.warn("Failed to expand action group " + myGroupId, throwable);
                    }
                    return;
                }

                applyNodes(nodes);
            }, uiAccess);
    }

    @RequiredUIAccess
    private void applyNodes(List<WebActionMenuExpander.MenuNode> nodes) {
        if (myContextMenu.isOpened()) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        appendSignature(nodes, builder);
        String signature = builder.toString();

        if (signature.equals(mySignature)) {
            return;
        }

        mySignature = signature;

        myContextMenu.removeAll();

        for (WebActionMenuExpander.MenuNode node : nodes) {
            MenuItem item = WebActionMenuExpander.createMenuItem(node, myContextSupplier, myPlace, myPresentationFactory);

            if (item instanceof WebMenuItemImpl webMenuItem) {
                webMenuItem.render(myContextMenu);
            }
        }
    }

    private static void appendSignature(List<WebActionMenuExpander.MenuNode> nodes, StringBuilder builder) {
        for (WebActionMenuExpander.MenuNode node : nodes) {
            if (node.isSeparator()) {
                builder.append("|-");
                continue;
            }

            builder.append('|')
                .append(node.text().get())
                .append(node.enabled() ? '+' : '-')
                .append(node.checked() == null ? "" : node.checked() ? "x" : "o")
                .append(node.shortcutText().get());

            List<WebActionMenuExpander.MenuNode> children = node.children();
            if (children != null) {
                builder.append('{');
                appendSignature(children, builder);
                builder.append('}');
            }
        }
    }
}
