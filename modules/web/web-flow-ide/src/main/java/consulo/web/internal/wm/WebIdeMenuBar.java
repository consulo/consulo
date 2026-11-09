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
package consulo.web.internal.wm;

import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.dom.DebouncePhase;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.MenuItem;
import consulo.ui.MenuSeparator;
import consulo.ui.UIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.impl.internal.action.ActionRunnerAsync;
import consulo.ui.ex.impl.internal.action.ActionUpdater;
import consulo.ui.ex.impl.internal.action.MenuItemPresentationFactory;
import consulo.ui.ex.impl.internal.action.UnifiedActionMenuExpander;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import consulo.web.ui.impl.internal.WebMenuBarImpl;
import consulo.web.ui.impl.internal.WebMenuImpl;
import consulo.web.ui.impl.internal.WebMenuItemImpl;
import consulo.web.ui.impl.internal.base.WebFocusTracker;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * Web analog of {@code consulo.desktop.awt.wm.impl.DefaultIdeMenuBar}. Builds main menu from {@link IdeActions#GROUP_MAIN_MENU}
 * using async action expansion.
 *
 * @author VISTALL
 * @since 2026-07-31
 */
public class WebIdeMenuBar {
    private static final Logger LOG = Logger.getInstance(WebIdeMenuBar.class);

    private static final String PERFORM_ONLY = "actionGroup.perform.only";

    private static final String RESYNC_OPEN_SUB_MENUS = """
        const bar = this;
        requestAnimationFrame(() => bar.updateComplete.then(() => {
            let items = bar.items;
            let subMenu = bar._subMenu;
            while (subMenu && subMenu.opened) {
                const target = subMenu._positionTarget;
                const component = target && target.isConnected ? (target.item ? target.item.component : target) : null;
                const item = component && items ? items.find(candidate => candidate.component === component) : null;
                const children = item ? item.children : null;
                if (!children || children.length === 0) {
                    subMenu.close();
                    return;
                }

                subMenu.items = children;
                subMenu._overlayElement.requestContentUpdate();

                items = children;
                subMenu = subMenu._subMenu;
            }
        }));
        """;

    private record MenuNode(
        @Nullable AnAction action,
        LocalizeValue text,
        @Nullable Image icon,
        @Nullable Image disabledIcon,
        boolean enabled,
        @Nullable Boolean checked,
        LocalizeValue shortcutText,
        boolean group
    ) {
        static MenuNode separator() {
            return new MenuNode(null, LocalizeValue.empty(), null, null, true, null, LocalizeValue.empty(), false);
        }

        boolean isSeparator() {
            return action == null;
        }
    }

    private record BuiltItem(MenuItem item, MenuNode node) {
    }

    private static final class Level {
        private String mySignature = "";
        private String myStructuralSignature = "";
        private List<BuiltItem> myItems = List.of();
        private @Nullable ProgressIndicator myIndicator;
    }

    private final consulo.ui.Component myContextComponent;
    private final WebMenuBarImpl myMenuBar = new WebMenuBarImpl();
    private final MenuItemPresentationFactory myPresentationFactory = new MenuItemPresentationFactory();

    private Level myRootLevel = new Level();

    @RequiredUIAccess
    public WebIdeMenuBar(consulo.ui.Component contextComponent) {
        myContextComponent = contextComponent;

        // without the full frame width the bar measures only its natural width and vaadin collapses the
        // trailing items into the overflow button even though the frame has room to spare
        myMenuBar.toVaadinComponent().setWidthFull();

        // there is no server side 'menu about to open' event in vaadin menu bar, so presentations are refreshed when pointer
        // reaches the menu bar - that gives one round trip before any menu is actually opened
        // mouseover bubbles from the individual buttons, unlike mouseenter, so moving along the bar keeps the
        // presentations fresh instead of refreshing them only on the first entry. trailing catches the last move
        myMenuBar.toVaadinComponent()
            .getElement()
            .addEventListener("mouseover", event -> updateMenuActions())
            .debounce(300, DebouncePhase.LEADING, DebouncePhase.TRAILING);
    }

    public consulo.ui.MenuBar getMenuBar() {
        return myMenuBar;
    }

    /**
     * Forgets that the current items were ever sent. The signature exists to skip rebuilding a menu the client
     * already has - after the frame moves to another ui the client has nothing, while the signature still says
     * everything is on screen. Cleared, the next {@link #updateMenuActions()} sends the full menu again.
     */
    @RequiredUIAccess
    public void reset() {
        myRootLevel = new Level();
        myMenuBar.clear();
    }

    @RequiredUIAccess
    public void updateMenuActions() {
        expandAsync(
            myRootLevel,
            CustomActionsSchema.getCorrectedGroupAsync(IdeActions.GROUP_MAIN_MENU),
            UIAccess.current(),
            nodes -> applyLevel(myRootLevel, nodes.stream().filter(MenuNode::group).toList(), items -> {
                myMenuBar.clear();
                items.forEach(myMenuBar::add);
            })
        );
    }

    @RequiredUIAccess
    private void openMenu(WebMenuImpl menu, Level level, ActionGroup group) {
        expandAsync(level, CompletableFuture.completedFuture(group), UIAccess.current(), nodes -> applyLevel(level, nodes, items -> {
            menu.removeAll();
            items.forEach(menu::add);
        }));
    }

    private void expandAsync(
        Level level,
        CompletableFuture<@Nullable ActionGroup> groupFuture,
        UIAccess uiAccess,
        Consumer<List<MenuNode>> apply
    ) {
        ProgressIndicator previousIndicator = level.myIndicator;
        if (previousIndicator != null) {
            previousIndicator.cancel();
        }

        ProgressIndicator indicator = new EmptyProgressIndicator();
        level.myIndicator = indicator;

        DataContext context = createDataContext();

        groupFuture.thenCompose(group -> group == null
                ? CompletableFuture.completedFuture(List.<MenuNode>of())
                : expandGroupAsync(group, context, uiAccess, indicator))
            .whenCompleteAsync((nodes, throwable) -> {
                if (level.myIndicator != indicator) {
                    return;
                }

                level.myIndicator = null;

                if (throwable != null) {
                    if (!isProcessCanceled(throwable)) {
                        LOG.warn("Failed to expand main menu", throwable);
                    }
                    return;
                }

                apply.accept(nodes);
            }, uiAccess);
    }

    private DataContext createDataContext() {
        return DataManager.getInstance().createAsyncDataContext(WebFocusTracker.createDataContext(myContextComponent));
    }

    private CompletableFuture<List<MenuNode>> expandGroupAsync(
        ActionGroup group,
        DataContext context,
        UIAccess uiAccess,
        ProgressIndicator indicator
    ) {
        ActionUpdater updater = new ActionUpdater(
            ActionManager.getInstance(),
            myPresentationFactory,
            context,
            ActionPlaces.MAIN_MENU,
            false,
            false,
            uiAccess
        );

        return updater.expandActionGroupAsync(group, group instanceof CompactActionGroup, indicator).thenApply(this::buildNodes);
    }

    private List<MenuNode> buildNodes(List<? extends AnAction> children) {
        List<MenuNode> result = new ArrayList<>(children.size());
        for (AnAction action : children) {
            if (action instanceof AnSeparator) {
                result.add(MenuNode.separator());
                continue;
            }

            Presentation presentation = myPresentationFactory.getPresentation(action);

            if (action instanceof ActionGroup && !Boolean.TRUE.equals(presentation.getClientProperty(PERFORM_ONLY))) {
                result.add(new MenuNode(
                    action,
                    presentation.getTextValue(),
                    presentation.getIcon(),
                    presentation.getDisabledIcon(),
                    presentation.isEnabled(),
                    null,
                    LocalizeValue.empty(),
                    true
                ));
                continue;
            }

            Boolean checked = action instanceof Toggleable ? Toggleable.isSelected(presentation) : null;

            result.add(new MenuNode(
                action,
                presentation.getTextValue(),
                presentation.getIcon(),
                presentation.getDisabledIcon(),
                presentation.isEnabled(),
                checked,
                LocalizeValue.of(KeymapUtil.getFirstKeyboardShortcutText(action)),
                false
            ));
        }
        return result;
    }

    @RequiredUIAccess
    private void applyLevel(Level level, List<MenuNode> nodes, Consumer<List<MenuItem>> rebuild) {
        String signature = signature(nodes, true);
        if (signature.equals(level.mySignature)) {
            return;
        }

        level.mySignature = signature;

        String structuralSignature = signature(nodes, false);
        if (structuralSignature.equals(level.myStructuralSignature)) {
            refreshEnabled(nodes, level.myItems);
            return;
        }

        level.myStructuralSignature = structuralSignature;

        List<BuiltItem> builtItems = new ArrayList<>(nodes.size());
        for (MenuNode node : nodes) {
            builtItems.add(new BuiltItem(createMenuItem(node), node));
        }
        level.myItems = builtItems;

        rebuild.accept(builtItems.stream().map(BuiltItem::item).toList());

        resyncOpenSubMenus();
    }

    @RequiredUIAccess
    private static void refreshEnabled(List<MenuNode> nodes, List<BuiltItem> builtItems) {
        for (int i = 0; i < nodes.size() && i < builtItems.size(); i++) {
            MenuNode node = nodes.get(i);
            if (!node.isSeparator()) {
                builtItems.get(i).item().setEnabled(node.enabled());
            }
        }
    }

    @RequiredUIAccess
    private void resyncOpenSubMenus() {
        MenuBar menuBar = myMenuBar.toVaadinComponent();

        menuBar.getElement().getNode().runWhenAttached(
            ui -> ui.beforeClientResponse(menuBar, context -> menuBar.getElement().executeJs(RESYNC_OPEN_SUB_MENUS))
        );
    }

    @RequiredUIAccess
    private MenuItem createMenuItem(MenuNode node) {
        if (node.isSeparator()) {
            return MenuSeparator.create();
        }

        if (node.group()) {
            WebMenuImpl menu = new WebMenuImpl(node.text());
            menu.setIcon(UnifiedActionMenuExpander.toDisplayIcon(node.icon(), node.disabledIcon(), node.enabled()));
            menu.setEnabled(node.enabled());

            WebMenuItemImpl placeholder = new WebMenuItemImpl(LocalizeValue.of("\u2026"));
            placeholder.setEnabled(false);
            menu.add(placeholder);

            Level level = new Level();
            ActionGroup group = (ActionGroup) node.action();
            menu.setOpenListener(() -> openMenu(menu, level, group));
            return menu;
        }

        WebMenuItemImpl item = new WebMenuItemImpl(node.text());
        item.setIcon(UnifiedActionMenuExpander.toDisplayIcon(node.icon(), node.disabledIcon(), node.enabled()));
        item.setEnabled(node.enabled());
        item.setChecked(node.checked());
        item.setShortcutText(node.shortcutText());

        AnAction action = node.action();
        if (action != null) {
            item.addClickListener(event -> performAction(action, event));
        }

        return item;
    }

    @RequiredUIAccess
    private void performAction(AnAction action, ComponentEvent<?> uiEvent) {
        UIAccess uiAccess = UIAccess.current();

        ActionManagerEx actionManager = (ActionManagerEx) ActionManager.getInstance();

        DataContext context = createDataContext();

        Presentation presentation = myPresentationFactory.getPresentation(action);

        AnActionEvent event =
            new AnActionEvent(null, context, ActionPlaces.MAIN_MENU, presentation, actionManager, 0, true, false, uiEvent);
        event.setInjectedContext(action.isInInjectedContext());

        ActionRunnerAsync.lastUpdateAndCheckDumbAsync(action, event, false).whenCompleteAsync((enabled, throwable) -> {
            if (throwable != null) {
                if (!isProcessCanceled(throwable)) {
                    LOG.warn("Failed to update action before performing: " + action, throwable);
                }
                return;
            }

            if (Boolean.TRUE.equals(enabled)) {
                UnifiedActionMenuExpander.performActionWithCallbacks(action, context, event);
            }
        }, uiAccess);
    }

    private static String signature(List<MenuNode> nodes, boolean withEnabled) {
        StringBuilder builder = new StringBuilder();
        for (MenuNode node : nodes) {
            if (node.isSeparator()) {
                builder.append("|-");
                continue;
            }

            builder.append('|')
                .append(node.group() ? '>' : ' ')
                .append(node.text().get())
                .append(withEnabled ? node.enabled() ? '+' : '-' : ' ')
                .append(node.checked() == null ? "" : node.checked() ? "x" : "o")
                .append(node.shortcutText().get());
        }
        return builder.toString();
    }

    private static boolean isProcessCanceled(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;

        return cause instanceof ProcessCanceledException || cause instanceof CancellationException;
    }
}
