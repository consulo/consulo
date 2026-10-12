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
import consulo.disposer.Disposable;
import consulo.externalService.internal.startup.StartupCustomizeContext;
import consulo.externalService.internal.startup.StartupCustomizeStep;
import consulo.externalService.internal.startup.StartupCustomizeUI;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import org.jspecify.annotations.Nullable;

/**
 * The last page, saying what finishing the wizard is going to do.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public class FinishStartupCustomizeStep implements StartupCustomizeStep {
    private final Application myApplication;

    private @Nullable Label mySummaryLabel;
    private @Nullable Component myComponent;

    public FinishStartupCustomizeStep(Application application) {
        myApplication = application;
    }

    static long pluginsToDownload(StartupCustomizeContext context) {
        return context.getPluginsToInstall().stream().filter(id -> !StartupCustomizePlugins.isInstalled(id)).count();
    }

    static boolean needsRestart(StartupCustomizeContext context) {
        return pluginsToDownload(context) > 0;
    }

    @Override
    public LocalizeValue getTitle() {
        return ExternalServiceLocalize.startupCustomizeFinishTitle();
    }

    @RequiredUIAccess
    @Override
    public Component getComponent(StartupCustomizeContext context, Disposable uiDisposable) {
        Component component = myComponent;
        if (component == null) {
            Label summary = Label.create(summary(context));
            mySummaryLabel = summary;

            component = StartupCustomizeUI.page(LocalizeValue.empty(), DockLayout.create().top(summary));
            myComponent = component;
        }
        return component;
    }

    @Override
    public void onStepEnter(StartupCustomizeContext context) {
        Label summary = mySummaryLabel;
        if (summary != null) {
            summary.setText(summary(context));
        }
    }

    private LocalizeValue summary(StartupCustomizeContext context) {
        long count = pluginsToDownload(context);
        if (count > 0) {
            return ExternalServiceLocalize.startupCustomizeFinishPlugins(count, myApplication.getName());
        }
        return ExternalServiceLocalize.startupCustomizeFinishNothing();
    }
}
