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

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationProperties;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.container.boot.ContainerPathManager;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.impl.internal.repository.RepositoryHelper;
import consulo.externalService.internal.startup.StartupCustomizeManager;
import consulo.externalService.update.UpdateSettings;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
@Singleton
@ServiceImpl
public class StartupCustomizeManagerImpl implements StartupCustomizeManager {
    private static final Logger LOG = Logger.getInstance(StartupCustomizeManagerImpl.class);

    private static final String PENDING_MARKER = "startup-customize.pending";
    private static final String TESTING_PROPERTY = "consulo.first.start.testing";

    private final Application myApplication;
    private final DialogService myDialogService;

    private @Nullable StartupCustomizeSession mySession;

    @Inject
    public StartupCustomizeManagerImpl(Application application, DialogService dialogService) {
        myApplication = application;
        myDialogService = dialogService;
    }

    @Override
    public void initialize(boolean newConfigFolder) {
        if (!newConfigFolder || ApplicationProperties.isInSandbox()) {
            return;
        }

        try {
            Path marker = marker();
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "");
        }
        catch (IOException e) {
            LOG.warn("Failed to remember the startup wizard as due", e);
        }
    }

    @Override
    public boolean isPending() {
        return Boolean.getBoolean(TESTING_PROPERTY) || Files.exists(marker());
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<?> showAsync(boolean firstShow) {
        StartupCustomizeSession session;
        synchronized (this) {
            session = mySession;
            if (session == null) {
                session = new StartupCustomizeSession(
                    myApplication,
                    myDialogService,
                    new StartupCustomizeContextImpl(firstShow),
                    this::loadRepository,
                    this::ended
                );
                mySession = session;
            }
        }
        return session.attach();
    }

    private void ended(StartupCustomizeSession session) {
        synchronized (this) {
            if (mySession == session) {
                mySession = null;
            }
        }
        clearPending();
    }

    private List<PluginDescriptor> loadRepository() {
        try {
            return RepositoryHelper.loadOnlyPluginsFromRepository(
                null,
                UpdateSettings.getInstance().getChannel(),
                EarlyAccessProgramManager.getInstance()
            );
        }
        catch (Exception e) {
            LOG.warn("The plugin repository is unavailable", e);
            return List.of();
        }
    }

    private void clearPending() {
        try {
            Files.deleteIfExists(marker());
        }
        catch (IOException e) {
            LOG.warn("Failed to forget the startup wizard", e);
        }
    }

    private static Path marker() {
        return Path.of(ContainerPathManager.get().getConfigPath(), PENDING_MARKER);
    }
}
