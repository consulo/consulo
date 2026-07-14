/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ui.ex.action.util;

import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionUpdateSession;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ActionGroupUtil {
    /**
     * Asynchronously validates the group and produces {@code true} if it has no enabled and visible action.
     * <p>
     * The walk uses the {@link ActionUpdateSession} of the current expansion: every child's presentation is taken
     * from the session cache (computed once, at the real place and with the real data context) instead of being
     * re-updated through a synthetic event. It short-circuits as soon as the first enabled and visible action is
     * visited. Requires a proper update session ({@link AnActionEvent#getUpdateSession()}).
     */
    public static Coroutine<?, Boolean> isGroupEmptyAsync(ActionGroup actionGroup, AnActionEvent e) {
        return Coroutine.first(CompletableFutureStep.<Object, Boolean>await(input -> isGroupEmptyAsync(actionGroup, e.getUpdateSession())));
    }

    /**
     * The {@link CompletableFuture} form of {@link #isGroupEmptyAsync(ActionGroup, AnActionEvent)}, for callers that
     * already compose futures with the {@link ActionUpdateSession} of the current expansion.
     */
    public static CompletableFuture<Boolean> isGroupEmptyAsync(ActionGroup group, ActionUpdateSession session) {
        return session.children(group).thenCompose(children -> isEmptyFrom(children, 0, session));
    }

    private static CompletableFuture<Boolean> isEmptyFrom(List<AnAction> children, int index, ActionUpdateSession session) {
        if (index >= children.size()) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        AnAction action = children.get(index);
        if (action instanceof AnSeparator) {
            return isEmptyFrom(children, index + 1, session);
        }

        return session.presentation(action).thenCompose(presentation -> {
            boolean enabledAndVisible = presentation != null && presentation.isEnabled() && presentation.isVisible();
            if (!enabledAndVisible) {
                return isEmptyFrom(children, index + 1, session);
            }

            if (action instanceof ActionGroup childGroup) {
                return isGroupEmptyAsync(childGroup, session).thenCompose(childEmpty -> {
                    // a visible non-empty subgroup makes the whole group non-empty; an empty one is skipped
                    return Boolean.TRUE.equals(childEmpty)
                        ? isEmptyFrom(children, index + 1, session)
                        : CompletableFuture.completedFuture(Boolean.FALSE);
                });
            }

            // a visible enabled leaf makes the group non-empty
            return CompletableFuture.completedFuture(Boolean.FALSE);
        });
    }
}
