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
package consulo.web.ui.impl.internal.action;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.ui.ex.action.CustomActionsSchema;
import consulo.logging.Logger;
import consulo.ui.MenuItem;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.impl.internal.action.MenuItemPresentationFactory;
import consulo.web.ui.impl.internal.WebMenuItemImpl;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Optional;

/**
 * Popup menu of an action group, shown by the browser on right click. Web analog of
 * {@code consulo.ui.ex.awt.PopupHandler#installPopupMenu}.
 *
 * @author VISTALL
 * @since 2026-07-31
 */
public final class WebActionContextMenu {
    private static final Logger LOG = Logger.getInstance(WebActionContextMenu.class);

    private final ContextMenu myContextMenu;
    private final com.vaadin.flow.component.Component myTarget;
    private final @Nullable String myGroupId;
    private final @Nullable ActionGroup myGroup;

    /**
     * The browser asked for the menu and waits for {@code openMenu}. The overlay is opened only after the items
     * of this right click are applied - answering right away would show the items of the previous click, and a
     * rebuild landing under an open overlay leaves it displaying items whose server side is gone, so every click
     * on them is silently dropped.
     */
    private boolean myOpenRequested;

    /**
     * Set when the group the next open belongs to is decided by what was under the pointer rather than by the
     * component the menu hangs on.
     */
    private @Nullable ActionGroup myOverrideGroup;
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
        myTarget = target;
        myPlace = place;
        myContextSupplier = contextSupplier;

        // the browser reports the right click and waits for openMenu - see beforeOpenMenu
        myContextMenu = new ContextMenu() {
            @Override
            protected boolean onBeforeOpenMenu(ObjectNode eventDetail) {
                beforeOpenMenu();
                return false;
            }
        };

        // created outside a VaadinComponentDelegate, so the small variant has to be added by hand here
        myContextMenu.getElement().getThemeList().add("small");

        myContextMenu.setTarget(target);
    }

    /**
     * A component that decides the group off what was under the pointer - the editor gutter does - reports it over
     * its own event, which the browser sends in the same batch as the open request and which is processed first,
     * so a refresh it already started is the right one and is only awaited here.
     */
    @RequiredUIAccess
    private void beforeOpenMenu() {
        myOpenRequested = true;

        if (myUpdateIndicator == null) {
            refresh();
        }
    }

    /**
     * The target connector holds the coordinates of the click the open was requested for, so the overlay still
     * opens at the pointer even though the items were expanded in between.
     * <p>
     * The open is enqueued through {@code beforeClientResponse}: vaadin regenerates the client side items array
     * the same way, and an open sent ahead of it shows the array of the previous build - items whose server side
     * is already gone, every click on them silently dropped.
     */
    @RequiredUIAccess
    private void openRequestedMenu() {
        if (!myOpenRequested) {
            return;
        }

        myOpenRequested = false;

        Optional<UI> maybeUI = myTarget.getUI();
        if (maybeUI.isEmpty()) {
            return;
        }
        UI ui = maybeUI.get();

        if (!myContextMenu.isAttached()) {
            // vetoing onBeforeOpenMenu also skipped the overlay auto attach that vaadin does before its own open
            ui.add(myContextMenu);
        }

        ui.beforeClientResponse(
            myContextMenu,
            context -> myTarget.getElement().callJsFunction("$contextMenuTargetConnector.openMenu", myContextMenu.getElement())
        );
    }

    /**
     * Which group the next open shows, or {@code null} to go back to the one the menu was built with.
     */
    public void setOverrideGroup(@Nullable ActionGroup group) {
        myOverrideGroup = group;
    }

    @RequiredUIAccess
    public void refresh() {
        ActionGroup group = myOverrideGroup != null ? myOverrideGroup : myGroup;
        if (group == null) {
            // the customization schema is what turns the id into the group the user actually configured
            AnAction correctedAction = CustomActionsSchema.getInstance().getCorrectedAction(myGroupId);
            if (!(correctedAction instanceof ActionGroup corrected)) {
                myOpenRequested = false;
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
                    myOpenRequested = false;
                    return;
                }

                applyNodes(nodes);

                openRequestedMenu();
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
