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
package consulo.ui.ex.internal;

import consulo.application.Application;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.UIAction;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 */
public final class ActionUpdateInvoker {
    private static final Logger LOG = Logger.getInstance(ActionUpdateInvoker.class);

    private ActionUpdateInvoker() {
    }

    private static Coroutine<?, ?> checkIt(AnAction action, Coroutine<?, ?> coroutine) {
        if (coroutine.anyStep(step -> step instanceof ReadLock || step instanceof WriteLock)) {
            LOG.error(action + " async update coroutine must not use blocking ReadLock/WriteLock steps; " +
                "use ActionSafeReadLock (non-blocking tryToRead) instead. Update skipped.");
            return Coroutine.empty();
        }

        return coroutine;
    }

    @SuppressWarnings("deprecation")
    public static @Nullable Coroutine<?, ?> createUpdateCoroutine(AnAction action, AnActionEvent e) {
        if (action instanceof AnActionWithAsyncUpdate async) {
            return checkIt(action, async.updateAsync(e));
        }

        if (action instanceof AnActionWithUIUpdate atUI) {
            return Coroutine.first(UIAction.<Object, Object>apply(input -> {
                atUI.updateAtUI(e);
                return input;
            }));
        }

        if (action instanceof AnActionWithSyncUpdate sync) {
            if (sync.getActionUpdateThread() == ActionUpdateThread.EDT) {
                return Coroutine.first(UIAction.<Object, Object>apply(input -> {
                    sync.update(e);
                    return input;
                }));
            }
            return Coroutine.first(CodeExecution.<Object, Object>apply(input -> {
                sync.update(e);
                return input;
            }));
        }

        return null;
    }

    /**
     * Runs the action's update asynchronously (no coroutine and a completed future when the action
     * implements no update interface). This is the async entry point for modules which cannot reach
     * the richer ide-impl runner; no dumb-mode bookkeeping is performed here.
     */
    public static CompletableFuture<Void> updateAsync(AnAction action, AnActionEvent e) {
        Coroutine<?, ?> coroutine = createUpdateCoroutine(action, e);
        if (coroutine == null) {
            return CompletableFuture.completedFuture(null);
        }

        Application application = Application.get();
        CoroutineScope scope = CoroutineScope.of(application.coroutineContext());
        scope.putCopyableUserData(UIAccess.KEY, application.getLastUIAccess());
        return coroutine.runAsync(scope, null).toFuture().thenApply(ignored -> null);
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    public static void updateSync(AnAction action, AnActionEvent e) {
        if (action instanceof AnActionWithSyncUpdate sync) {
            sync.update(e);
            return;
        }

        if (action instanceof AnActionWithUIUpdate atUI) {
            UIAccess.assertIsUIThread();
            atUI.updateAtUI(e);
            return;
        }

        if (action instanceof AnActionWithAsyncUpdate async) {
            Application application = Application.get();
            CoroutineScope scope = CoroutineScope.of(application.coroutineContext());
            scope.putCopyableUserData(UIAccess.KEY, application.getLastUIAccess());
            async.updateAsync(e).runBlocking(scope, null);
        }
    }
}
