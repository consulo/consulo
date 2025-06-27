/*
 * Copyright 2013-2021 consulo.io
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
package consulo.externalService.impl.internal.storage;

import consulo.application.AccessToken;
import consulo.application.AppUIExecutor;
import consulo.application.Application;
import consulo.application.ApplicationProperties;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.start.StartupActionScriptManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.store.internal.IComponentStore;
import consulo.component.store.internal.StoreUtil;
import consulo.externalService.ExternalService;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.externalService.ExternalServiceConfigurationListener;
import consulo.externalService.impl.internal.WebServiceApi;
import consulo.externalService.impl.internal.WebServiceApiSender;
import consulo.externalService.impl.internal.plugin.PluginInstallUtil;
import consulo.externalService.impl.internal.repository.NoContentException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.io.StreamUtil;
import consulo.util.io.UnsyncByteArrayInputStream;
import consulo.util.lang.Pair;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author VISTALL
 * @since 2021-09-11
 */
public class ExternalStorageManager {
    public static final NotificationGroup GROUP = new NotificationGroup(
        "externalStorage",
        LocalizeValue.localizeTODO("External Storage"),
        NotificationDisplayType.BALLOON,
        true
    );

    private static final Logger LOG = Logger.getInstance(ExternalStorageManager.class);

    @Nonnull
    private final ApplicationEx myApplication;
    @Nonnull
    private final ExternalStorage myStorage;
    @Nonnull
    private final IComponentStore myApplicationStore;
    @Nonnull
    private final ExternalStoragePluginManager myPluginManager;

    private Future<?> myCheckingFuture = CompletableFuture.completedFuture(null);

    private AtomicBoolean myCheckingState = new AtomicBoolean();

    public ExternalStorageManager(
        @Nonnull Application application,
        @Nonnull IComponentStore applicationStore,
        @Nonnull ExternalStorage storage,
        @Nonnull ExternalStoragePluginManager pluginManager
    ) {
        myApplicationStore = applicationStore;
        myPluginManager = pluginManager;
        myApplication = (ApplicationEx) application;
        myStorage = storage;
        application.getMessageBus().connect().subscribe(ExternalServiceConfigurationListener.class, this::configurationChanged);
    }

