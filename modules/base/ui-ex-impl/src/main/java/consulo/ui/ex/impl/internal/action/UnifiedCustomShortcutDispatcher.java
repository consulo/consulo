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
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
public final class UnifiedCustomShortcutDispatcher {
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

    @RequiredUIAccess
    public static CompletableFuture<Boolean> performFirstEnabled(List<AnAction> actions, DataContext context) {
        return performFirstEnabled(actions, 0, context, UIAccess.current());
    }

    private static CompletableFuture<Boolean> performFirstEnabled(List<AnAction> actions, int index, DataContext context, UIAccess uiAccess) {
        if (index >= actions.size()) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        AnAction action = actions.get(index);
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.KEYBOARD_SHORTCUT, context);
        return ActionRunnerAsync.lastUpdateAndCheckDumbAsync(action, event, false)
            .handleAsync((enabled, throwable) -> {
                if (throwable != null || !Boolean.TRUE.equals(enabled)) {
                    return performFirstEnabled(actions, index + 1, context, uiAccess);
                }

                UnifiedActionMenuExpander.performActionWithCallbacks(action, context, event);
                return CompletableFuture.completedFuture(Boolean.TRUE);
            }, uiAccess)
            .thenCompose(next -> next);
    }
}
