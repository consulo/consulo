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
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.container.plugin.PluginId;
import consulo.disposer.Disposable;
import consulo.externalService.impl.internal.auth.HubAccounts;
import consulo.externalService.impl.internal.storage.ExternalStoragePluginManager;
import consulo.externalService.impl.internal.storage.StoragePlugin;
import consulo.externalService.impl.internal.storage.StoragePluginState;
import consulo.externalService.internal.startup.StartupCustomizeContext;
import consulo.externalService.internal.startup.StartupCustomizeStep;
import consulo.externalService.internal.startup.StartupCustomizeUI;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Length;
import consulo.ui.RadioGroup;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.TileLayout;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Starts from scratch, or from the plugins the hub account of the user keeps. Hidden until the login works on a first start
 * and on the web - the property shows it for trying it out.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public class LoginStartupCustomizeStep implements StartupCustomizeStep {
    private static final String SHOW_PROPERTY = "consulo.startup.customize.login";

    private @Nullable RadioGroup<Boolean> myGroup;
    private @Nullable Label myStatusLabel;
    private @Nullable Component myComponent;

    @Override
    public LocalizeValue getTitle() {
        return ExternalServiceLocalize.startupCustomizeLoginTitle();
    }

    @Override
    public boolean isVisible(StartupCustomizeContext context) {
        return Boolean.getBoolean(SHOW_PROPERTY);
    }

    @RequiredUIAccess
    @Override
    public Component getComponent(StartupCustomizeContext context, Disposable uiDisposable) {
        Component component = myComponent;
        if (component != null) {
            return component;
        }

        RadioGroup<Boolean> group = RadioGroup.create();
        TileLayout tiles = TileLayout.create(Length.ofFont(12));
        tiles.add(StartupCustomizeUI.choiceCard(
            group,
            Boolean.FALSE,
            ExternalServiceLocalize.startupCustomizeLoginScratch(),
            ExternalServiceLocalize.startupCustomizeLoginScratchDescription(),
            null
        ));
        tiles.add(StartupCustomizeUI.choiceCard(
            group,
            Boolean.TRUE,
            ExternalServiceLocalize.startupCustomizeLoginHub(),
            ExternalServiceLocalize.startupCustomizeLoginHubDescription(),
            null
        ));
        group.setValue(Boolean.FALSE);
        myGroup = group;

        Label status = Label.create(LocalizeValue.empty());
        myStatusLabel = status;

        DockLayout content = DockLayout.create();
        content.top(tiles);
        content.bottom(status);

        component = StartupCustomizeUI.page(ExternalServiceLocalize.startupCustomizeLoginHeader(), content);
        myComponent = component;
        return component;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<?> validateStep(StartupCustomizeContext context) {
        RadioGroup<Boolean> group = myGroup;
        if (group == null || !Boolean.TRUE.equals(group.getValue())) {
            return CompletableFuture.completedFuture(null);
        }

        UIAccess uiAccess = UIAccess.current();
        Application application = Application.get();

        HubAccounts hubAccounts = application.getInstance(HubAccounts.class);
        CompletableFuture<?> login;
        if (hubAccounts.getActiveAccount() != null) {
            login = CompletableFuture.completedFuture(null);
        }
        else {
            setStatus(ExternalServiceLocalize.startupCustomizeLoginWaiting());
            login = hubAccounts.login();
        }

        return login
            .thenApplyAsync(ignored -> readStoragePlugins(), AppExecutorUtil.getAppExecutorService())
            .whenCompleteAsync((plugins, error) -> {
                setStatus(LocalizeValue.empty());
                if (plugins != null) {
                    for (StoragePlugin plugin : plugins) {
                        if (plugin.state == StoragePluginState.ENABLED) {
                            context.getPluginsToInstall().add(PluginId.getId(plugin.id));
                        }
                    }
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    private void setStatus(LocalizeValue text) {
        Label label = myStatusLabel;
        if (label != null) {
            label.setText(text);
        }
    }

    private static StoragePlugin[] readStoragePlugins() {
        try {
            return ExternalStoragePluginManager.list();
        }
        catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}
