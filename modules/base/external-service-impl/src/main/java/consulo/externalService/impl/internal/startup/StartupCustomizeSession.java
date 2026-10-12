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
package consulo.externalService.impl.internal.startup;

import consulo.application.Application;
import consulo.container.plugin.PluginDescriptor;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.internal.UIAccessInternal;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public class StartupCustomizeSession {
    private final Application myApplication;
    private final DialogService myDialogService;
    private final StartupCustomizeDialogDescriptor myDescriptor;
    private final Consumer<StartupCustomizeSession> myOnEnd;

    private @Nullable Dialog myDialog;
    private @Nullable CompletableFuture<Object> myResult;

    public StartupCustomizeSession(
        Application application,
        DialogService dialogService,
        StartupCustomizeContextImpl context,
        Supplier<List<PluginDescriptor>> repositoryLoader,
        Consumer<StartupCustomizeSession> onEnd
    ) {
        myApplication = application;
        myDialogService = dialogService;
        myDescriptor = new StartupCustomizeDialogDescriptor(application, context, repositoryLoader);
        myOnEnd = onEnd;
    }

    @RequiredUIAccess
    public CompletableFuture<?> attach() {
        UIAccess uiAccess = UIAccess.current();

        Dialog dialog;
        CompletableFuture<Object> previous;
        CompletableFuture<Object> result = new CompletableFuture<>();
        synchronized (this) {
            dialog = myDialog;
            previous = myResult;

            if (dialog != null && previous != null && isShownIn(dialog, uiAccess)) {
                return previous;
            }

            myResult = result;
        }

        if (previous != null && dialog != null && isShownInLiveUI(dialog)) {
            previous.cancel(false);
        }

        if (dialog == null) {
            dialog = myDialogService.build(myDescriptor);
            synchronized (this) {
                myDialog = dialog;
            }
            dialog.showAsync().whenComplete((value, error) -> closed());
        }
        else if (uiAccess instanceof UIAccessInternal internal) {
            internal.attachWindow(dialog.getWindow());
        }
        return result;
    }

    private static boolean isShownInLiveUI(Dialog dialog) {
        UIAccess shownIn = dialog.getWindow().getUIAccess();
        return shownIn != null && shownIn.isValid();
    }

    private static boolean isShownIn(Dialog dialog, UIAccess uiAccess) {
        UIAccess shownIn = dialog.getWindow().getUIAccess();
        return shownIn != null && UIAccessInternal.original(shownIn) == UIAccessInternal.original(uiAccess);
    }

    @RequiredUIAccess
    private void closed() {
        CompletableFuture<Object> result;
        synchronized (this) {
            result = myResult;
            myResult = null;
        }

        myOnEnd.accept(this);

        if (myDescriptor.isRestartRequested()) {
            myApplication.restart(true);
            return;
        }

        if (result != null) {
            result.complete(null);
        }
    }
}
