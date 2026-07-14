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
package consulo.ide.impl.idea.openapi.actionSystem.ex;

import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorListener;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.internal.ActionUpdateInvoker;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineContextOwner;
import consulo.util.concurrent.coroutine.CoroutineException;
import consulo.util.concurrent.coroutine.CoroutineScope;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author VISTALL
 */
public final class ActionRunnerAsync {
    public static boolean ENABLED = true;

    private ActionRunnerAsync() {
    }

    public static CompletableFuture<Boolean> lastUpdateAndCheckDumbAsync(AnAction action, AnActionEvent e, boolean visibilityMatters) {
        return performDumbAwareUpdateAsync(action, e).thenApply(__ -> {
            Project project = e.getData(Project.KEY);
            if (project != null && DumbService.getInstance(project).isDumb() && !action.isDumbAware()) {
                if (Boolean.FALSE.equals(e.getPresentation().getClientProperty(ActionImplUtil.WOULD_BE_ENABLED_IF_NOT_DUMB_MODE))) {
                    return false;
                }
                if (visibilityMatters
                    && Boolean.FALSE.equals(e.getPresentation().getClientProperty(ActionImplUtil.WOULD_BE_VISIBLE_IF_NOT_DUMB_MODE))) {
                    return false;
                }

                ActionImplUtil.showDumbModeWarning(e);
                return false;
            }

            if (!e.getPresentation().isEnabled()) {
                return false;
            }
            if (visibilityMatters && !e.getPresentation().isVisible()) {
                return false;
            }

            return true;
        });
    }

    public static CompletableFuture<Void> performDumbAwareUpdateAsync(AnAction action, AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setEnabledAndVisible(true);
        boolean dumbMode = ActionImplUtil.isDumbMode(e.getData(Project.KEY));
        Boolean wasEnabledBefore = (Boolean) presentation.getClientProperty(ActionImplUtil.WAS_ENABLED_BEFORE_DUMB);
        if (wasEnabledBefore != null && !dumbMode) {
            presentation.putClientProperty(ActionImplUtil.WAS_ENABLED_BEFORE_DUMB, null);
            presentation.setEnabled(wasEnabledBefore);
            presentation.setVisible(true);
        }
        boolean enabledBeforeUpdate = presentation.isEnabled();
        boolean notAllowed = dumbMode && !action.isDumbAware();

        return launchUpdateAsync(action, e).handle((ignored, throwable) -> {
            try {
                if (throwable != null) {
                    Throwable cause = unwrapCoroutineException(throwable);
                    if (cause instanceof ProcessCanceledException pce) {
                        throw pce;
                    }
                    if (cause instanceof IndexNotReadyException indexNotReady) {
                        if (notAllowed) {
                            return null;
                        }
                        throw indexNotReady;
                    }
                    if (cause instanceof RuntimeException re) {
                        throw re;
                    }
                    if (cause instanceof Error err) {
                        throw err;
                    }
                    throw new RuntimeException(cause);
                }

                presentation.putClientProperty(ActionImplUtil.WOULD_BE_ENABLED_IF_NOT_DUMB_MODE, notAllowed && presentation.isEnabled());
                presentation.putClientProperty(ActionImplUtil.WOULD_BE_VISIBLE_IF_NOT_DUMB_MODE, notAllowed && presentation.isVisible());
                return null;
            }
            finally {
                if (notAllowed) {
                    if (wasEnabledBefore == null) {
                        presentation.putClientProperty(ActionImplUtil.WAS_ENABLED_BEFORE_DUMB, enabledBeforeUpdate);
                    }
                    presentation.setEnabled(false);
                }
            }
        });
    }

    private static CompletableFuture<?> launchUpdateAsync(AnAction action, AnActionEvent e) {
        // a single-action update outside an expansion still needs an update session so group actions can resolve
        // their children/presentations
        ActionImplUtil.ensureUpdateSession(e);

        Coroutine<?, ?> coroutine = ActionUpdateInvoker.createUpdateCoroutine(action, e);
        if (coroutine == null) {
            return CompletableFuture.completedFuture(null);
        }

        Project project = e.getData(Project.KEY);
        CoroutineContext context = project instanceof CoroutineContextOwner owner
            ? owner.coroutineContext()
            : Application.get().coroutineContext();

        CoroutineScope scope = CoroutineScope.of(context);
        scope.putCopyableUserData(UIAccess.KEY, Application.get().getLastUIAccess());

        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        if (indicator != null) {
            indicator.addListener(new ProgressIndicatorListener() {
                @Override
                public void canceled() {
                    scope.cancel();
                }
            });
        }

        return coroutine.runAsync(scope, null).toFuture();
    }

    private static Throwable unwrapCoroutineException(Throwable throwable) {
        Throwable cause = throwable;
        while ((cause instanceof CompletionException || cause instanceof CoroutineException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
