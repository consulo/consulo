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
package consulo.ui.ex.action;

import consulo.application.Application;
import consulo.ui.UIAccess;
import consulo.ui.ex.coroutine.UIAction;
import consulo.ui.ex.internal.AnActionWithUIUpdate;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 */
public final class ActionUpdateInvoker {
    private ActionUpdateInvoker() {
    }

    public static boolean hasUpdate(AnAction action) {
        return action instanceof AnActionWithAsyncUpdate
            || action instanceof AnActionWithUIUpdate
            || action instanceof AnActionWithSyncUpdate;
    }

    @SuppressWarnings("deprecation")
    public static @Nullable Coroutine<?, ?> createUpdateCoroutine(AnAction action, AnActionEvent e) {
        if (action instanceof AnActionWithAsyncUpdate async) {
            return async.updateAsync(e);
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
                Application.get().runReadAction(() -> sync.update(e));
                return input;
            }));
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    public static void updateSync(AnAction action, AnActionEvent e) {
        if (action instanceof AnActionWithSyncUpdate sync) {
            sync.update(e);
            return;
        }

        if (action instanceof AnActionWithUIUpdate atUI) {
            atUI.updateAtUI(e);
            return;
        }

        if (action instanceof AnActionWithAsyncUpdate async) {
            Application application = Application.get();
            CoroutineScope scope = new CoroutineScope(application.coroutineContext());
            scope.putCopyableUserData(UIAccess.KEY, application.getLastUIAccess());
            async.updateAsync(e).runBlocking(scope, null);
        }
    }
}
