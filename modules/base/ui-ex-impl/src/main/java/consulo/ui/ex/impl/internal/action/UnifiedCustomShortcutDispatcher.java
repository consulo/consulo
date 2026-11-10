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

import consulo.dataContext.DataContext;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.ActionPromoterUtil;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
public final class UnifiedCustomShortcutDispatcher {
    private static final Logger LOG = Logger.getInstance(UnifiedCustomShortcutDispatcher.class);

    private UnifiedCustomShortcutDispatcher() {
    }

    public static List<AnAction> collectActions(Iterable<Component> components, KeyStroke keyStroke) {
        return collectActions(components, keyStroke::equals);
    }

    public static List<AnAction> collectActions(Iterable<Component> components, Predicate<KeyStroke> keyStrokeMatcher) {
        List<AnAction> result = new ArrayList<>();
        for (Component component : components) {
            List<AnAction> actions = component.getUserData(AnAction.ACTIONS_KEY);
            if (actions == null) {
                continue;
            }

            for (AnAction action : actions) {
                if (!result.contains(action) && matches(action.getShortcutSet(), keyStrokeMatcher)) {
                    result.add(action);
                }
            }
        }
        return result;
    }

    public static boolean hasCustomShortcuts(Component component) {
        List<AnAction> actions = component.getUserData(AnAction.ACTIONS_KEY);
        return actions != null && !actions.isEmpty();
    }

    private static boolean matches(ShortcutSet shortcutSet, Predicate<KeyStroke> keyStrokeMatcher) {
        for (Shortcut shortcut : shortcutSet.getShortcuts()) {
            if (shortcut instanceof KeyboardShortcut keyboardShortcut
                && keyboardShortcut.getSecondKeyStroke() == null
                && keyStrokeMatcher.test(keyboardShortcut.getFirstKeyStroke())) {
                return true;
            }
        }
        return false;
    }

    public static List<AnAction> collectKeymapActions(Iterable<KeyStroke> keyStrokes, boolean modalContext) {
        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        if (keymap == null) {
            return List.of();
        }

        ActionManager actionManager = ActionManager.getInstance();
        List<AnAction> actions = new ArrayList<>();
        for (KeyStroke keyStroke : keyStrokes) {
            for (String actionId : keymap.getActionIds(keyStroke)) {
                if (!hasSingleStrokeShortcut(keymap, actionId, keyStroke)) {
                    continue;
                }

                AnAction action = actionManager.getAction(actionId);
                if (action != null && !actions.contains(action) && (!modalContext || action.isEnabledInModalContext())) {
                    actions.add(action);
                }
            }
        }
        return actions;
    }

    private static boolean hasSingleStrokeShortcut(Keymap keymap, String actionId, KeyStroke keyStroke) {
        for (Shortcut shortcut : keymap.getShortcuts(actionId)) {
            if (shortcut instanceof KeyboardShortcut keyboardShortcut
                && keyboardShortcut.getSecondKeyStroke() == null
                && keyStroke.equals(keyboardShortcut.getFirstKeyStroke())) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    public static CompletableFuture<Boolean> performFirstEnabled(
        List<AnAction> actions,
        DataContext context,
        @Nullable ComponentEvent<?> uiEvent
    ) {
        List<AnAction> ordered = new ArrayList<>(ActionPromoterUtil.rearrangeByPromoters(actions, context));
        ordered.sort(Comparator.comparing(AnAction::getExecuteWeight).reversed());
        return performFirstEnabled(ordered, 0, context, uiEvent, UIAccess.current());
    }

    private static CompletableFuture<Boolean> performFirstEnabled(
        List<AnAction> actions,
        int index,
        DataContext context,
        @Nullable ComponentEvent<?> uiEvent,
        UIAccess uiAccess
    ) {
        if (index >= actions.size()) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        AnAction action = actions.get(index);
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.KEYBOARD_SHORTCUT, context, uiEvent);
        return ActionRunnerAsync.lastUpdateAndCheckDumbAsync(action, event, false)
            .handleAsync((enabled, throwable) -> {
                if (throwable != null && !UnifiedActionMenuExpander.isProcessCanceled(throwable)) {
                    LOG.warn("Action update failed: " + action.getClass().getName(), throwable);
                }

                if (throwable != null || !Boolean.TRUE.equals(enabled)) {
                    return performFirstEnabled(actions, index + 1, context, uiEvent, uiAccess);
                }

                UnifiedActionMenuExpander.performActionWithCallbacks(action, context, event);
                return CompletableFuture.completedFuture(Boolean.TRUE);
            }, uiAccess)
            .thenCompose(next -> next);
    }
}