    public void startChecking() {
        boolean inSandbox = ApplicationProperties.isInSandbox();
        int time = inSandbox ? 1 : 10;

        myCheckingFuture = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            () -> {
                if (!myCheckingState.compareAndSet(false, true)) {
                    return;
                }

                Task.Backgroundable.queue(null, LocalizeValue.localizeTODO("Checking external storage..."), this::checkForModifications);
            },
            time,
            time,
            TimeUnit.MINUTES
        );
    }

    private void checkForModifications(@Nonnull ProgressIndicator indicator) {
        try {
            boolean wantRestart = myPluginManager.updatePlugins(indicator);

            indicator.setTextValue(LocalizeValue.localizeTODO("Checking external storage for modifications..."));

            InfoAllBeanResponse response = WebServiceApiSender.doGet(WebServiceApi.STORAGE_API, "infoAll", InfoAllBeanResponse.class);

            assert response != null;

            Map<String, Long> localModsCount = myStorage.getModificationInfo();

            Set<String> reloadComponentNames = new LinkedHashSet<>();

            Set<String> fetchNewFileSpecs = new LinkedHashSet<>();

            for (Map.Entry<String, Long> serverEntry : response.files.entrySet()) {
                String serverFullFileSpec = serverEntry.getKey();
                Long serverModCount = serverEntry.getValue();

                Long localModCount = localModsCount.remove(serverFullFileSpec);

                if (localModCount == null) {
                    // register file spec for fetching
                    fetchNewFileSpecs.add(serverFullFileSpec);
                }
                else if (!Objects.equals(serverModCount, localModCount)) {
                    // register file spec for fetching
                    fetchNewFileSpecs.add(serverFullFileSpec);

                    // load component names for reloading
                    try (InputStream stream = myStorage.loadContent(serverFullFileSpec)) {
                        Set<String> removeStates = ExternalStorage.readComponentNames(stream);

                        reloadComponentNames.addAll(removeStates);
                    }
                    catch (Exception e) {
                        LOG.warn(e);
                    }
                }
            }

            for (Map.Entry<String, Long> localEntry : localModsCount.entrySet()) {
                String fullFileSpec = localEntry.getKey();

                try (InputStream stream = myStorage.loadContent(fullFileSpec)) {
                    Set<String> removeStates = ExternalStorage.readComponentNames(stream);

                    reloadComponentNames.addAll(removeStates);

                    myStorage.deleteWithoutServer(fullFileSpec);
                }
                catch (Exception e) {
                    LOG.warn(e);
                }
            }

            if (!reloadComponentNames.isEmpty() || !fetchNewFileSpecs.isEmpty()) {
                runRefresher(fetchNewFileSpecs, reloadComponentNames, indicator);
            }

            if (wantRestart) {
                myApplication.invokeLater(
                    () -> {
                        if (PluginInstallUtil.showRestartIDEADialog() == Messages.YES) {
                            myApplication.restart(true);
                        }
                    },
                    ModalityState.nonModal()
                );
            }
        }
        catch (Exception e) {
            LOG.warn(e);
        }
        finally {
            myCheckingState.set(false);
        }
    }

    private void runRefresher(Set<String> fetchNewFileSpecs, Set<String> reloadComponentNames, ProgressIndicator indicator) {
        indicator.setTextValue(LocalizeValue.localizeTODO("Refreshing from external storage..."));

        try (AccessToken unused = myApplication.startSaveBlock()) {
            // fist of all we need download changed or new files
            for (String fullFileSpec : fetchNewFileSpecs) {
                try {
                    byte[] compressed =
                        WebServiceApiSender.doGetBytes(WebServiceApi.STORAGE_API, "getFile", Map.of("filePath", fullFileSpec));

                    Pair<byte[], Integer> uncompressedData = DataCompressor.uncompress(new UnsyncByteArrayInputStream(compressed));

                    try (InputStream stream = new UnsyncByteArrayInputStream(uncompressedData.getFirst())) {
                        reloadComponentNames.addAll(ExternalStorage.readComponentNames(stream));
                    }

                    LOG.info("Reloading data: " + fullFileSpec);

                    myStorage.writeLocalFile(fullFileSpec, uncompressedData.getFirst(), uncompressedData.getSecond());
                }
                catch (Exception e) {
                    LOG.warn(e);
                }
            }

            LOG.info("Reloading components: " + reloadComponentNames);

            AppUIExecutor.onWriteThread(ModalityState.nonModal()).later().execute(() -> {
                myApplicationStore.reinitComponents(reloadComponentNames, true);

                myApplication.invokeLater(() -> {
                    Project project = null;
                    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
                    for (Project openProject : openProjects) {
                        IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(openProject);
                        if (ideFrame.isActive()) {
                            project = openProject;
                            break;
                        }
                    }

                    NotificationService.getInstance()
                        .newInfo(ExternalStorageManager.GROUP)
                        .title(LocalizeValue.localizeTODO("External Storage"))
                        .content(LocalizeValue.localizeTODO("Local configuration refreshed"))
                        .notify(project);
                });
            });
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }

    private void configurationChanged(@Nonnull ExternalServiceConfiguration configuration) {
        ThreeState state = configuration.getState(ExternalService.STORAGE);
        switch (state) {
            case YES ->
                Task.Backgroundable.queue(null, LocalizeValue.localizeTODO("Initialing external storage..."), this::initialize);
            case NO -> {
                myCheckingFuture.cancel(false);
                myStorage.setInitialized(false);
            }
        }
    }

    private void initialize(ProgressIndicator indicator) {
        try {
            // unzip all data
            byte[] bytes = WebServiceApiSender.doGetBytes(WebServiceApi.STORAGE_API, "getAll", Map.of());

            assert bytes != null;

            myStorage.wipe();

            try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
                ZipEntry entry;

                while ((entry = zipInputStream.getNextEntry()) != null) {
                    String fileName = entry.getName();

                    try (OutputStream fileStream = myStorage.openFile(fileName)) {
                        StreamUtil.copyStreamContent(zipInputStream, fileStream);
                    }
                }
            }

            // there not check for restart - restart will be anyway
            myPluginManager.updatePlugins(indicator);

            // add action for restart
            StartupActionScriptManager.addActionCommand(new StartupActionScriptManager.CreateFileCommand(myStorage.getInitializedFile()));

            myApplication.invokeLater(this::showRestartDialog, ModalityState.nonModal());
        }
        catch (NoContentException ignored) {
            // there no content on server. it will throw at doGetBytes. In this case we need initialize all data

            try {
                myStorage.wipe();
            }
            catch (IOException ignored2) {
            }

            myStorage.setInitialized(true);

            StoreUtil.save(myApplicationStore, true, null);

            // if there plugins change - require restart
            if (myPluginManager.updatePlugins(indicator)) {
                myApplication.invokeLater(this::showRestartDialog, ModalityState.nonModal());
            }
        }
        catch (IOException e) {
            LOG.warn(e);
        }
    }

    @RequiredUIAccess
    private void showRestartDialog() {
        if (Messages.showYesNoDialog(
            "Restart required for apply External Storage settings change. Restart now?",
            myApplication.getName().get(),
            UIUtil.getInformationIcon()
        ) == Messages.YES) {
            myApplication.restart(true);
        }
    }
}
