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
package consulo.externalStorage;

import com.intellij.ide.plugins.PluginInstallUtil;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import consulo.application.ApplicationProperties;
import consulo.components.impl.stores.IApplicationStore;
import consulo.components.impl.stores.StoreUtil;
import consulo.externalService.ExternalService;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.externalService.ExternalServiceConfigurationListener;
import consulo.externalService.NoContentException;
import consulo.externalService.impl.WebServiceApi;
import consulo.externalService.impl.WebServiceApiSender;
import consulo.externalStorage.storage.DataCompressor;
import consulo.externalStorage.storage.ExternalStorage;
import consulo.externalStorage.storage.InfoAllBeanResponse;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.lang.ThreeState;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author VISTALL
 * @since 11/09/2021
 */
public class ExternalStorageManager {
  private static final Logger LOG = Logger.getInstance(ExternalStorageManager.class);

  @Nonnull
  private final ApplicationEx myApplication;
  @Nonnull
  private final ExternalStorage myStorage;
  @Nonnull
  private final IApplicationStore myApplicationStore;
  @Nonnull
  private final ExternalStoragePluginManager myPluginManager;

  private Future<?> myCheckingFuture = CompletableFuture.completedFuture(null);

  private AtomicBoolean myCheckingState = new AtomicBoolean();

  public ExternalStorageManager(@Nonnull Application application, @Nonnull IApplicationStore applicationStore, @Nonnull ExternalStorage storage, @Nonnull ExternalStoragePluginManager pluginManager) {
    myApplicationStore = applicationStore;
    myPluginManager = pluginManager;
    myApplication = (ApplicationEx)application;
    myStorage = storage;
    application.getMessageBus().connect().subscribe(ExternalServiceConfigurationListener.TOPIC, this::configurationChanged);
  }

  public void startChecking() {
    boolean inSandbox = ApplicationProperties.isInSandbox();
    int time = inSandbox ? 1 : 10;
    
    myCheckingFuture = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
      if(!myCheckingState.compareAndSet(false, true)) {
        return;
      }
      
      Task.Backgroundable.queue(null, "Checking external storage...", this::checkForModifications);
    }, time, time, TimeUnit.MINUTES);
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

      if(wantRestart) {
        myApplication.invokeLater(() -> {
          if (PluginInstallUtil.showRestartIDEADialog() == Messages.YES) {
            Application.get().restart(true);
          }
        }, ModalityState.NON_MODAL);
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
          byte[] compressed = WebServiceApiSender.doGetBytes(WebServiceApi.STORAGE_API, "getFile", Map.of("filePath", fullFileSpec));

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

      AppUIExecutor.onWriteThread(ModalityState.NON_MODAL).later().execute(() -> {
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

          new Notification("externalStorage", "External Storage", "Local configuration refreshed", NotificationType.INFORMATION).notify(project);
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
      case YES:
        Task.Backgroundable.queue(null, "Initialing external storage...", this::initialize);
        break;
      case NO:
        myCheckingFuture.cancel(false);
        myStorage.setInitialized(false);
        break;
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

      myApplication.invokeLater(this::showRestartDialog, ModalityState.NON_MODAL);
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
      if(myPluginManager.updatePlugins(indicator)) {
        myApplication.invokeLater(this::showRestartDialog, ModalityState.NON_MODAL);
      }
    }
    catch (IOException e) {
      LOG.warn(e);
    }
  }

  private void showRestartDialog() {
    if (Messages.showYesNoDialog("Restart required for apply External Storage settings change. Restart now?", "Consulo", Messages.getInformationIcon()) == Messages.YES) {
      myApplication.restart(true);
    }
  }
}
