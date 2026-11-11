/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.internal.PreloadingActivity;
import consulo.application.progress.ProgressIndicator;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.touchBar.TouchBarController;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.internal.ActionPreInitializer;
import consulo.ui.ex.internal.TouchBarControllerInternal;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author yole
 */
@ExtensionImpl(order = "first")
public class ActionPreloader extends PreloadingActivity {
    /**
     * Creates every action in the background after they are registered - off by default. The stubs show the
     * presentation generated into their bindings, so nothing waits for it, and measured with a CPU limit it makes the
     * startup slower (about 1s at one CPU) for no gain elsewhere. Kept to compare.
     */
    private static final String PRELOAD_ACTIONS_PROPERTY = "consulo.actions.preload";

    private final Application myApplication;
    private final ActionManager myActionManager;
    private final Provider<TouchBarController> myTouchBarControllerProvider;

    @Inject
    public ActionPreloader(Application application,
                           ActionManager actionManager,
                           Provider<TouchBarController> touchBarControllerProvider) {
        myApplication = application;
        myActionManager = actionManager;
        myTouchBarControllerProvider = touchBarControllerProvider;
    }

    @Override
    public void preload(ProgressIndicator indicator) {
        ActionManagerEx actionManager = (ActionManagerEx) myActionManager;

        // only the registration blocks those who ask for an action - the stubs are there once it is done
        actionManager.initialize(() -> {
            actionManager.loadActions();

            myApplication.getExtensionPoint(ActionPreInitializer.class).forEach(i -> i.preload(actionManager));
        });

        TouchBarControllerInternal touchBarController = (TouchBarControllerInternal) myTouchBarControllerProvider.get();
        touchBarController.tryToInitialize();

        // the actions are not created ahead - a stub shows the presentation generated into its binding, and the action
        // is created when it is updated or performed
        if (Boolean.getBoolean(PRELOAD_ACTIONS_PROPERTY)) {
            actionManager.preloadActions(indicator);
        }
    }
}
