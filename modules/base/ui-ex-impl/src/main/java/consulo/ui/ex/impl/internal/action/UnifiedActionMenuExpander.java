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
package consulo.ui.ex.impl.internal.action;

import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.Menu;
import consulo.ui.MenuItem;
import consulo.ui.MenuSeparator;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * Expands an action group into presentation snapshots off the ui thread, so a frontend without a swing action
 * toolbar can build {@link consulo.ui} components out of the result. Action updates never run on the ui thread,
 * so nothing here may read live ui state - everything a presentation needs is taken from the data context that
 * was snapshotted before the expansion started.
 *
 * @author VISTALL
 * @since 2026-07-31
 */
public final class UnifiedActionMenuExpander {
    private static final Logger LOG = Logger.getInstance(UnifiedActionMenuExpander.class);

    private static final int MAX_DEPTH = 10;
    private static final String PERFORM_ONLY = "actionGroup.perform.only";

    public record MenuNode(
        @Nullable AnAction action,
        LocalizeValue text,
        @Nullable Image icon,
        boolean enabled,
        @Nullable Boolean checked,
        LocalizeValue shortcutText,
        @Nullable List<MenuNode> children
    ) {
        static MenuNode separator() {
            return new MenuNode(null, LocalizeValue.empty(), null, true, null, LocalizeValue.empty(), null);
        }

        public boolean isSeparator() {
            return action == null && children == null;
        }
    }

    private UnifiedActionMenuExpander() {
    }

    public static CompletableFuture<List<MenuNode>> expandAsync(
        ActionGroup group,
        DataContext context,
        String place,
        PresentationFactory presentationFactory,
        UIAccess uiAccess,
        ProgressIndicator indicator
    ) {
        return expandGroupAsync(group, context, place, presentationFactory, uiAccess, indicator, 0);
    }

    private static CompletableFuture<List<MenuNode>> expandGroupAsync(
        ActionGroup group,
        DataContext context,
        String place,
        PresentationFactory presentationFactory,
        UIAccess uiAccess,
        ProgressIndicator indicator,
        int depth
    ) {
        if (depth > MAX_DEPTH) {
            return CompletableFuture.completedFuture(List.of());
        }

        ActionUpdater updater = new ActionUpdater(
            ActionManager.getInstance(),
            presentationFactory,
            context,
            place,
            // everything expanded here ends up in a menu, and the platform reads that off the event - a toggle
            // drops its icon there so the leading slot can show the check mark instead
            true,
            false,
            uiAccess
        );

        return updater.expandActionGroupAsync(group, group instanceof CompactActionGroup, indicator)
            .thenCompose(children -> buildNodesAsync(
                children,
                0,
                new ArrayList<>(),
                context,
                place,
                presentationFactory,
                uiAccess,
                indicator,
                depth
            ));
    }

    private static CompletableFuture<List<MenuNode>> buildNodesAsync(
        List<? extends AnAction> children,
        int startIndex,
        List<MenuNode> result,
        DataContext context,
        String place,
        PresentationFactory presentationFactory,
        UIAccess uiAccess,
        ProgressIndicator indicator,
        int depth
    ) {
        for (int i = startIndex; i < children.size(); i++) {
            AnAction action = children.get(i);
            Presentation presentation = presentationFactory.getPresentation(action);

            if (action instanceof AnSeparator) {
                result.add(MenuNode.separator());
                continue;
            }

            if (action instanceof ActionGroup actionGroup && !Boolean.TRUE.equals(presentation.getClientProperty(PERFORM_ONLY))) {
                int nextIndex = i + 1;

                return expandGroupAsync(actionGroup, context, place, presentationFactory, uiAccess, indicator, depth + 1)
                    .thenCompose(groupChildren -> {
                        result.add(new MenuNode(
                            action,
                            presentation.getTextValue(),
                            presentation.getIcon(),
                            presentation.isEnabled(),
                            null,
                            LocalizeValue.empty(),
                            groupChildren
                        ));

                        return buildNodesAsync(
                            children,
                            nextIndex,
                            result,
                            context,
                            place,
                            presentationFactory,
                            uiAccess,
                            indicator,
                            depth
                        );
                    });
            }

            result.add(createActionNode(action, presentation));
        }

        return CompletableFuture.completedFuture(result);
    }

    private static MenuNode createActionNode(AnAction action, Presentation presentation) {
        Boolean checked = null;
        if (action instanceof Toggleable) {
            checked = Toggleable.isSelected(presentation);
        }

        return new MenuNode(
            action,
            presentation.getTextValue(),
            presentation.getIcon(),
            presentation.isEnabled(),
            checked,
            LocalizeValue.of(KeymapUtil.getFirstKeyboardShortcutText(action)),
            null
        );
    }

    @RequiredUIAccess
    public static MenuItem createMenuItem(
        MenuNode node,
        Supplier<DataContext> contextSupplier,
        String place,
        PresentationFactory presentationFactory
    ) {
        if (node.isSeparator()) {
            return MenuSeparator.create();
        }

        List<MenuNode> children = node.children();
        if (children != null) {
            Menu menu = Menu.create(node.text());
            menu.setIcon(node.icon());
            menu.setEnabled(node.enabled());

            for (MenuNode child : children) {
                menu.add(createMenuItem(child, contextSupplier, place, presentationFactory));
            }

            return menu;
        }

        MenuItem item = MenuItem.create(node.text());
        item.setIcon(node.icon());
        item.setEnabled(node.enabled());
        item.setChecked(node.checked());
        item.setShortcutText(node.shortcutText());

        AnAction action = node.action();
        if (action != null) {
            item.addClickListener(event -> performAction(action, contextSupplier.get(), place, presentationFactory));
        }

        return item;
    }

    @RequiredUIAccess
    public static void performAction(
        AnAction action,
        DataContext context,
        String place,
        PresentationFactory presentationFactory
    ) {
        UIAccess uiAccess = UIAccess.current();

        ActionManagerEx actionManager = (ActionManagerEx) ActionManager.getInstance();

        Presentation presentation = presentationFactory.getPresentation(action);

        AnActionEvent event = new AnActionEvent(null, context, place, presentation, actionManager, 0, true, false);
        event.setInjectedContext(action.isInInjectedContext());

        ActionRunnerAsync.lastUpdateAndCheckDumbAsync(action, event, false).whenCompleteAsync((enabled, throwable) -> {
            if (throwable != null) {
                if (!isProcessCanceled(throwable)) {
                    LOG.warn("Failed to update action before performing: " + action, throwable);
                }
                return;
            }

            if (Boolean.TRUE.equals(enabled)) {
                actionManager.fireBeforeActionPerformed(action, context, event);
                actionManager.performActionDumbAware(action, event);
                actionManager.queueActionPerformedEvent(action, context, event);
            }
        }, uiAccess);
    }

    public static boolean isProcessCanceled(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;

        return cause instanceof ProcessCanceledException || cause instanceof CancellationException;
    }

    public static void appendSignature(List<MenuNode> nodes, StringBuilder builder) {
        for (MenuNode node : nodes) {
            if (node.isSeparator()) {
                builder.append("|-");
                continue;
            }

            builder.append('|')
                .append(node.text().get())
                .append(node.enabled() ? '+' : '-')
                .append(node.checked() == null ? "" : node.checked() ? "x" : "o")
                .append(node.shortcutText().get());

            List<MenuNode> children = node.children();
            if (children != null) {
                builder.append('{');
                appendSignature(children, builder);
                builder.append('}');
            }
        }
    }
}
